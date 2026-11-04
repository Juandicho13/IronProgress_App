package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutineModelsTest {

    private fun sequentialIds(): IdGenerator {
        var next = 0
        return IdGenerator { "id-${next++}" }
    }

    private fun emptyRoutine(ids: IdGenerator) = RoutineDetail(
        routine = Routine(RoutineId(ids.newId()), folderId = null, name = "Pierna A"),
        blocks = listOf(WorkoutBlock(BlockId("b1"), 0, BlockFormat.SETS)),
        exercises = emptyList(),
        setTemplates = emptyList(),
    )

    @Test
    fun setTemplateRejectsInvalidRanges() {
        val exercise = RoutineExerciseId("e1")
        assertFailsWith<IllegalArgumentException> { SetTemplate(SetTemplateId("s"), exercise, 0, repsMin = 12, repsMax = 8) }
        assertFailsWith<IllegalArgumentException> { SetTemplate(SetTemplateId("s"), exercise, 0, repsMin = 0) }
        assertFailsWith<IllegalArgumentException> { SetTemplate(SetTemplateId("s"), exercise, 0, targetRir = 6) }
        assertFailsWith<IllegalArgumentException> {
            SetTemplate(SetTemplateId("s"), exercise, 0, setType = SetType.FAILURE, targetRir = 2)
        }
        SetTemplate(SetTemplateId("s"), exercise, 0, repsMin = 8, repsMax = 8, targetRir = 0)
    }

    @Test
    fun onlyWarmupDoesNotCountAsWorking() {
        assertFalse(SetType.WARMUP.countsAsWorking)
        assertTrue(SetType.entries.filter { it != SetType.WARMUP }.all { it.countsAsWorking })
    }

    @Test
    fun timedBlocksNeedTheirTimes() {
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(BlockId("b"), 0, BlockFormat.EMOM) }
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(BlockId("b"), 0, BlockFormat.AMRAP) }
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(BlockId("b"), 0, BlockFormat.INTERVALS, rounds = 8) }
        WorkoutBlock(BlockId("b"), 0, BlockFormat.INTERVALS, rounds = 8, workSeconds = 20, restSeconds = 10)
        WorkoutBlock(BlockId("b"), 0, BlockFormat.FOR_TIME)
    }

    @Test
    fun routineNeedsAName() {
        assertFailsWith<IllegalArgumentException> { Routine(RoutineId("r"), null, name = "  ") }
    }

    @Test
    fun detailRejectsExercisesInMissingBlocks() {
        assertFailsWith<IllegalArgumentException> {
            RoutineDetail(
                routine = Routine(RoutineId("r"), null, "R"),
                blocks = emptyList(),
                exercises = listOf(RoutineExercise(RoutineExerciseId("e"), BlockId("x"), ExerciseId("squat"), 0)),
                setTemplates = emptyList(),
            )
        }
    }

    @Test
    fun addExerciseAppendsWithItsSets() {
        val ids = sequentialIds()
        val detail = emptyRoutine(ids)
            .addExercise(ids, BlockId("b1"), ExerciseId("back-squat"), sets = 4)
            .addExercise(ids, BlockId("b1"), ExerciseId("leg-press"))

        val exercises = detail.exercisesOf(BlockId("b1"))
        assertEquals(listOf("back-squat", "leg-press"), exercises.map { it.exerciseId.value })
        assertEquals(listOf(0, 1), exercises.map { it.sortOrder })
        assertEquals(4, detail.setsOf(exercises[0].id).size)
        assertEquals(3, detail.setsOf(exercises[1].id).size)
        assertEquals(8..12, detail.setsOf(exercises[0].id).first().let { it.repsMin!!..it.repsMax!! })
    }

    @Test
    fun moveAndRemoveKeepTheOrderWithoutGaps() {
        val ids = sequentialIds()
        var detail = emptyRoutine(ids)
        listOf("a", "b", "c", "d").forEach { detail = detail.addExercise(ids, BlockId("b1"), ExerciseId(it), sets = 1) }
        val c = detail.exercises.first { it.exerciseId.value == "c" }.id

        detail = detail.moveExercise(c, 0)
        assertEquals(listOf("c", "a", "b", "d"), detail.exercisesOf(BlockId("b1")).map { it.exerciseId.value })

        val a = detail.exercises.first { it.exerciseId.value == "a" }.id
        detail = detail.removeExercise(a)
        val remaining = detail.exercisesOf(BlockId("b1"))
        assertEquals(listOf("c", "b", "d"), remaining.map { it.exerciseId.value })
        assertEquals(listOf(0, 1, 2), remaining.map { it.sortOrder })
        assertTrue(detail.setTemplates.none { it.routineExerciseId == a })
    }

    @Test
    fun supersetsGroupConsecutiveExercises() {
        val ids = sequentialIds()
        var detail = emptyRoutine(ids)
        listOf("bench", "row", "curl", "pushdown", "plank").forEach {
            detail = detail.addExercise(ids, BlockId("b1"), ExerciseId(it), sets = 1)
        }
        val byName = detail.exercises.associateBy { it.exerciseId.value }

        detail = detail
            .groupAsSuperset(listOf(byName.getValue("bench").id, byName.getValue("row").id))
            .groupAsSuperset(listOf(byName.getValue("curl").id, byName.getValue("pushdown").id))

        val groups = detail.supersetsOf(BlockId("b1")).map { group -> group.map { it.exerciseId.value } }
        assertEquals(listOf(listOf("bench", "row"), listOf("curl", "pushdown"), listOf("plank")), groups)
        assertEquals(listOf(1, 1, 2, 2, null), detail.exercisesOf(BlockId("b1")).map { it.supersetGroup })
        assertFailsWith<IllegalArgumentException> {
            detail.groupAsSuperset(listOf(byName.getValue("bench").id, byName.getValue("plank").id))
        }
    }
}
