package com.olarte.ironprogress.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Los datos de F1-06 cuadran con el catálogo: nadie se queda sin exigencia ni suma mal. */
class SeedCatalogFactsTest {

    private val ids = SeedCatalog.exercises.map { it.id.value }.toSet()

    @Test
    fun everyExerciseHasItsDemand() {
        assertEquals(ids, SeedCatalogFacts.demands.keys)
    }

    @Test
    fun sharesCoverTheSameMusclesAndAddUpTo100() {
        for (exercise in SeedCatalog.exercises) {
            if (exercise.muscles.isEmpty()) continue
            assertTrue(exercise.muscles.all { it.workShare != null }, "${exercise.id.value} sin porcentaje")
            assertEquals(100, exercise.muscles.sumOf { it.workShare ?: 0 }, exercise.id.value)
            val main = exercise.muscles.maxBy { it.workShare ?: 0 }.muscle
            assertTrue(main in exercise.primaryMuscles, "${exercise.id.value}: el de más porcentaje no es primario")
        }
        assertTrue(SeedCatalogFacts.workShares.keys.all { it in ids })
    }

    @Test
    fun laddersPointToRealExercisesAndNeverToThemselves() {
        for ((id, ladder) in SeedCatalogFacts.progressions) {
            assertTrue(id in ids, id)
            listOfNotNull(ladder.easier, ladder.harder).forEach {
                assertTrue(it.value in ids && it.value != id, "$id -> ${it.value}")
            }
        }
    }

    @Test
    fun theHeaviestLiftsAreTheMostDemanding() {
        val five = SeedCatalogFacts.demands.filterValues { it.overall == 5 }.keys
        assertEquals(
            setOf(
                "barbell-back-squat",
                "barbell-front-squat",
                "romanian-deadlift",
                "barbell-good-morning",
                "conventional-deadlift",
                "sumo-deadlift",
            ),
            five,
        )
    }
}
