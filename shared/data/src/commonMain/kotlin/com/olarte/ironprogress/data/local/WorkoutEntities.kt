package com.olarte.ironprogress.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Tablas de rutinas y sesiones (HANDOFF 10.1, 10.3 y 10.5). Toda tabla del usuario lleva las
// columnas de sincronización de 10.2: user_id (nulo hasta F3), created_at, updated_at y
// deleted_at, en ms desde época. Un borrado llena deleted_at y la fila se queda para que viaje
// entre dispositivos. Los enums se guardan como texto con su nombre.
// exercise_id no tiene llave foránea a propósito: un ejercicio propio puede llegar por la
// sincronización después de la rutina o la sesión que lo usa.

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["folder_id"])],
)
data class RoutineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "folder_id") val folderId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /** "USER" o el id del profesional (Etapa 4). */
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "source_program_version_id") val sourceProgramVersionId: String?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["status"]), Index(value = ["started_at"]), Index(value = ["routine_id"])],
)
data class WorkoutSessionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    /** Sin llave foránea: borrar una rutina no borra su historial. */
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "program_day_id") val programDayId: String?,
    @ColumnInfo(name = "gym_profile_id") val gymProfileId: String?,
    @ColumnInfo(name = "discipline") val discipline: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "adjustment_applied") val adjustmentApplied: Boolean,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/**
 * Bloque de una rutina (routine_id) o de una sesión (session_id): uno de los dos, nunca ambos.
 * result es JSON de BlockResult y solo lo llenan los bloques de sesión.
 */
@Entity(
    tableName = "workout_blocks",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routine_id"]), Index(value = ["session_id"])],
)
data class WorkoutBlockEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "format") val format: String,
    @ColumnInfo(name = "rounds") val rounds: Int?,
    @ColumnInfo(name = "work_seconds") val workSeconds: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "time_cap_seconds") val timeCapSeconds: Int?,
    @ColumnInfo(name = "result") val result: String?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["block_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routine_id"]), Index(value = ["block_id"]), Index(value = ["exercise_id"])],
)
data class RoutineExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "block_id") val blockId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "unit") val unit: String?,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "set_templates",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routine_exercise_id"])],
)
data class SetTemplateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_exercise_id") val routineExerciseId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "set_type") val setType: String,
    @ColumnInfo(name = "reps_min") val repsMin: Int?,
    @ColumnInfo(name = "reps_max") val repsMax: Int?,
    @ColumnInfo(name = "target_rir") val targetRir: Int?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["block_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"]), Index(value = ["block_id"]), Index(value = ["exercise_id"])],
)
data class SessionExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "block_id") val blockId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** La serie: todo en kg, metros, segundos, cm y vatios. El bloque sale de su session_exercise. */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_exercise_id"]), Index(value = ["completed_at"])],
)
data class WorkoutSetEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_exercise_id") val sessionExerciseId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "set_type") val setType: String,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double?,
    @ColumnInfo(name = "calories") val calories: Int?,
    @ColumnInfo(name = "height_cm") val heightCm: Double?,
    @ColumnInfo(name = "breaths") val breaths: Int?,
    @ColumnInfo(name = "watts") val watts: Int?,
    @ColumnInfo(name = "side") val side: String?,
    @ColumnInfo(name = "round") val round: Int?,
    @ColumnInfo(name = "rir") val rir: Int?,
    @ColumnInfo(name = "rpe") val rpe: Double?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "suggestion") val suggestion: String?,
    @ColumnInfo(name = "end_heart_rate") val endHeartRate: Int?,
    @ColumnInfo(name = "recovery_seconds") val recoverySeconds: Int?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "notes", defaultValue = "''") val notes: String = "",
)
