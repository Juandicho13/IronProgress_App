package com.olarte.ironprogress.ui.workout

import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.progression.StagnationKind
import com.olarte.ironprogress.domain.progression.StagnationProposal
import com.olarte.ironprogress.domain.progression.SuggestionReason
import com.olarte.ironprogress.domain.progression.suggestionRecord
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
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
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.days

/** El aviso de estancamiento en Entreno y sus propuestas (F2-05). */
@OptIn(ExperimentalCoroutinesApi::class)
class StagnationViewModelTest {

    private class OneRoutine(var detail: RoutineDetail) : RoutineRepository {
        override fun observeFolders() = MutableStateFlow(emptyList<Folder>())
        override fun observeRoutines() = MutableStateFlow(listOf(detail.routine))
        override suspend fun findRoutine(id: RoutineId) = detail.takeIf { it.routine.id == id }
        override suspend fun saveRoutine(detail: RoutineDetail) {
            this.detail = detail
        }
        override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit
        override suspend fun reorderFolders(ids: List<FolderId>) = Unit
        override suspend fun deleteRoutine(id: RoutineId) = Unit
        override suspend fun saveFolder(folder: Folder) = Unit
        override suspend fun deleteFolder(id: FolderId) = Unit
    }

    /** Cuatro sesiones terminadas de press, todas con 60 kg × 10: una meseta. */
    private fun plateau(): List<SessionDetail> = (0..3).map { round ->
        val clock = TestClock()
        val started = clock.now - (10 - round).days
        val session = WorkoutSession(SessionId("old$round"), null, Discipline.STRENGTH, started, started, SessionStatus.FINISHED)
        val block = SessionBlock(SessionBlockId("ob$round"), session.id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(SessionExerciseId("oe$round"), session.id, block.id, testBench.id, 0)
        val sets = (0..2).map {
            WorkoutSet(WorkoutSetId("os$round$it"), exercise.id, it, weightKg = 60.0, reps = 10, completedAt = started)
        }
        SessionDetail(session, listOf(block), listOf(exercise), sets)
    }

    private suspend fun TestScope.fixture(
        routines: RoutineRepository = EmptyRoutines(),
        block: suspend TestScope.(TestSessions, WorkoutViewModel) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val sessions = TestSessions()
        sessions.data.value = plateau()
        val clock = TestClock()
        var index = 0
        val ids = IdGenerator { "id-${index++}" }
        val model = WorkoutViewModel(
            sessions,
            TestCatalog(),
            routines,
            StartSession(sessions, ids, clock, TestCatalog()),
            CompleteSet(sessions, clock),
            AddSessionExercise(sessions, ids, TestCatalog()),
            ids,
            clock,
            com.olarte.ironprogress.ui.rest.RestNotifications { },
        ).also { store.put("model", it) }
        try {
            runCurrent()
            block(sessions, model)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aStalledExerciseInAFreeSessionShowsTheOptionsWithoutChangingTheRange() = runTest {
        fixture { _, model ->
            model.onIntent(WorkoutIntent.StartFree)
            runCurrent()
            model.onIntent(WorkoutIntent.AddExercise(testBench.id))
            runCurrent()
            val id = model.state.value.session!!.exercises.single().id
            val stagnation = assertNotNull(model.state.value.stagnation[id])
            assertEquals(StagnationKind.PLATEAU, stagnation.kind)
            assertEquals(listOf(StagnationProposal.DELOAD, StagnationProposal.SUBSTITUTE), stagnation.proposals)
            model.onIntent(WorkoutIntent.DismissStagnation(id))
            assertTrue(id in model.state.value.stagnationDismissed)
        }
    }

    @Test
    fun deloadLeavesSixtyPercentOfTheSetsAndClearsTheWarning() = runTest {
        var n = 0
        val routine = RoutineDetail(
            Routine(RoutineId("r"), null, "Empuje"),
            listOf(WorkoutBlock(BlockId("b"), 0, BlockFormat.SETS)),
            emptyList(),
            emptyList(),
        ).addExercise(IdGenerator { "t${n++}" }, BlockId("b"), testBench.id, sets = 5)
        val routines = OneRoutine(routine)
        fixture(routines) { sessions, model ->
            var m = 0
            val detail = StartSession(sessions, IdGenerator { "s${m++}" }, TestClock(), TestCatalog())(routine)
            runCurrent()
            val id = detail.exercises.single().id
            assertNotNull(model.state.value.stagnation[id])
            model.onIntent(WorkoutIntent.Deload(id))
            runCurrent()
            val sets = sessions.data.value.first { it.session.status == SessionStatus.IN_PROGRESS }.setsOf(id)
            assertEquals(3, sets.size)
            assertTrue(sets.all { it.suggestionRecord?.deload == true && it.suggestionRecord?.reason == SuggestionReason.DELOAD_SETS })
            assertNull(model.state.value.stagnation[id])
        }
    }

    @Test
    fun changingTheRangeUpdatesTheRoutineAndThePendingSets() = runTest {
        var n = 0
        val routine = RoutineDetail(
            Routine(RoutineId("r"), null, "Empuje"),
            listOf(WorkoutBlock(BlockId("b"), 0, BlockFormat.SETS)),
            emptyList(),
            emptyList(),
        ).addExercise(IdGenerator { "t${n++}" }, BlockId("b"), testBench.id, sets = 3)
        val routines = OneRoutine(routine)
        fixture(routines) { sessions, model ->
            var m = 0
            val detail = StartSession(sessions, IdGenerator { "s${m++}" }, TestClock(), TestCatalog())(routine)
            runCurrent()
            val id = detail.exercises.single().id
            assertEquals(StagnationProposal.CHANGE_RANGE, model.state.value.stagnation[id]?.proposals?.get(1))
            model.onIntent(WorkoutIntent.ChangeRange(id))
            runCurrent()
            assertTrue(routines.detail.setTemplates.all { it.repsMin == 5 && it.repsMax == 8 })
            val sets = sessions.data.value.first { it.session.status == SessionStatus.IN_PROGRESS }.setsOf(id)
            assertTrue(sets.all { it.reps == 5 })
            assertNull(model.state.value.stagnation[id])
        }
    }
}
