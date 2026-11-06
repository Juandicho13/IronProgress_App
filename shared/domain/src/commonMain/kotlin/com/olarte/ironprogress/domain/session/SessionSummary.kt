package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import kotlinx.coroutines.flow.first

/** Mejor serie de un ejercicio en la sesión que supera todo lo anterior (récord simple, SES-08). */
data class SessionRecord(val exerciseId: ExerciseId, val weightKg: Double, val reps: Int)

/** Lo hecho en un ejercicio: sus series de trabajo completadas, en orden. */
data class ExerciseSummary(val exerciseId: ExerciseId, val sets: List<WorkoutSet>)

/** La sesión anterior con la que se compara: misma rutina o, en un entreno libre, misma disciplina. */
data class SessionComparison(val sessionId: SessionId, val durationSeconds: Long, val volumeKg: Double)

/** Resumen al terminar (PANTALLAS 1.6). Todo en kg y segundos. */
data class SessionSummary(
    val session: WorkoutSession,
    val durationSeconds: Long,
    val volumeKg: Double,
    val workingSets: Int,
    val records: List<SessionRecord>,
    val exercises: List<ExerciseSummary>,
    /** Bloques temporizados con su resultado (intervalos, AMRAP, EMOM, por tiempo, rondas, flujo). */
    val blockResults: List<SessionBlock>,
    val previous: SessionComparison?,
) {
    /** Cambio del volumen frente a la sesión anterior, en porcentaje entero; null sin comparación. */
    val volumeChangePercent: Int?
        get() = previous?.volumeKg?.takeIf { it > 0 }?.let { kotlin.math.round((volumeKg - it) / it * 100).toInt() }

    /** Segundos de más (positivo) o de menos (negativo) frente a la sesión anterior. */
    val durationChangeSeconds: Long?
        get() = previous?.let { durationSeconds - it.durationSeconds }
}

private fun SessionDetail.durationSeconds(): Long {
    val end = session.endedAt ?: return 0
    return (end - session.startedAt).inWholeSeconds.coerceAtLeast(0)
}

private fun SessionDetail.workingSetsOf(exerciseId: ExerciseId): List<WorkoutSet> {
    val ids = exercises.filter { it.exerciseId == exerciseId }.map { it.id }.toSet()
    return sets.filter { it.sessionExerciseId in ids && it.isCompleted && it.setType.countsAsWorking }
}

/**
 * Arma el resumen de [detail] con las sesiones terminadas antes que ella ([history], en cualquier
 * orden). Un récord cuenta solo si el ejercicio ya se había hecho: más peso que nunca, o el mismo
 * peso máximo con más reps. La primera vez de un ejercicio no es récord.
 */
fun summarize(detail: SessionDetail, history: List<SessionDetail>): SessionSummary {
    val earlier = history.filter {
        it.session.id != detail.session.id &&
            it.session.status == SessionStatus.FINISHED &&
            it.session.startedAt < detail.session.startedAt
    }
    val exercises = detail.exercises.sortedBy { it.sortOrder }.map { it.exerciseId }.distinct().map { id ->
        ExerciseSummary(id, detail.workingSetsOf(id))
    }.filter { it.sets.isNotEmpty() }

    val records = exercises.mapNotNull { summary ->
        val today = summary.sets.filter { it.weightKg != null && (it.reps ?: 0) > 0 }
            .maxWithOrNull(compareBy<WorkoutSet> { it.weightKg }.thenBy { it.reps }) ?: return@mapNotNull null
        val before = earlier.flatMap { it.workingSetsOf(summary.exerciseId) }.filter { it.weightKg != null && (it.reps ?: 0) > 0 }
        if (before.isEmpty()) return@mapNotNull null
        val maxBefore = before.maxOf { it.weightKg!! }
        val repsAtMax = before.filter { it.weightKg == maxBefore }.maxOf { it.reps!! }
        val weight = today.weightKg!!
        val reps = today.reps!!
        if (weight > maxBefore || (weight == maxBefore && reps > repsAtMax)) SessionRecord(summary.exerciseId, weight, reps) else null
    }

    val previous = earlier.filter {
        if (detail.session.routineId != null) {
            it.session.routineId == detail.session.routineId
        } else {
            it.session.routineId == null && it.session.discipline == detail.session.discipline
        }
    }.maxByOrNull { it.session.startedAt }
        ?.let { SessionComparison(it.session.id, it.durationSeconds(), it.volumeKg) }

    return SessionSummary(
        session = detail.session,
        durationSeconds = detail.durationSeconds(),
        volumeKg = detail.volumeKg,
        workingSets = detail.workingSetsDone,
        records = records,
        exercises = exercises,
        blockResults = detail.blocks.filter { it.format != BlockFormat.SETS && it.result != null }.sortedBy { it.sortOrder },
        previous = previous,
    )
}

/** Carga la sesión y su historia y arma el resumen. Null si la sesión no existe o sigue en curso. */
class SummarizeSession(private val repository: SessionRepository) {
    suspend operator fun invoke(id: SessionId): SessionSummary? {
        val detail = repository.findSession(id)?.takeIf { it.session.status == SessionStatus.FINISHED } ?: return null
        val history = repository.observeFinishedSessions().first()
            .filter { it.id != id && it.startedAt < detail.session.startedAt }
            .mapNotNull { repository.findSession(it.id) }
        return summarize(detail, history)
    }
}
