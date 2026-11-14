package com.olarte.ironprogress.domain.feedback

import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.bench
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.catalog
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.curl
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.dips
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.feedback
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.session
import com.olarte.ironprogress.domain.feedback.FeedbackFixtures.squat
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SessionFeedbackRulesTest {
    private val now = Instant.parse("2026-11-12T18:00:00Z")
    private var next = 0
    private val newId = { "id-${next++}" }

    @Test
    fun ratesTheMusclesThatGotAtLeastOneEffectiveSetMostWorkedFirst() {
        // Empuje: 4 de press (pecho 4, tríceps 2, hombros 2) y 2 de fondos (tríceps 2, pecho 1).
        val push = session("push", now, listOf(Triple(bench, 4, 0), Triple(dips, 2, 0)))
        assertEquals(listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.SHOULDERS), SessionFeedbackRules.trainedMuscles(push, catalog))
    }

    @Test
    fun pendingSetsDoNotCountWhenChoosingMuscles() {
        val partial = session("p", now, listOf(Triple(bench, 1, 3)))
        assertEquals(listOf(Muscle.CHEST), SessionFeedbackRules.trainedMuscles(partial, catalog))
    }

    @Test
    fun buildMapsTheAnswersAndSendsJointPainToTheMusclesOfThatJoint() {
        val answers = FeedbackAnswers(
            muscles = listOf(
                MuscleAnswer(Muscle.CHEST, PerceivedLoad.LIGHT, pump = 1),
                MuscleAnswer(Muscle.BICEPS, PerceivedLoad.HARD),
            ),
            joints = setOf(Joint.SHOULDER),
            strongPain = true,
        )
        val rows = SessionFeedbackRules.build(SessionId("s"), answers, listOf(Muscle.CHEST), now, newId)
        val chest = rows.single { it.muscle == Muscle.CHEST }
        val biceps = rows.single { it.muscle == Muscle.BICEPS }
        assertEquals(1, chest.load)
        assertEquals(1, chest.pump)
        assertEquals(2, chest.jointPain)
        assertEquals(3, biceps.load)
        assertEquals(0, biceps.jointPain)
        assertTrue(rows.all { it.soreness == null && it.createdAt == now })
    }

    @Test
    fun aJointThatNoRatedMuscleMovesFallsOnThePrimaryMuscles() {
        // Muñeca en un día de pierna: la molestia queda en cuádriceps, el principal.
        val answers = FeedbackAnswers(listOf(MuscleAnswer(Muscle.QUADS), MuscleAnswer(Muscle.ABS)), joints = setOf(Joint.WRIST))
        val rows = SessionFeedbackRules.build(SessionId("s"), answers, listOf(Muscle.QUADS), now, newId)
        assertEquals(1, rows.single { it.muscle == Muscle.QUADS }.jointPain)
        assertEquals(0, rows.single { it.muscle == Muscle.ABS }.jointPain)
    }

    @Test
    fun easyLoadLowPumpAndNoSorenessAddsASet() {
        val row = feedback("a", Muscle.CHEST, now, load = 1, pump = 1, soreness = 0)
        assertEquals(SetChange.ADD, SessionFeedbackRules.weekChange(row, weeklySets = 12.0))
    }

    @Test
    fun basicModeWithoutPumpStillAddsWhenTheLoadWasEasy() {
        val row = feedback("a", Muscle.CHEST, now, load = 1, pump = null, soreness = 0)
        assertEquals(SetChange.ADD, SessionFeedbackRules.weekChange(row, weeklySets = 12.0))
    }

    @Test
    fun neverAddsAboveTheTopOfTheWeeklyRange() {
        val row = feedback("a", Muscle.CHEST, now, load = 1, pump = 0, soreness = 0)
        assertEquals(SetChange.KEEP, SessionFeedbackRules.weekChange(row, weeklySets = 19.5))
        assertEquals(SetChange.ADD, SessionFeedbackRules.weekChange(row, weeklySets = 19.0))
    }

    @Test
    fun highPumpOrRightLoadKeepsTheSets() {
        assertEquals(
            SetChange.KEEP,
            SessionFeedbackRules.weekChange(feedback("a", Muscle.CHEST, now, load = 1, pump = 3, soreness = 0), 10.0),
        )
        assertEquals(SetChange.KEEP, SessionFeedbackRules.weekChange(feedback("a", Muscle.CHEST, now, load = 2, soreness = 0), 10.0))
    }

    @Test
    fun sorenessOrAVeryHardLoadRemovesASetAndMildSorenessKeeps() {
        assertEquals(SetChange.REMOVE, SessionFeedbackRules.weekChange(feedback("a", Muscle.CHEST, now, load = 1, soreness = 2), 10.0))
        assertEquals(SetChange.REMOVE, SessionFeedbackRules.weekChange(feedback("a", Muscle.CHEST, now, load = 3), 10.0))
        assertEquals(SetChange.KEEP, SessionFeedbackRules.weekChange(feedback("a", Muscle.CHEST, now, load = 1, soreness = 1), 10.0))
    }

    @Test
    fun atStartNothingIsAddedUntilSorenessWasAnswered() {
        val unanswered = feedback("a", Muscle.CHEST, now, load = 1, pump = 0)
        assertEquals(SetChange.KEEP, SessionFeedbackRules.changeAtStart(unanswered, 10.0))
        assertEquals(SetChange.ADD, SessionFeedbackRules.changeAtStart(unanswered.copy(soreness = 0), 10.0))
    }

    @Test
    fun asksSorenessOnlyForPrimaryMusclesBeforeTheFirstSetAndWithinTwoWeeks() {
        val today = session("today", now, listOf(Triple(bench, 0, 3), Triple(curl, 0, 3)), finished = false)
        val latest = mapOf(
            Muscle.CHEST to feedback("old", Muscle.CHEST, now - 3.days),
            Muscle.TRICEPS to feedback("old", Muscle.TRICEPS, now - 3.days),
            Muscle.BICEPS to feedback("older", Muscle.BICEPS, now - 20.days),
            Muscle.QUADS to feedback("old", Muscle.QUADS, now - 3.days),
        )
        val questions = SessionFeedbackRules.sorenessQuestions(today, catalog, latest, now)
        assertEquals(listOf(Muscle.CHEST), questions.map { it.muscle })

        val answered = latest + (Muscle.CHEST to latest.getValue(Muscle.CHEST).copy(soreness = 1))
        assertEquals(emptyList(), SessionFeedbackRules.sorenessQuestions(today, catalog, answered, now))

        val started = session("today", now, listOf(Triple(bench, 1, 2)), finished = false)
        assertEquals(emptyList(), SessionFeedbackRules.sorenessQuestions(started, catalog, latest, now))
    }

    @Test
    fun addCopiesTheLastWorkingSetOfTheFirstExerciseOfThatMuscle() {
        val today = session("t", now, listOf(Triple(bench, 0, 3), Triple(dips, 0, 3)), finished = false)
        val plan = SessionFeedbackRules.planSetChanges(today, catalog, mapOf(Muscle.CHEST to SetChange.ADD), newId)
        val added = plan.added.single()
        assertEquals(today.exercises[0].id, added.sessionExerciseId)
        assertEquals(3, added.sortOrder)
        assertEquals(60.0, added.weightKg)
        assertEquals(8, added.reps)
        assertEquals(SetType.WORKING, added.setType)
        assertEquals(null, added.completedAt)
        assertEquals(SetChange.ADD, plan.applied[Muscle.CHEST])
    }

    @Test
    fun removeTakesTheLastPendingSetButNeverTheOnlyOne() {
        val today = session("t", now, listOf(Triple(dips, 0, 3)), finished = false)
        val plan = SessionFeedbackRules.planSetChanges(today, catalog, mapOf(Muscle.TRICEPS to SetChange.REMOVE), newId)
        assertEquals(listOf(today.sets.last().id), plan.removed)

        val single = session("u", now, listOf(Triple(dips, 0, 1)), finished = false)
        val none = SessionFeedbackRules.planSetChanges(single, catalog, mapOf(Muscle.TRICEPS to SetChange.REMOVE), newId)
        assertEquals(emptyList(), none.removed)
        assertEquals(SetChange.KEEP, none.applied[Muscle.TRICEPS])
    }

    @Test
    fun aMuscleThatIsNotTrainedTodayKeepsAndOneExerciseGetsOnlyOneExtraSet() {
        val today = session("t", now, listOf(Triple(bench, 0, 3)), finished = false)
        val plan = SessionFeedbackRules.planSetChanges(
            today,
            catalog,
            mapOf(Muscle.CHEST to SetChange.ADD, Muscle.QUADS to SetChange.ADD),
            newId,
        )
        assertEquals(1, plan.added.size)
        assertEquals(SetChange.KEEP, plan.applied[Muscle.QUADS])
    }

    @Test
    fun strongJointPainProposesSwappingTheExerciseThatCausedIt() {
        val painful = session("p", now - 4.days, listOf(Triple(bench, 3, 0), Triple(squat, 3, 0)))
        val today = session("t", now, listOf(Triple(bench, 0, 3), Triple(squat, 0, 3)), finished = false)
        val feedback = mapOf(
            painful.session.id to listOf(
                feedback("p", Muscle.SHOULDERS, now - 4.days, jointPain = 2),
                feedback("p", Muscle.QUADS, now - 4.days, jointPain = 1),
            ),
        )
        val swap = SessionFeedbackRules.exercisesToSwap(today, catalog, listOf(painful), feedback, now)
        assertEquals(setOf(today.exercises[0].id), swap)

        // Si después volvió a hacerlo sin molestias, ya no se propone.
        val later = session("l", now - 1.days, listOf(Triple(bench, 3, 0)))
        assertEquals(emptySet(), SessionFeedbackRules.exercisesToSwap(today, catalog, listOf(painful, later), feedback, now))
    }

    @Test
    fun rejectsValuesOutsideTheScales() {
        assertFailsWith<IllegalArgumentException> { feedback("a", Muscle.CHEST, now, load = 4) }
        assertFailsWith<IllegalArgumentException> { feedback("a", Muscle.CHEST, now, jointPain = 3) }
        assertFailsWith<IllegalArgumentException> { feedback("a", Muscle.CHEST, now, soreness = -1) }
    }

    @Test
    fun inMemoryRepositoryKeepsTheLatestRowPerMuscleAndStoresSoreness() = kotlinx.coroutines.test.runTest {
        val repository = InMemoryFeedbackRepository()
        repository.save(listOf(feedback("a", Muscle.CHEST, now - 5.days), feedback("b", Muscle.CHEST, now - 1.days)))
        val latest = repository.latestByMuscle().getValue(Muscle.CHEST)
        assertEquals(SessionId("b"), latest.sessionId)
        repository.setSoreness(latest.id, 3, now)
        assertEquals(3, repository.forSession(SessionId("b")).single().soreness)
        assertEquals(now, repository.forSession(SessionId("b")).single().sorenessAt)
    }
}
