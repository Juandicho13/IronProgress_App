package com.olarte.ironprogress.data.local

import androidx.room3.Room
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.catalog.SeedCatalog
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.Joint
import com.olarte.ironprogress.domain.catalog.JointImpact
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
    fun customExerciseSurvivesEditingArchivingAndReseeding() = runTest {
        val repository = RoomExerciseRepository(dao)
        val save = com.olarte.ironprogress.domain.catalog.SaveCustomExercise(
            repository,
            com.olarte.ironprogress.domain.common.UuidV7IdGenerator,
        )
        val custom = save(SeedCatalog.exercises.first().copy(name = LocalizedText("Mi ejercicio", "My exercise")))
        assertTrue(custom.isCustom)
        assertEquals('7', custom.id.value[14])
        repository.setArchived(custom.id, true)
        val edited = save(custom.copy(muscles = listOf(ExerciseMuscle(Muscle.BICEPS, MuscleRole.PRIMARY))), custom.id)
        assertTrue(edited.isArchived)
        assertEquals(edited, repository.findExercise(custom.id))
        repository.setArchived(custom.id, false)
        for (language in listOf("es", "en")) {
            for (text in listOf("mi ejercicio", "my exercise")) {
                assertEquals(
                    custom.id,
                    com.olarte.ironprogress.domain.catalog.ExerciseSearch.filter(
                        repository.observeExercises().first(),
                        com.olarte.ironprogress.domain.catalog.ExerciseQuery(text = text),
                        language,
                    ).single().id,
                )
            }
        }
        dao.upsertMetadata(CatalogMetadataEntity(CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION, intValue = 0))
        RoomExerciseRepository(dao).ensureSeeded()
        assertEquals(edited.copy(isArchived = false), repository.findExercise(custom.id))
        kotlin.test.assertFailsWith<IllegalArgumentException> { save(custom, SeedCatalog.exercises.first().id) }
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

    @Test
    fun archivingPersistsInTheDatabase() = runTest {
        val repository = RoomExerciseRepository(dao)
        val bench = SeedCatalog.exercises.first()

        repository.setArchived(bench.id, archived = true)

        assertTrue(assertNotNull(RoomExerciseRepository(dao).findExercise(bench.id)).isArchived)
    }

    @Test
    fun reseedingWithAHigherVersionKeepsWhatThePersonArchived() = runTest {
        val repository = RoomExerciseRepository(dao)
        val bench = SeedCatalog.exercises.first()
        repository.setArchived(bench.id, archived = true)

        val pairs = SeedCatalog.exercises.map { it.toEntities() }
        dao.replaceSeedCatalog(
            exercises = pairs.map { it.first },
            muscles = pairs.flatMap { it.second },
            version = SeedCatalog.VERSION + 1,
        )

        assertTrue(assertNotNull(dao.findExerciseWithMuscles(bench.id.value)).exercise.isArchived)
        assertEquals(
            SeedCatalog.VERSION + 1,
            dao.getMetadataInt(CatalogMetadataEntity.KEY_SEED_CATALOG_VERSION),
        )
    }

    @Test
    fun seedingVersion2StoresDemandLadderSourceAndShares() = runTest {
        val repository = RoomExerciseRepository(dao)

        val facts = repository.findFacts(ExerciseId("pull-up"))
        val pullUp = repository.findExercise(ExerciseId("pull-up"))

        assertNotNull(facts)
        assertEquals(4, facts.demand?.overall)
        assertEquals(JointImpact.HIGH, facts.demand?.impactOn(Joint.SHOULDER))
        assertEquals(ExerciseId("assisted-pull-up"), facts.progression?.easier)
        assertEquals("IronProgress", facts.source?.source)
        assertEquals(100, pullUp?.muscles?.sumOf { it.workShare ?: 0 })
        assertEquals(null, repository.findFacts(ExerciseId("no-existe")))
    }
}
