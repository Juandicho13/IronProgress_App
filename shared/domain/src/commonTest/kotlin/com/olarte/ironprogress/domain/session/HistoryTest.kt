package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HistoryTest {

    private class Memory(initial: List<SessionDetail>) : SessionRepository {
        val data = MutableStateFlow(initial)
        override fun observeActiveSession(): Flow<SessionDetail?> =
            data.map { all -> all.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS } }
        override suspend fun findSession(id: SessionId) = data.value.firstOrNull { it.session.id == id }
        override fun observeFinishedSessions() = data.map { all -> all.map { it.session }.filter { it.status == SessionStatus.FINISHED } }
        override suspend fun createSession(detail: SessionDetail) = detail
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit
        override suspend fun saveSession(session: WorkoutSession) = Unit
        override suspend fun saveBlock(block: SessionBlock) = Unit
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) {
            data.value = data.value.map { d -> d.copy(sets = d.sets.map { if (it.id == set.id) set else it }) }
        }
        override suspend fun deleteSet(id: WorkoutSetId) {
            data.value = data.value.map { d -> d.copy(sets = d.sets.filterNot { it.id == id }) }
        }
    }

    private val now = Instant.parse("2026-11-05T18:00:00Z")

    private fun detail(id: String, status: SessionStatus, start: Instant): SessionDetail {
        val sid = SessionId(id)
        val sets = SessionBlock(SessionBlockId("$id-sets"), sid, 0, BlockFormat.SETS)
        val amrap = SessionBlock(SessionBlockId("$id-amrap"), sid, 1, BlockFormat.AMRAP, BlockResult.RoundsAndReps(5, 3))
        val bench = SessionExercise(SessionExerciseId("$id-e"), sid, sets.id, ExerciseId("bench"), 0)
        val burpee = SessionExercise(SessionExerciseId("$id-b"), sid, amrap.id, ExerciseId("burpee"), 1)
        val ended = if (status == SessionStatus.IN_PROGRESS) null else start + 50.minutes
        return SessionDetail(
            WorkoutSession(sid, null, Discipline.STRENGTH, start, ended, status),
            listOf(sets, amrap),
            listOf(bench, burpee),
            listOf(
                WorkoutSet(WorkoutSetId("$id-1"), bench.id, 1, weightKg = 60.0, reps = 10, completedAt = start + 10.minutes),
                WorkoutSet(WorkoutSetId("$id-2"), bench.id, 2, weightKg = 60.0, reps = 8),
                WorkoutSet(WorkoutSetId("$id-3"), burpee.id, 1, reps = 10, completedAt = start + 40.minutes),
            ),
        )
    }

    @Test
    fun editingYesterdaysSetShowsItCorrected() = runTest {
        val yesterday = detail("ayer", SessionStatus.FINISHED, now - 1.days)
        val repo = Memory(listOf(yesterday))
        val original = yesterday.sets.first()

        EditPastSet(repo)(yesterday.session.id, original.copy(weightKg = 62.5, reps = 9, sortOrder = 99))

        val fixed = repo.findSession(yesterday.session.id)!!.sets.first { it.id == original.id }
        assertEquals(62.5, fixed.weightKg)
        assertEquals(9, fixed.reps)
        assertEquals(original.sortOrder, fixed.sortOrder)
        assertEquals(original.completedAt, fixed.completedAt)
        // El volumen de la sesión ya sale con la serie corregida.
        assertEquals(62.5 * 9, repo.findSession(yesterday.session.id)!!.volumeKg)
    }

    @Test
    fun aPendingSetBecomesDoneAtTheEndOfTheSession() = runTest {
        val yesterday = detail("ayer", SessionStatus.FINISHED, now - 1.days)
        val repo = Memory(listOf(yesterday))
        val saved = EditPastSet(repo)(yesterday.session.id, yesterday.sets[1].copy(reps = 6))
        assertEquals(yesterday.session.endedAt, saved.completedAt)
    }

    @Test
    fun onlyFinishedSessionsAndTheirOwnSetsAreEditable() = runTest {
        val active = detail("hoy", SessionStatus.IN_PROGRESS, now)
        val past = detail("ayer", SessionStatus.FINISHED, now - 1.days)
        val repo = Memory(listOf(active, past))
        assertFailsWith<IllegalArgumentException> { EditPastSet(repo)(active.session.id, active.sets.first().copy(reps = 1)) }
        assertFailsWith<IllegalArgumentException> { EditPastSet(repo)(past.session.id, active.sets.first()) }
        assertFailsWith<IllegalArgumentException> { DeletePastEntry(repo).set(active.session.id, active.sets.first().id) }
    }

    @Test
    fun deletingASetOrABlockRemovesItsSets() = runTest {
        val past = detail("ayer", SessionStatus.FINISHED, now - 1.days)
        val repo = Memory(listOf(past))
        DeletePastEntry(repo).set(past.session.id, past.sets.first().id)
        assertNull(repo.findSession(past.session.id)!!.sets.firstOrNull { it.id == past.sets.first().id })
        DeletePastEntry(repo).block(past.session.id, SessionBlockId("ayer-amrap"))
        assertEquals(listOf(WorkoutSetId("ayer-2")), repo.findSession(past.session.id)!!.sets.map { it.id })
    }

    @Test
    fun calendarMathMatchesKnownDates() {
        assertEquals(CivilDate(1970, 1, 1), CivilDate.fromEpochDay(0))
        val day = CivilDate(2026, 11, 5)
        assertEquals(day, CivilDate.fromEpochDay(day.toEpochDay()))
        assertEquals(3, day.dayOfWeek) // jueves
        assertEquals(CivilDate(2024, 2, 29), CivilDate.fromEpochDay(CivilDate(2024, 3, 1).toEpochDay() - 1))
        val november = monthGrid(2026, 11)
        // Noviembre de 2026 empieza en domingo: seis huecos y luego el 1.
        assertEquals(6, november.first().count { it == null })
        assertEquals(CivilDate(2026, 11, 1), november.first()[6])
        assertEquals(30, november.flatten().count { it != null })
        november.forEach { assertEquals(7, it.size) }
    }
}
