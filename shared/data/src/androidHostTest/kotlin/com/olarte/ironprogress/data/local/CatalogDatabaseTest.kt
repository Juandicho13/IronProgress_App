package com.olarte.ironprogress.data.local

import androidx.room3.Room
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.catalog.SeedCatalog
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas contra una base Room real en memoria con el SQLite empaquetado.
 * Corren en la JVM del computador con testAndroidHostTest, sin emulador.
 */
class CatalogDatabaseTest {

    private lateinit var database: IronProgressDatabase

    private val dao: CatalogDao
        get() = database.catalogDao()

    @BeforeTest
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
            factory = { IronProgressDatabaseConstructor.initialize() },
        ).buildIronProgressDatabase()
    }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun seedsTheWholeCatalogTheFirstTimeItIsObserved() = runTest {
        val exercises = RoomExerciseRepository(dao).observeExercises().first()

        assertEquals(SeedCatalog.exercises.size, exercises.size)
        assertEquals(
            SeedCatalog.VERSION,
            dao.getMetadataInt(CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION),
        )
    }

    @Test
    fun everySeedExerciseComesBackExactlyAsItWasSaved() = runTest {
        val repository = RoomExerciseRepository(dao)

        SeedCatalog.exercises.forEach { seed ->
            assertEquals(seed, repository.findExercise(seed.id))
        }
    }

    @Test
    fun aSecondStartDoesNotSeedAgain() = runTest {
        RoomExerciseRepository(dao).ensureSeeded()
        val benchId = SeedCatalog.exercises.first().id.value
        val bench = assertNotNull(dao.findExerciseWithMuscles(benchId)).exercise
        dao.upsertExercise(bench.copy(isArchived = true))

        // Un segundo arranque con la misma versión no debe pisar lo que cambió el usuario.
        RoomExerciseRepository(dao).ensureSeeded()

        assertTrue(assertNotNull(dao.findExerciseWithMuscles(benchId)).exercise.isArchived)
    }

    @Test
    fun aNewSeedVersionKeepsTheUsersOwnExercises() = runTest {
        val custom = Exercise(
            id = ExerciseId("mi-press-landmine"),
            name = LocalizedText(es = "Press landmine", en = "Landmine press"),
            trackingType = TrackingType.WEIGHT_REPS,
            equipment = Equipment.BARBELL,
            pattern = MovementPattern.VERTICAL_PUSH,
            muscles = listOf(
                ExerciseMuscle(Muscle.SHOULDERS, MuscleRole.PRIMARY),
                ExerciseMuscle(Muscle.TRICEPS, MuscleRole.SECONDARY),
            ),
            unilateral = true,
            preferredUnit = WeightUnit.LB,
            isCustom = true,
        )
        val (exerciseRow, muscleRows) = custom.toEntities()
        dao.upsertExercise(exerciseRow)
        dao.upsertExerciseMuscles(muscleRows)
        // Simula una app instalada con un catálogo semilla anterior.
        dao.upsertMetadata(
            CatalogMetadataEntity(
                key = CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION,
                intValue = SeedCatalog.VERSION - 1,
            ),
        )

        val repository = RoomExerciseRepository(dao)
        val exercises = repository.observeExercises().first()

        assertEquals(SeedCatalog.exercises.size + 1, exercises.size)
        assertEquals(custom, repository.findExercise(custom.id))
    }
}
