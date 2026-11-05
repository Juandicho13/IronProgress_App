package com.olarte.ironprogress.domain.discipline

import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.TimerEvent
import com.olarte.ironprogress.domain.time.eventsBetween
import com.olarte.ironprogress.domain.time.stateAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuidedTest {

    @Test
    fun sunSalutationHasSevenPosesFiveRoundsAndElevenBreathsEach() {
        val flow = GuidedFlows.SUN_SALUTATION_A
        val plan = flow.toPlan()
        assertEquals(35, plan.segments.size)
        // 6 posturas de 1 respiración y el perro boca abajo de 5, a 8 s: 88 s por ronda.
        assertEquals(5 * 88_000L, plan.totalDurationMs)
        val atDownDog = plan.stateAt(nowMs = 50_000, startedAtMs = 0)
        assertEquals("down-dog", atDownDog.label)
        assertEquals("Adho Mukha Svanasana", flow.poseFor(atDownDog.label)?.sanskrit)
        val third = plan.stateAt(nowMs = 2 * 88_000L + 1, startedAtMs = 0)
        assertEquals(3, third.round)
        assertEquals("mountain", third.label)
    }

    @Test
    fun bothSidesPosesKeepTheirPoseWithTheSide() {
        val flow = GuidedFlows.MOBILITY_WARMUP
        val labels = flow.toPlan().segments.map { it.label }
        assertTrue("low-lunge:left" in labels && "low-lunge:right" in labels)
        assertEquals("Estocada baja", flow.poseFor("low-lunge:right")?.name?.es)
    }

    @Test
    fun fiveMinutesOfBoxBreathingAreWholeCyclesOfFourPhases() {
        val pattern = BreathingPattern.BOX
        assertEquals(19, pattern.cyclesFor(5))
        val plan = pattern.toPlan(5)
        assertEquals(19 * 4, plan.segments.size)
        assertEquals(SegmentKind.HOLD_IN, plan.stateAt(nowMs = 5_000, startedAtMs = 0).kind)
        assertEquals(SegmentKind.HOLD_OUT, plan.stateAt(nowMs = 13_000, startedAtMs = 0).kind)
        val starts = plan.eventsBetween(0, 16_000, 0).filterIsInstance<TimerEvent.SegmentStarted>().map { it.kind }
        assertEquals(listOf(SegmentKind.HOLD_IN, SegmentKind.EXHALE, SegmentKind.HOLD_OUT, SegmentKind.INHALE), starts)
    }

    @Test
    fun onlyLongHoldsWarnBeforeStarting() {
        assertFalse(BreathingPattern.BOX.warnsBeforeStart)
        assertFalse(BreathingPattern.COHERENT.warnsBeforeStart)
        assertTrue(BreathingPattern.RELAXING.warnsBeforeStart)
        assertEquals(6, BreathingPattern.COHERENT.toPlan(1).segments.count { it.kind == SegmentKind.INHALE })
    }
}
