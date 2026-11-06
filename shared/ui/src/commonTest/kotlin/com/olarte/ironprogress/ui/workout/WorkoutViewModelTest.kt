package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private val bench = testBench
    private val catalog = TestCatalog()
    private val routines = EmptyRoutines()

    private suspend fun TestScope.fixture(
        notifications: com.olarte.ironprogress.ui.rest.RestNotifications = com.olarte.ironprogress.ui.rest.RestNotifications { },
        block: suspend TestScope.(TestSessions, TestClock, () -> WorkoutViewModel) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = TestSessions()
        val clock = TestClock()
        var index = 0
        val ids = IdGenerator { "id-${index++}" }
        var models = 0
        val factory = {
            WorkoutViewModel(
                sessions,
                catalog,
                routines,
                StartSession(sessions, ids, clock),
                CompleteSet(sessions, clock),
                AddSessionExercise(sessions, ids),
                ids,
                clock,
                notifications,
            )
                .also { store.put("model-${models++}", it) }
        }
        try {
            block(sessions, clock, factory)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.start(model: WorkoutViewModel): WorkoutSetId {
        runCurrent()
        model.onIntent(WorkoutIntent.StartFree)
        model.onIntent(WorkoutIntent.StartFree)
        runCurrent()
        model.onIntent(WorkoutIntent.AddExercise(bench.id))
        runCurrent()
        return model.state.value.session!!.sets.single().id
    }

    private fun TestScope.type(model: WorkoutViewModel, id: WorkoutSetId, field: WorkoutField, text: String) {
        model.onIntent(WorkoutIntent.OpenKeyboard(id, field))
        text.forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
        runCurrent()
    }

    @Test
    fun notificationsFollowSavedCompletionAdjustmentsAndCancellation() = runTest {
        val deadlines = mutableListOf<Long?>()
        fixture(com.olarte.ironprogress.ui.rest.RestNotifications { deadlines += it }) { sessions, clock, create ->
            val model = create()
            val id = start(model)
            assertTrue(deadlines.isEmpty())
            type(model, id, WorkoutField.WEIGHT, "40")
            type(model, id, WorkoutField.REPS, "8")
            sessions.failSave = true
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertTrue(deadlines.isEmpty())
            sessions.failSave = false
            model.onIntent(WorkoutIntent.Retry)
            runCurrent()
            val end = clock.now().toEpochMilliseconds() + 120000
            assertEquals(listOf<Long?>(end), deadlines)
            model.onIntent(WorkoutIntent.MinimizeRest)
            model.refreshRest()
            assertEquals(1, deadlines.size)
            model.onIntent(WorkoutIntent.AdjustRest(15))
            assertEquals(end + 15000, deadlines.last())
            model.onIntent(WorkoutIntent.AdjustRest(-15))
            assertEquals(end, deadlines.last())
            model.onIntent(WorkoutIntent.Undo)
            runCurrent()
            assertNull(deadlines.last())
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertEquals(end, deadlines.last())
            model.onIntent(WorkoutIntent.SkipRest)
            assertNull(deadlines.last())
        }
    }

    @Test
    fun finishWithADoneSetEndsTheSessionAndOpensTheSummary() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            val effects = mutableListOf<WorkoutEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            type(model, id, WorkoutField.WEIGHT, "60")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            model.onIntent(WorkoutIntent.Finish)
            runCurrent()
            val session = sessions.data.value.single().session
            assertEquals(SessionStatus.FINISHED, session.status)
            assertEquals(WorkoutEffect.Finished(session.id.value), effects.last())
            assertNull(model.state.value.session)
            assertNull(model.restController.state.value.timer)
        }
    }

    @Test
    fun poundsAreTypedAndShownButKilosAreStored() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            model.onIntent(WorkoutIntent.ToggleUnit(exercise))
            runCurrent()
            assertEquals(com.olarte.ironprogress.domain.catalog.WeightUnit.LB, model.state.value.unitOf(exercise))
            type(model, id, WorkoutField.WEIGHT, "137,8")
            val stored = sessions.data.value.single().sets.single().weightKg!!
            assertEquals(62.5, stored, 0.01)
            model.onIntent(WorkoutIntent.CloseKeyboard)
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
            assertEquals("137.8", model.state.value.keyboard?.text)
            // Volver a kg no cambia lo guardado.
            model.onIntent(WorkoutIntent.ToggleUnit(exercise))
            runCurrent()
            assertEquals(stored, sessions.data.value.single().sets.single().weightKg)
        }
    }

    @Test
    fun suggestedWarmupGoesBeforeTheWorkingSets() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            type(model, id, WorkoutField.WEIGHT, "100")
            model.onIntent(WorkoutIntent.CloseKeyboard)
            model.onIntent(WorkoutIntent.AddWarmup(exercise))
            model.onIntent(WorkoutIntent.AddWarmup(exercise))
            runCurrent()
            val sets = sessions.data.value.single().setsOf(exercise)
            assertEquals(listOf(20.0, 50.0, 70.0, 85.0, 100.0), sets.map { it.weightKg })
            assertEquals(4, sets.count { it.setType == SetType.WARMUP })
            assertEquals(id, sets.last().id)
        }
    }

    @Test
    fun editsSurviveRecreationAndDoubleCompletionIsIgnored() = runTest {
        fixture { sessions, clock, create ->
            val model = create()
            val id = start(model)
            assertEquals(1, sessions.data.value.size)
            type(model, id, WorkoutField.WEIGHT, "62,5")
            type(model, id, WorkoutField.REPS, "10")
            model.onIntent(WorkoutIntent.ChangeNote(id, "Controla la bajada"))
            model.onIntent(WorkoutIntent.ChangeType(id, SetType.DROP))
            model.onIntent(WorkoutIntent.Complete(id))
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertEquals(1, sessions.completedWrites)
            val restored = create()
            runCurrent()
            val saved = restored.state.value.session!!.sets.single()
            assertEquals(62.5, saved.weightKg)
            assertEquals(10, saved.reps)
            assertEquals("Controla la bajada", saved.notes)
            assertEquals(SetType.DROP, saved.setType)
            assertTrue(saved.isCompleted)
            clock.now += 30.minutes
            assertEquals(1800L, restored.elapsedSeconds())
        }
    }

    @Test
    fun copyPreviousRestoresValuesWithoutCompletingTheNewSet() = runTest {
        fixture { sessions, clock, create ->
            val model = create()
            val oldId = start(model)
            type(model, oldId, WorkoutField.WEIGHT, "62.5")
            type(model, oldId, WorkoutField.REPS, "10")
            model.onIntent(WorkoutIntent.Complete(oldId))
            runCurrent()
            val old = model.state.value.session!!.session
            sessions.saveSession(old.copy(status = SessionStatus.FINISHED, endedAt = clock.now))
            runCurrent()
            clock.now += 1.minutes
            val newId = start(model)
            type(model, newId, WorkoutField.WEIGHT, "50")
            model.onIntent(WorkoutIntent.CopyPrevious(newId))
            runCurrent()
            val set = model.state.value.session!!.sets.single()
            assertEquals(62.5, set.weightKg)
            assertEquals(10, set.reps)
            assertFalse(set.isCompleted)
            assertEquals(newId, set.id)
        }
    }

    @Test
    fun restStartsAfterSavingAndMinimizingDoesNotResetItsDeadline() = runTest {
        fixture { _, clock, create ->
            val model = create()
            val id = start(model)
            type(model, id, WorkoutField.WEIGHT, "40")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertTrue(model.state.value.restExpanded)
            val rest = assertNotNull(model.state.value.rest)
            model.onIntent(WorkoutIntent.MinimizeRest)
            clock.now += 1.minutes
            model.refreshRest()
            assertEquals(60L, model.state.value.rest!!.at(model.state.value.restNowMs).displaySecondsLeft)
            model.onIntent(WorkoutIntent.AdjustRest(15))
            assertEquals(75L, model.state.value.rest!!.at(model.state.value.restNowMs).displaySecondsLeft)
            model.onIntent(WorkoutIntent.ExpandRest)
            assertEquals(rest.startedAtMs, model.state.value.rest!!.startedAtMs)
            clock.now += 2.minutes
            model.refreshRest()
            assertNull(model.state.value.rest)
            assertFalse(model.state.value.restExpanded)
        }
    }

    @Test
    fun skippingOrUndoingCancelsRest() = runTest {
        fixture { _, _, create ->
            val model = create()
            val id = start(model)
            type(model, id, WorkoutField.WEIGHT, "40")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            model.onIntent(WorkoutIntent.Undo)
            runCurrent()
            assertNull(model.state.value.rest)
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertNotNull(model.state.value.rest)
            model.onIntent(WorkoutIntent.SkipRest)
            assertNull(model.state.value.rest)
            assertTrue(model.state.value.session!!.sets.single().isCompleted)
        }
    }

    @Test
    fun failedWritesKeepTheirOrderAndRetryTheLatestInput() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            sessions.failSave = true
            type(model, id, WorkoutField.WEIGHT, "62.5")
            assertTrue(model.state.value.failed)
            assertNull(model.state.value.session!!.sets.single().weightKg)
            assertNull(model.state.value.undoSetId)
            sessions.failSave = false
            model.onIntent(WorkoutIntent.Retry)
            runCurrent()
            assertEquals(62.5, model.state.value.session!!.sets.single().weightKg)
            assertEquals(0, model.state.value.pendingWrites)
            assertFalse(model.state.value.failed)
        }
    }

    @Test
    fun undoPreservesLaterEditsAndExpiresAfterFiveSeconds() = runTest {
        fixture { _, _, create ->
            val model = create()
            val id = start(model)
            type(model, id, WorkoutField.WEIGHT, "40")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            model.onIntent(WorkoutIntent.ChangeNote(id, "Nota posterior"))
            runCurrent()
            model.onIntent(WorkoutIntent.Undo)
            runCurrent()
            assertFalse(model.state.value.session!!.sets.single().isCompleted)
            assertEquals("Nota posterior", model.state.value.session!!.sets.single().notes)
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            advanceTimeBy(5000)
            runCurrent()
            assertNull(model.state.value.undoSetId)
            model.onIntent(WorkoutIntent.Undo)
            runCurrent()
            assertTrue(model.state.value.session!!.sets.single().isCompleted)
        }
    }

    @Test
    fun incompleteSetsDoNotCompleteAndRpeReplacesRir() = runTest {
        fixture { _, _, create ->
            val model = create()
            val id = start(model)
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            assertEquals(id, model.state.value.invalidSetId)
            assertFalse(model.state.value.session!!.sets.single().isCompleted)
            type(model, id, WorkoutField.RIR, "2")
            type(model, id, WorkoutField.RPE, "8,5")
            val saved = model.state.value.session!!.sets.single()
            assertNull(saved.rir)
            assertEquals(8.5, saved.rpe)
            assertNull(saved.withInput(WorkoutField.RPE, "8.3"))
            assertNull(saved.withInput(WorkoutField.REPS, "1.2"))
            assertNull(saved.withInput(WorkoutField.WEIGHT, "NaN"))
            assertNotNull(saved.withInput(WorkoutField.RIR, ""))
        }
    }

    @Test
    fun finishingWithNothingDoneDiscardsTheSessionSilently() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            start(model)
            val effects = mutableListOf<WorkoutEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            model.onIntent(WorkoutIntent.Finish)
            runCurrent()
            assertEquals(SessionStatus.DISCARDED, sessions.data.value.single().session.status)
            assertTrue(effects.none { it is WorkoutEffect.Finished })
            assertEquals(WorkoutEffect.Discarded, effects.last())
            assertNull(model.state.value.session)
        }
    }

    @Test
    fun cancelDiscardsEvenWithDoneSets() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            type(model, id, WorkoutField.WEIGHT, "60")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            model.onIntent(WorkoutIntent.Cancel)
            runCurrent()
            assertEquals(SessionStatus.DISCARDED, sessions.data.value.single().session.status)
            assertNull(model.restController.state.value.timer)
            assertNull(model.state.value.session)
        }
    }

    @Test
    fun rpeNeverGoesBelowOneAndRirStaysWhole() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            model.onIntent(WorkoutIntent.SetProMode(true))
            val id = start(model)
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.RPE))
            model.onIntent(WorkoutIntent.Adjust(-0.5))
            runCurrent()
            assertEquals(1.0, sessions.data.value.single().sets.single().rpe)
            // Un 0 al empezar o una coma sin número no hacen nada; 7,5 sí entra.
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.RPE))
            listOf("0", ",", "7", ",", "3", "5").forEach { model.onIntent(WorkoutIntent.Key(it)) }
            runCurrent()
            assertEquals("7.5", model.state.value.keyboard?.text)
            assertFalse(model.state.value.keyboard!!.invalid)
            assertEquals(7.5, sessions.data.value.single().sets.single().rpe)
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.RIR))
            listOf(",", "1", "1").forEach { model.onIntent(WorkoutIntent.Key(it)) }
            runCurrent()
            assertEquals("1", model.state.value.keyboard?.text)
            assertEquals(1, sessions.data.value.single().sets.single().rir)
        }
    }

    @Test
    fun basicModeHidesRirAndRpeFromTheKeyboard() = runTest {
        fixture { _, _, create ->
            val model = create()
            val id = start(model)
            assertEquals(listOf(WorkoutField.WEIGHT, WorkoutField.REPS), model.fieldsFor(id))
            model.onIntent(WorkoutIntent.SetProMode(true))
            assertEquals(listOf(WorkoutField.WEIGHT, WorkoutField.REPS, WorkoutField.RIR, WorkoutField.RPE), model.fieldsFor(id))
            model.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.RPE))
            model.onIntent(WorkoutIntent.SetProMode(false))
            assertNull(model.state.value.keyboard)
        }
    }

    @Test
    fun nextSetKeepsItsTargetRepsAndTakesTheWeight() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            val exercise = model.state.value.session!!.exercises.single().id
            model.onIntent(WorkoutIntent.AddSet(exercise))
            runCurrent()
            val second = model.state.value.session!!.sets.single { it.id != id }.id
            // La segunda serie ya trae 10 reps objetivo y no tiene peso.
            model.onIntent(WorkoutIntent.OpenKeyboard(second, WorkoutField.REPS))
            listOf("1", "0").forEach { model.onIntent(WorkoutIntent.Key(it)) }
            runCurrent()
            type(model, id, WorkoutField.WEIGHT, "20")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            val next = sessions.data.value.single().sets.single { it.id == second }
            assertEquals(20.0, next.weightKg)
            assertEquals(10, next.reps)
        }
    }

    @Test
    fun firstSetStartsWithWhatWasDoneLastTime() = runTest {
        fixture { sessions, _, create ->
            val model = create()
            val id = start(model)
            type(model, id, WorkoutField.WEIGHT, "62,5")
            type(model, id, WorkoutField.REPS, "8")
            model.onIntent(WorkoutIntent.Complete(id))
            runCurrent()
            model.onIntent(WorkoutIntent.Finish)
            runCurrent()
            val again = start(model)
            val prefilled = sessions.data.value.flatMap { it.sets }.single { it.id == again }
            assertEquals(62.5, prefilled.weightKg)
            assertEquals(8, prefilled.reps)
            assertNull(prefilled.completedAt)
        }
    }
}
