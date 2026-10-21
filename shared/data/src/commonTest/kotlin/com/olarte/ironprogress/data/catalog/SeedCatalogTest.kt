package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.domain.catalog.ExerciseQuery
import com.olarte.ironprogress.domain.catalog.ExerciseSearch
import com.olarte.ironprogress.domain.catalog.Muscle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SeedCatalogTest {

    @Test
    fun hasExpectedCountAndUniqueSlugIds() {
        val exercises = SeedCatalog.exercises
        val ids = exercises.map { it.id.value }

        assertEquals(77, exercises.size)
        assertEquals(exercises.size, ids.distinct().size)
        assertTrue(ids.all { it.matches(Regex("^[a-z0-9-]+$")) })
    }

    @Test
    fun coversEveryMuscleAsPrimaryAtLeastOnce() {
        val coveredMuscles = SeedCatalog.exercises
            .flatMap { it.primaryMuscles }
            .toSet()

        assertEquals(Muscle.entries.toSet(), coveredMuscles)
    }

    @Test
    fun hasNonBlankBilingualNames() {
        assertTrue(
            SeedCatalog.exercises.all {
                it.name.es.isNotBlank() && it.name.en.isNotBlank()
            },
        )
    }

    @Test
    fun tricepsFilterLeavesSixExercisesAndSearchFindsJalonAndBench() {
        val triceps = ExerciseSearch.filter(
            exercises = SeedCatalog.exercises,
            query = ExerciseQuery(muscle = Muscle.TRICEPS),
            languageCode = "es",
        )
        val jalon = ExerciseSearch.filter(
            exercises = SeedCatalog.exercises,
            query = ExerciseQuery(text = "jalon"),
            languageCode = "es",
        )
        val bench = ExerciseSearch.filter(
            exercises = SeedCatalog.exercises,
            query = ExerciseQuery(text = "bench"),
            languageCode = "es",
        )

        assertEquals(6, triceps.size)
        assertTrue(jalon.any { it.name.es == "Jalón al pecho" })
        assertTrue(bench.any { it.id.value == "barbell-bench-press" })
    }

    @Test
    fun repositoryObservesAndFindsExerciseById() = runTest {
        val repository = SeedExerciseRepository()

        val all = repository.observeExercises().first()
        val found = repository.findExercise(all.first().id)

        assertEquals(77, all.size)
        assertNotNull(found)
    }
}
