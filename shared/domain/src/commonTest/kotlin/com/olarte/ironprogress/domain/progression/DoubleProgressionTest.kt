package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val DONE = Instant.parse("2026-11-01T12:00:00Z")

internal fun done(weight: Double?, reps: Int?, rir: Int? = null, type: SetType = SetType.WORKING, duration: Int? = null) = WorkoutSet(
    WorkoutSetId("s-$weight-$reps-$rir-$type"),
    SessionExerciseId("e"),
    0,
    setType = type,
    weightKg = weight,
    reps = reps,
    rir = rir,
    durationSeconds = duration,
    completedAt = DONE,
)

internal fun session(vararg sets: WorkoutSet, deload: Boolean = false) = PastPerformance(sets.toList(), deload)

private fun close(expected: Double, actual: Double?) =
    assertTrue(actual != null && abs(expected - actual) < 0.01, "Esperaba $expected y fue $actual")

class OneRepMaxTest {
    @Test
    fun epleyAddsTheRepsInReserve() {
        close(60 * (1 + 12 / 30.0), OneRepMax.estimate(60.0, 10, 2))
        close(100 * (1 + 5 / 30.0), OneRepMax.estimate(100.0, 5))
        assertNull(OneRepMax.estimate(60.0, 12, 1), "Más de 12 entre reps y RIR ya no se estima")
        assertNull(OneRepMax.estimate(0.0, 5))
        assertNull(OneRepMax.estimate(60.0, 0))
    }

    @Test
    fun onlyLoadedWorkingSetsCount() {
        val sets = listOf(done(60.0, 10, 2), done(40.0, 10, type = SetType.WARMUP), done(62.5, 8, 1))
        // 60 × 10 con RIR 2 estima más que 62,5 × 8 con RIR 1.
        close(60 * (1 + 12 / 30.0), sets.bestOneRepMax(TrackingType.WEIGHT_REPS))
        assertNull(sets.bestOneRepMax(TrackingType.ASSISTED_BODYWEIGHT), "En las asistidas el peso es ayuda")
        assertNull(done(60.0, 10).copy(completedAt = null).estimatedOneRepMax(TrackingType.WEIGHT_REPS))
    }
}

class WeightStepsTest {
    @Test
    fun barbellMovesWithTheSmallestPairOfPlates() {
        assertEquals(62.5, WeightSteps.nextUp(60.0, Equipment.BARBELL, WeightUnit.KG))
        assertEquals(140.0, WeightSteps.nextUp(135.0, Equipment.BARBELL, WeightUnit.LB))
        // 62,5 kg se ven como 137,8 lb: el siguiente armable en lb es 140.
        assertEquals(140.0, WeightSteps.nextUp(WeightUnits.fromKg(62.5, WeightUnit.LB), Equipment.BARBELL, WeightUnit.LB))
        assertEquals(57.5, WeightSteps.nextDown(60.0, Equipment.BARBELL, WeightUnit.KG))
    }

    @Test
    fun dumbbellsGoToTheNextPair() {
        assertEquals(12.0, WeightSteps.nextUp(10.0, Equipment.DUMBBELL, WeightUnit.KG))
        assertEquals(7.0, WeightSteps.nextUp(6.0, Equipment.DUMBBELL, WeightUnit.KG))
        assertEquals(55.0, WeightSteps.nextUp(50.0, Equipment.DUMBBELL, WeightUnit.KG))
        assertEquals(25.0, WeightSteps.nextUp(20.0, Equipment.DUMBBELL, WeightUnit.LB))
        assertEquals(12.0, WeightSteps.nextUp(11.0, Equipment.DUMBBELL, WeightUnit.KG), "Un peso raro sube al siguiente par que existe")
    }

    @Test
    fun machinesAndCablesUseTheirStack() {
        assertEquals(45.0, WeightSteps.nextUp(40.0, Equipment.MACHINE, WeightUnit.KG))
        assertEquals(42.5, WeightSteps.nextUp(40.0, Equipment.MACHINE, WeightUnit.KG, young = true))
        assertEquals(22.5, WeightSteps.nextUp(20.0, Equipment.CABLE, WeightUnit.KG))
        assertEquals(110.0, WeightSteps.nextUp(100.0, Equipment.MACHINE, WeightUnit.LB))
    }

    @Test
    fun roundingNeverGoesBelowTheBar() {
        assertEquals(
            20.0,
            WeightSteps.roundDown(15.0, Equipment.BARBELL, WeightUnit.KG, WeightSteps.minimum(Equipment.BARBELL, WeightUnit.KG)),
        )
        assertEquals(55.0, WeightSteps.roundDown(55.5, Equipment.BARBELL, WeightUnit.KG))
        assertEquals(16.0, WeightSteps.roundUp(15.0, Equipment.KETTLEBELL, WeightUnit.KG))
    }
}

class DoubleProgressionTest {
    private val bench = ProgressionInput(TrackingType.WEIGHT_REPS, Equipment.BARBELL, range = RepRange(8, 12, 2), plannedSets = 3)

