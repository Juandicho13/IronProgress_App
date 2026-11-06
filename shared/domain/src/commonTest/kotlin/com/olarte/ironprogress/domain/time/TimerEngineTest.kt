package com.olarte.ironprogress.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimerEngineTest {

    private val start = 1_700_000_000_000L

    @Test
    fun restCountsDownFromTheStartInstant() {
        val plan = TimerPlans.rest(90)

        val state = plan.stateAt(start + 72_000, start)

        assertEquals(SegmentKind.REST, state.kind)
        assertEquals(18_000, state.remainingInSegmentMs)
        assertEquals(18, state.displaySecondsLeft)
        assertFalse(state.isFinished)
        assertTrue(plan.stateAt(start + 90_000, start).isFinished)
    }

    @Test
    fun twentyRestsWithPausesAndAdjustmentsStayExactToTheMillisecond() {
        repeat(20) { i ->
            val s = start + i * 123_457L
            val plan = TimerPlans.rest(90)
            val edits = listOf(
                TimerEdit.Pause(s + 30_000),
                TimerEdit.Resume(s + 35_000),
                TimerEdit.Adjust(s + 40_000, deltaMs = 15_000),
            )
            // 90 s + 5 s de pausa + 15 s sumados: termina a los 110 s del inicio.
            assertEquals(s + 110_000, plan.segmentEndsAtMs(s + 50_000, s, edits))
            assertEquals(1, plan.stateAt(s + 109_999, s, edits).remainingInSegmentMs)
            assertFalse(plan.stateAt(s + 109_999, s, edits).isFinished)
            assertTrue(plan.stateAt(s + 110_000, s, edits).isFinished)
        }
    }

    @Test
    fun pausedClockDoesNotMove() {
        val plan = TimerPlans.rest(60)
        val edits = listOf(TimerEdit.Pause(start + 10_000))

        val a = plan.stateAt(start + 20_000, start, edits)
        val b = plan.stateAt(start + 500_000, start, edits)

        assertTrue(a.isPaused)
        assertEquals(50_000, a.remainingInSegmentMs)
        assertEquals(a.remainingInSegmentMs, b.remainingInSegmentMs)
        assertNull(plan.segmentEndsAtMs(start + 20_000, start, edits))
    }

    @Test
    fun subtractingMoreThanWhatIsLeftEndsTheRestRightThere() {
        val plan = TimerPlans.rest(90)
        val edits = listOf(TimerEdit.Adjust(start + 30_000, deltaMs = -75_000))

        assertFalse(plan.stateAt(start + 29_999, start, edits).isFinished)
        assertTrue(plan.stateAt(start + 30_000, start, edits).isFinished)
    }

    @Test
    fun skipEndsTheCurrentSegment() {
        val plan = TimerPlans.intervals(workSeconds = 20, restSeconds = 10, rounds = 2)
        val edits = listOf(TimerEdit.Skip(start + 5_000))

        val state = plan.stateAt(start + 5_000, start, edits)

        assertEquals(SegmentKind.REST, state.kind)
        assertEquals(0, state.elapsedInSegmentMs)
    }

    @Test
    fun emomOfTenMinutes() {
        val plan = TimerPlans.emom(10)

        val state = plan.stateAt(start + 270_000, start)

        assertEquals(5, state.round)
        assertEquals(30_000, state.remainingInSegmentMs)
        assertEquals(600_000, plan.totalDurationMs)
        assertTrue(plan.stateAt(start + 600_000, start).isFinished)
    }

    @Test
    fun amrapOfTwelveMinutesShowsWhatIsLeft() {
        val plan = TimerPlans.amrap(12 * 60)

        // A los 4:18 quedan 7:42, como en la pantalla del AMRAP.
        assertEquals(462, plan.stateAt(start + 258_000, start).displaySecondsLeft)
    }

    @Test
    fun tabataTwentyTenForEightRounds() {
        val plan = TimerPlans.intervals(workSeconds = 20, restSeconds = 10, rounds = 8)

        assertEquals(230_000, plan.totalDurationMs)
        val resting = plan.stateAt(start + 25_000, start)
        assertEquals(SegmentKind.REST, resting.kind)
        assertEquals(1, resting.round)
        assertEquals(5_000, resting.remainingInSegmentMs)
        val working = plan.stateAt(start + 30_000, start)
        assertEquals(SegmentKind.WORK, working.kind)
        assertEquals(2, working.round)
        assertTrue(plan.stateAt(start + 230_000, start).isFinished)
    }

    @Test
    fun intervalsAlternateExercisesAndRestBetweenSets() {
        val plan = TimerPlans.intervals(
            workSeconds = 15,
            restSeconds = 45,
            rounds = 2,
            sets = 2,
            setRestSeconds = 120,
            exerciseIds = listOf("burpee", "kettlebell-swing"),
        )

        assertEquals("burpee", plan.stateAt(start, start).label)
        assertEquals("kettlebell-swing", plan.stateAt(start + 60_000, start).label)
        // Serie 1: 15 + 45 + 15 = 75 s; luego 120 s de descanso entre series.
        val betweenSets = plan.stateAt(start + 80_000, start)
        assertEquals(SegmentKind.REST, betweenSets.kind)
        assertEquals(115_000, betweenSets.remainingInSegmentMs)
        val secondSet = plan.stateAt(start + 195_000, start)
        assertEquals(2, secondSet.set)
        assertEquals("burpee", secondSet.label)
    }

    @Test
    fun flowWithBreathsAndBothSides() {
        val plan = TimerPlans.flow(
            listOf(
                TimerPlans.Pose("mountain", breaths = 1),
                TimerPlans.Pose("warrior-2", seconds = 30, bothSides = true),
            ),
        )

        val state = plan.stateAt(start + 40_000, start)

        assertEquals("warrior-2:right", state.label)
        assertEquals(2_000, state.elapsedInSegmentMs)
        assertEquals(68_000, plan.totalDurationMs)
    }

    @Test
    fun squareBreathing() {
        val plan = TimerPlans.breathing(inhale = 4, holdIn = 4, exhale = 4, holdOut = 4, cycles = 2)

        val state = plan.stateAt(start + 9_000, start)

        assertEquals(SegmentKind.EXHALE, state.kind)
        assertEquals(1, state.round)
        assertEquals(1_000, state.elapsedInSegmentMs)
        assertEquals(32_000, plan.totalDurationMs)
    }

    @Test
    fun forTimeWithoutCapCountsUpUntilTheSkip() {
        val plan = TimerPlans.forTime(capSeconds = null)

        val running = plan.stateAt(start + 100_000, start)
        assertEquals(100_000, running.elapsedInSegmentMs)
        assertNull(running.remainingInSegmentMs)

        val done = plan.stateAt(start + 400_000, start, listOf(TimerEdit.Skip(start + 300_000)))
        assertTrue(done.isFinished)
        assertEquals(300_000, done.totalElapsedMs)
    }

    @Test
    fun eventsAnnounceHalfwayCountdownPhaseChangesAndTheEnd() {
        val plan = TimerPlans.intervals(workSeconds = 20, restSeconds = 10, rounds = 2)

        assertEquals(listOf<TimerEvent>(TimerEvent.Halfway(0)), plan.eventsBetween(start + 9_900, start + 10_000, start))
        assertEquals(listOf<TimerEvent>(TimerEvent.SecondsLeft(0, 5)), plan.eventsBetween(start + 14_500, start + 15_000, start))
        assertEquals(
            listOf<TimerEvent>(TimerEvent.SegmentStarted(1, SegmentKind.REST, 1, null)),
            plan.eventsBetween(start + 19_900, start + 20_000, start),
        )
        assertEquals(
            listOf(TimerEvent.SegmentStarted(2, SegmentKind.WORK, 2, null), TimerEvent.Finished),
            plan.eventsBetween(start + 29_000, start + 60_000, start),
        )
        assertEquals(emptyList(), plan.eventsBetween(start + 60_000, start + 70_000, start))
    }

    @Test
    fun aShortSegmentCountsDownOnlyFromItsOwnLength() {
        val plan = TimerPlan(
            listOf(
                TimerSegment(SegmentKind.WORK, 10_000),
                TimerSegment(SegmentKind.REST, 3_000),
            ),
        )

        val events = plan.eventsBetween(start + 9_999, start + 10_000, start)

        assertEquals(
            listOf(TimerEvent.SegmentStarted(1, SegmentKind.REST, 1, null), TimerEvent.SecondsLeft(1, 3)),
            events,
        )
    }
}
