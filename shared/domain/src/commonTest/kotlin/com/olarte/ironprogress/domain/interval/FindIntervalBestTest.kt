package com.olarte.ironprogress.domain.interval

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class FindIntervalBestTest {
    private val day = Instant.parse("2026-11-10T18:00:00Z")

    private fun hiit(id: String, name: String, reps: List<Int>, discipline: Discipline = Discipline.HIIT): SessionDetail {
        val sid = SessionId(id)
        return SessionDetail(
            WorkoutSession(sid, null, discipline, day, day, SessionStatus.FINISHED, notes = name),
            listOf(SessionBlock(SessionBlockId("$id-b"), sid, 0, BlockFormat.INTERVALS, BlockResult.RepsPerRound(reps))),
            emptyList(),
            emptyList(),
        )
    }

    private class Sessions(all: List<SessionDetail>) : SessionRepository {
        private val data = MutableStateFlow(all)
        override fun observeActiveSession() = data.map { null }
        override suspend fun findSession(id: SessionId) = data.value.firstOrNull { it.session.id == id }
        override fun observeFinishedSessions() = data.map { list -> list.map { it.session } }
        override suspend fun createSession(detail: SessionDetail) = detail
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit
        override suspend fun saveSession(session: WorkoutSession) = Unit
        override suspend fun saveBlock(block: SessionBlock) = Unit
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) = Unit
        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    @Test
    fun theBestIsTheHighestAveragePerRoundOfTheSameName() = runTest {
        val sessions = Sessions(
            listOf(
                hiit("a", "Tabata", listOf(10, 10, 10)),
                hiit("b", "Tabata", listOf(12, 11, 13)),
                hiit("c", "Otro", listOf(30, 30)),
                hiit("d", "Tabata", listOf(0, 0, 0)),
                hiit("e", "Tabata", listOf(40, 40), discipline = Discipline.FUNCTIONAL),
            ),
        )
        assertEquals(BlockResult.RepsPerRound(listOf(12, 11, 13)), FindIntervalBest(sessions)("Tabata"))
        assertNull(FindIntervalBest(sessions)("Nuevo"))
    }
}
