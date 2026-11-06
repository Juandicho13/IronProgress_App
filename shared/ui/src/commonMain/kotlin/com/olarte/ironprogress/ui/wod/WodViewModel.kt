package com.olarte.ironprogress.ui.wod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.time.TimerEdit
import com.olarte.ironprogress.domain.time.TimerState
import com.olarte.ironprogress.domain.time.stateAt
import com.olarte.ironprogress.domain.voice.VoiceLine
import com.olarte.ironprogress.domain.voice.VoiceSettings
import com.olarte.ironprogress.domain.voice.voiceLinesBetween
import com.olarte.ironprogress.domain.wod.FindWodBest
import com.olarte.ironprogress.domain.wod.SaveWodResult
import com.olarte.ironprogress.domain.wod.Wod
import com.olarte.ironprogress.domain.wod.WodMovement
import com.olarte.ironprogress.domain.wod.WodScore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

enum class WodPhase { SETUP, RUNNING, FINISHED }

data class WodState(
    val phase: WodPhase = WodPhase.SETUP,
    val name: String = "",
    val format: BlockFormat = BlockFormat.AMRAP,
    val minutes: Int? = 12,
    val movements: List<WodMovement> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val timer: TimerState? = null,
    val score: WodScore = WodScore(),
    val best: BlockResult? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val failed: Boolean = false,
) {
    val wod: Wod?
        get() = runCatching { Wod(name.ifBlank { "WOD" }, format, minutes, movements) }.getOrNull()

    val canStart: Boolean get() = wod != null && phase == WodPhase.SETUP

    val elapsedSeconds: Int get() = ((timer?.totalElapsedMs ?: 0L) / 1000).toInt()

    fun exerciseName(id: ExerciseId, languageCode: String): String =
        exercises.firstOrNull { it.id == id }?.name?.resolve(languageCode) ?: id.value
}

sealed interface WodIntent {
    data class Name(val value: String) : WodIntent
    data class Format(val value: BlockFormat) : WodIntent
    data class Minutes(val delta: Int) : WodIntent
    data class AddMovement(val id: ExerciseId) : WodIntent
    data class RemoveMovement(val index: Int) : WodIntent
    data class Reps(val index: Int, val delta: Int) : WodIntent
    data class Load(val index: Int, val value: String) : WodIntent
    data object Start : WodIntent
    data object Tick : WodIntent
    data object AddRound : WodIntent
    data object TogglePause : WodIntent
    data object Finish : WodIntent
    data object Save : WodIntent
}

/**
 * Bloque funcional (PANTALLAS 7.4). Se arma (formato, minutos y movimientos), se corre con el
 * motor de tiempo y al acabar se guarda con su resultado. La voz cuenta los últimos 5 s.
 */
class WodViewModel(
    private val catalog: ExerciseRepository,
    private val findBest: FindWodBest,
    private val save: SaveWodResult,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WodState())
    val state = mutableState.asStateFlow()
    private val voice = Channel<List<VoiceLine>>(Channel.BUFFERED)
    val voiceLines = voice.receiveAsFlow()
    private val edits = mutableListOf<TimerEdit>()
    private var startedAt: Instant? = null
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val exercises = catalog.observeExercises().first().filter { !it.isArchived }
            mutableState.update { it.copy(exercises = exercises) }
        }
    }

    fun onIntent(intent: WodIntent) {
        val current = state.value
        when (intent) {
            is WodIntent.Name -> setup { it.copy(name = intent.value.take(MAX_NAME)) }
            is WodIntent.Format -> setup {
                val minutes = when (intent.value) {
                    BlockFormat.AMRAP, BlockFormat.EMOM -> it.minutes ?: DEFAULT_MINUTES
                    BlockFormat.FOR_TIME -> it.minutes
                    else -> null
                }
                it.copy(format = intent.value, minutes = minutes)
            }
            is WodIntent.Minutes -> setup {
                val next = (it.minutes ?: 0) + intent.delta
                val optional = it.format == BlockFormat.FOR_TIME
                it.copy(minutes = if (optional && next <= 0) null else next.coerceIn(1, Wod.MAX_MINUTES))
            }
            is WodIntent.AddMovement -> setup { it.copy(movements = it.movements + WodMovement(intent.id, DEFAULT_REPS)) }
            is WodIntent.RemoveMovement -> setup { it.copy(movements = it.movements.filterIndexed { i, _ -> i != intent.index }) }
            is WodIntent.Reps -> setup { state ->
                state.copy(
                    movements = state.movements.mapIndexed { i, m ->
                        if (i == intent.index) m.copy(reps = (m.reps + intent.delta).coerceIn(1, MAX_REPS)) else m
                    },
                )
            }
            is WodIntent.Load -> setup { state ->
                val load = intent.value.ifBlank { null }
                state.copy(movements = state.movements.mapIndexed { i, m -> if (i == intent.index) m.copy(load = load) else m })
            }
            WodIntent.Start -> current.wod?.takeIf { current.canStart }?.let(::start)
            WodIntent.Tick -> running { it.copy(score = it.score.tick(it.wod!!)) }
            WodIntent.AddRound -> running { it.copy(score = it.score.addRound()) }
            WodIntent.TogglePause -> if (current.phase == WodPhase.RUNNING) {
                val now = clock.now().toEpochMilliseconds()
                edits += if (current.timer?.isPaused == true) TimerEdit.Resume(now) else TimerEdit.Pause(now)
            }
            WodIntent.Finish -> if (current.phase == WodPhase.RUNNING) edits += TimerEdit.Skip(clock.now().toEpochMilliseconds())
            WodIntent.Save -> saveResult()
        }
    }

    private fun setup(change: (WodState) -> WodState) {
        if (state.value.phase == WodPhase.SETUP) mutableState.update(change)
    }

    private fun running(change: (WodState) -> WodState) {
        if (state.value.phase == WodPhase.RUNNING) mutableState.update(change)
    }

    private fun start(wod: Wod) {
        val plan = wod.toPlan()
        val begin = clock.now()
        startedAt = begin
        val startMs = begin.toEpochMilliseconds()
        mutableState.update { it.copy(phase = WodPhase.RUNNING, name = wod.name) }
        viewModelScope.launch {
            val best = findBest(wod)
            mutableState.update { it.copy(best = best) }
        }
        var previous = startMs - 1
        ticker = viewModelScope.launch {
            while (isActive) {
                val now = clock.now().toEpochMilliseconds()
                val timer = plan.stateAt(now, startMs, edits.toList())
                val lines = plan.voiceLinesBetween(previous, now, startMs, edits.toList(), VOICE)
                previous = now
                if (lines.isNotEmpty()) voice.trySend(lines)
                mutableState.update { it.copy(timer = timer) }
                if (timer.isFinished) {
                    mutableState.update { it.copy(phase = WodPhase.FINISHED) }
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private fun saveResult() {
        val current = state.value
        val wod = current.wod ?: return
        val started = startedAt ?: return
        if (current.phase != WodPhase.FINISHED || current.saving || current.saved) return
        mutableState.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                save(wod, started, current.score, current.elapsedSeconds)
                mutableState.update { it.copy(saving = false, saved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    private companion object {
        const val TICK_MS = 100L
        const val DEFAULT_MINUTES = 12
        const val DEFAULT_REPS = 10
        const val MAX_REPS = 200
        const val MAX_NAME = 40

        /** Solo la cuenta atrás de los últimos 5 s y el fin. */
        val VOICE = VoiceSettings(saysWhatsNext = false)
    }
}
