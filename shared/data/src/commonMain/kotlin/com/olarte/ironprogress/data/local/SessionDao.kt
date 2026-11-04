package com.olarte.ironprogress.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Sesiones, sus bloques, ejercicios y series. Cada serie se guarda sola, al completarla. */
@Dao
interface SessionDao {

    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' AND deleted_at IS NULL " +
            "ORDER BY started_at DESC LIMIT 1",
    )
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId AND deleted_at IS NULL LIMIT 1")
    suspend fun findSession(sessionId: String): WorkoutSessionEntity?

    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'FINISHED' AND deleted_at IS NULL " +
            "ORDER BY started_at DESC",
    )
    fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_blocks WHERE session_id = :sessionId AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun blocksOfSession(sessionId: String): List<WorkoutBlockEntity>

    @Query("SELECT * FROM session_exercises WHERE session_id = :sessionId AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun exercisesOfSession(sessionId: String): List<SessionExerciseEntity>

    @Query(
        "SELECT s.* FROM workout_sets s " +
            "JOIN session_exercises e ON e.id = s.session_exercise_id " +
            "WHERE e.session_id = :sessionId AND s.deleted_at IS NULL AND e.deleted_at IS NULL " +
            "ORDER BY e.sort_order ASC, s.sort_order ASC",
    )
    suspend fun setsOfSession(sessionId: String): List<WorkoutSetEntity>

    /** Las series de la sesión cada vez que cambia una: lo que pinta la pantalla del entreno. */
    @Query(
        "SELECT s.* FROM workout_sets s " +
            "JOIN session_exercises e ON e.id = s.session_exercise_id " +
            "WHERE e.session_id = :sessionId AND s.deleted_at IS NULL AND e.deleted_at IS NULL " +
            "ORDER BY e.sort_order ASC, s.sort_order ASC",
    )
    fun observeSetsOfSession(sessionId: String): Flow<List<WorkoutSetEntity>>

    /** Series completadas de un ejercicio en sesiones terminadas, de la más reciente a la más vieja. */
    @Query(
        "SELECT s.* FROM workout_sets s " +
            "JOIN session_exercises e ON e.id = s.session_exercise_id " +
            "JOIN workout_sessions w ON w.id = e.session_id " +
            "WHERE e.exercise_id = :exerciseId AND w.status = 'FINISHED' AND s.completed_at IS NOT NULL " +
            "AND s.deleted_at IS NULL AND e.deleted_at IS NULL AND w.deleted_at IS NULL " +
            "ORDER BY s.completed_at DESC LIMIT :limit",
    )
    suspend fun recentSetsOfExercise(exerciseId: String, limit: Int): List<WorkoutSetEntity>

    @Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Upsert
    suspend fun upsertBlock(block: WorkoutBlockEntity)

    @Upsert
    suspend fun upsertExercise(exercise: SessionExerciseEntity)

    @Upsert
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Query("UPDATE workout_sets SET deleted_at = :now, updated_at = :now WHERE id = :setId")
    suspend fun markSetDeleted(setId: String, now: Long)

    @Query("SELECT created_at FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun setCreatedAt(setId: String): Long?

    @Query("SELECT created_at FROM workout_blocks WHERE id = :blockId LIMIT 1")
    suspend fun blockCreatedAt(blockId: String): Long?

    @Query("SELECT created_at FROM session_exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun exerciseCreatedAt(exerciseId: String): Long?

    @Query("SELECT created_at FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun sessionCreatedAt(sessionId: String): Long?
}
