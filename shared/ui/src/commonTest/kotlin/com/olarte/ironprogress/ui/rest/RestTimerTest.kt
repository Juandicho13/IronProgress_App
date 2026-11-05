package com.olarte.ironprogress.ui.rest

import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.time.TimerEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestTimerTest {
    @Test
    fun twentyRestsKeepExactTimeWithIrregularFramesAndAdjustments() {
        repeat(20) { index ->
            val start = 100_000L * (index + 1)
            val rest = RestTimer(WorkoutSetId("set-$index"), start, 120)
                .adjust(start + 1234, 15).adjust(start + 5689, -15)
            listOf(0L, 17, 1234, 5701, 38941, 119999).forEach { offset ->
                val added = if (offset in 1234..5688) 15000 else 0
                assertEquals(120000L + added - offset, rest.at(start + offset).remainingInSegmentMs)
                assertFalse(rest.at(start + offset).isFinished)
            }
            assertTrue(rest.at(start + 120000).isFinished)
            assertEquals(listOf(TimerEvent.Finished), rest.events(start + 119999, start + 120000))
            assertTrue(rest.events(start + 120000, start + 120001).isEmpty())
        }
    }

    @Test
    fun reducingBelowElapsedFinishesWithoutNegativeTime() {
        val rest = RestTimer(WorkoutSetId("set"), 1000, 10).adjust(9000, -15)
        assertEquals(0L, rest.at(9000).remainingInSegmentMs)
        assertTrue(rest.at(9000).isFinished)
    }
}
