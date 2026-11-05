package com.olarte.ironprogress.ui.workout

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId

/** Campos del teclado. No se persisten como enum ni modifican los enums del catálogo. */
enum class WorkoutField(val measure: SetField? = null) {
    WEIGHT(SetField.WEIGHT),
    REPS(SetField.REPS),
    DURATION(SetField.DURATION),
    DISTANCE(SetField.DISTANCE),
    CALORIES(SetField.CALORIES),
    HEIGHT(SetField.HEIGHT),
    BREATHS(SetField.BREATHS),
    WATTS(SetField.WATTS),
    RIR,
    RPE,
}

data class WorkoutKeyboard(
    val setId: WorkoutSetId,
    val field: WorkoutField,
    val text: String,
    val replace: Boolean = true,
    val invalid: Boolean = false,
)

data class WorkoutState(
    val loading: Boolean = true,
    val session: SessionDetail? = null,
    val routineName: String? = null,
    val catalog: Map<ExerciseId, Exercise> = emptyMap(),
    val previous: Map<ExerciseId, List<WorkoutSet>> = emptyMap(),
    val selectedExerciseId: SessionExerciseId? = null,
    val keyboard: WorkoutKeyboard? = null,
    val pendingWrites: Int = 0,
    val failed: Boolean = false,
    val invalidSetId: WorkoutSetId? = null,
    val undoSetId: WorkoutSetId? = null,
    val highlightedSetId: WorkoutSetId? = null,
    val choosingExercise: Boolean = false,
    val query: String = "",
)

sealed interface WorkoutIntent {
    data object StartFree : WorkoutIntent
    data object Retry : WorkoutIntent
    data class SelectExercise(val id: SessionExerciseId) : WorkoutIntent
    data object ChooseExercise : WorkoutIntent
    data object ClosePicker : WorkoutIntent
    data class Search(val query: String) : WorkoutIntent
    data class AddExercise(val id: ExerciseId) : WorkoutIntent
    data class AddSet(val id: SessionExerciseId) : WorkoutIntent
    data class OpenKeyboard(val id: WorkoutSetId, val field: WorkoutField) : WorkoutIntent
    data object CloseKeyboard : WorkoutIntent
    data class Key(val key: String) : WorkoutIntent
    data class Adjust(val amount: Double) : WorkoutIntent
    data class SetValue(val value: Double) : WorkoutIntent
    data object NextField : WorkoutIntent
    data class CopyPrevious(val id: WorkoutSetId) : WorkoutIntent
    data class Complete(val id: WorkoutSetId) : WorkoutIntent
    data object Undo : WorkoutIntent
    data class ChangeType(val id: WorkoutSetId, val type: SetType) : WorkoutIntent
    data class ChangeNote(val id: WorkoutSetId, val note: String) : WorkoutIntent
}

sealed interface WorkoutEffect {
    data class OpenWorkout(val sessionId: String) : WorkoutEffect
    data object SetCompleted : WorkoutEffect
}
