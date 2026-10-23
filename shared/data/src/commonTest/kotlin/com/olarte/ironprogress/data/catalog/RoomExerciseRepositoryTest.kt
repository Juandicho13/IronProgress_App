package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.data.local.entity.dao.CatalogDao
import com.olarte.ironprogress.data.local.entity.CatalogMetadataEntity
import com.olarte.ironprogress.data.local.entity.ExerciseEntity
import com.olarte.ironprogress.data.local.entity.ExerciseMuscleEntity
import com.olarte.ironprogress.data.local.entity.ExerciseWithMusclesRelation
import com.olarte.ironprogress.domain.catalog.ExerciseId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifica que [RoomExerciseRepository] siembre automaticamente los 77 ejercicios de [SeedCatalog]
 * cuando la version almacenada es menor a [SeedCatalog.VERSION] y mapee correctamente a [Exercise].
 */
class RoomExerciseRepositoryTest {

    @Test
    fun seedsAll77ExercisesOnFirstObservationAndMapsDomainProperties() = runTest {
        val fakeDao = FakeCatalogDao()
        val repository = RoomExerciseRepository(catalogDao = fakeDao)

        val exercises = repository.observeExercises().first()

        assertEquals(77, exercises.size)
        assertEquals(
            SeedCatalog.VERSION,
            fakeDao.getMetadataInt(CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION),
        )

        val firstSeed = SeedCatalog.exercises.first()
        val loaded = repository.findExercise(ExerciseId(firstSeed.id.value))
        assertNotNull(loaded)
        assertEquals(firstSeed.name, loaded.name)
        assertEquals(firstSeed.pattern, loaded.pattern)
        assertEquals(firstSeed.unilateral, loaded.unilateral)
        assertNull(loaded.preferredUnit)
    }
}

private class FakeCatalogDao : CatalogDao {
    private val exercisesById = linkedMapOf<String, ExerciseEntity>()
    private val musclesByExerciseId = linkedMapOf<String, MutableList<ExerciseMuscleEntity>>()
    private val metadataByKey = mutableMapOf<String, Int>()
    private val relationsFlow = MutableStateFlow<List<ExerciseWithMusclesRelation>>(emptyList())

    override fun observeAllExercisesWithMuscles(): Flow<List<ExerciseWithMusclesRelation>> =
        relationsFlow

    override suspend fun findExerciseWithMuscles(exerciseId: String): ExerciseWithMusclesRelation? {
        val entity = exercisesById[exerciseId] ?: return null
        return ExerciseWithMusclesRelation(
            exercise = entity,
            muscles = musclesByExerciseId[exerciseId].orEmpty(),
        )
    }

    override suspend fun getMetadataInt(key: String): Int? = metadataByKey[key]

    override suspend fun upsertMetadata(metadata: CatalogMetadataEntity) {
        metadataByKey[metadata.key] = metadata.intValue
    }

    override suspend fun upsertExercises(exercises: List<ExerciseEntity>) {
        exercises.forEach { exercisesById[it.id] = it }
        emitCurrentRelations()
    }

    override suspend fun upsertExercise(exercise: ExerciseEntity) {
        exercisesById[exercise.id] = exercise
        emitCurrentRelations()
    }

    override suspend fun deleteMusclesForExercises(exerciseIds: List<String>) {
        exerciseIds.forEach { musclesByExerciseId.remove(it) }
        emitCurrentRelations()
    }

    override suspend fun insertExerciseMuscles(muscles: List<ExerciseMuscleEntity>) {
        muscles.forEach { muscle ->
            val list = musclesByExerciseId.getOrPut(muscle.exerciseId) { mutableListOf() }
            list.removeAll { it.muscleId == muscle.muscleId }
            list.add(muscle)
        }
        emitCurrentRelations()
    }

    private fun emitCurrentRelations() {
        relationsFlow.value = exercisesById.values
            .sortedBy { it.id }
            .map { entity ->
                ExerciseWithMusclesRelation(
                    exercise = entity,
                    muscles = musclesByExerciseId[entity.id].orEmpty(),
                )
            }
    }
}