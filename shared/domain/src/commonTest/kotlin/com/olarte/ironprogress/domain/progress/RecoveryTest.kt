package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.bench
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.catalog
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.curl
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.feedback
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.session
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.squat
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class RecoveryTest {
    private val now = Instant.parse("2026-11-12T18:00:00Z")

    private fun near(expected: Double, actual: Double, tolerance: Double = 0.01) =
        assertTrue(abs(expected - actual) < tolerance, "esperaba $expected y salió $actual")

    @Test
    fun fatigueHalvesEveryHalfLife() {
        // Pecho es grande: 48 h. Cuatro series hace 48 h valen 2; hace 96 h, 1.
        near(2.0, Recovery.fatigue(List(4) { (now - 48.hours) to 1.0 }, 48, now))
        near(1.0, Recovery.fatigue(List(4) { (now - 96.hours) to 1.0 }, 48, now))
        // Bíceps es pequeño: 24 h.
        near(2.0, Recovery.fatigue(List(4) { (now - 24.hours) to 1.0 }, 24, now))
    }

    @Test
    fun levelsAndHeatFollowTheThresholds() {
        assertEquals(RecoveryLevel.FRESH, Recovery.level(2.9))
        assertEquals(RecoveryLevel.RECOVERING, Recovery.level(3.0))
        assertEquals(RecoveryLevel.RECOVERING, Recovery.level(6.0))
        assertEquals(RecoveryLevel.LOADED, Recovery.level(6.1))
        assertEquals(listOf(0, 1, 2, 3, 4), listOf(0.2, 2.0, 3.5, 5.0, 7.0).map { Recovery.heat(it) })
    }

    @Test
    fun readyAtIsWhenFatigueDropsBelowThree() {
        // 6 en pecho: una vida media (48 h) para llegar a 3.
        val ready = assertNotNull(Recovery.readyAt(6.0, 48, now))
        near(48.0, (ready - now).inWholeMinutes / 60.0, 0.05)
        assertNull(Recovery.readyAt(2.0, 48, now))
    }

    @Test
    fun aHeavyChestDayYesterdayLeavesChestLoadedTricepsRecoveringAndLegsFresh() {
        // 8 series de press ayer: pecho 8 → 8 · 0,5^(24/48) = 5,66 (ámbar); tríceps 4 → 4 · 0,5 = 2 (verde).
        val yesterday = session("y", now - 24.hours, listOf(Triple(bench, 8, 0)))
        val states = Recovery.of(listOf(yesterday), catalog, now)
        val chest = states.getValue(Muscle.CHEST)
        near(5.66, chest.fatigue)
        assertEquals(RecoveryLevel.RECOVERING, chest.level)
        assertEquals(3, chest.heat)
        assertNotNull(chest.readyAt)
        assertEquals(now - 24.hours, chest.lastTrainedAt)
        assertEquals(RecoveryLevel.FRESH, states.getValue(Muscle.TRICEPS).level)
        assertEquals(0, states.getValue(Muscle.QUADS).heat)
        assertEquals(Muscle.entries.size, states.size)
    }

    @Test
    fun stackedSessionsCanPushAMuscleIntoRed() {
        val twoDaysAgo = session("a", now - 48.hours, listOf(Triple(squat, 6, 0)))
        val today = session("b", now - 2.hours, listOf(Triple(squat, 6, 0)))
        val quads = Recovery.of(listOf(twoDaysAgo, today), catalog, now).getValue(Muscle.QUADS)
        assertEquals(RecoveryLevel.LOADED, quads.level)
        assertEquals(4, quads.heat)
    }

    @Test
    fun setsOlderThanTwoWeeksAndPendingSetsDoNotCount() {
        val old = session("o", now - 15.days, listOf(Triple(curl, 20, 0)))
        val pending = session("p", now - 1.hours, listOf(Triple(curl, 0, 6)), finished = false)
        val biceps = Recovery.of(listOf(old, pending), catalog, now).getValue(Muscle.BICEPS)
        assertEquals(0.0, biceps.fatigue)
        assertNull(biceps.lastTrainedAt)
    }

    @Test
    fun recentSorenessKeepsAMuscleAtLeastAmber() {
        val sore = feedback("x", Muscle.BICEPS, now - 3.days, soreness = 2, sorenessAt = now - 10.hours)
        val states = Recovery.of(emptyList(), catalog, now, listOf(sore))
        val biceps = states.getValue(Muscle.BICEPS)
        assertEquals(RecoveryLevel.RECOVERING, biceps.level)
        assertTrue(biceps.sore)
        assertEquals(2, biceps.heat)
        assertEquals(now - 10.hours + 48.hours, biceps.readyAt)

        val oldSoreness = sore.copy(sorenessAt = now - 3.days)
        assertEquals(RecoveryLevel.FRESH, Recovery.of(emptyList(), catalog, now, listOf(oldSoreness)).getValue(Muscle.BICEPS).level)
    }

    @Test
    fun notReadyListsTheMostLoadedFirst() {
        val legs = session("l", now - 6.hours, listOf(Triple(squat, 8, 0)))
        val push = session("p", now - 30.hours, listOf(Triple(bench, 6, 0)))
        val list = Recovery.notReady(Recovery.of(listOf(legs, push), catalog, now))
        assertEquals(Muscle.QUADS, list.first().muscle)
        assertTrue(list.all { it.level != RecoveryLevel.FRESH })
    }

    @Test
    fun warmupTargetsTheFirstWeightedExerciseOfAMuscleThatIsStillRecovering() {
        val yesterday = session("y", now - 20.hours, listOf(Triple(bench, 8, 0)))
        val recovery = Recovery.of(listOf(yesterday), catalog, now)
        val today = session("t", now, listOf(Triple(squat, 0, 3), Triple(bench, 0, 3)), finished = false)
        val targets = RecoveryWarmups.targets(today, catalog, recovery)
        assertEquals(setOf(today.exercises[1].id), targets.keys)
        assertEquals(Muscle.CHEST, targets.getValue(today.exercises[1].id).muscle)
        assertEquals(1, RecoveryWarmups.extraLight(RecoveryLevel.RECOVERING))
        assertEquals(2, RecoveryWarmups.extraLight(RecoveryLevel.LOADED))
        assertEquals(0, RecoveryWarmups.extraLight(RecoveryLevel.FRESH))
    }

    @Test
    fun noWarmupNoteOnceTheExerciseStartedOrHasNoWeight() {
        val yesterday = session("y", now - 20.hours, listOf(Triple(bench, 8, 0)))
        val recovery = Recovery.of(listOf(yesterday), catalog, now)
        val started = session("t", now, listOf(Triple(bench, 1, 2)), finished = false)
        assertEquals(emptyMap(), RecoveryWarmups.targets(started, catalog, recovery))
        val bodyweight = session("u", now, listOf(Triple(bench, 0, 3)), finished = false, weightKg = 0.0)
        assertEquals(emptyMap(), RecoveryWarmups.targets(bodyweight, catalog, recovery))
    }
}
