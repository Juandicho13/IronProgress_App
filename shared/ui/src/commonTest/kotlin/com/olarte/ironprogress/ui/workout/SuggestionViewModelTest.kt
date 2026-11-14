package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.progression.RepRange
import com.olarte.ironprogress.domain.progression.Suggestion
import com.olarte.ironprogress.domain.progression.SuggestionReason
import com.olarte.ironprogress.domain.progression.recordFor
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
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

/** La sugerencia del motor en Entreno (F2-02): se puede volver a poner y la siguiente sigue el peso real. */
@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionViewModelTest {
    private val exerciseId = SessionExerciseId("ex")

    private fun suggested(): SessionDetail {
        val session = WorkoutSession(SessionId("s"), null, Discipline.STRENGTH, TestClock().now)
        val block = SessionBlock(SessionBlockId("b"), session.id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(exerciseId, session.id, block.id, testBench.id, 0)
        val suggestion = Suggestion(SuggestionReason.INCREASE, 62.5, listOf(8, 8, 8), RepRange(8, 12, 2))
        val sets = (0..2).map { index ->
            val record = suggestion.recordFor(index).encode()
            WorkoutSet(WorkoutSetId("w$index"), exercise.id, index, weightKg = 62.5, reps = 8, suggestion = record)
        }
        return SessionDetail(session, listOf(block), listOf(exercise), sets)
    }

    private suspend fun TestScope.fixture(block: suspend TestScope.(TestSessions, WorkoutViewModel) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = TestSessions()
        sessions.createSession(suggested())
        val model = newWorkoutViewModel(sessions).also { store.put("model", it) }
        try {
            runCurrent()
            block(sessions, model)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun useBringsBackWhatTheEngineSuggested() = runTest {
        fixture { sessions, model ->
            val first = sessions.data.value.single().sets.first { it.id.value == "w0" }
            sessions.saveSet(first.copy(weightKg = 50.0, reps = 5))
            runCurrent()
            model.onIntent(WorkoutIntent.ApplySuggestion(first.id))
            runCurrent()
            val set = sessions.data.value.single().sets.first { it.id == first.id }
            assertEquals(62.5, set.weightKg)
            assertEquals(8, set.reps)
        }
    }

    @Test
    fun theNextSetFollowsTheWeightActuallyUsed() = runTest {
        fixture { sessions, model ->
            val first = sessions.data.value.single().sets.first { it.id.value == "w0" }
            sessions.saveSet(first.copy(weightKg = 60.0))
            runCurrent()
            model.onIntent(WorkoutIntent.Complete(first.id))
            runCurrent()
            val sets = sessions.data.value.single().sets.associateBy { it.id.value }
            assertEquals(60.0, sets.getValue("w1").weightKg)
            assertEquals(8, sets.getValue("w1").reps)
            // La tercera se mueve cuando se complete la segunda, no antes.
            assertEquals(62.5, sets.getValue("w2").weightKg)
            model.onIntent(WorkoutIntent.SkipRest)
        }
    }
}