    @Test
    fun allSetsAtTheTopWithTheTargetRirGoUpAndBackToTheMinimum() {
        // El ejemplo de HANDOFF 11.2: 3 × 12 con 60 kg y RIR 2, hoy 62,5 kg entre 8 y 12.
        val result = DoubleProgression.suggest(
            bench.copy(history = listOf(session(done(60.0, 12, 2), done(60.0, 12, 2), done(60.0, 12, 3)))),
        )
        assertEquals(SuggestionReason.INCREASE, result.reason)
        assertEquals(62.5, result.weightKg)
        assertEquals(listOf(8, 8, 8), result.reps)
        assertEquals(PreviousSummary(3, 12, 12, 60.0, 2), result.previous)
    }

    @Test
    fun topButAtFailureRepeatsTheWeight() {
        val result = DoubleProgression.suggest(
            bench.copy(history = listOf(session(done(60.0, 12, 0), done(60.0, 12, 1), done(60.0, 12, 0)))),
        )
        assertEquals(SuggestionReason.REPEAT_HARD, result.reason)
        assertEquals(60.0, result.weightKg)
        assertEquals(listOf(12, 12, 12), result.reps)
    }

    @Test
    fun withoutTargetRirOnlyRirZeroCountsAsFailure() {
        val noTarget = bench.copy(range = RepRange(8, 12))
        assertEquals(
            SuggestionReason.INCREASE,
            DoubleProgression.suggest(noTarget.copy(history = listOf(session(done(60.0, 12, 1), done(60.0, 12, 1))))).reason,
        )
        assertEquals(
            SuggestionReason.REPEAT_HARD,
            DoubleProgression.suggest(noTarget.copy(history = listOf(session(done(60.0, 12, 0), done(60.0, 12, 1))))).reason,
        )
    }

    @Test
    fun basicModeWithoutRirStillProgresses() {
        val result = DoubleProgression.suggest(bench.copy(history = listOf(session(done(60.0, 12), done(60.0, 12), done(60.0, 12)))))
        assertEquals(SuggestionReason.INCREASE, result.reason)
    }

    @Test
    fun setsInsideTheRangeAddOneRepWhereTheyMissed() {
        val result = DoubleProgression.suggest(
            bench.copy(history = listOf(session(done(60.0, 12, 2), done(60.0, 10, 2), done(60.0, 9, 1)))),
        )
        assertEquals(SuggestionReason.ADD_REP, result.reason)
        assertEquals(60.0, result.weightKg)
        assertEquals(listOf(12, 11, 10), result.reps)
    }

    @Test
    fun oneSessionBelowTheMinimumAimsForTheMinimum() {
        val result = DoubleProgression.suggest(bench.copy(history = listOf(session(done(60.0, 8), done(60.0, 7), done(60.0, 6)))))
        assertEquals(SuggestionReason.AIM_MIN, result.reason)
        assertEquals(listOf(9, 8, 8), result.reps)
    }

    @Test
    fun twoSessionsBelowTheMinimumDeloadBetweenFiveAndTenPercent() {
        val low = session(done(60.0, 7), done(60.0, 6), done(60.0, 6))
        val result = DoubleProgression.suggest(bench.copy(history = listOf(low, low)))
        assertEquals(SuggestionReason.DELOAD, result.reason)
        assertEquals(55.0, result.weightKg)
        assertEquals(listOf(8, 8, 8), result.reps)
        val drop = 1 - result.weightKg!! / 60.0
        assertTrue(drop in 0.05..0.10, "Bajó $drop")
    }

    @Test
    fun deloadAlwaysDropsAtLeastOneStep() {
        val low = session(done(20.0, 5), done(20.0, 5))
        val cable = ProgressionInput(TrackingType.WEIGHT_REPS, Equipment.CABLE, range = RepRange(10, 15), plannedSets = 2)
        assertEquals(17.5, DoubleProgression.suggest(cable.copy(history = listOf(low, low))).weightKg)
        // En barra nunca baja de la barra vacía.
        assertEquals(20.0, DoubleProgression.suggest(bench.copy(history = listOf(low, low))).weightKg)
    }

    @Test
    fun noHistoryIsATrialSet() {
        val result = DoubleProgression.suggest(bench)
        assertEquals(SuggestionReason.FIRST_TIME, result.reason)
        assertNull(result.weightKg)
        assertEquals(listOf(8, 8, 8), result.reps)
    }

    @Test
    fun withoutRangeItRepeatsLastTime() {
        val result = DoubleProgression.suggest(
            bench.copy(range = RepRange(), plannedSets = 2, history = listOf(session(done(50.0, 10), done(50.0, 9)))),
        )
        assertEquals(SuggestionReason.SAME_AS_LAST, result.reason)
        assertEquals(50.0, result.weightKg)
        assertEquals(listOf(10, 9), result.reps)
    }

