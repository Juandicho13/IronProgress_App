package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.feedback.FeedbackId
import com.olarte.ironprogress.domain.feedback.InMemoryFeedbackRepository
import com.olarte.ironprogress.domain.feedback.PerceivedLoad
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.domain.progress.RecoveryLevel
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.routine.addExercise
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.SummarizeSession
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.summary.SummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Feedback, agujetas, molestias y calentamiento según la recuperación en Entreno y el Resumen (F2-06 y F2-14). */
@OptIn(ExperimentalCoroutinesApi::class)
class BodyFeedbackViewModelTest {
    private val clock = TestClock()

    /** Una sesión terminada de press hace [ago], con [sets] series de 60 kg × 8. */
    private fun pastPush(id: String, ago: Duration, sets: Int = 3): SessionDetail {
        val at = clock.now - ago
        val session = WorkoutSession(SessionId(id), null, Discipline.STRENGTH, at, at, SessionStatus.FINISHED)
        val block = SessionBlock(SessionBlockId("$id-b"), session.id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(SessionExerciseId("$id-e"), session.id, block.id, testBench.id, 0)
        return SessionDetail(
            session,
            listOf(block),
            listOf(exercise),
            (0 until sets).map { WorkoutSet(WorkoutSetId("$id-s$it"), exercise.id, it, weightKg = 60.0, reps = 8, completedAt = at) },
        )
    }

    private fun chestFeedback(session: String, load: Int = 1, jointPain: Int = 0) = SessionFeedback(
        id = FeedbackId("$session-chest"),
        sessionId = SessionId(session),
        muscle = Muscle.CHEST,
        load = load,
        jointPain = jointPain,
        createdAt = clock.now - 4.days,
    )

    private val routine: RoutineDetail = run {
        var n = 0
        RoutineDetail(
            Routine(RoutineId("r"), null, "Empuje"),
            listOf(WorkoutBlock(BlockId("b"), 0, BlockFormat.SETS)),
            emptyList(),
            emptyList(),
        ).addExercise(IdGenerator { "t${n++}" }, BlockId("b"), testBench.id, sets = 3)
    }

    private suspend fun TestScope.fixture(
        past: List<SessionDetail>,
        feedback: InMemoryFeedbackRepository,
        block: suspend TestScope.(TestSessions, WorkoutViewModel, SessionDetail) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = TestSessions()
        sessions.data.value = past
        var index = 0
        val ids = IdGenerator { "id-${index++}" }
        val model = WorkoutViewModel(
            sessions,
            TestCatalog(),
            EmptyRoutines(),
            StartSession(sessions, ids, clock, TestCatalog()),
            CompleteSet(sessions, clock),
            AddSessionExercise(sessions, ids, TestCatalog()),
            ids,
            clock,
            com.olarte.ironprogress.ui.rest.RestNotifications { },
            feedback = feedback,
        ).also { store.put("model", it) }
        try {
            runCurrent()
            var m = 0
            val detail = StartSession(sessions, IdGenerator { "s${m++}" }, clock, TestCatalog())(routine)
            runCurrent()
            block(sessions, model, detail)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun TestSessions.active() = data.value.first { it.session.status == SessionStatus.IN_PROGRESS }

    @Test
    fun noSorenessAfterAnEasySessionAddsASetToday() = runTest {
        val feedback = InMemoryFeedbackRepository(listOf(chestFeedback("past")))
        fixture(listOf(pastPush("past", 4.days)), feedback) { sessions, model, detail ->
            assertEquals(listOf(Muscle.CHEST), model.state.value.soreness.map { it.muscle })
            model.onIntent(WorkoutIntent.AnswerSoreness(Muscle.CHEST, 0))
            model.onIntent(WorkoutIntent.ApplySoreness)
            runCurrent()
            val sets = sessions.active().setsOf(detail.exercises.single().id)
            assertEquals(4, sets.count { it.setType == SetType.WORKING })
            assertEquals(mapOf(Muscle.CHEST to SetChange.ADD), model.state.value.sorenessResult)
            assertEquals(emptyList(), model.state.value.soreness)
            assertEquals(0, feedback.latestByMuscle().getValue(Muscle.CHEST).soreness)
            model.onIntent(WorkoutIntent.DismissSorenessResult)
            assertNull(model.state.value.sorenessResult)
        }
    }

    @Test
    fun strongSorenessRemovesASetAndSkippingChangesNothing() = runTest {
        val feedback = InMemoryFeedbackRepository(listOf(chestFeedback("past")))
        fixture(listOf(pastPush("past", 4.days)), feedback) { sessions, model, detail ->
            model.onIntent(WorkoutIntent.AnswerSoreness(Muscle.CHEST, 2))
            model.onIntent(WorkoutIntent.ApplySoreness)
            runCurrent()
            assertEquals(2, sessions.active().setsOf(detail.exercises.single().id).size)
            assertEquals(SetChange.REMOVE, model.state.value.sorenessResult?.get(Muscle.CHEST))
        }
        val other = InMemoryFeedbackRepository(listOf(chestFeedback("past")))
        fixture(listOf(pastPush("past", 4.days)), other) { sessions, model, detail ->
            model.onIntent(WorkoutIntent.SkipSoreness)
            runCurrent()
            assertEquals(emptyList(), model.state.value.soreness)
            assertEquals(3, sessions.active().setsOf(detail.exercises.single().id).size)
            assertNull(other.latestByMuscle().getValue(Muscle.CHEST).soreness)
        }
    }

    @Test
    fun strongJointPainLastTimeProposesASwap() = runTest {
        val feedback = InMemoryFeedbackRepository(listOf(chestFeedback("past", jointPain = 2).copy(soreness = 0)))
        fixture(listOf(pastPush("past", 4.days)), feedback) { _, model, detail ->
            val id = detail.exercises.single().id
            assertTrue(id in model.state.value.painSwap)
            model.onIntent(WorkoutIntent.DismissPainSwap(id))
            assertTrue(id in model.state.value.painSwapDismissed)
        }
    }

    @Test
    fun aChestStillRecoveringGetsMoreApproachSets() = runTest {
        // 8 series de press hace 20 h: el pecho sigue en ámbar.
        fixture(listOf(pastPush("past", 20.hours, sets = 8)), InMemoryFeedbackRepository()) { sessions, model, detail ->
            val id = detail.exercises.single().id
            val note = assertNotNull(model.state.value.warmups[id])
            assertEquals(Muscle.CHEST, note.muscle)
            assertEquals(RecoveryLevel.RECOVERING, note.level)
            // 60 kg: barra, 40 % (22,5), 50 % (30), 70 % (40) y 85 % (50).
            assertEquals(5, note.sets)
            model.onIntent(WorkoutIntent.AddWarmup(id))
            runCurrent()
            val warmups = sessions.active().setsOf(id).filter { it.setType == SetType.WARMUP }
            assertEquals(listOf(20.0, 22.5, 30.0, 40.0, 50.0), warmups.map { it.weightKg })
            assertNull(model.state.value.warmups[id])
        }
    }

    @Test
    fun summaryFeedbackIsOneTapAndPreviewsNextWeek() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val sessions = TestSessions()
            sessions.data.value = listOf(pastPush("done", 1.hours))
            val feedback = InMemoryFeedbackRepository()
            var n = 0
            val model = SummaryViewModel(
                SummarizeSession(sessions),
                TestCatalog(),
                EmptyRoutines(),
                "done",
                feedback,
                sessions,
                IdGenerator { "f${n++}" },
                clock,
            ).also { store.put("summary", it) }
            runCurrent()
            val form = assertNotNull(model.state.value.feedback)
            assertEquals(listOf(Muscle.CHEST), form.muscles)
            assertEquals(PerceivedLoad.RIGHT, form.loadOf(Muscle.CHEST))
            assertEquals(SetChange.KEEP, form.preview(withPump = false)[Muscle.CHEST])
            model.setLoad(Muscle.CHEST, PerceivedLoad.LIGHT)
            assertEquals(SetChange.ADD, model.state.value.feedback!!.preview(withPump = false)[Muscle.CHEST])
            var done = false
            model.save(withPump = false) { done = true }
            runCurrent()
            assertTrue(done)
            val saved = feedback.forSession(SessionId("done")).single()
            assertEquals(1, saved.load)
            assertNull(saved.pump)
            assertNull(model.state.value.feedback)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
