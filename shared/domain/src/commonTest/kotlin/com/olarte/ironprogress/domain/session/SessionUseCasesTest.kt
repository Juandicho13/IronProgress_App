package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.routine.addExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SessionUseCasesTest {

    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    /** Repositorio en memoria: solo lo que necesitan los casos de uso. */
    private class FakeSessionRepository : SessionRepository {
        val sessions = MutableStateFlow<Map<SessionId, WorkoutSession>>(emptyMap())
        val blocks = mutableListOf<SessionBlock>()
        val exercises = mutableListOf<SessionExercise>()
        val sets = linkedMapOf<WorkoutSetId, WorkoutSet>()

        private fun detail(session: WorkoutSession) = SessionDetail(
            session,
            blocks.filter { it.sessionId == session.id },
            exercises.filter { it.sessionId == session.id },
            sets.values.filter { set -> exercises.any { it.id == set.sessionExerciseId && it.sessionId == session.id } },
        )

        override fun observeActiveSession(): Flow<SessionDetail?> =
            sessions.map { all -> all.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }?.let(::detail) }

        override suspend fun findSession(id: SessionId) = sessions.value[id]?.let(::detail)

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

    private val start = Instant.fromEpochMilliseconds(1_700_000_000_000)

    private fun ids(): IdGenerator {
        var next = 0
        return IdGenerator { "id-${next++}" }
    }

    private fun routine(ids: IdGenerator): RoutineDetail = RoutineDetail(
        routine = Routine(RoutineId("r1"), null, "Torso"),
        blocks = listOf(WorkoutBlock(BlockId("b1"), 0, BlockFormat.SETS)),
        exercises = emptyList(),
        setTemplates = emptyList(),
    ).addExercise(ids, BlockId("b1"), ExerciseId("bench-press"), sets = 3)
        .addExercise(ids, BlockId("b1"), ExerciseId("barbell-row"), sets = 2)

    @Test
    fun startFromRoutineCopiesBlocksExercisesAndPendingSets() = runTest {
        val repo = FakeSessionRepository()
        val ids = ids()
        val detail = StartSession(repo, ids, FakeClock(start))(routine(ids))

        assertEquals(RoutineId("r1"), detail.session.routineId)
        assertEquals(start, detail.session.startedAt)
        assertEquals(1, detail.blocks.size)
        assertEquals(listOf("bench-press", "barbell-row"), detail.exercises.map { it.exerciseId.value })
        assertEquals(5, detail.sets.size)
        assertEquals(0, detail.workingSetsDone)
        assertNotNull(repo.observeActiveSessionValue())
    }

    @Test
    fun onlyOneSessionInProgress() = runTest {
        val repo = FakeSessionRepository()
        val start1 = StartSession(repo, ids(), FakeClock(start))
        val first = start1()
        val second = start1()

        assertEquals(first.session.id, second.session.id)
        assertEquals(1, repo.sessions.value.size)
    }

    @Test
    fun completingSetsSavesEachOneAndCountsVolume() = runTest {
        val repo = FakeSessionRepository()
        val ids = ids()
        val clock = FakeClock(start)
        val session = StartSession(repo, ids, clock)(routine(ids))
        val complete = CompleteSet(repo, clock)

        clock.now = start + 5.minutes
        val bench = session.sets.first()
        complete(bench.copy(weightKg = 80.0, reps = 8, rir = 2))
        complete(session.sets[1].copy(setType = SetType.WARMUP, weightKg = 40.0, reps = 10))

        val saved = repo.findSession(session.session.id)!!
        assertEquals(start + 5.minutes, saved.sets.first { it.id == bench.id }.completedAt)
        assertEquals(1, saved.workingSetsDone)
        assertEquals(640.0, saved.volumeKg)
    }

    @Test
    fun endingClosesTheSessionAndFreesTheSlot() = runTest {
        val repo = FakeSessionRepository()
        val clock = FakeClock(start)
        val session = StartSession(repo, ids(), clock)().session

        clock.now = start + 50.minutes
        val ended = EndSession(repo, clock)(session)

        assertEquals(SessionStatus.FINISHED, ended.status)
        assertEquals(start + 50.minutes, ended.endedAt)
        assertNull(repo.observeActiveSessionValue())
        assertFailsWith<IllegalArgumentException> { EndSession(repo, clock)(ended) }
    }

    @Test
    fun setValidation() {
        val id = WorkoutSetId("s")
        val exercise = SessionExerciseId("e")
        assertFailsWith<IllegalArgumentException> { WorkoutSet(id, exercise, 0, weightKg = -1.0) }
        assertFailsWith<IllegalArgumentException> { WorkoutSet(id, exercise, 0, rpe = 7.3) }
        assertFailsWith<IllegalArgumentException> { WorkoutSet(id, exercise, 0, round = 0) }
        WorkoutSet(id, exercise, 0, rpe = 8.5, calories = 15, heightCm = 60.0, breaths = 5, watts = 250, round = 3)
    }

    @Test
    fun sessionCannotEndBeforeItStarts() {
        assertFailsWith<IllegalArgumentException> {
            WorkoutSession(
                SessionId("s"),
                null,
                Discipline.STRENGTH,
                startedAt = start,
                endedAt = start - 1.minutes,
                status = SessionStatus.FINISHED,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutSession(SessionId("s"), null, Discipline.YOGA, startedAt = start, endedAt = start)
        }
    }

    private suspend fun FakeSessionRepository.observeActiveSessionValue(): SessionDetail? =
        sessions.value.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }?.let { findSession(it.id) }
}