    @Test
    fun timeBasedExercisesRepeatLastTime() {
        val plank = ProgressionInput(TrackingType.DURATION, Equipment.BODYWEIGHT, range = RepRange(8, 12), plannedSets = 1)
        val result = DoubleProgression.suggest(plank.copy(history = listOf(session(done(null, null, duration = 45)))))
        assertEquals(SuggestionReason.SAME_AS_LAST, result.reason)
        assertNull(result.weightKg)
    }

    @Test
    fun poundsGoUpFiveAtATime() {
        val lb = bench.copy(unit = WeightUnit.LB)
        val last = WeightUnits.toKg(135.0, WeightUnit.LB)
        val result = DoubleProgression.suggest(lb.copy(history = listOf(session(done(last, 12), done(last, 12), done(last, 12)))))
        close(140.0, result.weightKg?.let { WeightUnits.fromKg(it, WeightUnit.LB) })
    }

    @Test
    fun assistedExercisesProgressByRemovingHelp() {
        val assisted = ProgressionInput(TrackingType.ASSISTED_BODYWEIGHT, Equipment.MACHINE, range = RepRange(6, 10), plannedSets = 2)
        val up = DoubleProgression.suggest(assisted.copy(history = listOf(session(done(30.0, 10), done(30.0, 10)))))
        assertEquals(SuggestionReason.INCREASE, up.reason)
        assertEquals(25.0, up.weightKg)
        val low = session(done(30.0, 4), done(30.0, 4))
        assertEquals(35.0, DoubleProgression.suggest(assisted.copy(history = listOf(low, low))).weightKg)
    }

    @Test
    fun bodyweightWithoutLoadAddsRepsUntilTheTop() {
        val pushUp = ProgressionInput(TrackingType.REPS, Equipment.BODYWEIGHT, range = RepRange(8, 15), plannedSets = 3)
        val result = DoubleProgression.suggest(pushUp.copy(history = listOf(session(done(null, 15), done(null, 12), done(null, 10)))))
        assertEquals(SuggestionReason.ADD_REP, result.reason)
        assertEquals(listOf(15, 13, 11), result.reps)
        assertNull(result.weightKg)
        val top = DoubleProgression.suggest(pushUp.copy(history = listOf(session(done(null, 15), done(null, 15), done(null, 15)))))
        assertEquals(SuggestionReason.AT_TOP, top.reason)
    }

    @Test
    fun adjustmentDayNeverAddsWeight() {
        val result = DoubleProgression.suggest(
            bench.copy(adjustmentActive = true, history = listOf(session(done(60.0, 12), done(60.0, 12), done(60.0, 12)))),
        )
        assertEquals(SuggestionReason.ADJUSTMENT_HOLD, result.reason)
        assertEquals(60.0, result.weightKg)
    }

    @Test
    fun moreSetsTodayThanLastTimeReuseTheLastOne() {
        val result = DoubleProgression.suggest(bench.copy(plannedSets = 4, history = listOf(session(done(60.0, 10), done(60.0, 9)))))
        assertEquals(listOf(11, 10, 10, 10), result.reps)
    }

    @Test
    fun warmupsDoNotCount() {
        val result = DoubleProgression.suggest(
            bench.copy(history = listOf(session(done(20.0, 5, type = SetType.WARMUP), done(60.0, 12), done(60.0, 12), done(60.0, 12)))),
        )
        assertEquals(SuggestionReason.INCREASE, result.reason)
        assertEquals(3, result.previous?.sets)
    }
}

class YoungModeTest {
    private val bench =
        ProgressionInput(TrackingType.WEIGHT_REPS, Equipment.BARBELL, range = RepRange(8, 12, 1), plannedSets = 2, young = true)

    @Test
    fun noFailureAndAtLeastTwoInReserve() {
        assertFalse(SetType.FAILURE in YoungRules.allowedSetTypes(young = true))
        assertTrue(SetType.FAILURE in YoungRules.allowedSetTypes(young = false))
        assertEquals(2, YoungRules.effectiveTargetRir(1, young = true))
        assertEquals(2, YoungRules.effectiveTargetRir(null, young = true))
        assertEquals(1, YoungRules.effectiveTargetRir(1, young = false))
        assertFalse(YoungRules.showsOneRepMax(young = true))
    }

    @Test
    fun goesUpOnlyAfterTwoSessionsAtTheTop() {
        val top = session(done(40.0, 12, 2), done(40.0, 12, 2))
        val once = DoubleProgression.suggest(bench.copy(history = listOf(top, session(done(40.0, 11, 2), done(40.0, 10, 2)))))
        assertEquals(SuggestionReason.YOUNG_WAIT, once.reason)
        assertEquals(40.0, once.weightKg)
        val twice = DoubleProgression.suggest(bench.copy(history = listOf(top, top)))
        assertEquals(SuggestionReason.INCREASE, twice.reason)
        assertEquals(42.5, twice.weightKg)
    }

    @Test
    fun rirOneIsTooCloseToFailureForYoungPeople() {
        val close = session(done(40.0, 12, 1), done(40.0, 12, 1))
        assertEquals(SuggestionReason.REPEAT_HARD, DoubleProgression.suggest(bench.copy(history = listOf(close, close))).reason)
    }
}
