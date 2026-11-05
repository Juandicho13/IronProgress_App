package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionSummaryTest {

    private val bench = ExerciseId("bench")
    private val dips = ExerciseId("dips")
    private val routine = RoutineId("empuje-a")
    private val day = Instant.parse("2026-09-23T18:00:00Z")

    /** Sesión terminada con [minutes] de duración y, por ejercicio, pares de peso y reps. */
    private fun session(
        id: String,
        start: Instant,
        minutes: Int,
        work: Map<ExerciseId, List<Pair<Double, Int>>>,
        warmup: Pair<Double, Int>? = null,
        routineId: RoutineId? = routine,
        blocks: List<SessionBlock> = emptyList(),
    ): SessionDetail {
        val sid = SessionId(id)
        val block = SessionBlock(SessionBlockId("$id-b"), sid, 0, BlockFormat.SETS)
        val exercises = work.keys.mapIndexed { i, ex -> SessionExercise(SessionExerciseId("$id-$i"), sid, block.id, ex, i) }
        val sets = exercises.flatMap { ex ->
            work.getValue(ex.exerciseId).mapIndexed { i, (kg, reps) ->
                WorkoutSet(WorkoutSetId("${ex.id.value}-$i"), ex.id, i + 1, weightKg = kg, reps = reps, completedAt = start)
            }
        } + listOfNotNull(
            warmup?.let { (kg, reps) ->
                WorkoutSet(WorkoutSetId("$id-w"), exercises.first().id, 0, SetType.WARMUP, kg, reps, completedAt = start)
            },
        )
        val ended = start + minutes.minutes
        return SessionDetail(
            WorkoutSession(sid, routineId, Discipline.STRENGTH, start, ended, SessionStatus.FINISHED),
            listOf(block) + blocks,
            exercises,
            sets,
        )
    }

    @Test
    fun numbersMatchATestSession() {
        val last = session(
            "s1",
            day - 7.days,
            62,
            mapOf(bench to listOf(60.0 to 10, 60.0 to 10, 60.0 to 9), dips to listOf(10.0 to 8)),
        )
        val amrap = SessionBlock(SessionBlockId("s2-amrap"), SessionId("s2"), 1, BlockFormat.AMRAP, BlockResult.RoundsAndReps(6, 4))
        val today = session(
            "s2",
            day,
            58,
            mapOf(bench to listOf(62.5 to 11, 62.5 to 10, 62.5 to 10, 62.5 to 9), dips to listOf(12.5 to 8)),
            warmup = 40.0 to 10,
            blocks = listOf(amrap),
        )

        val summary = summarize(today, listOf(last))

        assertEquals(58 * 60L, summary.durationSeconds)
        // 62,5 × 40 reps + 12,5 × 8; el calentamiento no suma.
        assertEquals(2_600.0, summary.volumeKg)
        assertEquals(5, summary.workingSets)
        assertEquals(listOf(SessionRecord(bench, 62.5, 11), SessionRecord(dips, 12.5, 8)), summary.records)
        assertEquals(SessionId("s1"), summary.previous?.sessionId)
        // Antes: 60 × 29 + 10 × 8 = 1.820 kg. Ahora 2.600: un 43 % más.
        assertEquals(43, summary.volumeChangePercent)
        assertEquals(-4 * 60L, summary.durationChangeSeconds)
        assertEquals(listOf(amrap), summary.blockResults)
        assertEquals(listOf(bench, dips), summary.exercises.map { it.exerciseId })
    }

    @Test
    fun sameTopWeightNeedsMoreRepsAndFirstTimeIsNoRecord() {
        val before = session("a", day - 2.days, 50, mapOf(bench to listOf(62.5 to 10)))
        val sameReps = session("b", day, 50, mapOf(bench to listOf(62.5 to 10), dips to listOf(0.0 to 12)))
        val summary = summarize(sameReps, listOf(before))
        assertTrue(summary.records.isEmpty())

        val moreReps = session("c", day, 50, mapOf(bench to listOf(62.5 to 11)))
        assertEquals(listOf(SessionRecord(bench, 62.5, 11)), summarize(moreReps, listOf(before)).records)
    }

    @Test
    fun comparesOnlyWithEarlierSessionsOfTheSameRoutine() {
        val other = session("x", day - 1.days, 30, mapOf(bench to listOf(50.0 to 5)), routineId = RoutineId("pierna"))
        val later = session("y", day + 1.days, 30, mapOf(bench to listOf(100.0 to 5)))
        val today = session("z", day, 45, mapOf(bench to listOf(60.0 to 5)))
        val summary = summarize(today, listOf(other, later))
        assertNull(summary.previous)
        assertNull(summary.volumeChangePercent)
        // El récord sí mira todas las sesiones anteriores, de cualquier rutina.
        assertEquals(listOf(SessionRecord(bench, 60.0, 5)), summary.records)
    }

    @Test
    fun durationIgnoresSecondsBeforeStart() {
        val detail = session("d", day, 0, mapOf(bench to listOf(20.0 to 5)))
        val withSeconds = detail.copy(session = detail.session.copy(endedAt = day + 75.seconds))
        assertEquals(75, summarize(withSeconds, emptyList()).durationSeconds)
    }
}
