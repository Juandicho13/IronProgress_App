package com.olarte.ironprogress.ui.discipline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.discipline.BreathingPattern
import com.olarte.ironprogress.domain.discipline.GuidedFlow
import com.olarte.ironprogress.domain.discipline.GuidedFlows
import com.olarte.ironprogress.domain.discipline.SaveGuidedSession
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.TimerEdit
import com.olarte.ironprogress.domain.time.TimerEvent
import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.TimerState
import com.olarte.ironprogress.domain.time.eventsBetween
import com.olarte.ironprogress.domain.time.stateAt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

/** Un cambio de tramo: la pantalla lo dice con la voz y lo marca con una vibración. */
data class GuidedCue(val kind: SegmentKind, val label: String?, val round: Int)

data class GuidedRunState(
    val started: Boolean = false,
    val timer: TimerState? = null,
    val muted: Boolean = false,
    val saved: Boolean = false,
    val failed: Boolean = false,
) {
    val paused: Boolean get() = timer?.isPaused == true
    val finished: Boolean get() = timer?.isFinished == true || saved
}

sealed interface GuidedIntent {
    data object Start : GuidedIntent
    data object TogglePause : GuidedIntent
    data object Skip : GuidedIntent

    /** Terminar antes: guarda lo hecho hasta ahora. */
    data object Finish : GuidedIntent
    data object ToggleMute : GuidedIntent
}

/**
 * Reloj de Flujo y Respiración (F1-20). El tiempo sale del motor (stateAt) en cada consulta y los
 * cambios de tramo de eventsBetween; la consulta cada 100 ms solo refresca la pantalla.
 */
abstract class GuidedRunViewModel(private val save: SaveGuidedSession, private val clock: Clock) : ViewModel() {
    protected val mutableState = MutableStateFlow(GuidedRunState())
    val state: StateFlow<GuidedRunState> = mutableState.asStateFlow()
    private val cueChannel = Channel<GuidedCue>(Channel.BUFFERED)
    val cues: Flow<GuidedCue> = cueChannel.receiveAsFlow()
    private val edits = mutableListOf<TimerEdit>()
    private var startedAt: Instant? = null
    private var ticker: Job? = null

    protected abstract fun plan(): TimerPlan
    protected abstract val discipline: Discipline
    protected abstract val sessionName: String

    open fun onIntent(intent: GuidedIntent) {
        val now = clock.now().toEpochMilliseconds()
        when (intent) {
            GuidedIntent.Start -> start()
            GuidedIntent.TogglePause -> if (state.value.started && !state.value.finished) {
                edits += if (state.value.paused) TimerEdit.Resume(now) else TimerEdit.Pause(now)
            }
            GuidedIntent.Skip -> if (state.value.started && !state.value.finished) edits += TimerEdit.Skip(now)
            GuidedIntent.Finish -> if (state.value.started && !state.value.finished) {
                ticker?.cancel()
                viewModelScope.launch { finish() }
            }
            GuidedIntent.ToggleMute -> mutableState.update { it.copy(muted = !it.muted) }
        }
    }

    private fun start() {
        if (state.value.started) return
        val plan = plan()
        val begin = clock.now()
        startedAt = begin
        val startMs = begin.toEpochMilliseconds()
        val first = plan.segments.first()
        mutableState.update { it.copy(started = true, timer = plan.stateAt(startMs, startMs)) }
        cue(GuidedCue(first.kind, first.label, first.round))
        var previous = startMs
        var lastCued = 0
        ticker = viewModelScope.launch {
            while (isActive) {
                val now = clock.now().toEpochMilliseconds()
                val snapshot = edits.toList()
                val timer = plan.stateAt(now, startMs, snapshot)
                // Los tramos que empezaron salen de eventsBetween. Un salto con Siguiente cambia el
                // tramo sin que pase el tiempo, por eso también se mira el índice del estado.
                val started = if (timer.isFinished) {
                    emptyList()
                } else {
                    plan.eventsBetween(previous, now, startMs, snapshot).filterIsInstance<TimerEvent.SegmentStarted>().map { it.index }
                }
                val reached = if (timer.isFinished) lastCued else timer.segmentIndex
                (started + (lastCued + 1..reached)).filter { it > lastCued && it <= plan.segments.lastIndex }.distinct().sorted()
                    .forEach { index ->
                        val segment = plan.segments[index]
                        cue(GuidedCue(segment.kind, segment.label, segment.round))
                        lastCued = index
                    }
                previous = now
                mutableState.update { it.copy(timer = timer) }
                if (timer.isFinished) {
                    finish()
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    /** La vibración sale siempre; con la voz apagada la pantalla no habla. */
    private fun cue(cue: GuidedCue) {
        cueChannel.trySend(cue)
    }

    private suspend fun finish() {
        val started = startedAt ?: return
        if (state.value.saved) return
        val active = ((state.value.timer?.totalElapsedMs ?: 0L) / 1000).toInt()
        try {
            save(discipline, sessionName, started, active)
            mutableState.update { it.copy(saved = true) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            mutableState.update { it.copy(failed = true) }
        }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}

/** Flujo de yoga, pilates o movilidad (PANTALLAS 7.2). Empieza solo al abrirse. */
class FlowViewModel(save: SaveGuidedSession, clock: Clock, flowId: String, languageCode: String) : GuidedRunViewModel(save, clock) {
    val flow: GuidedFlow = GuidedFlows.find(flowId) ?: GuidedFlows.SUN_SALUTATION_A
    override val discipline: Discipline = flow.discipline
    override val sessionName: String = flow.name.resolve(languageCode)

    override fun plan(): TimerPlan = flow.toPlan()

    init {
        onIntent(GuidedIntent.Start)
    }
}

data class BreathingChoice(val pattern: BreathingPattern = BreathingPattern.BOX, val minutes: Int = BreathingPattern.DEFAULT_MINUTES)

/** Respiración guiada (PANTALLAS 7.3). Se elige el patrón y empieza con Empezar. */
class BreathingViewModel(save: SaveGuidedSession, clock: Clock, private val sessionLabel: String) : GuidedRunViewModel(save, clock) {
    private val mutableChoice = MutableStateFlow(BreathingChoice())
    val choice: StateFlow<BreathingChoice> = mutableChoice.asStateFlow()
    override val discipline: Discipline = Discipline.BREATHING
    override val sessionName: String get() = sessionLabel

    override fun plan(): TimerPlan = choice.value.let { it.pattern.toPlan(it.minutes) }

    fun choose(pattern: BreathingPattern) {
        if (!state.value.started) mutableChoice.update { it.copy(pattern = pattern) }
    }

    fun changeMinutes(delta: Int) {
        if (!state.value.started) mutableChoice.update { it.copy(minutes = (it.minutes + delta).coerceIn(1, MAX_MINUTES)) }
    }

    private companion object {
        const val MAX_MINUTES = 30
    }
}
