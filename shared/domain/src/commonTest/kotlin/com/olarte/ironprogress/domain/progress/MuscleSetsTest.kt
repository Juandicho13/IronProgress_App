package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class MuscleSetsTest {
    private val now = Instant.parse("2026-11-05T22:00:00Z")
    private val bench = Exercise(
        id = ExerciseId("bench"),
        name = LocalizedText(es = "Press banca", en = "Bench press"),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY), ExerciseMuscle(Muscle.TRICEPS, MuscleRole.SECONDARY)),
    )
    private val catalog = mapOf(bench.id to bench)

    private fun session(id: String, at: Instant, sets: List<Pair<SetType, Boolean>>): SessionDetail {
        val sid = SessionId(id)
        val block = SessionBlock(SessionBlockId("$id-b"), sid, 0, BlockFormat.SETS)
        val exercise = SessionExercise(SessionExerciseId("$id-e"), sid, block.id, bench.id, 0)
        return SessionDetail(
            WorkoutSession(sid, null, Discipline.STRENGTH, at, at, SessionStatus.FINISHED),
            listOf(block),
            listOf(exercise),
            sets.mapIndexed { i, (type, done) ->
                WorkoutSet(WorkoutSetId("$id-$i"), exercise.id, i, type, 60.0, 8, completedAt = if (done) at else null)
            },
        )
    }

    @Test
    fun primaryCountsOneAndSecondaryHalf() {
        val recent = session("a", now - 2.days, listOf(SetType.WORKING to true, SetType.WORKING to true, SetType.WORKING to true))
        val totals = MuscleSets.lastSevenDays(listOf(recent), catalog, now)
        assertEquals(3.0, totals[Muscle.CHEST])
        assertEquals(1.5, totals[Muscle.TRICEPS])
    }

    @Test
    fun skipsWarmupsPendingSetsAndOldSessions() {
        val recent = session("a", now - 1.days, listOf(SetType.WARMUP to true, SetType.WORKING to false, SetType.WORKING to true))
        val old = session("b", now - 8.days, listOf(SetType.WORKING to true))
        val totals = MuscleSets.lastSevenDays(listOf(recent, old), catalog, now)
        assertEquals(1.0, totals[Muscle.CHEST])
    }

    @Test
    fun noSessionsMeansNoNumbers() {
        assertTrue(MuscleSets.lastSevenDays(emptyList(), catalog, now).isEmpty())
    }
}
