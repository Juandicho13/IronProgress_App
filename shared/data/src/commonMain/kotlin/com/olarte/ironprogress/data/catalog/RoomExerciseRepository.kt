package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.data.local.CatalogDao
import com.olarte.ironprogress.data.local.CatalogMetadataEntity
import com.olarte.ironprogress.data.local.ExerciseSourceEntity
import com.olarte.ironprogress.data.local.toDomain
import com.olarte.ironprogress.data.local.toEntities
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseFacts
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementacion de [ExerciseRepository] respaldada por Room 3.
 * Verifica y siembra el [SeedCatalog] de forma perezosa y segura para concurrencia.
 */
class RoomExerciseRepository(private val catalogDao: CatalogDao) : ExerciseRepository {

    private val seedMutex = Mutex()
    private var seedVerified = false

    override fun observeExercises(): Flow<List<Exercise>> = catalogDao.observeAllExercisesWithMuscles()
        .onStart { ensureSeeded() }
        .map { relations ->
            relations.map { it.toDomain() }
        }

    override suspend fun findExercise(id: ExerciseId): Exercise? {
        ensureSeeded()
        return catalogDao.findExerciseWithMuscles(id.value)?.toDomain()
    }

    override suspend fun saveCustomExercise(exercise: Exercise) {
        ensureSeeded()
        val (entity, muscles) = exercise.toEntities()
        catalogDao.saveCustomExercise(entity, muscles)
    }

    override suspend fun findFacts(id: ExerciseId): ExerciseFacts? {
        ensureSeeded()
        if (catalogDao.findExerciseWithMuscles(id.value) == null) return null
        return ExerciseFacts(
            demand = catalogDao.findDemand(id.value)?.toDomain(),
            progression = catalogDao.findProgression(id.value)?.toDomain(),
            source = catalogDao.findSource(id.value)?.toDomain(),
        )
    }

    override suspend fun setArchived(id: ExerciseId, archived: Boolean) {
        ensureSeeded()
        catalogDao.setArchived(id.value, archived)
    }

    /**
     * Siembra o actualiza el catalogo semilla si la version almacenada es menor a [SeedCatalog.VERSION].
     */
    suspend fun ensureSeeded() {
        if (seedVerified) return
        seedMutex.withLock {
            if (seedVerified) return
            val installedVersion = catalogDao.getMetadataInt(
                CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION,
            ) ?: 0

            if (installedVersion < SeedCatalog.VERSION) {
                val pairs = SeedCatalog.exercises.map { it.toEntities() }
                val exerciseEntities = pairs.map { it.first }
                val muscleEntities = pairs.flatMap { it.second }
                catalogDao.replaceSeedCatalogWithFacts(
                    exercises = exerciseEntities,
                    muscles = muscleEntities,
                    demands = SeedCatalogFacts.demands.map { (id, demand) -> demand.toEntity(id) },
                    sources = exerciseEntities.map {
                        ExerciseSourceEntity(it.id, SeedCatalogFacts.SOURCE, SeedCatalogFacts.LICENSE, author = null)
                    },
                    progressions = SeedCatalogFacts.progressions.map { (id, ladder) -> ladder.toEntity(id) },
                    version = SeedCatalog.VERSION,
                )
            }
            seedVerified = true
        }
    }
}
