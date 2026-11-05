package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.ExerciseId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Sesiones y series. Cada serie se guarda al completarla, nunca al final (HANDOFF 8.4). */
interface SessionRepository {
    /** La sesión en curso, si hay una. Solo puede haber una a la vez. */
    fun observeActiveSession(): Flow<SessionDetail?>

    suspend fun findSession(id: SessionId): SessionDetail?

    /** Sesiones terminadas, de la más reciente a la más vieja. */
    fun observeFinishedSessions(): Flow<List<WorkoutSession>>

    /** Crea la sesión completa en una transacción, o devuelve la que ya está activa. */
    suspend fun createSession(detail: SessionDetail): SessionDetail

    /** Agrega el ejercicio y sus series juntos, sin dejar filas incompletas. */
    suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>)

    /** Series hechas en la última aparición del ejercicio dentro de una sesión terminada. */
    suspend fun previousSets(exerciseId: ExerciseId): List<WorkoutSet> {
        for (session in observeFinishedSessions().first().sortedByDescending { it.endedAt }) {
            val detail = findSession(session.id) ?: continue
            val exercise = detail.exercises.lastOrNull { it.exerciseId == exerciseId } ?: continue
            val sets = detail.setsOf(exercise.id).filter { it.isCompleted }
            if (sets.isNotEmpty()) return sets
        }
        return emptyList()
    }

    suspend fun saveSession(session: WorkoutSession)

    suspend fun saveBlock(block: SessionBlock)

    suspend fun saveExercise(exercise: SessionExercise)

    suspend fun saveSet(set: WorkoutSet)

    suspend fun deleteSet(id: WorkoutSetId)

    /**
     * Borra un bloque con sus ejercicios y series (historial, F1-23). Room marca las tres tablas;
     * esta versión por defecto, para repositorios en memoria, borra solo sus series.
     */
    suspend fun deleteBlock(detail: SessionDetail, id: SessionBlockId) {
        detail.exercises.filter { it.blockId == id }.forEach { exercise -> detail.setsOf(exercise.id).forEach { deleteSet(it.id) } }
    }
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
