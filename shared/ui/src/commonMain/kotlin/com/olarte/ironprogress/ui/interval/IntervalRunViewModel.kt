package com.olarte.ironprogress.ui.interval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.IntervalTemplateRepository
import com.olarte.ironprogress.domain.interval.SaveIntervalResult
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.TimerEdit
import com.olarte.ironprogress.domain.time.TimerState
import com.olarte.ironprogress.domain.time.stateAt
import com.olarte.ironprogress.domain.voice.VoiceLine
import com.olarte.ironprogress.domain.voice.voiceLinesBetween
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class IntervalRunState(
    val template: IntervalTemplate? = null,
    val names: Map<String, String> = emptyMap(),
    val timer: TimerState? = null,
    /** Reps anotadas por ronda; null si esa ronda todavía no se anotó. */
    val reps: List<Int?> = emptyList(),
    /** La última vez de cada ejercicio (id a reps), para los chips y "supéralos". */
    val lastTime: Map<String, Int> = emptyMap(),
    val muted: Boolean = false,
    val saved: Boolean = false,
    val failed: Boolean = false,
    val notFound: Boolean = false,
) {
    val finished: Boolean get() = timer?.isFinished == true

    /** La ronda que se anota en el descanso: la de trabajo que acaba de terminar. */
    val roundToLog: Int? get() = timer?.takeIf { it.kind == SegmentKind.REST && !it.isFinished }?.round

    /** Lo que se guarda si no se elige nada: la ronda anterior del mismo ejercicio o la última vez. */
    fun guessFor(round: Int): Int {
        val template = template ?: return 0
        val exercise = template.exerciseFor(round)?.value
        val earlier = (round - 1 downTo 1).firstOrNull { template.exerciseFor(it)?.value == exercise && reps.getOrNull(it - 1) != null }
        return earlier?.let { reps[it - 1] } ?: exercise?.let { lastTime[it] } ?: DEFAULT_GUESS
    }

    private companion object {
        const val DEFAULT_GUESS = 10
    }
}

sealed interface IntervalRunIntent {
    data object TogglePause : IntervalRunIntent
    data object Skip : IntervalRunIntent
    data object ToggleMute : IntervalRunIntent
    data class LogReps(val round: Int, val reps: Int) : IntervalRunIntent
}

/**
 * Corre unos intervalos (PANTALLAS 5.3). El tiempo sale del motor (stateAt) en cada consulta;
 * la consulta cada 100 ms solo refresca la pantalla, nunca cuenta segundos. La voz sale de
 * voiceLinesBetween entre una consulta y la siguiente.
 */
class IntervalRunViewModel(
    private val templates: IntervalTemplateRepository,
    private val catalog: ExerciseRepository,
    private val sessions: SessionRepository,
    private val saveResult: SaveIntervalResult,
    private val clock: Clock,
    private val templateId: String,
    private val languageCode: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(IntervalRunState())
    val state = mutableState.asStateFlow()
    private val voice = Channel<List<VoiceLine>>(Channel.BUFFERED)
    val voiceLines = voice.receiveAsFlow()
    private val edits = mutableListOf<TimerEdit>()
    private var startedAt: Instant? = null
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val template = templates.findTemplate(IntervalTemplateId(templateId))
            if (template == null) {
                mutableState.update { it.copy(notFound = true) }
                return@launch
            }
            val names = template.exerciseIds.distinct().associate { id ->
                id.value to (catalog.findExercise(id)?.name?.resolve(languageCode) ?: id.value)
            }
            val lastTime = template.exerciseIds.distinct().mapNotNull { id ->
                sessions.previousSets(id).lastOrNull { it.reps != null }?.reps?.let { id.value to it }
            }.toMap()
            mutableState.update { it.copy(template = template, names = names, lastTime = lastTime, reps = List(template.rounds) { null }) }
            start(template)
        }
    }

    fun onIntent(intent: IntervalRunIntent) {
        val now = clock.now().toEpochMilliseconds()
        when (intent) {
            IntervalRunIntent.TogglePause -> {
                val paused = state.value.timer?.isPaused == true
                edits += if (paused) TimerEdit.Resume(now) else TimerEdit.Pause(now)
            }
            IntervalRunIntent.Skip -> edits += TimerEdit.Skip(now)
            IntervalRunIntent.ToggleMute -> mutableState.update { it.copy(muted = !it.muted) }
            is IntervalRunIntent.LogReps -> mutableState.update { state ->
                state.copy(reps = state.reps.toMutableList().also { if (intent.round in 1..it.size) it[intent.round - 1] = intent.reps })
            }
        }
    }

    private fun start(template: IntervalTemplate) {
        val plan = template.toPlan()
        val begin = clock.now()
        startedAt = begin
        val startMs = begin.toEpochMilliseconds()
        var previous = startMs - 1
        ticker = viewModelScope.launch {
            while (isActive) {
                val now = clock.now().toEpochMilliseconds()
                val timer = plan.stateAt(now, startMs, edits.toList())
                val muted = state.value.muted
                val lines = plan.voiceLinesBetween(previous, now, startMs, edits.toList(), template.voice)
                previous = now
                if (lines.isNotEmpty() && !muted) voice.trySend(lines)
                mutableState.update { it.copy(timer = timer) }
                if (timer.isFinished) {
                    finish(template)
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private suspend fun finish(template: IntervalTemplate) {
        val started = startedAt ?: return
        val current = state.value
        val reps = List(template.rounds) { index -> current.reps[index] ?: current.guessFor(index + 1) }
        mutableState.update { it.copy(reps = reps) }
        try {
            saveResult(template, started, reps)
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
