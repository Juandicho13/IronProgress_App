package com.olarte.ironprogress.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Feedback por músculo (F2-06). */
@Dao
interface FeedbackDao {

    @Query("SELECT * FROM session_feedback ORDER BY created_at ASC")
    fun observeAll(): Flow<List<SessionFeedbackEntity>>

    @Query("SELECT * FROM session_feedback ORDER BY created_at ASC")
    suspend fun all(): List<SessionFeedbackEntity>

    @Query("SELECT * FROM session_feedback WHERE session_id = :sessionId ORDER BY created_at ASC")
    suspend fun forSession(sessionId: String): List<SessionFeedbackEntity>

    @Upsert
    suspend fun upsert(rows: List<SessionFeedbackEntity>)

    @Query("UPDATE session_feedback SET soreness = :soreness, soreness_at = :at, updated_at = :at WHERE id = :id")
    suspend fun setSoreness(id: String, soreness: Int, at: Long)
}
