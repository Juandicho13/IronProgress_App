package com.olarte.ironprogress.domain.session

import kotlinx.coroutines.flow.Flow

/** Sesiones y series. Cada serie se guarda al completarla, nunca al final (HANDOFF 8.4). */
interface SessionRepository {
    /** La sesión en curso, si hay una. Solo puede haber una a la vez. */
    fun observeActiveSession(): Flow<SessionDetail?>

    suspend fun findSession(id: SessionId): SessionDetail?

    /** Sesiones terminadas, de la más reciente a la más vieja. */
    fun observeFinishedSessions(): Flow<List<WorkoutSession>>

    suspend fun saveSession(session: WorkoutSession)

    suspend fun saveBlock(block: SessionBlock)

    suspend fun saveExercise(exercise: SessionExercise)

    suspend fun saveSet(set: WorkoutSet)

    suspend fun deleteSet(id: WorkoutSetId)
}

/** Sesión con todo lo que se hizo, en orden. */
data class SessionDetail(
    val session: WorkoutSession,
    val blocks: List<SessionBlock>,
    val exercises: List<SessionExercise>,
    val sets: List<WorkoutSet>,
) {
    fun setsOf(exerciseId: SessionExerciseId): List<WorkoutSet> =
        sets.filter { it.sessionExerciseId == exerciseId }.sortedBy { it.sortOrder }

    /** Series completadas que cuentan como trabajo (sin calentamiento). */
    val workingSetsDone: Int
        get() = sets.count { it.isCompleted && it.setType.countsAsWorking }

    /** Volumen en kg: peso por reps de las series de trabajo completadas. */
    val volumeKg: Double
        get() = sets.filter { it.isCompleted && it.setType.countsAsWorking }.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
}
