package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.StartSession
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Lo que Juan encontró en el gimnasio: el teclado que pegaba cifras, el modo de cada ejercicio y
 * Entreno con una serie a la vez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutGymRoundTest {

    private suspend fun TestScope.fixture(block: suspend TestScope.(TestSessions, TestClock, WorkoutViewModel) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = TestSessions()
        val clock = TestClock()
        var index = 0
        val ids = IdGenerator { "id-${index++}" }
        val model = WorkoutViewModel(
            sessions,
            TestCatalog(),
            EmptyRoutines(),
            StartSession(sessions, ids, clock),
            CompleteSet(sessions, clock),
            AddSessionExercise(sessions, ids),
            ids,
            clock,
            com.olarte.ironprogress.ui.rest.RestNotifications { },
        ).also { store.put("model", it) }
        try {
            block(sessions, clock, model)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.start(model: WorkoutViewModel): WorkoutSetId {
        runCurrent()
        model.onIntent(WorkoutIntent.StartFree)
        runCurrent()
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        runCurrent()
        return model.state.value.session!!.sets.single().id
    }

    @Test
    fun typingOnAPrefilledValueReplacesItAndNextFieldStartsFresh() = runTest {
        fixture { sessions, _, model ->
            val id = start(model)
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
            "21".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            // Siguiente llega antes de que termine de guardarse el peso: igual pasa a las reps.
            model.onIntent(WorkoutIntent.NextField)
            assertEquals(WorkoutField.REPS, model.state.value.keyboard?.field)
            "22".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
            runCurrent()
            val set = sessions.data.value.single().sets.single()
            assertEquals(21.0, set.weightKg)
            assertEquals(22, set.reps)

            // Volver a abrir el peso ya escrito: la primera tecla lo reemplaza, no lo pega detrás.
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
            assertEquals("21", model.state.value.keyboard?.text)
            model.onIntent(WorkoutIntent.Key("6"))
            model.onIntent(WorkoutIntent.Key("0"))
            runCurrent()
            assertEquals(60.0, sessions.data.value.single().sets.single().weightKg)
        }
    }

    @Test
    fun theCurrentSetIsTheFirstPendingOneAndMovesOnAfterCompleting() = runTest {
        fixture { sessions, _, model ->
            val first = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            model.onIntent(WorkoutIntent.AddSet(exercise))
            model.onIntent(WorkoutIntent.AddSet(exercise))
            runCurrent()
            model.onIntent(WorkoutIntent.OpenKeyboard(first, WorkoutField.WEIGHT))
            model.onIntent(WorkoutIntent.Key("5"))
            model.onIntent(WorkoutIntent.Key("0"))
            model.onIntent(WorkoutIntent.NextField)
            model.onIntent(WorkoutIntent.Key("8"))
            runCurrent()

            fun current() = model.state.value.session!!.setsOf(exercise).firstOrNull { !it.isCompleted }
            assertEquals(first, current()?.id)
            model.onIntent(WorkoutIntent.Complete(first))
            runCurrent()
            val second = assertNotNull(current())
            assertTrue(second.id != first)
            assertEquals(2, model.state.value.session!!.setsOf(exercise).indexOf(second) + 1)
            // La nueva serie trae lo de la anterior para no escribirlo otra vez.
            assertEquals(50.0, second.weightKg)
            assertEquals(1, sessions.data.value.single().sets.count { it.isCompleted })
        }
    }

    @Test
    fun withoutRestTheNextSetComesRightAway() = runTest {
        fixture { _, _, model ->
            val first = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            model.onIntent(WorkoutIntent.AddSet(exercise))
            model.onIntent(WorkoutIntent.SetMode(exercise, ExerciseMode.NO_REST))
            runCurrent()
            assertEquals(ExerciseMode.NO_REST, model.state.value.modeOf(exercise))
            model.onIntent(WorkoutIntent.OpenKeyboard(first, WorkoutField.WEIGHT))
            model.onIntent(WorkoutIntent.Key("4"))
            model.onIntent(WorkoutIntent.NextField)
            model.onIntent(WorkoutIntent.Key("9"))
            runCurrent()
            model.onIntent(WorkoutIntent.Complete(first))
            runCurrent()
            assertNull(model.state.value.rest)

            model.onIntent(WorkoutIntent.SetMode(exercise, ExerciseMode.REST, 45))
            runCurrent()
            val exerciseNow = model.state.value.session!!.exercises.single()
            assertEquals(45, exerciseNow.restSeconds)
            val second = model.state.value.session!!.setsOf(exercise).first { !it.isCompleted }.id
            model.onIntent(WorkoutIntent.Complete(second))
            runCurrent()
            assertEquals(45, model.state.value.rest?.seconds)
        }
    }

    @Test
    fun aTimedSetCountsDownAndCompletesItself() = runTest {
        fixture { sessions, clock, model ->
            val id = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            model.onIntent(WorkoutIntent.SetMode(exercise, ExerciseMode.TIMED, 40))
            runCurrent()
            assertEquals(listOf(WorkoutField.WEIGHT, WorkoutField.DURATION), model.state.value.measuresOf(exercise))
            model.onIntent(WorkoutIntent.StartWork(id))
            assertEquals(40, model.state.value.work?.seconds)
            clock.now += 20.seconds
            model.refreshWork()
            assertNotNull(model.state.value.work)
            clock.now += 21.seconds
            model.refreshWork()
            runCurrent()
            assertNull(model.state.value.work)
            val saved = sessions.data.value.single().sets.single()
            assertTrue(saved.isCompleted)
            assertEquals(40, saved.durationSeconds)
        }
    }

    @Test
    fun supersetGroupsWithTheNextExerciseAndBackToRestUngroups() = runTest {
        fixture { _, _, model ->
            start(model)
            model.onIntent(WorkoutIntent.AddExercise(ExerciseId("bench")))
            runCurrent()
            val exercises = model.state.value.session!!.exercises.sortedBy { it.sortOrder }
            assertEquals(2, exercises.size)
            model.onIntent(WorkoutIntent.SetMode(exercises.first().id, ExerciseMode.SUPERSET))
            runCurrent()
            assertEquals(ExerciseMode.SUPERSET, model.state.value.modeOf(exercises.first().id))
            assertEquals(ExerciseMode.SUPERSET, model.state.value.modeOf(exercises.last().id))
            model.onIntent(WorkoutIntent.SetMode(exercises.first().id, ExerciseMode.REST))
            runCurrent()
            assertEquals(ExerciseMode.REST, model.state.value.modeOf(exercises.first().id))
            assertEquals(ExerciseMode.REST, model.state.value.modeOf(exercises.last().id))
        }
    }
}
