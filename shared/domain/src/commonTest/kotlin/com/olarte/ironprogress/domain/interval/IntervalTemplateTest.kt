package com.olarte.ironprogress.domain.interval

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.stateAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IntervalTemplateTest {

    private val bursts = IntervalTemplate(
        id = IntervalTemplateId("t1"),
        name = "Ráfagas de 15",
        workSeconds = 15,
        restSeconds = 45,
        rounds = 10,
        exerciseIds = listOf(ExerciseId("burpee"), ExerciseId("kettlebell-swing")),
    )

    @Test
    fun totalSkipsTheLastRest() {
        // 10 × 15 + 9 × 45 = 555 s = 9:15, como el botón Empezar de 5.2.
        assertEquals(555, bursts.totalSeconds)
        assertEquals(60, bursts.roundSeconds)
        assertEquals(555_000, bursts.toPlan().totalDurationMs)
    }

    @Test
    fun exercisesAlternateEachRound() {
        assertEquals(ExerciseId("burpee"), bursts.exerciseFor(1))
        assertEquals(ExerciseId("kettlebell-swing"), bursts.exerciseFor(2))
        assertEquals(ExerciseId("burpee"), bursts.exerciseFor(3))
        val start = 0L
        assertEquals("kettlebell-swing", bursts.toPlan().stateAt(start + 61_000, start).label)
        assertEquals(SegmentKind.REST, bursts.toPlan().stateAt(start + 20_000, start).kind)
    }

    @Test
    fun presetsAreRecognizedAndApplied() {
        assertEquals(IntervalPreset.FIFTEEN_FORTY_FIVE, bursts.preset)
        val tabata = bursts.withPreset(IntervalPreset.TABATA)
        assertEquals(20, tabata.workSeconds)
        assertEquals(8, tabata.rounds)
        assertEquals(IntervalPreset.TABATA, tabata.preset)
        assertNull(bursts.copy(workSeconds = 25).preset)
    }

    @Test
    fun limitsMatchTheSteppers() {
        assertFailsWith<IllegalArgumentException> { bursts.copy(workSeconds = 0) }
        assertFailsWith<IllegalArgumentException> { bursts.copy(workSeconds = 17) }
        assertFailsWith<IllegalArgumentException> { bursts.copy(restSeconds = 305) }
        assertFailsWith<IllegalArgumentException> { bursts.copy(rounds = 51) }
        bursts.copy(restSeconds = 0, rounds = 1, workSeconds = 300)
    }
}
