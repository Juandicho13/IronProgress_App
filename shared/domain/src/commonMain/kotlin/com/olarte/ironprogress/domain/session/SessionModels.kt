package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class SessionId(val value: String)

@JvmInline
value class SessionBlockId(val value: String)

@JvmInline
value class SessionExerciseId(val value: String)

@JvmInline
value class WorkoutSetId(val value: String)

/** Estados de la sesión (HANDOFF 10.2, valores propuestos). */
enum class SessionStatus { IN_PROGRESS, FINISHED, DISCARDED }

/** Lado de una serie unilateral. */
enum class Side { LEFT, RIGHT }

data class WorkoutSession(
    val id: SessionId,
    val routineId: RoutineId?,
    val discipline: Discipline,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val notes: String = "",
    val adjustmentApplied: Boolean = false,
    val programDayId: String? = null,
    val gymProfileId: String? = null,
) {
    init {
        require(endedAt == null || endedAt >= startedAt) { "La sesión no puede terminar antes de empezar" }
        require((status == SessionStatus.IN_PROGRESS) == (endedAt == null)) {
            "Solo la sesión en curso no tiene hora de fin"
        }
    }
}

/** Bloque de lo que de verdad se hizo, con su resultado (desde F1 se guarda; récords en F2-13). */
data class SessionBlock(
    val id: SessionBlockId,
    val sessionId: SessionId,
    val sortOrder: Int,
    val format: BlockFormat,
    val result: BlockResult? = null,
)

/**
 * Resultado de un bloque según su formato. Se guarda como JSON en `workout_blocks.result`: los
 * nombres de [SerialName] son estables y no se cambian.
 */
@Serializable
sealed interface BlockResult {
    @Serializable
    @SerialName("time")
    data class Time(val seconds: Int) : BlockResult

    @Serializable
    @SerialName("rounds_reps")
    data class RoundsAndReps(val rounds: Int, val reps: Int) : BlockResult

    @Serializable
    @SerialName("rounds")
    data class Rounds(val rounds: Int) : BlockResult

    @Serializable
    @SerialName("reps_per_round")
    data class RepsPerRound(val reps: List<Int>) : BlockResult

    @Serializable
    @SerialName("completed")
    data class Completed(val feeling: Int? = null) : BlockResult
}

data class SessionExercise(
    val id: SessionExerciseId,
    val sessionId: SessionId,
    val blockId: SessionBlockId,
    val exerciseId: ExerciseId,
    val sortOrder: Int,
    val supersetGroup: Int? = null,
    val notes: String = "",
    /** Descanso de este ejercicio en la sesión; null usa el de la rutina. 0 es sin descanso. */
    val restSeconds: Int? = null,
    /** Segundos de cada serie cuando va por tiempo; null en series normales. */
    val timedSeconds: Int? = null,
) {
    val mode: com.olarte.ironprogress.domain.routine.ExerciseMode
        get() = com.olarte.ironprogress.domain.routine.ExerciseMode.of(supersetGroup, restSeconds, timedSeconds)
}

/** Unidad atómica del registro. Todo en kg, metros, segundos, cm y vatios. */
data class WorkoutSet(
    val id: WorkoutSetId,
    val sessionExerciseId: SessionExerciseId,
    val sortOrder: Int,
    val setType: SetType = SetType.WORKING,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val calories: Int? = null,
    val heightCm: Double? = null,
    val breaths: Int? = null,
    val watts: Int? = null,
    val side: Side? = null,
    /** Ronda dentro del bloque (intervalos, AMRAP, EMOM); null en series normales. */
    val round: Int? = null,
    val rir: Int? = null,
    val rpe: Double? = null,
    val completedAt: Instant? = null,
    /** Lo que sugirió el motor, para comparar después (F2-02). */
    val suggestion: String? = null,
    val endHeartRate: Int? = null,
    val recoverySeconds: Int? = null,
    val notes: String = "",
) {
    init {
        require(weightKg == null || weightKg >= 0) { "El peso no puede ser negativo" }
        require(listOfNotNull(reps, durationSeconds, calories, breaths, watts, recoverySeconds).all { it >= 0 }) {
            "Los valores de la serie no pueden ser negativos"
        }
        require(distanceMeters == null || distanceMeters >= 0) { "La distancia no puede ser negativa" }
        require(heightCm == null || heightCm >= 0) { "La altura no puede ser negativa" }
        require(rir == null || rir in 0..10) { "El RIR va de 0 a 10" }
        require(rpe == null || (rpe in 1.0..10.0 && (rpe * 2) % 1.0 == 0.0)) { "El RPE va de 1 a 10 en pasos de 0,5" }
        require(round == null || round > 0) { "Las rondas empiezan en 1" }
    }

    val isCompleted: Boolean get() = completedAt != null
}
