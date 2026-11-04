package com.olarte.ironprogress.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.NextRoutine
import com.olarte.ironprogress.domain.routine.ObserveNextRoutine
import com.olarte.ironprogress.domain.routine.buildWeekProgress
import com.olarte.ironprogress.domain.routine.estimatedDurationMinutes
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.StartSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** ViewModel MVI de la pantalla Hoy conectada a la rotación real de rutinas y sesiones. */
class TodayViewModel(
    private val observeNextRoutine: ObserveNextRoutine,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository,
    private val startSession: StartSession,
    private val clock: Clock,
    private val epochDay: (Long) -> Long = ::localEpochDay,
) : ViewModel() {

    private val mutableState = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = mutableState.asStateFlow()

    private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = effectChannel.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        observe()
    }

    private fun observe() {
        val now = clock.now()
        mutableState.update {
            it.copy(
                loading = true,
                failed = false,
                todayEpochMs = now.toEpochMilliseconds(),
            )
        }
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            try {
                combine(
                    observeNextRoutine(),
                    exerciseRepository.observeExercises(),
                    sessionRepository.observeFinishedSessions(),
                    sessionRepository.observeActiveSession(),
                ) { nextRoutine, catalog, finishedSessions, active ->
                    val currentNow = clock.now()
                    val catalogById = catalog.associateBy { it.id }
                    TodayState(
                        loading = false,
                        starting = mutableState.value.starting,
                        failed = false,
                        todayEpochMs = currentNow.toEpochMilliseconds(),
                        weekProgress = buildWeekProgress(finishedSessions, currentNow) { epochDay(it.toEpochMilliseconds()) },
                        nextRoutine = nextRoutine?.toCard(catalogById),
                        activeSessionId = active?.session?.id?.value,
                    )
                }.catch {
                    mutableState.update { it.copy(loading = false, failed = true) }
                }.collect { newState ->
                    mutableState.value = newState
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun onIntent(intent: TodayIntent) {
        when (intent) {
            TodayIntent.Refresh -> if (!state.value.starting) observe()
            TodayIntent.Retry -> {
                if (state.value.loading || observationJob?.isActive != true) {
                    observe()
                } else {
                    startNextWorkout()
                }
            }
            TodayIntent.StartWorkout -> startNextWorkout()
        }
    }

    private fun startNextWorkout() {
        val current = state.value
        if (current.starting || current.loading) return
        current.activeSessionId?.let {
            effectChannel.trySend(TodayEffect.OpenWorkout(it))
            return
        }
        val next = current.nextRoutine ?: return
        if (next.exercises.none { it.workingSetsCount > 0 }) return
        mutableState.update { it.copy(starting = true, failed = false) }
        viewModelScope.launch {
            try {
                val session = startSession(
                    routine = next.detail,
                    discipline = next.primaryDiscipline,
                )
                mutableState.update { it.copy(starting = false) }
                effectChannel.send(TodayEffect.OpenWorkout(session.session.id.value))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(starting = false, failed = true) }
            }
        }
    }

    private fun NextRoutine.toCard(catalogById: Map<com.olarte.ironprogress.domain.catalog.ExerciseId, Exercise>): TodayRoutineCard {
        val orderedExercises = detail.blocks
            .sortedBy { it.sortOrder }
            .flatMap { block -> detail.exercisesOf(block.id) }

        val rows = orderedExercises.map { routineExercise ->
            val catalogItem = catalogById[routineExercise.exerciseId]
            val allSets = detail.setsOf(routineExercise.id)
            val workingSets = allSets.filter { it.setType.countsAsWorking }
            val firstTemplate = workingSets.firstOrNull()
            TodayExerciseRow(
                id = routineExercise.id,
                exerciseId = routineExercise.exerciseId,
                name = catalogItem?.name,
                fallbackName = routineExercise.exerciseId.value,
                workingSetsCount = workingSets.size,
                repsMin = firstTemplate?.repsMin,
                repsMax = firstTemplate?.repsMax,
                targetRir = firstTemplate?.targetRir,
            )
        }

        val primaryDiscipline = orderedExercises
            .firstNotNullOfOrNull { catalogById[it.exerciseId]?.discipline }
            ?: Discipline.STRENGTH

        return TodayRoutineCard(
            detail = detail,
            folderName = folder?.name,
            positionInRotation = positionInRotation,
            rotationSize = rotationSize,
            isActiveSession = isActiveSession,
            estimatedMinutes = detail.estimatedDurationMinutes(),
            primaryDiscipline = primaryDiscipline,
            exercises = rows,
        )
    }
}
