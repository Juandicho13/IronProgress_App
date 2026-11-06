package com.olarte.ironprogress.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.EditRoutines
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineExerciseId
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.routine.SetTemplate
import com.olarte.ironprogress.domain.routine.SetTemplateId
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.routine.addExercise
import com.olarte.ironprogress.domain.routine.moveExercise
import com.olarte.ironprogress.domain.routine.removeExercise
import com.olarte.ironprogress.domain.routine.values
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetDraft(val minimum: String, val maximum: String, val rir: String) {
    fun valid(): Boolean {
        val min = minimum.toIntOrNull()
        val max = maximum.toIntOrNull()
        val target = rir.toIntOrNull()
        return (minimum.isBlank() || min != null && min > 0) &&
            (maximum.isBlank() || max != null && max > 0) &&
            (min == null || max == null || min <= max) &&
            (rir.isBlank() || target != null && target in 0..5)
    }
}
data class RoutineEditorState(
    val detail: RoutineDetail? = null,
    val name: String = "",
    val notes: String = "",
    val rest: Map<RoutineExerciseId, String> = emptyMap(),
    val sets: Map<SetTemplateId, SetDraft> = emptyMap(),
    val catalog: List<Exercise> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    val failed: Boolean = false,
    val confirmDiscard: Boolean = false,
) {
    val valid: Boolean get() = detail != null &&
        name.isNotBlank() &&
        sets.values.all { it.valid() } &&
        rest.values.all { it.isBlank() || (it.toIntOrNull() ?: -1) >= 0 }
}
sealed interface RoutineEditorIntent {
    data class Name(val value: String) : RoutineEditorIntent
    data class Notes(val value: String) : RoutineEditorIntent
    data class FolderSelect(val id: FolderId?) : RoutineEditorIntent
    data object AddBlock : RoutineEditorIntent
    data class RemoveBlock(val id: BlockId) : RoutineEditorIntent
    data class AddExercise(val block: BlockId, val exercise: ExerciseId) : RoutineEditorIntent
    data class RemoveExercise(val id: RoutineExerciseId) : RoutineEditorIntent
    data class MoveExercise(val id: RoutineExerciseId, val target: Int) : RoutineEditorIntent
    data class ExerciseNotes(val id: RoutineExerciseId, val value: String) : RoutineEditorIntent
    data class Rest(val id: RoutineExerciseId, val value: String) : RoutineEditorIntent

    /** Cómo se hace el ejercicio: con o sin descanso, por tiempo o en superserie con el siguiente. */
    data class Mode(val id: RoutineExerciseId, val mode: ExerciseMode, val seconds: Int? = null) : RoutineEditorIntent
    data class AddSet(val id: RoutineExerciseId) : RoutineEditorIntent
    data class RemoveSet(val id: SetTemplateId) : RoutineEditorIntent
    data class ChangeSet(val id: SetTemplateId, val value: SetDraft) : RoutineEditorIntent
    data object Save : RoutineEditorIntent
    data object Retry : RoutineEditorIntent
    data object Back : RoutineEditorIntent
    data object Discard : RoutineEditorIntent
    data object Keep : RoutineEditorIntent
}
sealed interface RoutineEditorEffect {
    data object Close : RoutineEditorEffect
}

