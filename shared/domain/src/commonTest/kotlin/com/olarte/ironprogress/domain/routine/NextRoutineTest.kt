package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class NextRoutineTest {

    private val baseInstant = Instant.fromEpochMilliseconds(1_730_736_000_000)

    private fun routine(id: String, name: String, sortOrder: Int, folderId: FolderId? = null) = Routine(
        id = RoutineId(id),
        folderId = folderId,
        name = name,
        sortOrder = sortOrder,
    )

    private fun finishedSession(id: String, routineId: RoutineId?, startedAt: Instant, endedAt: Instant = startedAt + 45.minutes) =
        WorkoutSession(
            id = SessionId(id),
            routineId = routineId,
            discipline = Discipline.STRENGTH,
            startedAt = startedAt,
            endedAt = endedAt,
            status = SessionStatus.FINISHED,
        )

    @Test
    fun emptyRoutinesReturnsNull() {
        val result = selectNextRoutine(routines = emptyList())
        assertNull(result)
    }

    @Test
    fun withoutFinishedSessionsSelectsFirstRoutineInFirstFolder() {
        val folderA = Folder(FolderId("fa"), "Gimnasio", sortOrder = 0)
        val folderB = Folder(FolderId("fb"), "Casa", sortOrder = 1)
        val routines = listOf(
            routine("r-home", "Cuerpo completo", sortOrder = 0, folderId = folderB.id),
            routine("r-push", "Empuje A", sortOrder = 1, folderId = folderA.id),
            routine("r-legs", "Pierna A", sortOrder = 0, folderId = folderA.id),
        )

        val next = selectNextRoutine(routines = routines, folders = listOf(folderB, folderA))

        assertNotNull(next)
        assertEquals(RoutineId("r-legs"), next.routine.id)
        assertEquals(folderA, next.folder)
        assertEquals(1, next.positionInRotation)
        assertEquals(2, next.rotationSize)
        assertFalse(next.isActiveSession)
    }

    @Test
    fun advancesThroughFolderRotationAndWrapsAround() {
        val gym = Folder(FolderId("gym"), "Gimnasio", sortOrder = 0)
        val legs = routine("r1", "Pierna A", sortOrder = 0, folderId = gym.id)
        val pull = routine("r2", "Tirón A", sortOrder = 1, folderId = gym.id)
        val push = routine("r3", "Empuje A", sortOrder = 2, folderId = gym.id)
        val routines = listOf(legs, pull, push)

        val afterLegs = selectNextRoutine(
            routines = routines,
            folders = listOf(gym),
            finishedSessions = listOf(finishedSession("s1", legs.id, baseInstant)),
        )
        assertEquals(pull.id, afterLegs?.routine?.id)
        assertEquals(2, afterLegs?.positionInRotation)
        assertEquals(3, afterLegs?.rotationSize)

        val afterPull = selectNextRoutine(
            routines = routines,
            folders = listOf(gym),
            finishedSessions = listOf(
                finishedSession("s1", legs.id, baseInstant),
                finishedSession("s2", pull.id, baseInstant + 1.days),
            ),
        )
        assertEquals(push.id, afterPull?.routine?.id)
        assertEquals(3, afterPull?.positionInRotation)

        val afterPush = selectNextRoutine(
            routines = routines,
            folders = listOf(gym),
            finishedSessions = listOf(
                finishedSession("s1", legs.id, baseInstant),
                finishedSession("s2", pull.id, baseInstant + 1.days),
                finishedSession("s3", push.id, baseInstant + 2.days),
            ),
        )
        assertEquals(legs.id, afterPush?.routine?.id)
        assertEquals(1, afterPush?.positionInRotation)
    }

    @Test
    fun ignoresFreeDiscardedAndDeletedRoutineSessions() {
        val r1 = routine("r1", "Torso A", sortOrder = 0)
        val r2 = routine("r2", "Pierna A", sortOrder = 1)
        val discarded = WorkoutSession(
            id = SessionId("s-discarded"),
            routineId = r2.id,
            discipline = Discipline.STRENGTH,
            startedAt = baseInstant + 3.hours,
            endedAt = baseInstant + 4.hours,
            status = SessionStatus.DISCARDED,
        )
        val freeSession = finishedSession("s-free", routineId = null, startedAt = baseInstant + 5.hours)
        val deletedRoutineSession = finishedSession("s-deleted", routineId = RoutineId("deleted"), startedAt = baseInstant + 6.hours)

        val next = selectNextRoutine(
            routines = listOf(r1, r2),
            finishedSessions = listOf(
                finishedSession("s1", r1.id, baseInstant),
                discarded,
                freeSession,
                deletedRoutineSession,
            ),
        )

        assertEquals(r2.id, next?.routine?.id)
    }

    @Test
    fun rotatesInsideTheFolderOfTheLastCompletedRoutine() {
        val gym = Folder(FolderId("gym"), "Gimnasio", sortOrder = 0)
        val mobility = Folder(FolderId("mob"), "Movilidad", sortOrder = 1)
        val gym1 = routine("g1", "Empuje", sortOrder = 0, folderId = gym.id)
        val gym2 = routine("g2", "Tirón", sortOrder = 1, folderId = gym.id)
        val mob1 = routine("m1", "Cadera", sortOrder = 0, folderId = mobility.id)
        val mob2 = routine("m2", "Hombros", sortOrder = 1, folderId = mobility.id)

        val nextMobility = selectNextRoutine(
            routines = listOf(gym1, gym2, mob1, mob2),
            folders = listOf(gym, mobility),
            finishedSessions = listOf(
                finishedSession("s1", gym1.id, baseInstant),
                finishedSession("s2", mob1.id, baseInstant + 1.days),
            ),
        )

        assertEquals(mob2.id, nextMobility?.routine?.id)
        assertEquals(mobility, nextMobility?.folder)
        assertEquals(2, nextMobility?.positionInRotation)
        assertEquals(2, nextMobility?.rotationSize)
    }

    @Test
    fun observeNextRoutineEmitsActiveSessionRoutineAndDetail() = runTest {
        var nextId = 0
        val ids = IdGenerator { "id-${nextId++}" }
        val routineRepo = FakeRoutineRepo()
        val sessionRepo = FakeSessionRepo()

        val detailA = RoutineDetail(
            routine = routine("r1", "Pierna A", sortOrder = 0),
            blocks = listOf(WorkoutBlock(BlockId("b1"), 0, BlockFormat.SETS)),
            exercises = emptyList(),
            setTemplates = emptyList(),
        ).addExercise(ids, BlockId("b1"), ExerciseId("barbell-back-squat"), sets = 4)

        val detailB = RoutineDetail(
            routine = routine("r2", "Empuje A", sortOrder = 1),
            blocks = listOf(WorkoutBlock(BlockId("b2"), 0, BlockFormat.SETS)),
            exercises = emptyList(),
            setTemplates = emptyList(),
        ).addExercise(ids, BlockId("b2"), ExerciseId("barbell-bench-press"), sets = 3)

        routineRepo.saveRoutine(detailA)
        routineRepo.saveRoutine(detailB)

        val observe = ObserveNextRoutine(routineRepo, sessionRepo)
        val initial = observe().first()
        assertEquals(RoutineId("r1"), initial?.detail?.routine?.id)
        assertNotNull(initial)
        assertFalse(initial.isActiveSession)
        assertTrue(initial.detail.estimatedDurationMinutes() > 0)

        sessionRepo.saveSession(
            WorkoutSession(
                id = SessionId("active-1"),
                routineId = RoutineId("r2"),
                discipline = Discipline.STRENGTH,
                startedAt = baseInstant,
                status = SessionStatus.IN_PROGRESS,
            ),
        )

        val active = observe().first()
        assertEquals(RoutineId("r2"), active?.detail?.routine?.id)
        assertTrue(active?.isActiveSession == true)
    }

    @Test
    fun buildWeekProgressMarksCompletedDaysAndStreak() {
        // 2026-11-04 es miércoles (índice 2).
        val wednesday = Instant.fromEpochMilliseconds(1_793_793_600_000)
        val monday = wednesday - 2.days
        val previousWeekWednesday = wednesday - 7.days

        val progress = buildWeekProgress(
            finishedSessions = listOf(
                finishedSession("s1", RoutineId("r1"), monday),
                finishedSession("s2", RoutineId("r2"), wednesday - 1.hours),
                finishedSession("s3", RoutineId("r1"), previousWeekWednesday),
            ),
            now = wednesday,
        )

        assertEquals(7, progress.days.size)
        assertTrue(progress.days[0].completed)
        assertFalse(progress.days[1].completed)
        assertTrue(progress.days[2].completed)
        assertTrue(progress.days[2].isToday)
        assertEquals(2, progress.trainedDaysCount)
        assertEquals(2, progress.streakWeeks)
    }

    @Test
    fun localSundayNightDoesNotBecomeMondayInUtc() {
        val now = Instant.parse("2026-11-09T02:00:00Z")
        val done = finishedSession("s", RoutineId("r"), now - 1.hours)
        val progress = buildWeekProgress(listOf(done), now) {
            (it.toEpochMilliseconds() - 5L * 3_600_000L).floorDiv(86_400_000L)
        }
        assertTrue(progress.days[6].isToday)
        assertTrue(progress.days[6].completed)
        assertEquals(1, progress.trainedDaysCount)
    }

    @Test
    fun ignoresFutureSessionsAndCountsTwoWorkoutsOnOneDayOnce() {
        val now = Instant.parse("2026-11-04T18:00:00Z")
        val progress = buildWeekProgress(
            listOf(
                finishedSession("a", RoutineId("r"), now - 2.hours),
                finishedSession("b", RoutineId("r"), now - 1.hours),
                finishedSession("future", RoutineId("r"), now + 1.days),
            ),
            now,
        )
        assertEquals(1, progress.trainedDaysCount)
        assertFalse(progress.days[3].completed)
    }

    private class FakeRoutineRepo : RoutineRepository {
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

        override suspend fun reorderRoutines(ids: List<RoutineId>) {
            ids.forEachIndexed { index, id ->
                details[id]?.let { details[id] = it.copy(routine = it.routine.copy(sortOrder = index)) }
            }
            routinesFlow.value = details.values.map { it.routine }.sortedBy { it.sortOrder }
        }

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

    private class FakeSessionRepo : SessionRepository {
        private val sessions = MutableStateFlow<Map<SessionId, WorkoutSession>>(emptyMap())

        override fun observeActiveSession(): Flow<SessionDetail?> = sessions.map { all ->
            all.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }?.let {
                SessionDetail(it, emptyList(), emptyList(), emptyList())
            }
        }

        override suspend fun findSession(id: SessionId): SessionDetail? =
            sessions.value[id]?.let { SessionDetail(it, emptyList(), emptyList(), emptyList()) }

        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> =
            sessions.map { all -> all.values.filter { it.status == SessionStatus.FINISHED } }

        override suspend fun saveSession(session: WorkoutSession) {
            sessions.value = sessions.value + (session.id to session)
        }

        override suspend fun saveBlock(block: SessionBlock) = Unit

        override suspend fun saveExercise(exercise: SessionExercise) = Unit

        override suspend fun saveSet(set: WorkoutSet) = Unit

        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }
}
