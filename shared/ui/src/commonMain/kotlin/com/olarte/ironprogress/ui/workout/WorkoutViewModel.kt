package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.copyMeasurements
import com.olarte.ironprogress.domain.session.equivalentPreviousSet
import com.olarte.ironprogress.domain.session.hasRequiredMeasurements
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
) : ViewModel() {
    private val mutableState = MutableStateFlow(WorkoutState())
    val state = mutableState.asStateFlow()
    private val effectChannel = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var observation: Job? = null
    private var writer: Job? = null
    private val writes = ArrayDeque<suspend () -> Unit>()
    private val completing = mutableSetOf<WorkoutSetId>()
    private var undoJob: Job? = null
    private var highlightJob: Job? = null
    private var undoExpiresAt = 0L

    init {
        observe()
    }

    fun elapsedSeconds(): Long = state.value.session?.session?.let { (clock.now() - it.startedAt).inWholeSeconds.coerceAtLeast(0) } ?: 0

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(sessions.observeActiveSession(), exercises.observeExercises()) { detail, catalog ->
                    detail to catalog.associateBy { it.id }
                }.collectLatest { (detail, catalog) ->
                    val old = state.value
                    val sameSession = old.session?.session?.id == detail?.session?.id
                    val previous = if (sameSession) old.previous.toMutableMap() else mutableMapOf()
                    detail?.exercises?.forEach {
                        if (it.exerciseId !in previous) previous[it.exerciseId] = sessions.previousSets(it.exerciseId)
                    }
                    val name = detail?.session?.routineId?.let { routines.findRoutine(it)?.routine?.name }
                    mutableState.update {
                        val selected = it.selectedExerciseId?.takeIf { id -> detail?.exercises?.any { it.id == id } == true }
                            ?: detail?.exercises?.firstOrNull { item -> detail.setsOf(item.id).any { !it.isCompleted } }?.id
                            ?: detail?.exercises?.firstOrNull()?.id
                        it.copy(
                            loading = false,
                            session = detail,
                            catalog = catalog,
                            previous = previous,
                            routineName = name,
                            selectedExerciseId = selected,
                            keyboard = it.keyboard?.takeIf { keyboard -> detail?.sets?.any { set -> set.id == keyboard.setId } == true },
                        )
                    }
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
            WorkoutIntent.StartFree -> {
                if (state.value.pendingWrites > 0) return
                enqueue {
                    val session = startSession()
                    effectChannel.send(WorkoutEffect.OpenWorkout(session.session.id.value))
                }
            }
            WorkoutIntent.Retry -> {
                mutableState.update { it.copy(failed = false) }
                observe()
                drain()
            }
            is WorkoutIntent.SelectExercise -> mutableState.update { it.copy(selectedExerciseId = intent.id) }
            WorkoutIntent.ChooseExercise -> mutableState.update { it.copy(choosingExercise = true, query = "") }
            WorkoutIntent.ClosePicker -> mutableState.update { it.copy(choosingExercise = false) }
            is WorkoutIntent.Search -> mutableState.update { it.copy(query = intent.query) }
            is WorkoutIntent.AddExercise -> {
                mutableState.update { it.copy(choosingExercise = false) }
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
                mutableState.update { it.copy(undoSetId = null) }
                edit(id) { it.copy(completedAt = null) }
            }
            is WorkoutIntent.ChangeType -> edit(intent.id) { it.copy(setType = intent.type) }
            is WorkoutIntent.ChangeNote -> edit(intent.id) { it.copy(notes = intent.note) }
        }
    }

    fun fieldsFor(id: WorkoutSetId): List<WorkoutField> {
        val detail = state.value.session ?: return listOf(WorkoutField.WEIGHT, WorkoutField.REPS, WorkoutField.RIR, WorkoutField.RPE)
        val set = detail.sets.firstOrNull { it.id == id }
        val exercise = detail.exercises.firstOrNull { it.id == set?.sessionExerciseId }
        val fields = state.value.catalog[exercise?.exerciseId]?.trackingType?.fields.orEmpty()
        return WorkoutField.entries.filter { it.measure in fields || it.measure == null }
    }

    private fun openKeyboard(id: WorkoutSetId, field: WorkoutField) {
        val set = state.value.session?.sets?.firstOrNull { it.id == id } ?: return
        // Este texto es de edición; el texto visible se formatea con Formats en la pantalla.
        val text = set.value(field)?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
        mutableState.update { it.copy(keyboard = WorkoutKeyboard(id, field, text), invalidSetId = null) }
    }

    private fun typeKey(key: String) {
        val keyboard = state.value.keyboard ?: return
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
        saveKeyboardText(value.coerceAtLeast(0.0).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() })
    }

    private fun saveKeyboardText(text: String) {
        val keyboard = state.value.keyboard ?: return
        val set = state.value.session?.sets?.firstOrNull { it.id == keyboard.setId } ?: return
        val valid = set.withInput(keyboard.field, text) != null
        mutableState.update { it.copy(keyboard = keyboard.copy(text = text, replace = false, invalid = !valid), invalidSetId = null) }
        if (valid) edit(keyboard.setId) { requireNotNull(it.withInput(keyboard.field, text)) }
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
            if (type == null || !set.hasRequiredMeasurements(type)) {
                mutableState.update { it.copy(invalidSetId = id) }
                completing.remove(id)
                return@enqueue
            }
            completeSet(set)
            completing.remove(id)
            val next = detail.exercises.firstOrNull { item -> detail.setsOf(item.id).any { !it.isCompleted && it.id != id } }
            undoExpiresAt = clock.now().toEpochMilliseconds() + 5_000
            mutableState.update {
                it.copy(
                    keyboard = null,
                    invalidSetId = null,
                    undoSetId = id,
                    highlightedSetId = id,
                    selectedExerciseId =
                    next?.id ?: exercise.id,
                )
            }
            effectChannel.send(WorkoutEffect.SetCompleted)
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
}
