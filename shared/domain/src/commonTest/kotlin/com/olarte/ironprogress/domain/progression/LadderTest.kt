package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseProgression
import com.olarte.ironprogress.domain.catalog.TrackingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Escaleras cuando no hay peso que subir (HANDOFF 11.10, F2-12). */
class LadderTest {
    private val ladder = ExerciseProgression(easier = ExerciseId("knee-push-up"), harder = ExerciseId("decline-push-up"))
    private val pushUp =
        ProgressionInput(TrackingType.REPS, Equipment.BODYWEIGHT, range = RepRange(8, 15, 2), plannedSets = 3, ladder = ladder)
    private val top = session(done(null, 15, 2), done(null, 15, 2), done(null, 15, 3))
    private val low = session(done(null, 7), done(null, 6), done(null, 6))

    @Test
    fun twoSessionsAtTheTopMoveToTheHarderVariationAtTheMinimum() {
        val result = DoubleProgression.suggest(pushUp.copy(history = listOf(top, top)))
        assertEquals(SuggestionReason.LADDER_HARDER, result.reason)
        assertEquals(ExerciseId("decline-push-up"), result.ladderTarget)
        // Si no cambia, sigue en el tope; la variante nueva empieza en el mínimo del rango.
        assertEquals(listOf(15, 15, 15), result.reps)
        assertEquals(8, result.recordFor(0).repsMin)
        assertNull(result.weightKg)
        assertEquals("decline-push-up", result.recordFor(0).ladderTarget)
    }

    @Test
    fun oneSessionAtTheTopSaysOneMore() {
        val result = DoubleProgression.suggest(pushUp.copy(history = listOf(top, session(done(null, 13), done(null, 12)))))
        assertEquals(SuggestionReason.LADDER_ONE_MORE, result.reason)
        assertEquals(listOf(15, 15, 15), result.reps)
        assertEquals(ExerciseId("decline-push-up"), result.ladderTarget)
    }

    @Test
    fun theTopNeedsTheTargetRir() {
        val failure = session(done(null, 15, 0), done(null, 15, 0), done(null, 15, 0))
        val result = DoubleProgression.suggest(pushUp.copy(history = listOf(failure, failure)))
        assertEquals(SuggestionReason.ADD_REP, result.reason)
        assertNull(result.ladderTarget)
    }

    @Test
    fun twoSessionsBelowTheMinimumMoveToTheEasierVariation() {
        val result = DoubleProgression.suggest(pushUp.copy(history = listOf(low, low)))
        assertEquals(SuggestionReason.LADDER_EASIER, result.reason)
        assertEquals(ExerciseId("knee-push-up"), result.ladderTarget)
        assertEquals(listOf(8, 8, 8), result.reps)
    }

    @Test
    fun withoutAStepOnTheLadderItStaysWithReps() {
        val noLadder = pushUp.copy(ladder = ExerciseProgression(null, null))
        assertEquals(SuggestionReason.AT_TOP, DoubleProgression.suggest(noLadder.copy(history = listOf(top, top))).reason)
        assertEquals(SuggestionReason.AIM_MIN, DoubleProgression.suggest(noLadder.copy(history = listOf(low, low))).reason)
    }

    @Test
    fun assistedWithoutHelpLeftClimbsToTheRealThing() {
        val assisted = ProgressionInput(
            TrackingType.ASSISTED_BODYWEIGHT,
            Equipment.MACHINE,
            range = RepRange(6, 10),
            plannedSets = 2,
            ladder = ExerciseProgression(ExerciseId("lat-pulldown"), ExerciseId("pull-up")),
        )
        val noHelp = session(done(0.0, 10), done(0.0, 10))
        assertEquals(SuggestionReason.LADDER_HARDER, DoubleProgression.suggest(assisted.copy(history = listOf(noHelp, noHelp))).reason)
        val withHelp = session(done(20.0, 10), done(20.0, 10))
        assertEquals(SuggestionReason.INCREASE, DoubleProgression.suggest(assisted.copy(history = listOf(withHelp, withHelp))).reason)
    }

    @Test
    fun weightedExercisesNeverJumpVariations() {
        val bench = ProgressionInput(TrackingType.WEIGHT_REPS, Equipment.BARBELL, range = RepRange(8, 12), plannedSets = 1, ladder = ladder)
        val result = DoubleProgression.suggest(bench.copy(history = listOf(session(done(60.0, 12)), session(done(60.0, 12)))))
        assertEquals(SuggestionReason.INCREASE, result.reason)
        assertNull(result.ladderTarget)
    }
}
