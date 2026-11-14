package com.olarte.ironprogress.ui.workout

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.domain.progress.RecordHit
import com.olarte.ironprogress.domain.progress.RecoveryLevel
import com.olarte.ironprogress.domain.progression.Stagnation
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
    /** Modo Pro de Ajustes: con él se ven y se anotan RIR y RPE; sin él, solo peso y reps. */
    val proMode: Boolean = false,
    /** Ejercicios de la sesión estancados según HANDOFF 11.3 (F2-05). */
    val stagnation: Map<SessionExerciseId, Stagnation> = emptyMap(),
    /** Los que se cerraron con "Ahora no": no vuelven a salir en esta sesión. */
    val stagnationDismissed: Set<SessionExerciseId> = emptySet(),
    /** Récord recién batido que se está mostrando (F2-03); null sin aviso. */
    val record: RecordHit? = null,
    /** Agujetas por preguntar antes de la primera serie (F2-06): el feedback anterior de cada músculo. */
    val soreness: List<SessionFeedback> = emptyList(),
    val sorenessAnswers: Map<Muscle, Int> = emptyMap(),
    /** Lo que cambió hoy después de contestar las agujetas; null sin aviso. */
    val sorenessResult: Map<Muscle, SetChange>? = null,
    /** Ejercicios que la última vez dejaron una molestia fuerte (F2-06): se propone cambiarlos. */
    val painSwap: Set<SessionExerciseId> = emptySet(),
    val painSwapDismissed: Set<SessionExerciseId> = emptySet(),
    /** Calentamiento según la recuperación (F2-14): el aviso de cada ejercicio. */
    val warmups: Map<SessionExerciseId, RecoveryWarmupNote> = emptyMap(),
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

/** Aviso de calentamiento: el músculo que sigue cargado y cuántas aproximaciones se sugieren. */
data class RecoveryWarmupNote(val muscle: Muscle, val level: RecoveryLevel, val sets: Int)

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

    /** Llega de Ajustes: el modo Pro muestra RIR y RPE. */
    data class SetProMode(val on: Boolean) : WorkoutIntent

    /** Cancela el entreno: no se guarda nada ni cuenta en Hoy ni en la racha. */
    data object Cancel : WorkoutIntent

    /** Vuelve a poner en la serie el peso y las reps que sugirió el motor (F2-02). */
    data class ApplySuggestion(val id: WorkoutSetId) : WorkoutIntent

    /** Estancamiento: descarga de hoy, 40 % menos series con el mismo peso (F2-05). */
    data class Deload(val id: SessionExerciseId) : WorkoutIntent

    /** Estancamiento: cambia el rango de reps del ejercicio en la rutina y en las series pendientes. */
    data class ChangeRange(val id: SessionExerciseId) : WorkoutIntent

    /** "Ahora no": el aviso de estancamiento no vuelve a salir en esta sesión. */
    data class DismissStagnation(val id: SessionExerciseId) : WorkoutIntent

    /**
     * Escalera (F2-12): cambia el ejercicio por la variante que propuso el motor; lo hecho se queda.
     * Con [inRoutine] también la cambia en la rutina.
     */
    data class ApplyLadder(val id: SessionExerciseId, val inRoutine: Boolean) : WorkoutIntent

    /** Cierra el aviso de récord antes de tiempo. */
    data object DismissRecord : WorkoutIntent

    /** Agujetas de un músculo, de 0 (nada) a 3 (muchas) (F2-06). */
    data class AnswerSoreness(val muscle: Muscle, val value: Int) : WorkoutIntent

    /** Guarda las agujetas contestadas y ajusta las series de hoy (11.4). */
    data object ApplySoreness : WorkoutIntent

    /** No contesta las agujetas: hoy no cambia nada. */
    data object SkipSoreness : WorkoutIntent

    data object DismissSorenessResult : WorkoutIntent

    /** "Ahora no" en la propuesta de cambiar un ejercicio por molestias. */
    data class DismissPainSwap(val id: SessionExerciseId) : WorkoutIntent
}

sealed interface WorkoutEffect {
    data object RestTick : WorkoutEffect
    data object RestFinished : WorkoutEffect
    data class OpenWorkout(val sessionId: String) : WorkoutEffect
    data object SetCompleted : WorkoutEffect
    data class Finished(val sessionId: String) : WorkoutEffect

    /** La sesión se descartó (cancelada o terminada sin ninguna serie hecha): se vuelve a Hoy. */
    data object Discarded : WorkoutEffect

    /** Se batió un récord al completar una serie: vibración de récord (F2-03). */
    data class Record(val hit: RecordHit) : WorkoutEffect
}
