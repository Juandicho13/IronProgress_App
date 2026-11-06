package com.olarte.ironprogress.domain.wod

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.time.stateAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WodTest {

    // El AMRAP de 12 min de PANTALLAS 7.4: 10 wall balls, 15 swings y 20 saltos dobles (45 reps).
    private val amrap = Wod(
        "Cindy de la casa",
        BlockFormat.AMRAP,
        12,
        listOf(
            WodMovement(ExerciseId("wall-ball"), 10, "Balón de 9 kg"),
            WodMovement(ExerciseId("kettlebell-swing"), 15, "16 kg"),
            WodMovement(ExerciseId("double-under"), 20, "Cuerda"),
        ),
    )

    @Test
    fun tickingTheLastMovementAddsTheRound() {
        var score = WodScore()
        repeat(3) { score = score.tick(amrap) }
        assertEquals(WodScore(1, 0), score)
        score = score.tick(amrap)
        assertEquals(10, score.reps(amrap))
        assertEquals(WodScore(2, 1), score.addRound())
    }

    @Test
    fun paceProjectsTheRoundsOfTheWholeAmrap() {
        // 3 + 10 a los 4:18: (3 + 10/45) × 720 / 258 = 8,99 → 8 rondas, como en la pantalla.
        val score = WodScore(rounds = 3, movementsDone = 1)
        assertEquals(8, score.paceRounds(amrap, 258))
        assertNull(score.paceRounds(amrap, 5))
    }

    @Test
    fun resultDependsOnTheFormat() {
        val score = WodScore(8, 1)
        assertEquals(BlockResult.RoundsAndReps(8, 10), score.result(amrap, 720))
        assertEquals(BlockResult.Rounds(8), score.result(amrap.copy(format = BlockFormat.EMOM, minutes = 10), 600))
        assertEquals(BlockResult.Time(431), score.result(amrap.copy(format = BlockFormat.FOR_TIME, minutes = null), 431))
    }

    @Test
    fun plansMatchTheFormats() {
        assertEquals(720_000, amrap.toPlan().totalDurationMs)
        val emom = amrap.copy(format = BlockFormat.EMOM, minutes = 10)
        assertEquals(4, emom.toPlan().stateAt(200_000, 0).round)
        assertNull(amrap.copy(format = BlockFormat.ROUNDS, minutes = null).toPlan().totalDurationMs)
        assertTrue(amrap.countsDown)
        assertFalse(amrap.copy(format = BlockFormat.FOR_TIME, minutes = 15).countsDown)
        assertFailsWith<IllegalArgumentException> { amrap.copy(minutes = null) }
        assertFailsWith<IllegalArgumentException> { amrap.copy(format = BlockFormat.SETS) }
    }

    @Test
    fun betterMarks() {
        assertTrue(isBetter(BlockResult.RoundsAndReps(8, 12), BlockResult.RoundsAndReps(8, 10)))
        assertFalse(isBetter(BlockResult.RoundsAndReps(7, 40), BlockResult.RoundsAndReps(8, 0)))
        assertTrue(isBetter(BlockResult.Time(400), BlockResult.Time(431)))
        assertTrue(isBetter(BlockResult.Rounds(10), null))
    }
}
