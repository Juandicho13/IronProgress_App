package com.olarte.ironprogress.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Feedback por músculo de una sesión (F2-06, HANDOFF 10.2 SessionFeedback y 11.4). Las agujetas
 * llegan después, al volver a entrenar el músculo, con su hora en soreness_at. Si se borra la
 * sesión, su feedback se va con ella.
 */
@Entity(
    tableName = "session_feedback",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"]), Index(value = ["muscle_id"]), Index(value = ["created_at"])],
)
data class SessionFeedbackEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "muscle_id") val muscleId: String,
    @ColumnInfo(name = "pump") val pump: Int?,
    @ColumnInfo(name = "soreness") val soreness: Int?,
    @ColumnInfo(name = "joint_pain") val jointPain: Int,
    @ColumnInfo(name = "load") val load: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "soreness_at") val sorenessAt: Long?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
