package com.olarte.ironprogress.domain.routine

import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseModeTest {

    @Test
    fun theModeComesFromTheFieldsThatAlreadyExist() {
        assertEquals(ExerciseMode.REST, ExerciseMode.of(null, null, null))
        assertEquals(ExerciseMode.REST, ExerciseMode.of(null, 90, null))
        assertEquals(ExerciseMode.NO_REST, ExerciseMode.of(null, 0, null))
        assertEquals(ExerciseMode.TIMED, ExerciseMode.of(null, 0, 45))
        assertEquals(ExerciseMode.SUPERSET, ExerciseMode.of(1, 90, null))
    }

    @Test
    fun changingTheModeKeepsWhatMakesSense() {
        assertEquals(ExerciseModeValues(0, null), ExerciseMode.NO_REST.values(currentRest = 90, currentTimed = null))
        assertEquals(ExerciseModeValues(null, null), ExerciseMode.REST.values(currentRest = 0, currentTimed = 30))
        assertEquals(ExerciseModeValues(120, null), ExerciseMode.REST.values(currentRest = 90, currentTimed = null, restSeconds = 120))
        assertEquals(ExerciseModeValues(90, 30), ExerciseMode.TIMED.values(currentRest = 90, currentTimed = null))
        assertEquals(ExerciseModeValues(90, 45), ExerciseMode.TIMED.values(currentRest = 90, currentTimed = 30, timedSeconds = 45))
        assertEquals(ExerciseModeValues(60, null), ExerciseMode.SUPERSET.values(currentRest = 60, currentTimed = 20))
    }

    @Test
    fun aRoutineExerciseCarriesItsModeIntoTheSession() {
        val planned = RoutineExercise(
            RoutineExerciseId("e"),
            BlockId("b"),
            com.olarte.ironprogress.domain.catalog.ExerciseId("plank"),
            0,
            restSeconds = 0,
            timedSeconds = 40,
        )
        assertEquals(ExerciseMode.TIMED, planned.mode)
    }
}
