package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.TrackingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StagnationTest {
    private val range = RepRange(8, 12, 2)

    /** Una sesión de 3 series con [weight] × [reps] (más nueva primero en las listas). */
    private fun s(weight: Double, reps: Int, deload: Boolean = false) =
        session(done(weight, reps), done(weight, reps), done(weight, reps), deload = deload)

    @Test
    fun fourSessionsWithoutBeatingTheFirstByMoreThanOnePercentIsAPlateau() {
        val history = listOf(s(60.0, 10), s(60.0, 9), s(60.0, 10), s(60.0, 10))
        val result = StagnationDetector.detect(history, TrackingType.WEIGHT_REPS, range)
        assertEquals(StagnationKind.PLATEAU, result?.kind)
        assertEquals(4, result?.sessions)
        assertEquals(
            listOf(StagnationProposal.DELOAD, StagnationProposal.CHANGE_RANGE, StagnationProposal.SUBSTITUTE),
            result?.proposals,
        )
        assertEquals(RepRange(5, 8, 2), result?.newRange)
    }

    @Test
    fun anyRealImprovementIsNotAPlateau() {
        val history = listOf(s(62.5, 10), s(60.0, 10), s(60.0, 10), s(60.0, 10))
        assertNull(StagnationDetector.detect(history, TrackingType.WEIGHT_REPS, range))
    }

    @Test
    fun fewerThanFourSessionsSaysNothing() {
        assertNull(StagnationDetector.detect(listOf(s(60.0, 10), s(60.0, 10), s(60.0, 10)), TrackingType.WEIGHT_REPS, range))
    }

    @Test
    fun deloadSessionsDoNotCount() {
        val history = listOf(s(60.0, 10), s(50.0, 10, deload = true), s(60.0, 10), s(60.0, 10))
        assertNull(StagnationDetector.detect(history, TrackingType.WEIGHT_REPS, range), "Con la descarga fuera quedan tres")
        val longer = history + s(60.0, 10)
        assertEquals(StagnationKind.PLATEAU, StagnationDetector.detect(longer, TrackingType.WEIGHT_REPS, range)?.kind)
    }

    @Test
    fun twoSessionsBelowTheMinimumAlsoCount() {
        val history = listOf(s(60.0, 6), s(60.0, 7), s(55.0, 10))
        val result = StagnationDetector.detect(history, TrackingType.WEIGHT_REPS, range)
        assertEquals(StagnationKind.BELOW_MIN, result?.kind)
        assertEquals(8, result?.minReps)
    }

    @Test
    fun bodyweightUsesTheBestReps() {
        val pushUps = listOf(s(0.0, 12), s(0.0, 12), s(0.0, 11), s(0.0, 12))
        assertEquals(StagnationKind.PLATEAU, StagnationDetector.detect(pushUps, TrackingType.REPS, RepRange(8, 15))?.kind)
        val better = listOf(s(0.0, 14), s(0.0, 12), s(0.0, 11), s(0.0, 12))
        assertNull(StagnationDetector.detect(better, TrackingType.REPS, RepRange(8, 15)))
    }

    @Test
    fun proposalsFollowTheOrderOfTheRules() {
        val history = listOf(s(60.0, 10), s(60.0, 10), s(60.0, 10), s(60.0, 10))
        val free = StagnationDetector.detect(history, TrackingType.WEIGHT_REPS, RepRange(), lowRecoveryDays = true, canChangeRange = false)
        assertEquals(listOf(StagnationProposal.RECOVERY, StagnationProposal.DELOAD, StagnationProposal.SUBSTITUTE), free?.proposals)
        assertNull(free?.newRange)
    }

    @Test
    fun rangesSwapBetweenStrengthAndVolume() {
        assertEquals(RepRange(5, 8, 2), StagnationDetector.proposedRange(RepRange(8, 12, 2)))
        assertEquals(RepRange(10, 15, null), StagnationDetector.proposedRange(RepRange(5, 8)))
    }

    @Test
    fun aDeloadKeepsSixtyPercentOfTheSets() {
        assertEquals(2, StagnationDetector.deloadSetCount(3))
        assertEquals(2, StagnationDetector.deloadSetCount(4))
        assertEquals(3, StagnationDetector.deloadSetCount(5))
        assertEquals(1, StagnationDetector.deloadSetCount(1))
        assertEquals(1, StagnationDetector.deloadSetCount(2))
    }
}

class DeloadTest {
    private fun pending(index: Int) =
        done(60.0, 8).copy(id = com.olarte.ironprogress.domain.session.WorkoutSetId("p$index"), sortOrder = index, completedAt = null)

    @Test
    fun keepsSixtyPercentRemovingFromTheEndAndMarksTheRest() {
        val sets = (0..4).map { pending(it) }
        val change = deload(sets)
        assertEquals(listOf("p3", "p4"), change.remove.map { it.value })
        assertEquals(listOf("p0", "p1", "p2"), change.save.map { it.id.value })
        change.save.forEach {
            val record = it.suggestionRecord!!
            assertEquals(true, record.deload)
            assertEquals(SuggestionReason.DELOAD_SETS, record.reason)
            assertEquals(true, record.matches(it), "La descarga no deja nada para Usar")
        }
    }

    @Test
    fun doneSetsStayAndAtLeastOneRemains() {
        val first = pending(0).copy(completedAt = kotlin.time.Instant.parse("2026-11-01T12:00:00Z"))
        assertEquals(listOf("p2"), deload(listOf(first, pending(1), pending(2))).remove.map { it.value })
        // Dos pendientes y ninguna hecha: queda una para hacer.
        val two = deload(listOf(pending(0), pending(1)))
        assertEquals(listOf("p1"), two.remove.map { it.value })
        assertEquals(listOf("p0"), two.save.map { it.id.value })
    }
}
