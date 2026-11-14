package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Mejores marcas y puntos por sesión del detalle de un ejercicio (F2-04). */
class ExerciseProgressTest {
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
    fun bestsAndProgressPerSession() {
        val history = listOf(
            entry("a", day - 40.days, 60.0, 10),
            entry("a", day - 40.days, 60.0, 9),
            entry("b", day - 10.days, 65.0, 6, rir = 1),
            entry("c", day, 62.5, 10),
            entry("c", day, 40.0, 10, type = SetType.WARMUP),
        )
        val bests = Records.bests(history)
        close(65.0, bests.heaviestWeight?.value)
        close(83.33, bests.estimatedOneRepMax?.value)
        assertEquals(SessionId("c"), bests.estimatedOneRepMax?.sessionId)
        close(1_140.0, bests.bestSessionVolume?.value)
        close(10.0, bests.mostRepsByWeight[60.0]?.value)

        val progress = ExerciseProgress.of(history)
        assertEquals(listOf("a", "b", "c"), progress.points.map { it.sessionId.value })
        assertEquals(65.0, progress.points[1].bestSet.weightKg)
        // El calentamiento no entra: el volumen del día c es 625.
        close(625.0, progress.points[2].volumeKg)
        // Hoy 83,33 frente a hace 40 días 80.
        close(3.33, progress.changeOverFourWeeks(ProgressMetric.ESTIMATED_ONE_REP_MAX))
        assertEquals(2, progress.inRange(ProgressRange.THREE_MONTHS, day + 60.days).size)
        assertEquals(60.0, progress.mostUsedWeightKey)
    }
}
