package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private class FakeClock(var now: Instant = Instant.parse("2026-11-05T12:00:00Z")) : Clock {
        override fun now() = now
    }

    private class Sessions : SessionRepository {
        val data = MutableStateFlow<List<SessionDetail>>(emptyList())
        var failSave = false
        var completedWrites = 0
        override fun observeActiveSession() = data.map { all -> all.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS } }
        override fun observeFinishedSessions() = data.map { all -> all.map { it.session }.filter { it.status == SessionStatus.FINISHED } }
        override suspend fun findSession(id: SessionId) = data.value.firstOrNull { it.session.id == id }
        override suspend fun createSession(detail: SessionDetail): SessionDetail {
            data.value.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS }?.let { return it }
            data.value += detail
            return detail
        }
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) {
            data.value =
                data.value.map {
                    if (it.session.id ==
                        exercise.sessionId
                    ) {
                        it.copy(
                            blocks = it.blocks.filterNot { b -> b.id == block.id } + block,
                            exercises =
                            it.exercises + exercise,
                            sets = it.sets + sets,
                        )
                    } else {
                        it
                    }
                }
        }
        override suspend fun saveSession(session: WorkoutSession) {
            data.value =
                data.value.map { if (it.session.id == session.id) it.copy(session = session) else it }
        }
        override suspend fun saveBlock(block: SessionBlock) = Unit
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) {
            check(!failSave) { "Fallo de escritura de prueba" }
            if (set.isCompleted) completedWrites++
            data.value =
                data.value.map { detail ->
                    if (detail.exercises.any { it.id == set.sessionExerciseId }) {
                        detail.copy(
                            sets =
                            detail.sets.filterNot { it.id == set.id } + set,
                        )
                    } else {
                        detail
                    }
                }
        }
        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private val bench =
        Exercise(
            ExerciseId("bench"),
            LocalizedText("Press", "Press"),
            TrackingType.WEIGHT_REPS,
            Equipment.BARBELL,
            MovementPattern.HORIZONTAL_PUSH,
            listOf(
                com.olarte.ironprogress.domain.catalog.ExerciseMuscle(
                    com.olarte.ironprogress.domain.catalog.Muscle.CHEST,
                    com.olarte.ironprogress.domain.catalog.MuscleRole.PRIMARY,
                ),
            ),
        )
    private val catalog = object : ExerciseRepository {
        override fun observeExercises() = MutableStateFlow(listOf(bench))
        override suspend fun findExercise(id: ExerciseId) = bench.takeIf { it.id == id }
        override suspend fun saveCustomExercise(exercise: Exercise) = Unit
        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }
    private val routines = object : RoutineRepository {
        override fun observeFolders() = MutableStateFlow(emptyList<Folder>())
        override fun observeRoutines() = MutableStateFlow(emptyList<Routine>())
        override suspend fun findRoutine(id: RoutineId): RoutineDetail? = null
        override suspend fun saveRoutine(detail: RoutineDetail) = Unit
        override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit
        override suspend fun reorderFolders(ids: List<FolderId>) = Unit
        override suspend fun deleteRoutine(id: RoutineId) = Unit
        override suspend fun saveFolder(folder: Folder) = Unit
        override suspend fun deleteFolder(id: FolderId) = Unit
    }

    private suspend fun TestScope.fixture(block: suspend TestScope.(Sessions, FakeClock, () -> WorkoutViewModel) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = Sessions()
        val clock = FakeClock()
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
}
