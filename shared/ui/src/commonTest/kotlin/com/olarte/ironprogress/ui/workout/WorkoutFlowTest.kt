package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.SummarizeSession
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Entreno y Resumen vistos como flujos con Turbine (F1-27). */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutFlowTest {
    private suspend fun TestScope.withMain(block: suspend TestScope.(ViewModelStore) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            block(store)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.logSet(model: WorkoutViewModel) {
        runCurrent()
        model.onIntent(WorkoutIntent.StartFree)
        runCurrent()
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        runCurrent()
        val id = model.state.value.session!!.sets.single().id
        model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
        "60".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
        model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.REPS))
        model.onIntent(WorkoutIntent.Key("8"))
        runCurrent()
        model.onIntent(WorkoutIntent.Complete(id))
        runCurrent()
    }

    @Test
    fun finishingEmitsExactlyOneFinishedEffect() = runTest {
        withMain { store ->
            val sessions = TestSessions()
            val model = newWorkoutViewModel(sessions).also { store.put("w", it) }
            logSet(model)
            model.effects.test {
                model.onIntent(WorkoutIntent.Finish)
                runCurrent()
                val session = sessions.data.value.single().session
                // En cola quedaron abrir Entreno y la serie guardada; después llega Finished.
                assertEquals(WorkoutEffect.OpenWorkout(session.id.value), awaitItem())
                assertEquals(WorkoutEffect.SetCompleted, awaitItem())
                assertEquals(WorkoutEffect.Finished(session.id.value), awaitItem())
                assertEquals(SessionStatus.FINISHED, session.status)
                expectNoEvents()
            }
        }
    }

    @Test
    fun summaryGoesFromLoadingToTheSavedNumbers() = runTest {
        withMain { store ->
            val sessions = TestSessions()
            val model = newWorkoutViewModel(sessions).also { store.put("w", it) }
            logSet(model)
            model.onIntent(WorkoutIntent.Finish)
            runCurrent()
            val id = sessions.data.value.single().session.id.value
            val summary = SummaryViewModel(SummarizeSession(sessions), TestCatalog(), EmptyRoutines(), id).also { store.put("s", it) }
            summary.state.test {
                assertTrue(awaitItem().loading)
                runCurrent()
                val loaded = awaitItem()
                assertFalse(loaded.loading)
                assertFalse(loaded.failed)
                val numbers = assertNotNull(loaded.summary)
                assertEquals(480.0, numbers.volumeKg)
                assertEquals(1, numbers.workingSets)
                assertEquals("Press", loaded.exerciseName(testBench.id, "es"))
            }
        }
    }
}
