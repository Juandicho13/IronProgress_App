package com.olarte.ironprogress.ui.progress

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.progress.Period
import com.olarte.ironprogress.domain.progress.PeriodKind
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodSummaryViewModelTest {
    private val dayMs = 86_400_000L

    /** 12 de noviembre de 2026 a las 18:00 UTC. */
    private val now = Instant.parse("2026-11-12T18:00:00Z")

    private fun finished(id: String, at: Instant) = SessionDetail(
        WorkoutSession(SessionId(id), null, Discipline.STRENGTH, at, at, SessionStatus.FINISHED),
        emptyList<SessionBlock>(),
        emptyList<SessionExercise>(),
        emptyList<WorkoutSet>(),
    )

    private class Sessions(private val all: List<SessionDetail>) : SessionRepository {
        override fun observeActiveSession(): Flow<SessionDetail?> = MutableStateFlow(null)
        override suspend fun findSession(id: SessionId): SessionDetail? = all.firstOrNull { it.session.id == id }
        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = MutableStateFlow(all.map { it.session })
        override suspend fun createSession(detail: SessionDetail): SessionDetail = detail
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit
        override suspend fun saveSession(session: WorkoutSession) = Unit
        override suspend fun saveBlock(block: SessionBlock) = Unit
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) = Unit
        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private class Catalog : ExerciseRepository {
        override fun observeExercises(): Flow<List<Exercise>> = MutableStateFlow(emptyList())
        override suspend fun findExercise(id: ExerciseId): Exercise? = null
        override suspend fun saveCustomExercise(exercise: Exercise) = Unit
        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    private val dispatcher = StandardTestDispatcher()
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val utcDay: (Instant) -> Long = { it.toEpochMilliseconds().floorDiv(dayMs) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsOnTheCurrentMonthAndStopsAtTheFirstAndTheCurrent() = runTest(dispatcher) {
        val sessions = Sessions(listOf(finished("oct", Instant.parse("2026-10-03T10:00:00Z")), finished("nov", now)))
        val model = PeriodSummaryViewModel(sessions, Catalog(), clock, "", utcDay)
        backgroundScope.launch { model.state.collect {} }
        runCurrent()

        assertEquals(Period(PeriodKind.MONTH, 2026, 11), model.state.value.period)
        assertEquals(1, model.state.value.summary?.stats?.sessions)
        assertTrue(model.state.value.canGoBack)
        assertFalse(model.state.value.canGoForward)

        model.previous()
        runCurrent()
        assertEquals(Period(PeriodKind.MONTH, 2026, 10), model.state.value.period)
        assertFalse(model.state.value.canGoBack)
        model.previous()
        runCurrent()
        assertEquals(Period(PeriodKind.MONTH, 2026, 10), model.state.value.period)

        model.show(PeriodKind.YEAR)
        runCurrent()
        assertEquals(Period(PeriodKind.YEAR, 2026), model.state.value.period)
        assertEquals(2, model.state.value.summary?.stats?.sessions)
        model.show(PeriodKind.MONTH)
        runCurrent()
        assertEquals(Period(PeriodKind.MONTH, 2026, 11), model.state.value.period)
    }

    @Test
    fun keysRoundTripAndBadKeysFallBackToTheCurrentMonth() = runTest(dispatcher) {
        val year = Period(PeriodKind.YEAR, 2025)
        assertEquals(year, PeriodSummaryViewModel.parseKey(PeriodSummaryViewModel.keyOf(year)))
        assertNull(PeriodSummaryViewModel.parseKey("MONTH:2026:13"))

        val model = PeriodSummaryViewModel(Sessions(emptyList()), Catalog(), clock, "basura", utcDay)
        backgroundScope.launch { model.state.collect {} }
        runCurrent()
        assertEquals(Period(PeriodKind.MONTH, 2026, 11), model.state.value.period)
        assertFalse(model.state.value.hasAnySession)
        assertTrue(model.state.value.summary?.isEmpty == true)
    }
}
