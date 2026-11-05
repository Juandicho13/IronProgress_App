package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SessionEditsTest {

    private val start = Instant.fromEpochMilliseconds(1_700_000_000_000)
    private val session = WorkoutSession(SessionId("s"), null, Discipline.STRENGTH, start)
    private val block = SessionBlock(SessionBlockId("b"), session.id, 0, BlockFormat.SETS)

    private fun exercise(id: String, order: Int, group: Int? = null) =
        SessionExercise(SessionExerciseId(id), session.id, block.id, ExerciseId(id), order, group)

    private fun set(exercise: String, order: Int, done: Boolean = false) = WorkoutSet(
        WorkoutSetId("$exercise-$order"),
        SessionExerciseId(exercise),
        order,
        weightKg = 50.0,
        reps = 8,
        completedAt = if (done) start else null,
    )

    private val detail = SessionDetail(
        session,
        listOf(block),
        listOf(exercise("bench", 0), exercise("row", 1), exercise("curl", 2), exercise("plank", 3)),
        listOf(set("bench", 0, done = true), set("bench", 1), set("row", 0), set("curl", 0), set("plank", 0)),
    )

    private fun SessionDetail.apply(changed: List<SessionExercise>): SessionDetail {
        val updated = exercises.map { old -> changed.firstOrNull { it.id == old.id } ?: old }
        val added = changed.filter { new -> exercises.none { it.id == new.id } }
        return copy(exercises = updated + added)
    }

    private fun SessionDetail.order() = exercises.sortedBy { it.sortOrder }.map { it.exerciseId.value }

    @Test
    fun movingAndSkippingRenumberWithoutGaps() {
        val moved = detail.apply(detail.moveExercise(SessionExerciseId("curl"), -2))
        assertEquals(listOf("curl", "bench", "row", "plank"), moved.order())

        val skipped = detail.apply(detail.skipExercise(SessionExerciseId("bench")))
        assertEquals(listOf("row", "curl", "plank", "bench"), skipped.order())
        assertEquals(emptyList(), detail.moveExercise(SessionExerciseId("bench"), -1))
    }

    @Test
    fun supersetsAndCircuitsAlternateWithoutRestUntilTheLastOne() {
        var grouped = detail.apply(detail.groupWithNext(SessionExerciseId("bench")))
        grouped = grouped.apply(grouped.groupWithNext(SessionExerciseId("row")))
        assertEquals(listOf("bench", "row", "curl"), grouped.groupOf(SessionExerciseId("row")).map { it.exerciseId.value })

        assertEquals("row", grouped.nextInGroupWithoutRest(SessionExerciseId("bench"))?.exerciseId?.value)
        assertEquals("curl", grouped.nextInGroupWithoutRest(SessionExerciseId("row"))?.exerciseId?.value)
        assertNull(grouped.nextInGroupWithoutRest(SessionExerciseId("curl")))
        assertEquals("bench", grouped.firstPendingInGroup(SessionExerciseId("curl"))?.exerciseId?.value)
        assertNull(detail.nextInGroupWithoutRest(SessionExerciseId("bench")))

        val split = grouped.apply(grouped.ungroup(SessionExerciseId("curl")))
        assertEquals(2, split.groupOf(SessionExerciseId("bench")).size)
        val alone = split.apply(split.ungroup(SessionExerciseId("row")))
        assertNull(alone.exercises.first { it.id.value == "bench" }.supersetGroup)
    }

    @Test
    fun substitutingKeepsWhatWasDoneAndMovesWhatIsPending() {
        var next = 0
        val ids = IdGenerator { "new-${next++}" }

        val untouched = detail.substitute(SessionExerciseId("row"), ExerciseId("pull-up"), ids)!!
        assertEquals(ExerciseId("pull-up"), untouched.exercises.single().exerciseId)
        assertEquals(SessionExerciseId("row"), untouched.selected)

        val started = detail.substitute(SessionExerciseId("bench"), ExerciseId("dumbbell-bench"), ids)!!
        val replacement = started.exercises.first { it.exerciseId == ExerciseId("dumbbell-bench") }
        assertEquals(1, replacement.sortOrder)
        assertEquals(listOf("bench-1"), started.sets.map { it.id.value })
        assertEquals(replacement.id, started.sets.single().sessionExerciseId)
        assertNull(started.sets.single().weightKg)
        assertEquals(listOf(2, 3, 4), started.exercises.filter { it != replacement }.map { it.sortOrder })
    }
}
