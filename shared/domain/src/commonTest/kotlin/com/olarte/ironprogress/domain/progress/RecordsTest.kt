package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RecordsTest {
    private val bench = ExerciseId("bench")
    private val day = Instant.parse("2026-11-10T18:00:00Z")
    private var counter = 0

    private fun entry(
        session: String,
        at: Instant,
        kg: Double?,
        reps: Int,
        rir: Int? = null,
        type: SetType = SetType.WORKING,
        round: Int? = null,
    ): ExerciseHistoryEntry {
        counter++
        val set = WorkoutSet(
            id = WorkoutSetId("set-$counter"),
            sessionExerciseId = SessionExerciseId("$session-e"),
            sortOrder = counter,
            setType = type,
            weightKg = kg,
            reps = reps,
            rir = rir,
            round = round,
            completedAt = at,
        )
        return ExerciseHistoryEntry(SessionId(session), at, set)
    }

    private fun close(expected: Double, actual: Double?) =
        assertTrue(actual != null && abs(expected - actual) < 0.01, "$expected != $actual")

    @Test
    fun epleyAddsRepsInReserveAndStopsAboveTwelve() {
        // 60 × (1 + (10 + 2) / 30) = 84
        close(84.0, Records.estimatedOneRepMax(60.0, 10, 2))
        close(80.0, Records.estimatedOneRepMax(60.0, 10, null))
        assertNull(Records.estimatedOneRepMax(60.0, 11, 2))
        assertNull(Records.estimatedOneRepMax(null, 10, 0))
        assertNull(Records.estimatedOneRepMax(60.0, 0, 0))
    }

    @Test
    fun firstTimeIsNeverARecord() {
        val first = entry("today", day, 100.0, 5)
        assertTrue(Records.detect(bench, emptyList(), emptyList(), first).isEmpty())
        // Tampoco la segunda serie del primer día: hace falta una marca de otra sesión.
        val second = entry("today", day + 3.minutes, 105.0, 5)
        assertTrue(Records.detect(bench, emptyList(), listOf(first), second).isEmpty())
    }

    @Test
    fun beatsStrengthWeightRepsAndVolume() {
        val history = listOf(entry("a", day - 7.days, 60.0, 10), entry("a", day - 7.days, 60.0, 8))
        // 62,5 × 9: fuerza estimada 81,25 frente a 80, más peso que 60; aún no pasa el volumen (1.080).
        val first = entry("b", day, 62.5, 9)
        val hits = Records.detect(bench, history, emptyList(), first)
        assertEquals(listOf(RecordKind.ESTIMATED_ONE_REP_MAX, RecordKind.HEAVIEST_WEIGHT), hits.map { it.kind })
        close(81.25, hits.first().value)
        close(80.0, hits.first().previous)

        // 62,5 × 10 después: más reps con 62,5 y el volumen pasa de 1.080 a 1.187,5.
        val second = entry("b", day + 3.minutes, 62.5, 10)
        val next = Records.detect(bench, history, listOf(first), second)
        assertEquals(
            listOf(RecordKind.ESTIMATED_ONE_REP_MAX, RecordKind.MOST_REPS, RecordKind.BEST_SESSION_VOLUME),
            next.map { it.kind },
        )
        close(10.0, next.first { it.kind == RecordKind.MOST_REPS }.value)
        close(1_187.5, next.first { it.kind == RecordKind.BEST_SESSION_VOLUME }.value)
    }

    @Test
    fun warmupsAndRoundsDoNotCount() {
        val history = listOf(entry("a", day - 2.days, 60.0, 8))
        assertTrue(Records.detect(bench, history, emptyList(), entry("b", day, 100.0, 5, type = SetType.WARMUP)).isEmpty())
        assertTrue(Records.detect(bench, history, emptyList(), entry("b", day, 100.0, 5, round = 1)).isEmpty())
    }

    @Test
    fun bodyweightCountsOnlyReps() {
        val history = listOf(entry("a", day - 2.days, null, 10))
        val hits = Records.detect(bench, history, emptyList(), entry("b", day, null, 12))
        assertEquals(listOf(RecordKind.MOST_REPS), hits.map { it.kind })
    }

    @Test
    fun replayKeepsTheBestOfEachKindAndTheSessionVolume() {
        val history = listOf(entry("a", day - 7.days, 60.0, 10))
        val session = listOf(entry("b", day, 62.5, 8), entry("b", day + 2.minutes, 65.0, 8), entry("b", day + 4.minutes, 50.0, 10))
        val hits = Records.replay(bench, history, session)
        assertEquals(65.0, hits.first { it.kind == RecordKind.HEAVIEST_WEIGHT }.value)
        // 500 + 520 + 500: el volumen se muestra entero.
        close(1_520.0, hits.first { it.kind == RecordKind.BEST_SESSION_VOLUME }.value)
    }
}
