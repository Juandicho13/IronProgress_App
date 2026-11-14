package com.olarte.ironprogress.domain.progression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SuggestionRecordTest {
    private val suggestion = Suggestion(
        SuggestionReason.ADD_REP,
        60.0,
        listOf(12, 11, 10),
        RepRange(8, 12, 2),
        PreviousSummary(3, 9, 12, 60.0, 1),
    )

    @Test
    fun eachSetKeepsItsOwnRepsAndTheWhy() {
        val record = suggestion.recordFor(1)
        assertEquals(11, record.reps)
        assertEquals(60.0, record.weightKg)
        assertEquals(8, record.repsMin)
        assertEquals(12, record.repsMax)
        assertEquals(1, record.previousRir)
        assertEquals(10, suggestion.recordFor(5).reps, "Más series que sugerencias usan la última")
    }

    @Test
    fun itSurvivesTheRoundTripAndIgnoresGarbage() {
        val record = suggestion.recordFor(0).copy(ladderTarget = "push-up", deload = true)
        assertEquals(record, SuggestionRecord.decode(record.encode()))
        assertNull(SuggestionRecord.decode(null))
        assertNull(SuggestionRecord.decode("no es json"))
        assertNull(SuggestionRecord.decode("""{"why":"FUTURE_RULE"}"""))
        assertEquals(
            SuggestionReason.INCREASE,
            SuggestionRecord.decode("""{"why":"INCREASE","w":62.5,"campo_nuevo":1}""")?.reason,
            "Los campos nuevos se ignoran",
        )
    }

    @Test
    fun matchesOnlyWhileNobodyTouchedTheSet() {
        val record = suggestion.recordFor(0)
        val set = done(60.0, 12).copy(completedAt = null, suggestion = record.encode())
        assertTrue(record.matches(set))
        assertFalse(record.matches(set.copy(weightKg = 65.0)))
        assertFalse(record.matches(set.copy(reps = 9)))
        assertEquals(record, set.suggestionRecord)
    }

    @Test
    fun theNextSetFollowsTheWeightActuallyUsed() {
        val record = Suggestion(SuggestionReason.INCREASE, 62.5, listOf(8, 8), RepRange(8, 12)).recordFor(1)
        val next = done(62.5, 8).copy(completedAt = null, suggestion = record.encode())
        val carried = carryWeightForward(done(60.0, 8), next)
        assertEquals(60.0, carried?.weightKg)
        assertEquals(8, carried?.reps)
        assertNull(carryWeightForward(done(62.5, 8), next), "Mismo peso: nada que cambiar")
        assertNull(carryWeightForward(done(60.0, 8), next.copy(weightKg = 70.0)), "Si la tocaron, no se pisa")
        assertNull(carryWeightForward(done(60.0, 8), next.copy(suggestion = null)), "Sin sugerencia no se toca")
    }
}
