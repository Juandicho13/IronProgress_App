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

    @Query("DELETE FROM exercise_muscles WHERE exercise_id IN (:exerciseIds)")
    suspend fun deleteMusclesForExercises(exerciseIds: List<String>)

    @Upsert
    suspend fun upsertExerciseMuscles(muscles: List<ExerciseMuscleEntity>)

    /**
     * Siembra o actualiza el catalogo base en una sola transaccion atomica sin tocar
     * los ejercicios personalizados creados por el usuario.
     */
    @Transaction
    suspend fun replaceSeedCatalog(
        exercises: List<ExerciseEntity>,
        muscles: List<ExerciseMuscleEntity>,
        version: Int,
    ) {
        if (exercises.isNotEmpty()) {
            upsertExercises(exercises)
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