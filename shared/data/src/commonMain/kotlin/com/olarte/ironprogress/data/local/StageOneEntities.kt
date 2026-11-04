package com.olarte.ironprogress.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Tablas nuevas de la Etapa 1 (HANDOFF 10.3 y 10.4). Las columnas JSON guardan listas u
// objetos; su modelo de dominio y su prueba de ida y vuelta llegan con el ticket que las usa
// (F1-06, F1-18, F1-20 y F1-26). Exigencia, fuente y escalera viajan con el catálogo base, así
// que no llevan columnas de sincronización.

/** Intervalos guardados (PANTALLAS 5.2). exercise_ids y voice en JSON. */
@Entity(tableName = "interval_templates")
data class IntervalTemplateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "work_seconds") val workSeconds: Int,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "rounds") val rounds: Int,
    @ColumnInfo(name = "sets") val sets: Int,
    @ColumnInfo(name = "set_rest_seconds") val setRestSeconds: Int,
    @ColumnInfo(name = "exercise_ids") val exerciseIds: String,
    @ColumnInfo(name = "voice") val voice: String,
    @ColumnInfo(name = "intensity") val intensity: String?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** Patrón de respiración. phases en JSON (segundos de inhalar, retener, exhalar y retener). */
@Entity(tableName = "breathing_patterns")
data class BreathingPatternEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phases") val phases: String,
    @ColumnInfo(name = "cycles") val cycles: Int?,
    @ColumnInfo(name = "minutes") val minutes: Int?,
    @ColumnInfo(name = "risk_level") val riskLevel: String,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** Escalera de un ejercicio (11.10): el más fácil y el más difícil. */
@Entity(
    tableName = "exercise_progressions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseProgressionEntity(
    @PrimaryKey @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "easier_id") val easierId: String?,
    @ColumnInfo(name = "harder_id") val harderId: String?,
)

/** De dónde salió un ejercicio y con qué licencia (RNF-16). */
@Entity(
    tableName = "exercise_sources",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseSourceEntity(
    @PrimaryKey @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "license") val license: String,
    @ColumnInfo(name = "author") val author: String?,
)

/** Exigencia de un ejercicio (11.10). joint_impact en JSON: articulación a LOW, MEDIUM o HIGH. */
@Entity(
    tableName = "exercise_demands",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseDemandEntity(
    @PrimaryKey @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "overall") val overall: Int,
    @ColumnInfo(name = "technique") val technique: Int,
    @ColumnInfo(name = "spine") val spine: Int,
    @ColumnInfo(name = "joint_impact") val jointImpact: String,
    @ColumnInfo(name = "met") val met: Double?,
)

/** Tamizaje PAR-Q+ (RF-SEG-01). answers en JSON. */
@Entity(tableName = "screening_answers", indices = [Index(value = ["date"])])
data class ScreeningAnswerEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    /** Día del tamizaje en formato ISO (2026-11-04). */
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "answers") val answers: String,
    @ColumnInfo(name = "result") val result: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/** Permiso de los padres en el modo joven (10.4). guardian y scope en JSON. */
@Entity(tableName = "guardian_consents", indices = [Index(value = ["minor_user_id"])])
data class GuardianConsentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "minor_user_id") val minorUserId: String?,
    @ColumnInfo(name = "guardian") val guardian: String,
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "scope") val scope: String,
    @ColumnInfo(name = "granted_at") val grantedAt: Long,
    @ColumnInfo(name = "revoked_at") val revokedAt: Long?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