/** Aplica el modo a un ejercicio; superserie lo agrupa con el siguiente del mismo bloque. */
internal fun RoutineEditorState.withMode(detail: RoutineDetail, intent: RoutineEditorIntent.Mode): RoutineEditorState? {
    val exercise = detail.exercises.firstOrNull { it.id == intent.id } ?: return null
    if (intent.mode == ExerciseMode.SUPERSET) {
        val inBlock = detail.exercises.filter { it.blockId == exercise.blockId }.sortedBy { it.sortOrder }
        val next = inBlock.getOrNull(inBlock.indexOf(exercise) + 1) ?: return null
        val group =
            exercise.supersetGroup ?: next.supersetGroup ?: ((detail.exercises.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
        return copy(
            detail = detail.copy(
                exercises = detail.exercises.map {
                    if (it.id == exercise.id || it.id == next.id) it.copy(supersetGroup = group, timedSeconds = null) else it
                },
            ),
        )
    }
    val values = intent.mode.values(
        rest[exercise.id]?.toIntOrNull() ?: exercise.restSeconds,
        exercise.timedSeconds,
        restSeconds = intent.seconds.takeIf { intent.mode == ExerciseMode.REST },
        timedSeconds = intent.seconds.takeIf { intent.mode == ExerciseMode.TIMED },
    )
    var exercises = detail.exercises.map {
        if (it.id == exercise.id) it.copy(supersetGroup = null, timedSeconds = values.timedSeconds) else it
    }
    // Si el compañero de superserie queda solo, deja de estar agrupado.
    exercise.supersetGroup?.let { group ->
        if (exercises.count { it.supersetGroup == group } == 1) {
            exercises = exercises.map { if (it.supersetGroup == group) it.copy(supersetGroup = null) else it }
        }
    }
    return copy(detail = detail.copy(exercises = exercises), rest = rest + (exercise.id to (values.restSeconds?.toString() ?: "")))
}

class RoutineEditorViewModel(
    private val repository: RoutineRepository,
    private val catalog: ExerciseRepository,
    private val edit: EditRoutines,
    private val ids: IdGenerator,
    private val id: String?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RoutineEditorState())
    val state = mutableState.asStateFlow()
    private val events = Channel<RoutineEditorEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()
    init {
        load()
    }
    private fun load() {
        mutableState.update { it.copy(loading = true, failed = false) }
        viewModelScope.launch {
            try {
                // El nombre provisional nunca se persiste ni se muestra.
                val detail = if (id == null) edit.draft("draft") else requireNotNull(repository.findRoutine(RoutineId(id)))
                mutableState.value = RoutineEditorState(
                    detail = detail,
                    name = if (id == null) "" else detail.routine.name,
                    notes = detail.routine.notes,
                    rest = detail.exercises.associate { it.id to (it.restSeconds?.toString() ?: "") },
                    sets = detail.setTemplates.associate { it.id to it.draft() },
                    catalog = catalog.observeExercises().first(),
                    folders = repository.observeFolders().first(),
                    loading = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }
    fun onIntent(intent: RoutineEditorIntent) {
        if (state.value.saving) return
        when (intent) {
            RoutineEditorIntent.Retry -> if (state.value.detail == null) load() else persist()
            RoutineEditorIntent.Save -> persist()
            RoutineEditorIntent.Back -> if (state.value.dirty) {
                mutableState.update {
                    it.copy(confirmDiscard = true)
                }
            } else {
                events.trySend(RoutineEditorEffect.Close)
            }
            RoutineEditorIntent.Discard -> events.trySend(RoutineEditorEffect.Close)
            RoutineEditorIntent.Keep -> mutableState.update { it.copy(confirmDiscard = false) }
            else -> change(intent)
        }
    }
    private fun change(intent: RoutineEditorIntent) {
        val current = state.value
        val detail = current.detail ?: return
        val updated = when (intent) {
            is RoutineEditorIntent.Name -> current.copy(name = intent.value)
            is RoutineEditorIntent.Notes -> current.copy(notes = intent.value)
            is RoutineEditorIntent.FolderSelect -> current.copy(detail = detail.copy(routine = detail.routine.copy(folderId = intent.id)))
            is RoutineEditorIntent.Rest -> current.copy(rest = current.rest + (intent.id to intent.value))
            is RoutineEditorIntent.Mode -> current.withMode(detail, intent) ?: return
            is RoutineEditorIntent.ChangeSet -> current.copy(sets = current.sets + (intent.id to intent.value))
            is RoutineEditorIntent.ExerciseNotes -> current.copy(
                detail = detail.copy(
                    exercises = detail.exercises.map {
                        if (it.id ==
                            intent.id
                        ) {
                            it.copy(notes = intent.value)
                        } else {
                            it
                        }
                    },
                ),
            )
            RoutineEditorIntent.AddBlock -> current.copy(
                detail = detail.copy(
                    blocks =
                    detail.blocks + WorkoutBlock(BlockId(ids.newId()), detail.blocks.size, BlockFormat.SETS),
                ),
            )
            is RoutineEditorIntent.RemoveBlock -> {
                val exerciseIds = detail.exercisesOf(intent.id).map { it.id }.toSet()
                current.copy(
                    detail = detail.copy(
                        blocks = detail.blocks.filterNot {
                            it.id == intent.id
                        }.mapIndexed { index, block -> block.copy(sortOrder = index) },
                        exercises = detail.exercises.filterNot { it.id in exerciseIds },
                        setTemplates = detail.setTemplates.filterNot {
                            it.routineExerciseId in
                                exerciseIds
                        },
                    ),
                )
            }
            is RoutineEditorIntent.AddExercise -> current.copy(detail = detail.addExercise(ids, intent.block, intent.exercise))
            is RoutineEditorIntent.RemoveExercise -> current.copy(detail = detail.removeExercise(intent.id))
            is RoutineEditorIntent.MoveExercise -> current.copy(detail = detail.moveExercise(intent.id, intent.target))
            is RoutineEditorIntent.AddSet -> current.copy(
                detail = detail.copy(
                    setTemplates =
                    detail.setTemplates +
                        SetTemplate(
                            SetTemplateId(ids.newId()),
                            intent.id,
                            detail.setsOf(intent.id).size,
                            repsMin = 8,
                            repsMax = 12,
                            targetRir = 2,
                        ),
                ),
            )
            is RoutineEditorIntent.RemoveSet -> current.copy(
                detail = detail.copy(
                    setTemplates = detail.setTemplates.filterNot {
                        it.id ==
                            intent.id
                    }.groupBy {
                        it.routineExerciseId
                    }.values.flatMap { list -> list.mapIndexed { index, set -> set.copy(sortOrder = index) } },
                ),
            )
            else -> current
        }
        val result = updated.detail ?: return
        mutableState.value = updated.copy(
            dirty = true,
            failed = false,
            sets = result.setTemplates.associate { it.id to (updated.sets[it.id] ?: it.draft()) },
            rest = result.exercises.associate { it.id to (updated.rest[it.id] ?: "") },
        )
    }
    private fun persist() {
        val current = state.value
        if (!current.valid) return
        val detail = current.detail ?: return
        mutableState.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                edit.save(
                    detail.copy(
                        routine = detail.routine.copy(name = current.name, notes = current.notes),
                        exercises = detail.exercises.map { it.copy(restSeconds = current.rest[it.id]?.toIntOrNull()) },
                        setTemplates = detail.setTemplates.map {
                            val draft = current.sets.getValue(it.id)
                            it.copy(
                                repsMin = draft.minimum.toIntOrNull(),
                                repsMax = draft.maximum.toIntOrNull(),
                                targetRir = draft.rir.toIntOrNull(),
                            )
                        },
                    ),
                )
                mutableState.update { it.copy(saving = false, dirty = false) }
                events.send(RoutineEditorEffect.Close)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failed = true) }
            }
        }
    }
}
private fun SetTemplate.draft() = SetDraft(repsMin?.toString() ?: "", repsMax?.toString() ?: "", targetRir?.toString() ?: "")
