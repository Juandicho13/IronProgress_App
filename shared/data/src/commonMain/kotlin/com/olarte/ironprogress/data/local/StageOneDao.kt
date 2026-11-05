package com.olarte.ironprogress.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Intervalos guardados, respiración, datos del catálogo de la Etapa 1, tamizaje y consentimiento. */
@Dao
interface StageOneDao {

    @Query("SELECT * FROM interval_templates WHERE deleted_at IS NULL ORDER BY name ASC")
    fun observeIntervalTemplates(): Flow<List<IntervalTemplateEntity>>

    @Upsert
    suspend fun upsertIntervalTemplate(template: IntervalTemplateEntity)

    @Query("SELECT * FROM interval_templates WHERE id = :templateId AND deleted_at IS NULL LIMIT 1")
    suspend fun findIntervalTemplate(templateId: String): IntervalTemplateEntity?

    @Query("UPDATE interval_templates SET deleted_at = :now, updated_at = :now WHERE id = :templateId")
    suspend fun markIntervalTemplateDeleted(templateId: String, now: Long)

    @Query("SELECT * FROM breathing_patterns WHERE deleted_at IS NULL ORDER BY name ASC")
    fun observeBreathingPatterns(): Flow<List<BreathingPatternEntity>>

    @Upsert
    suspend fun upsertBreathingPattern(pattern: BreathingPatternEntity)

    @Query("SELECT * FROM exercise_progressions WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findProgression(exerciseId: String): ExerciseProgressionEntity?

    @Upsert
    suspend fun upsertProgressions(progressions: List<ExerciseProgressionEntity>)

    @Query("SELECT * FROM exercise_sources WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findSource(exerciseId: String): ExerciseSourceEntity?

    @Upsert
    suspend fun upsertSources(sources: List<ExerciseSourceEntity>)

    @Query("SELECT * FROM exercise_demands WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findDemand(exerciseId: String): ExerciseDemandEntity?

    @Upsert
    suspend fun upsertDemands(demands: List<ExerciseDemandEntity>)

    @Query("SELECT * FROM screening_answers WHERE deleted_at IS NULL ORDER BY date DESC LIMIT 1")
    suspend fun latestScreening(): ScreeningAnswerEntity?

    @Upsert
    suspend fun upsertScreening(answer: ScreeningAnswerEntity)

    @Query("SELECT * FROM guardian_consents WHERE revoked_at IS NULL AND deleted_at IS NULL ORDER BY granted_at DESC LIMIT 1")
    suspend fun activeGuardianConsent(): GuardianConsentEntity?

    @Upsert
    suspend fun upsertGuardianConsent(consent: GuardianConsentEntity)
}
