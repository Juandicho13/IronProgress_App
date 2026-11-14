package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.domain.routine.BlockFormat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** Récords en vivo al completar una serie (F2-03). */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRecordsTest {

    /** Una sesión terminada de hace una semana con press 60 × 8. */
    private fun lastWeek(clock: TestClock): SessionDetail {
        val id = SessionId("old")
        val at = clock.now() - 7.days
        val block = SessionBlock(SessionBlockId("old-b"), id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(SessionExerciseId("old-e"), id, block.id, testBench.id, 0)
        return SessionDetail(
            WorkoutSession(id, null, Discipline.STRENGTH, at, at, SessionStatus.FINISHED),
            listOf(block),
            listOf(exercise),
            listOf(WorkoutSet(WorkoutSetId("old-s"), exercise.id, 0, weightKg = 60.0, reps = 8, completedAt = at)),
        )
    }

    @Test
    fun beatingLastWeekShowsTheRecordAndHidesItAfterAWhile() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val sessions = TestSessions()
            val clock = TestClock()
            sessions.data.value = listOf(lastWeek(clock))
            val model = newWorkoutViewModel(sessions, clock).also { store.put("m", it) }
            val effects = mutableListOf<WorkoutEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            runCurrent()
            model.onIntent(WorkoutIntent.StartFree)
            runCurrent()
            model.onIntent(WorkoutIntent.AddExercise(testBench.id))
            runCurrent()
            val id = model.state.value.session!!.sets.first().id
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
            "70".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.REPS))
            "8".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            runCurrent()
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()

            val record = model.state.value.record
            assertEquals(RecordKind.ESTIMATED_ONE_REP_MAX, record?.kind)
            assertEquals(id, record?.setId)
            assertTrue(effects.any { it is WorkoutEffect.Record })

            advanceTimeBy(5_000)
            runCurrent()
            assertNull(model.state.value.record)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun undoingTheSetRemovesItsRecord() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val sessions = TestSessions()
            val clock = TestClock()
            sessions.data.value = listOf(lastWeek(clock))
            val model = newWorkoutViewModel(sessions, clock).also { store.put("m", it) }
            runCurrent()
            model.onIntent(WorkoutIntent.StartFree)
            runCurrent()
            model.onIntent(WorkoutIntent.AddExercise(testBench.id))
            runCurrent()
            val id = model.state.value.session!!.sets.first().id
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
            "65".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.REPS))
            "8".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            runCurrent()
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertEquals(id, model.state.value.record?.setId)

            model.onIntent(WorkoutIntent.Undo)
            runCurrent()
            assertNull(model.state.value.record)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
