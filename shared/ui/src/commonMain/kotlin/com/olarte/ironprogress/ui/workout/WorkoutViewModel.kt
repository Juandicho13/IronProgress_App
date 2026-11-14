package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.progress.DetectRecords
import com.olarte.ironprogress.domain.progression.RepRange
import com.olarte.ironprogress.domain.progression.StagnationDetector
import com.olarte.ironprogress.domain.progression.carryWeightForward
import com.olarte.ironprogress.domain.progression.deload
import com.olarte.ironprogress.domain.progression.performances
import com.olarte.ironprogress.domain.progression.suggestionRecord
import com.olarte.ironprogress.domain.progression.workingRange
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.routine.values
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.EndSession
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.copyMeasurements
import com.olarte.ironprogress.domain.session.equivalentPreviousSet
import com.olarte.ironprogress.domain.session.fillMissingFrom
import com.olarte.ironprogress.domain.session.firstPendingInGroup
import com.olarte.ironprogress.domain.session.groupWithNext
import com.olarte.ironprogress.domain.session.hasRequiredMeasurements
import com.olarte.ironprogress.domain.session.moveExercise
import com.olarte.ironprogress.domain.session.nextInGroupWithoutRest
import com.olarte.ironprogress.domain.session.skipExercise
import com.olarte.ironprogress.domain.session.substitute
import com.olarte.ironprogress.domain.session.ungroup
import com.olarte.ironprogress.domain.session.warmupSets
import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.rest.RestNotifications
import com.olarte.ironprogress.ui.rest.RestPhase
import com.olarte.ironprogress.ui.rest.RestTimer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Una instancia vive junto a la navegación para que minimizar no cancele escrituras. */
class WorkoutViewModel(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val startSession: StartSession,
    private val completeSet: CompleteSet,
    private val addExercise: AddSessionExercise,
    private val ids: IdGenerator,
    private val clock: Clock,
    /** Aviso local del fin del descanso en iPhone (F1-15); en Android lo hace el servicio de F1-14. */
    private val restNotifications: RestNotifications = RestNotifications { },
    val restController: RestController = RestController(),
    /** Récords al completar una serie (F2-03); la Live Activity los tomará de aquí (F2-15). */
    private val detectRecords: DetectRecords = DetectRecords(sessions),
) : ViewModel() {
    private var notifiedDeadline: Long? = null

    /** Solo avisa cuando cambia la hora de fin, nunca en cada cuadro. */
    private fun notifyRestDeadline(timer: RestTimer?) {
        val deadline = timer?.endsAtMs(clock.now().toEpochMilliseconds())
        if (deadline == notifiedDeadline) return
        notifiedDeadline = deadline
        restNotifications.replace(deadline)
    }

    private val mutableState = MutableStateFlow(WorkoutState())
    val state = mutableState.asStateFlow()
    private val effectChannel = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var observation: Job? = null
    private var restObservation: Job? = null
    private var writer: Job? = null
    private val writes = ArrayDeque<suspend () -> Unit>()
    private val completing = mutableSetOf<WorkoutSetId>()
    private var undoJob: Job? = null
    private var highlightJob: Job? = null
    private var recordJob: Job? = null
    private var undoExpiresAt = 0L
    private var lastRestFrameMs = 0L

    /** Ejercicios ya revisados en busca de estancamiento en esta sesión. */
    private val analyzed = mutableSetOf<SessionExerciseId>()
    private var analyzedSession: SessionId? = null

    /** Series ya rellenadas con la última vez: cada una una sola vez, aunque el usuario la vacíe. */
    private val prefilled = mutableSetOf<WorkoutSetId>()

    fun refreshRest() {
        val rest = state.value.rest ?: return
        val now = clock.now().toEpochMilliseconds()
        val events = rest.events(lastRestFrameMs, now)
        lastRestFrameMs = now
        val timer = rest.at(now)
        if (timer.isFinished) {
            mutableState.update { it.copy(rest = null, restExpanded = false, restNowMs = now) }
            restController.onRestCountdownFinished(now)
            notifyRestDeadline(null)
            effectChannel.trySend(WorkoutEffect.RestFinished)
        } else {
            mutableState.update { it.copy(restNowMs = now) }
            if (events.any { it is com.olarte.ironprogress.domain.time.TimerEvent.SecondsLeft && it.seconds in 1..3 } &&
                now - rest.startedAtMs > 0
            ) {
                effectChannel.trySend(WorkoutEffect.RestTick)
            }
        }
    }

    /** Cada cuadro mientras corre una serie por tiempo; al llegar a 0 la guarda y la completa. */
    fun refreshWork() {
        val work = state.value.work ?: return
        val now = clock.now().toEpochMilliseconds()
        if (work.isFinished(now)) {
            mutableState.update { it.copy(work = null, workNowMs = now) }
            edit(work.setId) { it.copy(durationSeconds = work.seconds) }
            complete(work.setId)
        } else {
            mutableState.update { it.copy(workNowMs = now) }
        }
    }

    init {
        observe()
        observeSharedRest()
    }

    fun elapsedSeconds(): Long = state.value.session?.session?.let { (clock.now() - it.startedAt).inWholeSeconds.coerceAtLeast(0) } ?: 0

    private fun observeSharedRest() {
        restObservation?.cancel()
        restObservation = viewModelScope.launch {
            restController.state.collect { restState ->
                val now = clock.now().toEpochMilliseconds()
                val activeTimer = if (restState.phase == RestPhase.RESTING) {
                    restState.timer?.takeIf { !it.at(now).isFinished }
                } else {
                    null
                }
                val previousTimer = mutableState.value.rest
                if (activeTimer != previousTimer) {
                    lastRestFrameMs = now
                    val isNewRest = activeTimer != null && previousTimer?.setId != activeTimer.setId
                    mutableState.update { current ->
                        current.copy(
                            rest = activeTimer,
                            restNowMs = now,
                            restExpanded = when {
                                activeTimer == null -> false
                                isNewRest -> true
                                else -> current.restExpanded
                            },
                        )
                    }
                }
            }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(sessions.observeActiveSession(), exercises.observeExercises()) { detail, catalog ->
                    detail to catalog.associateBy { it.id }
                }.collectLatest { (detail, catalog) ->
                    val old = state.value
                    val sameSession = old.session?.session?.id == detail?.session?.id
                    if (detail == null) {
                        restController.clearRest()
                        notifyRestDeadline(null)
                    }
                    val previous = if (sameSession) old.previous.toMutableMap() else mutableMapOf()
                    detail?.exercises?.forEach {
                        if (it.exerciseId !in previous) previous[it.exerciseId] = sessions.previousSets(it.exerciseId)
                    }
                    val name = detail?.session?.routineId?.let { routines.findRoutine(it)?.routine?.name }
                    mutableState.update {
                        val selected = it.selectedExerciseId?.takeIf { id ->
                            detail?.exercises?.any { ex -> ex.id == id && detail.setsOf(ex.id).any { s -> !s.isCompleted } } == true
                        }
                            ?: detail?.exercises?.firstOrNull { item -> detail.setsOf(item.id).any { !it.isCompleted } }?.id
                            ?: it.selectedExerciseId?.takeIf { id -> detail?.exercises?.any { ex -> ex.id == id } == true }
                            ?: detail?.exercises?.firstOrNull()?.id
                        it.copy(
                            loading = false,
                            rest = it.rest.takeIf { sameSession },
                            restExpanded = it.restExpanded && sameSession,
                            session = detail,
                            catalog = catalog,
                            previous = previous,
                            routineName = name,
                            selectedExerciseId = selected,
                            keyboard = it.keyboard?.takeIf { keyboard -> detail?.sets?.any { set -> set.id == keyboard.setId } == true },
                            stagnation = if (sameSession) it.stagnation else emptyMap(),
                            stagnationDismissed = if (sameSession) it.stagnationDismissed else emptySet(),
                        )
                    }
                    if (detail != null) prefillFromLastTime(detail, previous)
                    if (detail != null) analyze(detail, catalog)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    /** Cola FIFO: cada tecla válida se escribe, sin debounce ni sobrescribir una edición posterior. */
    private fun enqueue(action: suspend () -> Unit) {
        writes.addLast(action)
        mutableState.update { it.copy(pendingWrites = writes.size) }
        drain()
    }

    private fun drain() {
        if (writer?.isActive == true || state.value.failed) return
        writer = viewModelScope.launch {
            while (writes.isNotEmpty()) {
                try {
                    writes.first().invoke()
                    writes.removeFirst()
                    mutableState.update { it.copy(pendingWrites = writes.size) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    mutableState.update { it.copy(failed = true) }
                    break
                }
            }
        }
    }

    private suspend fun active(): SessionDetail {
        val id = requireNotNull(state.value.session).session.id
        return requireNotNull(sessions.findSession(id)).also { require(it.session.status == SessionStatus.IN_PROGRESS) }
    }

    private fun edit(id: WorkoutSetId, transform: (WorkoutSet) -> WorkoutSet) = enqueue {
        val detail = active()
        val set = detail.sets.first { it.id == id }
        sessions.saveSet(transform(set))
    }

    fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            is WorkoutIntent.AdjustRest -> {
                refreshRest()
                val rest = state.value.rest ?: return
                val now = clock.now().toEpochMilliseconds()
                val adjusted = rest.adjust(now, intent.seconds)
                mutableState.update { it.copy(rest = adjusted) }
                restController.updateTimer(adjusted, now)
                notifyRestDeadline(adjusted)
                refreshRest()
            }
            WorkoutIntent.SkipRest -> {
                val now = clock.now().toEpochMilliseconds()
                mutableState.update { it.copy(rest = null, restExpanded = false) }
                restController.skipFromUi(now)
                notifyRestDeadline(null)
            }
            WorkoutIntent.ExpandRest -> {
                refreshRest()
                mutableState.update { it.copy(restExpanded = it.rest != null) }
            }
            WorkoutIntent.MinimizeRest -> mutableState.update { it.copy(restExpanded = false) }
            WorkoutIntent.StartFree -> {
                if (state.value.pendingWrites > 0) return
                enqueue {
                    val session = startSession()
                    effectChannel.send(WorkoutEffect.OpenWorkout(session.session.id.value))
                }
            }
            is WorkoutIntent.ToggleUnit -> {
                val exercise = state.value.session?.exercises?.firstOrNull { it.id == intent.id } ?: return
                val next = if (state.value.unitOf(intent.id) == WeightUnit.LB) null else WeightUnit.LB
                mutableState.update { it.copy(keyboard = null) }
                enqueue { exercises.setPreferredUnit(exercise.exerciseId, next) }
            }
            is WorkoutIntent.AddWarmup -> enqueue {
                val detail = active()
                val sets = detail.setsOf(intent.id)
                if (sets.any { it.setType == SetType.WARMUP }) return@enqueue
                val work = sets.firstOrNull { it.setType.countsAsWorking && it.weightKg != null }?.weightKg ?: return@enqueue
                val unit = state.value.unitOf(intent.id)
                val steps = warmupSets(
                    work,
                    barKg = WeightUnits.toKg(WeightUnits.defaultBar(unit), unit),
                    stepKg = WeightUnits.toKg(if (unit == WeightUnit.LB) 5.0 else 2.5, unit),
                )
                if (steps.isEmpty()) return@enqueue
                sets.forEach { sessions.saveSet(it.copy(sortOrder = it.sortOrder + steps.size)) }
                val first = sets.minOfOrNull { it.sortOrder } ?: 0
                steps.forEachIndexed { index, step ->
                    sessions.saveSet(
                        WorkoutSet(
                            WorkoutSetId(ids.newId()),
                            intent.id,
                            first + index,
                            SetType.WARMUP,
                            weightKg = step.weightKg,
                            reps = step.reps,
                        ),
                    )
                }
            }
            is WorkoutIntent.SetProMode -> mutableState.update { current ->
                val keyboard = current.keyboard?.takeIf { intent.on || it.field !in PRO_FIELDS }
                current.copy(proMode = intent.on, keyboard = keyboard)
            }
            WorkoutIntent.Cancel -> {
                if (state.value.session == null) return
                enqueue { discard() }
            }
            WorkoutIntent.Finish -> {
                if (state.value.pendingWrites > 0) return
                enqueue {
                    val detail = active()
                    // Sin ninguna serie hecha no hay entreno que guardar: se descarta sin preguntar.
                    if (detail.sets.none { it.isCompleted }) {
                        discard()
                        return@enqueue
                    }
                    EndSession(sessions, clock)(detail.session)
                    restController.clearRest()
                    notifyRestDeadline(null)
                    mutableState.update { it.copy(rest = null, restExpanded = false, keyboard = null, undoSetId = null) }
                    effectChannel.send(WorkoutEffect.Finished(detail.session.id.value))
                }
            }
            WorkoutIntent.Retry -> {
                mutableState.update { it.copy(failed = false) }
                observe()
                drain()
            }
            is WorkoutIntent.SelectExercise -> mutableState.update { it.copy(selectedExerciseId = intent.id) }
            WorkoutIntent.ChooseExercise -> mutableState.update { it.copy(choosingExercise = true, query = "", substitutingId = null) }
            is WorkoutIntent.Substitute -> mutableState.update { it.copy(choosingExercise = true, query = "", substitutingId = intent.id) }
            is WorkoutIntent.MoveExercise -> enqueue {
                active().moveExercise(intent.id, intent.delta).forEach { sessions.saveExercise(it) }
            }
            is WorkoutIntent.SkipExercise -> enqueue {
                val detail = active()
                detail.skipExercise(intent.id).forEach { sessions.saveExercise(it) }
                val next = detail.exercises.sortedBy { it.sortOrder }
                    .firstOrNull { item -> item.id != intent.id && detail.setsOf(item.id).any { !it.isCompleted } }
                mutableState.update { it.copy(selectedExerciseId = next?.id ?: intent.id) }
            }
            is WorkoutIntent.ToggleGroup -> enqueue {
                val detail = active()
                val inGroup = detail.exercises.firstOrNull { it.id == intent.id }?.supersetGroup != null
                (if (inGroup) detail.ungroup(intent.id) else detail.groupWithNext(intent.id)).forEach { sessions.saveExercise(it) }
            }
            is WorkoutIntent.RemoveSet -> enqueue { sessions.deleteSet(intent.id) }
            is WorkoutIntent.SetMode -> enqueue { setMode(intent) }
            is WorkoutIntent.StartWork -> {
                val detail = state.value.session ?: return
                val set = detail.sets.firstOrNull { it.id == intent.id && !it.isCompleted } ?: return
                val exercise = detail.exercises.firstOrNull { it.id == set.sessionExerciseId } ?: return
                val seconds = set.durationSeconds?.takeIf { it > 0 } ?: exercise.timedSeconds
                    ?: ExerciseMode.DEFAULT_TIMED_SECONDS
                val now = clock.now().toEpochMilliseconds()
                mutableState.update { it.copy(work = WorkTimer(set.id, now, seconds), workNowMs = now, keyboard = null) }
            }
            WorkoutIntent.StopWork -> mutableState.update { it.copy(work = null) }
            WorkoutIntent.ClosePicker -> mutableState.update { it.copy(choosingExercise = false) }
            is WorkoutIntent.Search -> mutableState.update { it.copy(query = intent.query) }
            is WorkoutIntent.AddExercise -> {
                val substituting = state.value.substitutingId
                mutableState.update { it.copy(choosingExercise = false, substitutingId = null) }
                if (substituting != null) {
                    enqueue {
                        val change = active().substitute(substituting, intent.id, ids) ?: return@enqueue
                        change.exercises.forEach { sessions.saveExercise(it) }
                        change.sets.forEach { sessions.saveSet(it) }
                        mutableState.update { it.copy(selectedExerciseId = change.selected) }
                    }
                    return
                }
                enqueue {
                    val id = addExercise(active().session.id, intent.id)
                    mutableState.update { it.copy(selectedExerciseId = id) }
                }
            }
            is WorkoutIntent.AddSet -> enqueue {
                val detail = active()
                val existing = detail.setsOf(intent.id)
                val next = WorkoutSet(WorkoutSetId(ids.newId()), intent.id, (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1)
                sessions.saveSet(existing.lastOrNull()?.let { next.copyMeasurements(it) } ?: next)
            }
            is WorkoutIntent.OpenKeyboard -> openKeyboard(intent.id, intent.field)
            WorkoutIntent.CloseKeyboard -> mutableState.update { it.copy(keyboard = null) }
            is WorkoutIntent.Key -> typeKey(intent.key)
            is WorkoutIntent.Adjust -> setKeyboardValue((state.value.keyboard?.text?.toDoubleOrNull() ?: 0.0) + intent.amount)
            is WorkoutIntent.SetValue -> setKeyboardValue(intent.value)
            WorkoutIntent.NextField -> {
                val keyboard = state.value.keyboard ?: return
                val fields = fieldsFor(keyboard.setId)
                openKeyboard(keyboard.setId, fields[(fields.indexOf(keyboard.field) + 1).mod(fields.size)])
            }
            is WorkoutIntent.CopyPrevious -> {
                val detail = state.value.session ?: return
                val set = detail.sets.firstOrNull { it.id == intent.id } ?: return
                val exercise = detail.exercises.first { it.id == set.sessionExerciseId }
                val previous =
                    equivalentPreviousSet(set, detail.setsOf(exercise.id), state.value.previous[exercise.exerciseId].orEmpty()) ?: return
                edit(set.id) { it.copyMeasurements(previous) }
            }
            is WorkoutIntent.Complete -> complete(intent.id)
            WorkoutIntent.Undo -> {
                val id = state.value.undoSetId ?: return
                if (clock.now().toEpochMilliseconds() > undoExpiresAt) return
                undoJob?.cancel()
                // Deshacer la serie también quita su récord: ya no cuenta.
                if (state.value.record?.setId == id) recordJob?.cancel()
                mutableState.update { it.copy(undoSetId = null, record = it.record?.takeIf { hit -> hit.setId != id }) }
                edit(id) { it.copy(completedAt = null) }
                if (state.value.rest?.setId == id) {
                    mutableState.update { it.copy(rest = null, restExpanded = false) }
                    restController.clearIfSet(id)
                    notifyRestDeadline(null)
                }
            }
            is WorkoutIntent.Deload -> enqueue {
                val change = deload(active().setsOf(intent.id))
                change.remove.forEach { sessions.deleteSet(it) }
                change.save.forEach { sessions.saveSet(it) }
                mutableState.update { it.copy(stagnation = it.stagnation - intent.id) }
            }
            is WorkoutIntent.ChangeRange -> enqueue { changeRange(intent.id) }
            is WorkoutIntent.ApplyLadder -> enqueue { applyLadder(intent.id, intent.inRoutine) }
            is WorkoutIntent.DismissStagnation -> mutableState.update { it.copy(stagnationDismissed = it.stagnationDismissed + intent.id) }
            is WorkoutIntent.ApplySuggestion -> edit(intent.id) { set ->
                val record = set.suggestionRecord ?: return@edit set
                set.copy(weightKg = record.weightKg ?: set.weightKg, reps = record.reps ?: set.reps)
            }
            WorkoutIntent.DismissRecord -> {
                recordJob?.cancel()
                mutableState.update { it.copy(record = null) }
            }
            is WorkoutIntent.ChangeType -> edit(intent.id) { it.copy(setType = intent.type) }
            is WorkoutIntent.ChangeNote -> edit(intent.id) { it.copy(notes = intent.note) }
        }
    }

    fun fieldsFor(id: WorkoutSetId): List<WorkoutField> {
        val pro = if (state.value.proMode) PRO_FIELDS else emptyList()
        val detail = state.value.session ?: return listOf(WorkoutField.WEIGHT, WorkoutField.REPS) + pro
        val set = detail.sets.firstOrNull { it.id == id }
        return state.value.measuresOf(set?.sessionExerciseId) + pro
    }

    /**
     * La primera serie pendiente de cada ejercicio, si está vacía, arranca con lo que se hizo la
     * última vez en esa misma serie: completar es un solo toque. Nunca pisa lo ya escrito.
     */
    private fun prefillFromLastTime(
        detail: SessionDetail,
        previous: Map<com.olarte.ironprogress.domain.catalog.ExerciseId, List<WorkoutSet>>,
    ) {
        detail.exercises.forEach { exercise ->
            val sets = detail.setsOf(exercise.id)
            val pending = sets.firstOrNull { !it.isCompleted } ?: return@forEach
            if (pending.id in prefilled) return@forEach
            prefilled += pending.id
            if (!pending.isBlank()) return@forEach
            val last = equivalentPreviousSet(pending, sets, previous[exercise.exerciseId].orEmpty()) ?: return@forEach
            enqueue {
                val current =
                    active().sets.firstOrNull { it.id == pending.id }?.takeIf { !it.isCompleted && it.isBlank() } ?: return@enqueue
                sessions.saveSet(current.fillMissingFrom(last))
            }
        }
    }

    /**
     * Busca estancamiento (F2-05) una sola vez por ejercicio de la sesión, con su historial y el
     * rango de la rutina. Las series por tiempo, los ejercicios sin reps y los que ya están en
     * descarga no se miran.
     */
    private suspend fun analyze(detail: SessionDetail, catalog: Map<ExerciseId, com.olarte.ironprogress.domain.catalog.Exercise>) {
        if (analyzedSession != detail.session.id) {
            analyzed.clear()
            analyzedSession = detail.session.id
        }
        val pending = detail.exercises.filter { it.id !in analyzed }
        if (pending.isEmpty()) return
        val routine = detail.session.routineId?.let { routines.findRoutine(it) }
        val found = mutableMapOf<SessionExerciseId, com.olarte.ironprogress.domain.progression.Stagnation>()
        pending.forEach { exercise ->
            analyzed += exercise.id
            val info = catalog[exercise.exerciseId] ?: return@forEach
            if (SetField.REPS !in info.trackingType.fields || exercise.mode == ExerciseMode.TIMED) return@forEach
            if (detail.setsOf(exercise.id).any { it.suggestionRecord?.deload == true }) return@forEach
            val planned = routine?.exercises?.firstOrNull { it.exerciseId == exercise.exerciseId }
            val range = planned?.let { routine.setsOf(it.id).workingRange() } ?: RepRange()
            val history = sessions.performances(exercise.exerciseId)
            StagnationDetector.detect(history, info.trackingType, range, canChangeRange = planned != null)?.let { found[exercise.id] = it }
        }
        if (found.isNotEmpty()) mutableState.update { it.copy(stagnation = it.stagnation + found) }
    }

    /** Cambia el rango de reps del ejercicio en la rutina y pone el nuevo mínimo en las series pendientes. */
    private suspend fun changeRange(id: SessionExerciseId) {
        val range = state.value.stagnation[id]?.newRange ?: return
        val detail = active()
        val exercise = detail.exercises.firstOrNull { it.id == id } ?: return
        val routine = detail.session.routineId?.let { routines.findRoutine(it) } ?: return
        val planned = routine.exercises.firstOrNull { it.exerciseId == exercise.exerciseId } ?: return
        routines.saveRoutine(
            routine.copy(
                setTemplates = routine.setTemplates.map { template ->
                    if (template.routineExerciseId == planned.id && template.setType.countsAsWorking) {
                        template.copy(repsMin = range.min, repsMax = range.max)
                    } else {
                        template
                    }
                },
            ),
        )
        detail.setsOf(id).filter { !it.isCompleted && it.setType.countsAsWorking }.forEach { set ->
            val record = set.suggestionRecord?.copy(reps = range.min, repsMin = range.min, repsMax = range.max)
            sessions.saveSet(set.copy(reps = range.min, suggestion = record?.encode() ?: set.suggestion))
        }
        mutableState.update { it.copy(stagnation = it.stagnation - id) }
    }

    /**
     * Cambia el ejercicio por la variante de la escalera que trae su serie pendiente (F2-12). Las
     * series pendientes pasan a la variante con el mínimo del rango; con [inRoutine] la rutina
     * también cambia.
     */
    private suspend fun applyLadder(id: SessionExerciseId, inRoutine: Boolean) {
        val detail = active()
        val exercise = detail.exercises.firstOrNull { it.id == id } ?: return
        val record = detail.setsOf(id).firstOrNull { !it.isCompleted }?.suggestionRecord ?: return
        val target = record.ladderTarget?.let(::ExerciseId) ?: return
        val change = detail.substitute(id, target, ids) ?: return
        change.exercises.forEach { sessions.saveExercise(it) }
        val startReps = record.repsMin ?: record.reps
        change.sets.forEach { set -> sessions.saveSet(if (set.setType.countsAsWorking) set.copy(reps = startReps) else set) }
        if (inRoutine) {
            detail.session.routineId?.let { routines.findRoutine(it) }?.let { routine ->
                val planned = routine.exercises.firstOrNull { it.exerciseId == exercise.exerciseId }
                if (planned != null) {
                    val swapped = routine.exercises.map { if (it.id == planned.id) it.copy(exerciseId = target) else it }
                    routines.saveRoutine(routine.copy(exercises = swapped))
                }
            }
        }
        mutableState.update { it.copy(selectedExerciseId = change.selected) }
    }

    /** Marca la sesión como descartada: no sale en el historial ni cuenta en Hoy ni en la racha. */
    private suspend fun discard() {
        val detail = active()
        EndSession(sessions, clock)(detail.session, discard = true)
        restController.clearRest()
        notifyRestDeadline(null)
        mutableState.update { it.copy(rest = null, restExpanded = false, keyboard = null, undoSetId = null, work = null) }
        effectChannel.send(WorkoutEffect.Discarded)
    }

    private suspend fun setMode(intent: WorkoutIntent.SetMode) {
        val detail = active()
        val exercise = detail.exercises.firstOrNull { it.id == intent.id } ?: return
        if (intent.mode == ExerciseMode.SUPERSET) {
            if (exercise.supersetGroup == null) detail.groupWithNext(intent.id).forEach { sessions.saveExercise(it) }
            return
        }
        val current = if (exercise.supersetGroup != null) {
            val changed = detail.ungroup(intent.id)
            changed.forEach { sessions.saveExercise(it) }
            changed.firstOrNull { it.id == intent.id } ?: exercise.copy(supersetGroup = null)
        } else {
            exercise
        }
        val values = intent.mode.values(
            current.restSeconds,
            current.timedSeconds,
            restSeconds = intent.seconds.takeIf { intent.mode == ExerciseMode.REST },
            timedSeconds = intent.seconds.takeIf { intent.mode == ExerciseMode.TIMED },
        )
        sessions.saveExercise(current.copy(supersetGroup = null, restSeconds = values.restSeconds, timedSeconds = values.timedSeconds))
    }

    private fun openKeyboard(id: WorkoutSetId, field: WorkoutField) {
        val set = state.value.session?.sets?.firstOrNull { it.id == id } ?: return
        // Este texto es de edición; el texto visible se formatea con Formats en la pantalla.
        val shown = set.value(field)?.let { if (field == WorkoutField.WEIGHT) shownWeight(it, state.value.unitOfSet(id)) else it }
        val text = shown?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
        mutableState.update { it.copy(keyboard = WorkoutKeyboard(id, field, text), invalidSetId = null) }
    }

    private fun typeKey(key: String) {
        val keyboard = state.value.keyboard ?: return
        val base = if (keyboard.replace) "" else keyboard.text
        if (key != "backspace" && !allowsKey(keyboard.field, base, key)) return
        val text = when (key) {
            "backspace" -> keyboard.text.dropLast(1)
            ".", "," -> if (keyboard.replace ||
                keyboard.text.isEmpty()
            ) {
                "0."
            } else if ('.' in keyboard.text) {
                keyboard.text
            } else {
                keyboard.text + "."
            }
            else -> if (key.length == 1 && key[0].isDigit()) (if (keyboard.replace) "" else keyboard.text) + key else return
        }
        if (text.length <= 10) saveKeyboardText(text)
    }

    private fun setKeyboardValue(value: Double) {
        val field = state.value.keyboard?.field ?: return
        val clamped = when (field) {
            WorkoutField.RPE -> (kotlin.math.round(value * 2) / 2).coerceIn(RPE_MIN, RPE_MAX)
            WorkoutField.RIR -> kotlin.math.round(value).coerceIn(0.0, RIR_MAX)
            else -> value.coerceAtLeast(0.0)
        }
        saveKeyboardText(clamped.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() })
    }

    /**
     * RIR va de 0 a 10 en enteros; RPE de 1 a 10 en medios (7, 7,5, 8). Una tecla que dejaría un
     * valor fuera de rango no hace nada, así nunca hace falta un aviso en rojo.
     */
    private fun allowsKey(field: WorkoutField, base: String, key: String): Boolean {
        if (field != WorkoutField.RIR && field != WorkoutField.RPE) return true
        val decimal = key == "." || key == ","
        if (field == WorkoutField.RIR) {
            if (decimal) return false
            val next = (base + key).toIntOrNull() ?: return false
            return next <= RIR_MAX && !(base == "0")
        }
        if (decimal) return base.isNotEmpty() && '.' !in base && (base.toDoubleOrNull() ?: 0.0) in RPE_MIN..(RPE_MAX - 0.5)
        if ('.' in base) return base.substringAfter('.').isEmpty() && (key == "0" || key == "5")
        val next = (base + key).toDoubleOrNull() ?: return false
        return next in RPE_MIN..RPE_MAX && !(base.isEmpty() && key == "0")
    }

    private fun saveKeyboardText(text: String) {
        val keyboard = state.value.keyboard ?: return
        val set = state.value.session?.sets?.firstOrNull { it.id == keyboard.setId } ?: return
        val stored = storedText(keyboard.field, text, state.value.unitOfSet(keyboard.setId))
        val valid = set.withInput(keyboard.field, stored) != null
        mutableState.update { it.copy(keyboard = keyboard.copy(text = text, replace = false, invalid = !valid), invalidSetId = null) }
        if (valid) edit(keyboard.setId) { requireNotNull(it.withInput(keyboard.field, stored)) }
    }

    private fun complete(id: WorkoutSetId) {
        if (state.value.keyboard?.invalid == true || !completing.add(id)) return
        enqueue {
            val detail = active()
            val set = detail.sets.first { it.id == id }
            val exercise = detail.exercises.first { it.id == set.sessionExerciseId }
            val type = state.value.catalog[exercise.exerciseId]?.trackingType
            if (set.isCompleted) {
                completing.remove(id)
                return@enqueue
            }
            val timed = exercise.mode == ExerciseMode.TIMED
            val ready = if (timed) {
                (set.durationSeconds ?: exercise.timedSeconds) != null
            } else {
                type != null && set.hasRequiredMeasurements(type)
            }
            if (!ready) {
                mutableState.update { it.copy(invalidSetId = id) }
                completing.remove(id)
                return@enqueue
            }
            val routine = detail.session.routineId?.let { routines.findRoutine(it) }
            val planned = routine?.blocks?.sortedBy {
                it.sortOrder
            }?.flatMap { routine.exercisesOf(it.id) }?.getOrNull(detail.exercises.indexOf(exercise))
            val restSeconds = exercise.restSeconds
                ?: planned?.takeIf { it.exerciseId == exercise.exerciseId }?.restSeconds
                ?: routine?.routine?.restSeconds ?: com.olarte.ironprogress.domain.routine.Routine.DEFAULT_REST_SECONDS
            val toComplete = if (timed && set.durationSeconds == null) set.copy(durationSeconds = exercise.timedSeconds) else set
            val completed = completeSet(toComplete)
            if (state.value.work?.setId == id) mutableState.update { it.copy(work = null) }
            // La siguiente serie arranca con lo que se acaba de hacer en lo que tenga vacío: si ya
            // trae las reps objetivo de la rutina, se quedan y solo se copia el peso.
            // Si la siguiente sigue tal como la sugirió el motor y aquí se usó otro peso, sigue ese peso.
            detail.setsOf(exercise.id).firstOrNull { !it.isCompleted && it.id != id }
                ?.let { next ->
                    val filled = if (next.isBlank()) {
                        next.copyMeasurements(completed).copy(rir = null, rpe = null)
                    } else {
                        next.fillMissingFrom(completed)
                    }
                    carryWeightForward(completed, filled) ?: filled
                }
                ?.let { sessions.saveSet(it) }
            completing.remove(id)
            // Superserie o circuito: sin descanso hasta el último del grupo; después, la vuelta empieza
            // por el primero del grupo con series pendientes (F1-12).
            val after = detail.copy(sets = detail.sets.map { if (it.id == id) completed else it })
            val nextInGroup = after.nextInGroupWithoutRest(exercise.id)
            val next = nextInGroup
                ?: after.firstPendingInGroup(exercise.id)?.takeIf { exercise.supersetGroup != null }
                ?: detail.exercises.firstOrNull { item -> detail.setsOf(item.id).any { !it.isCompleted && it.id != id } }
            val started = requireNotNull(completed.completedAt).toEpochMilliseconds()
            lastRestFrameMs = started
            val rest = if (restSeconds > 0 && nextInGroup == null) RestTimer(id, started, restSeconds) else null
            if (rest != null) {
                restController.startRest(detail.session.id, rest)
            } else {
                restController.clearRest()
            }
            notifyRestDeadline(rest)
            undoExpiresAt = clock.now().toEpochMilliseconds() + 5_000
            mutableState.update {
                it.copy(
                    rest = rest,
                    restNowMs = started,
                    restExpanded = rest != null,
                    keyboard = null,
                    invalidSetId = null,
                    undoSetId = id,
                    highlightedSetId = id,
                    selectedExerciseId =
                    next?.id ?: exercise.id,
                )
            }
            effectChannel.send(WorkoutEffect.SetCompleted)
            announceRecords(after, id)
            undoJob?.cancel()
            undoJob = viewModelScope.launch {
                delay(5_000)
                mutableState.update { it.copy(undoSetId = null) }
            }
            highlightJob?.cancel()
            highlightJob = viewModelScope.launch {
                delay(900)
                mutableState.update { it.copy(highlightedSetId = null) }
            }
        }
    }

    /**
     * Busca los récords de la serie recién completada sin frenar la cola de escrituras. Muestra el
     * más importante unos segundos; si falla la consulta, no hay aviso y el entreno sigue igual.
     */
    private fun announceRecords(detail: SessionDetail, id: WorkoutSetId) {
        recordJob?.cancel()
        recordJob = viewModelScope.launch {
            val hits = try {
                detectRecords(detail, id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            val top = hits.firstOrNull() ?: return@launch
            mutableState.update { it.copy(record = top) }
            effectChannel.send(WorkoutEffect.Record(top))
            delay(RECORD_VISIBLE_MS)
            mutableState.update { if (it.record == top) it.copy(record = null) else it }
        }
    }

    private companion object {
        /** Lo que dura el aviso de récord en pantalla. */
        const val RECORD_VISIBLE_MS = 4_500L
        val PRO_FIELDS = listOf(WorkoutField.RIR, WorkoutField.RPE)
        const val RPE_MIN = 1.0
        const val RPE_MAX = 10.0
        const val RIR_MAX = 10.0
    }

    private fun WorkoutSet.isBlank(): Boolean = WorkoutField.entries.filter { it.measure != null }.all { value(it) == null }

    /** Peso que se escribe en el teclado: en lb con un decimal, en kg tal cual. */
    private fun shownWeight(kg: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LB) kotlin.math.round(WeightUnits.fromKg(kg, unit) * 10) / 10 else kg

    /** Lo escrito en lb pasa a kg antes de guardarse; el resto de campos no cambia. */
    private fun storedText(field: WorkoutField, text: String, unit: WeightUnit): String {
        if (field != WorkoutField.WEIGHT || unit == WeightUnit.KG || text.isBlank()) return text
        val number = text.replace(',', '.').toDoubleOrNull() ?: return text
        return WeightUnits.toKg(number, unit).toString()
    }
}
