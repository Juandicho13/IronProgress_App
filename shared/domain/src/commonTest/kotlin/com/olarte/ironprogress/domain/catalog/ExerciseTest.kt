package com.olarte.ironprogress.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExerciseTest {

    @Test
    fun requiresAtLeastOnePrimaryMuscleUnlessCardio() {
        assertFailsWith<IllegalArgumentException> {
            Exercise(
                id = ExerciseId("barbell-bench-press"),
                name = LocalizedText(es = "Press banca con barra", en = "Barbell Bench Press"),
                trackingType = TrackingType.WEIGHT_REPS,
                equipment = Equipment.BARBELL,
                pattern = MovementPattern.HORIZONTAL_PUSH,
                muscles = listOf(ExerciseMuscle(Muscle.TRICEPS, MuscleRole.SECONDARY)),
            )
        }
    }

    @Test
    fun allowsCardioWithoutPrimaryMuscles() {
        val cardio = Exercise(
            id = ExerciseId("stationary-bike"),
            name = LocalizedText(es = "Bicicleta estática", en = "Stationary Bike"),
            trackingType = TrackingType.DISTANCE_DURATION,
            equipment = Equipment.MACHINE,
            pattern = MovementPattern.CARDIO,
            muscles = emptyList(),
        )

        assertEquals(emptyList(), cardio.primaryMuscles)
    }

    @Test
    fun rejectsDuplicateMuscles() {
        assertFailsWith<IllegalArgumentException> {
            Exercise(
                id = ExerciseId("barbell-row"),
                name = LocalizedText(es = "Remo con barra", en = "Barbell Row"),
                trackingType = TrackingType.WEIGHT_REPS,
                equipment = Equipment.BARBELL,
                pattern = MovementPattern.HORIZONTAL_PULL,
                muscles = listOf(
                    ExerciseMuscle(Muscle.LATS, MuscleRole.PRIMARY),
                    ExerciseMuscle(Muscle.LATS, MuscleRole.SECONDARY),
                ),
            )
        }
    }

    @Test
    fun computesPrimarySecondaryAndSetWeightForMuscle() {
        val benchPress = Exercise(
            id = ExerciseId("barbell-bench-press"),
            name = LocalizedText(es = "Press banca con barra", en = "Barbell Bench Press"),
            trackingType = TrackingType.WEIGHT_REPS,
            equipment = Equipment.BARBELL,
            pattern = MovementPattern.HORIZONTAL_PUSH,
            muscles = listOf(
                ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY),
                ExerciseMuscle(Muscle.TRICEPS, MuscleRole.SECONDARY),
                ExerciseMuscle(Muscle.SHOULDERS, MuscleRole.SECONDARY),
            ),
        )

        assertEquals(listOf(Muscle.CHEST), benchPress.primaryMuscles)
        assertEquals(listOf(Muscle.TRICEPS, Muscle.SHOULDERS), benchPress.secondaryMuscles)
        assertEquals(1.0, benchPress.setWeightFor(Muscle.CHEST))
        assertEquals(0.5, benchPress.setWeightFor(Muscle.TRICEPS))
        assertEquals(0.0, benchPress.setWeightFor(Muscle.QUADS))
    }

    @Test
    fun localizedTextResolvesSpanishAndFallsBackToEnglish() {
        val name = LocalizedText(es = "Sentadilla con barra", en = "Barbell Squat")

        assertEquals("Sentadilla con barra", name.resolve("es"))
        assertEquals("Sentadilla con barra", name.resolve("es-CO"))
        assertEquals("Barbell Squat", name.resolve("en"))
        assertEquals("Barbell Squat", name.resolve("pt-BR"))
    }

    @Test
    fun muscleIdsAreStableAndLookupWorks() {
        assertEquals("lower_back", Muscle.LOWER_BACK.id)
        assertEquals(Muscle.LOWER_BACK, Muscle.fromId("lower_back"))
        assertNull(Muscle.fromId("neck"))
    }
}
