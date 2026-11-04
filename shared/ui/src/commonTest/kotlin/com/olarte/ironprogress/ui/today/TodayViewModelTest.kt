package com.olarte.ironprogress.ui.today

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.ObserveNextRoutine
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.routine.addExercise
import com.olarte.ironprogress.domain.session.EndSession
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
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
class TodayViewModelTest {

    private class FakeClock(var current: Instant) : Clock {
        override fun now(): Instant = current
    }

    @Test
    fun showsEmptyWhenNoRoutinesAndRotatesAsSessionsFinish() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var nextId = 0
            val ids = IdGenerator { "id-${nextId++}" }
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_793_793_600_000))
            val routineRepo = FakeRoutineRepository()
            val sessionRepo = FakeSessionRepository()
            val catalogRepo = FakeExerciseRepository()

            val viewModel = TodayViewModel(
                observeNextRoutine = ObserveNextRoutine(routineRepo, sessionRepo),
                exerciseRepository = catalogRepo,
                sessionRepository = sessionRepo,
                startSession = StartSession(sessionRepo, ids, clock),
                clock = clock,
            )
            runCurrent()

            assertFalse(viewModel.state.value.loading)
            assertNull(viewModel.state.value.nextRoutine)

            val folder = Folder(FolderId("gym"), "Gimnasio", sortOrder = 0)
            routineRepo.saveFolder(folder)

            val routineA = RoutineDetail(
                routine = Routine(RoutineId("r1"), folder.id, "Empuje A", sortOrder = 0),
                blocks = listOf(WorkoutBlock(BlockId("b1"), 0, BlockFormat.SETS)),
                exercises = emptyList(),
                setTemplates = emptyList(),
            ).addExercise(ids, BlockId("b1"), ExerciseId("bench"), sets = 4, repsMin = 8, repsMax = 10)

            val routineB = RoutineDetail(
                routine = Routine(RoutineId("r2"), folder.id, "Tirón A", sortOrder = 1),
                blocks = listOf(WorkoutBlock(BlockId("b2"), 0, BlockFormat.SETS)),
                exercises = emptyList(),
                setTemplates = emptyList(),
            ).addExercise(ids, BlockId("b2"), ExerciseId("row"), sets = 3, repsMin = 10, repsMax = 12)

            routineRepo.saveRoutine(routineA)
            routineRepo.saveRoutine(routineB)
            runCurrent()

            val firstCard = viewModel.state.value.nextRoutine
            assertNotNull(firstCard)
            assertEquals("Empuje A", firstCard.detail.routine.name)
            assertEquals("Gimnasio", firstCard.folderName)
            assertEquals(1, firstCard.positionInRotation)
            assertEquals(2, firstCard.rotationSize)
            assertEquals("Press banca", firstCard.exercises.single().name?.resolve("es"))
            assertEquals(4, firstCard.exercises.single().workingSetsCount)

            viewModel.onIntent(TodayIntent.StartWorkout)
            viewModel.onIntent(TodayIntent.StartWorkout)
            runCurrent()
            assertEquals(1, sessionRepo.sessionCount)

            val effect = viewModel.effects.first()
            assertTrue(effect is TodayEffect.OpenWorkout)
            assertTrue(viewModel.state.value.nextRoutine?.isActiveSession == true)

            val activeSession = sessionRepo.activeSession()
            assertNotNull(activeSession)
            clock.current += 45.minutes
            EndSession(sessionRepo, clock)(activeSession)
            runCurrent()

            val secondCard = viewModel.state.value.nextRoutine
            assertNotNull(secondCard)
            assertEquals("Tirón A", secondCard.detail.routine.name)
            assertEquals(2, secondCard.positionInRotation)
            assertFalse(secondCard.isActiveSession)
            assertEquals(1, viewModel.state.value.weekProgress.trainedDaysCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun resumesFreeSessionWithoutRequiringARoutineOrCreatingAnother() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val clock = FakeClock(Instant.parse("2026-11-04T18:00:00Z"))
            val routines = FakeRoutineRepository()
            val sessions = FakeSessionRepository()
            val active = WorkoutSession(SessionId("free"), null, Discipline.STRENGTH, clock.now())
            sessions.saveSession(active)
            val model = TodayViewModel(
                ObserveNextRoutine(routines, sessions),
                FakeExerciseRepository(),
                sessions,
                StartSession(sessions, IdGenerator { error("No debe crear una sesión") }, clock),
                clock,
            )
            runCurrent()
            assertNull(model.state.value.nextRoutine)
            assertEquals("free", model.state.value.activeSessionId)
            model.onIntent(TodayIntent.StartWorkout)
            runCurrent()
            assertEquals(TodayEffect.OpenWorkout("free"), model.effects.first())
            assertEquals(active, sessions.activeSession())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshUpdatesTheDayAfterMidnightWithoutDatabaseChanges() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val clock = FakeClock(Instant.parse("2026-11-09T04:59:00Z"))
            val routines = FakeRoutineRepository()
            val sessions = FakeSessionRepository()
            val model = TodayViewModel(
                ObserveNextRoutine(routines, sessions),
                FakeExerciseRepository(),
                sessions,
                StartSession(sessions, IdGenerator { "new" }, clock),
                clock,
                epochDay = { (it - 5L * 3_600_000L).floorDiv(86_400_000L) },
            )
            runCurrent()
            assertTrue(model.state.value.weekProgress.days[6].isToday)
            clock.current += 2.minutes
            model.onIntent(TodayIntent.Refresh)
            runCurrent()
            assertTrue(model.state.value.weekProgress.days[0].isToday)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeRoutineRepository : RoutineRepository {
        private val details = linkedMapOf<RoutineId, RoutineDetail>()
        private val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
        private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())

        override fun observeFolders(): Flow<List<Folder>> = foldersFlow

        override fun observeRoutines(): Flow<List<Routine>> = routinesFlow

        override suspend fun findRoutine(id: RoutineId): RoutineDetail? = details[id]

        override suspend fun saveRoutine(detail: RoutineDetail) {
            details[detail.routine.id] = detail
            routinesFlow.value = details.values.map { it.routine }.sortedBy { it.sortOrder }
        }

        override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit

        override suspend fun reorderFolders(ids: List<FolderId>) = Unit

        override suspend fun deleteRoutine(id: RoutineId) {
            details.remove(id)
            routinesFlow.value = details.values.map { it.routine }.sortedBy { it.sortOrder }
        }

        override suspend fun saveFolder(folder: Folder) {
            foldersFlow.value = (foldersFlow.value.filterNot { it.id == folder.id } + folder).sortedBy { it.sortOrder }
        }

        override suspend fun deleteFolder(id: FolderId) {
            foldersFlow.value = foldersFlow.value.filterNot { it.id == id }
        }
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessions = MutableStateFlow<Map<SessionId, WorkoutSession>>(emptyMap())
        val sessionCount: Int get() = sessions.value.size
        private val blocks = mutableListOf<SessionBlock>()
        private val exercises = mutableListOf<SessionExercise>()
        private val sets = linkedMapOf<WorkoutSetId, WorkoutSet>()

        fun activeSession(): WorkoutSession? = sessions.value.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

        private fun detail(session: WorkoutSession) = SessionDetail(
            session = session,
            blocks = blocks.filter { it.sessionId == session.id },
            exercises = exercises.filter { it.sessionId == session.id },
            sets = sets.values.filter { set -> exercises.any { it.id == set.sessionExerciseId && it.sessionId == session.id } },
        )

        override fun observeActiveSession(): Flow<SessionDetail?> =
            sessions.map { all -> all.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }?.let(::detail) }

        override suspend fun findSession(id: SessionId): SessionDetail? = sessions.value[id]?.let(::detail)

        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> =
            sessions.map { all -> all.values.filter { it.status == SessionStatus.FINISHED } }

        override suspend fun saveSession(session: WorkoutSession) {
            sessions.value = sessions.value + (session.id to session)
        }

        override suspend fun saveBlock(block: SessionBlock) {
            blocks += block
        }

        override suspend fun saveExercise(exercise: SessionExercise) {
            exercises += exercise
        }

        override suspend fun saveSet(set: WorkoutSet) {
            sets[set.id] = set
        }

        override suspend fun deleteSet(id: WorkoutSetId) {
            sets.remove(id)
        }
    }

    private class FakeExerciseRepository : ExerciseRepository {
        private val list = MutableStateFlow(
            listOf(
                Exercise(
                    id = ExerciseId("bench"),
                    name = LocalizedText(es = "Press banca", en = "Bench press"),
                    equipment = Equipment.BARBELL,
                    pattern = MovementPattern.HORIZONTAL_PUSH,
                    trackingType = TrackingType.WEIGHT_REPS,
                    muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY)),
                ),
                Exercise(
                    id = ExerciseId("row"),
                    name = LocalizedText(es = "Remo con barra", en = "Barbell row"),
                    equipment = Equipment.BARBELL,
                    pattern = MovementPattern.HORIZONTAL_PULL,
                    trackingType = TrackingType.WEIGHT_REPS,
                    muscles = listOf(ExerciseMuscle(Muscle.LATS, MuscleRole.PRIMARY)),
                ),
            ),
        )

        override fun observeExercises(): Flow<List<Exercise>> = list

        override suspend fun findExercise(id: ExerciseId): Exercise? = list.value.firstOrNull { it.id == id }

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }
}
