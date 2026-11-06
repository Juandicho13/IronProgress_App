package com.olarte.ironprogress.ui.workout

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.ExerciseMode
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

/** Serie por tiempo en marcha: cuenta atrás de [seconds] desde [startedAtMs]. */
data class WorkTimer(val setId: WorkoutSetId, val startedAtMs: Long, val seconds: Int) {
    fun remainingMs(nowMs: Long): Long = (startedAtMs + seconds * 1000L - nowMs).coerceAtLeast(0)
    fun isFinished(nowMs: Long): Boolean = remainingMs(nowMs) == 0L
}

data class WorkoutState(
    val work: WorkTimer? = null,
    val workNowMs: Long = 0,
    val rest: com.olarte.ironprogress.ui.rest.RestTimer? = null,
    val restNowMs: Long = 0,
    val restExpanded: Boolean = false,
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
    /** Si el selector está abierto para cambiar este ejercicio en vez de agregar uno (F1-12). */
    val substitutingId: SessionExerciseId? = null,
    val query: String = "",
) {
    /** Unidad con la que se ve y se escribe el peso de un ejercicio de la sesión. */
    fun unitOf(id: SessionExerciseId?): WeightUnit {
        val exercise = session?.exercises?.firstOrNull { it.id == id } ?: return WeightUnit.KG
        return catalog[exercise.exerciseId]?.preferredUnit ?: WeightUnit.KG
    }

    fun unitOfSet(id: WorkoutSetId): WeightUnit = unitOf(session?.sets?.firstOrNull { it.id == id }?.sessionExerciseId)

    fun modeOf(id: SessionExerciseId?): ExerciseMode = session?.exercises?.firstOrNull { it.id == id }?.mode ?: ExerciseMode.REST

    /**
     * Medidas de una serie de este ejercicio. Por tiempo cambia las reps por la duración y deja
     * el peso si el ejercicio lo lleva.
     */
    fun measuresOf(id: SessionExerciseId?): List<WorkoutField> {
        val exercise = session?.exercises?.firstOrNull { it.id == id }
        val fields = catalog[exercise?.exerciseId]?.trackingType?.fields.orEmpty()
        val base = WorkoutField.entries.filter { it.measure != null && it.measure in fields }
        if (exercise?.mode != ExerciseMode.TIMED) return base
        val timed = base.filter { it != WorkoutField.REPS && it != WorkoutField.DURATION } + WorkoutField.DURATION
        return timed.distinct()
    }
}

sealed interface WorkoutIntent {
    data class AdjustRest(val seconds: Int) : WorkoutIntent
    data object SkipRest : WorkoutIntent
    data object ExpandRest : WorkoutIntent
    data object MinimizeRest : WorkoutIntent
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
    data class MoveExercise(val id: SessionExerciseId, val delta: Int) : WorkoutIntent
    data class SkipExercise(val id: SessionExerciseId) : WorkoutIntent
    data class Substitute(val id: SessionExerciseId) : WorkoutIntent
    data class ToggleGroup(val id: SessionExerciseId) : WorkoutIntent
    data class RemoveSet(val id: WorkoutSetId) : WorkoutIntent

    /**
     * Cómo se hace este ejercicio: con descanso ([seconds] de descanso), sin descanso, por tiempo
     * ([seconds] por serie) o en superserie con el siguiente.
     */
    data class SetMode(val id: SessionExerciseId, val mode: ExerciseMode, val seconds: Int? = null) : WorkoutIntent

    /** Arranca la cuenta atrás de una serie por tiempo; al llegar a 0 la serie se completa sola. */
    data class StartWork(val id: WorkoutSetId) : WorkoutIntent

    /** Para la cuenta atrás sin completar la serie. */
    data object StopWork : WorkoutIntent

    /** Termina la sesión con el gesto de Deslizar para terminar (F1-21). */
    data object Finish : WorkoutIntent

    /** Cambia entre kg y lb para este ejercicio; los pesos siguen guardados en kg (F1-22). */
    data class ToggleUnit(val id: SessionExerciseId) : WorkoutIntent

    /** Agrega el calentamiento sugerido antes de las series de trabajo (F1-22). */
    data class AddWarmup(val id: SessionExerciseId) : WorkoutIntent
}

sealed interface WorkoutEffect {
    data object RestTick : WorkoutEffect
    data object RestFinished : WorkoutEffect
    data class OpenWorkout(val sessionId: String) : WorkoutEffect
    data object SetCompleted : WorkoutEffect
    data class Finished(val sessionId: String) : WorkoutEffect
}
