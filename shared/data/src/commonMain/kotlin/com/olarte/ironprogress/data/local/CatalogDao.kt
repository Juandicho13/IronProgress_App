package com.olarte.ironprogress.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Transaction
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun observeAllExercisesWithMuscles(): Flow<List<ExerciseWithMusclesRelation>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun findExerciseWithMuscles(exerciseId: String): ExerciseWithMusclesRelation?

    @Query("SELECT int_value FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadataInt(key: String): Int?

    @Upsert
    suspend fun upsertMetadata(metadata: CatalogMetadataEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET is_archived = :archived WHERE id = :exerciseId")
    suspend fun setArchived(exerciseId: String, archived: Boolean)

    @Query("UPDATE exercises SET preferred_unit = :unit WHERE id = :exerciseId")
    suspend fun setPreferredUnit(exerciseId: String, unit: String?)

    @Query("SELECT * FROM exercises WHERE preferred_unit IS NOT NULL AND id IN (:exerciseIds)")
    suspend fun findWithPreferredUnit(exerciseIds: List<String>): List<ExerciseEntity>

    @Query("SELECT id FROM exercises WHERE is_archived = 1 AND id IN (:exerciseIds)")
    suspend fun findArchivedIds(exerciseIds: List<String>): List<String>

    @Query("DELETE FROM exercise_muscles WHERE exercise_id IN (:exerciseIds)")
    suspend fun deleteMusclesForExercises(exerciseIds: List<String>)

    @Upsert
    suspend fun upsertExerciseMuscles(muscles: List<ExerciseMuscleEntity>)

    @Transaction
    suspend fun saveCustomExercise(exercise: ExerciseEntity, muscles: List<ExerciseMuscleEntity>) {
        require(exercise.isCustom)
        val existing = findExerciseWithMuscles(exercise.id)
        require(existing == null || existing.exercise.isCustom)
        upsertExercise(exercise)
        deleteMusclesForExercises(listOf(exercise.id))
        upsertExerciseMuscles(muscles)
    }

    /**
     * Siembra o actualiza el catalogo base en una sola transaccion atomica sin tocar
     * los ejercicios personalizados creados por el usuario. Conserva el archivado: un
     * ejercicio base que la persona archivó sigue archivado aunque suba la version.
     */
    @Upsert
    suspend fun upsertDemands(demands: List<ExerciseDemandEntity>)

    @Upsert
    suspend fun upsertSources(sources: List<ExerciseSourceEntity>)

    @Upsert
    suspend fun upsertProgressions(progressions: List<ExerciseProgressionEntity>)

    @Query("SELECT * FROM exercise_demands WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findDemand(exerciseId: String): ExerciseDemandEntity?

    @Query("SELECT * FROM exercise_sources WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findSource(exerciseId: String): ExerciseSourceEntity?

    @Query("SELECT * FROM exercise_progressions WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun findProgression(exerciseId: String): ExerciseProgressionEntity?

    /** Siembra el catálogo y sus datos de la Etapa 1 en una sola transacción. */
    @Transaction
    suspend fun replaceSeedCatalogWithFacts(
        exercises: List<ExerciseEntity>,
        muscles: List<ExerciseMuscleEntity>,
        demands: List<ExerciseDemandEntity>,
        sources: List<ExerciseSourceEntity>,
        progressions: List<ExerciseProgressionEntity>,
        version: Int,
    ) {
        replaceSeedCatalog(exercises, muscles, version)
        upsertDemands(demands)
        upsertSources(sources)
        upsertProgressions(progressions)
    }

    @Transaction
    suspend fun replaceSeedCatalog(exercises: List<ExerciseEntity>, muscles: List<ExerciseMuscleEntity>, version: Int) {
        if (exercises.isNotEmpty()) {
            val archivedIds = findArchivedIds(exercises.map { it.id }).toSet()
            // La unidad elegida por ejercicio (F1-22) sobrevive a una nueva versión de la semilla.
            val units = findWithPreferredUnit(exercises.map { it.id }).associate { it.id to it.preferredUnit }
            upsertExercises(
                exercises.map {
                    val kept = if (it.id in archivedIds) it.copy(isArchived = true) else it
                    units[it.id]?.let { unit -> kept.copy(preferredUnit = unit) } ?: kept
                },
            )
            deleteMusclesForExercises(exercises.map { it.id })
            upsertExerciseMuscles(muscles)
        }
        upsertMetadata(
            CatalogMetadataEntity(
                key = CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION,
                intValue = version,
            ),
        )
    }
}
