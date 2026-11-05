package com.olarte.ironprogress.ui.discipline

import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.discipline.BreathingPattern
import com.olarte.ironprogress.domain.discipline.SaveGuidedSession
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.time.SegmentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedViewModelTest {

    private class FakeClock(var nowMs: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
    }

    private class Sessions : SessionRepository {
        val sessions = mutableListOf<WorkoutSession>()
        val blocks = mutableListOf<SessionBlock>()

        override fun observeActiveSession(): Flow<SessionDetail?> = flowOf(null)
        override suspend fun findSession(id: SessionId): SessionDetail? = null
        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = flowOf(sessions)
        override suspend fun createSession(detail: SessionDetail): SessionDetail = detail
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit
        override suspend fun saveSession(session: WorkoutSession) {
            sessions += session
        }
        override suspend fun saveBlock(block: SessionBlock) {
            blocks += block
        }
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) = Unit
        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock(1_000_000)
    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }

    private val models = mutableListOf<GuidedRunViewModel>()

    /** Si una aserción falla, el reloj del ViewModel no se queda corriendo para siempre. */
    private fun stopAfter(model: GuidedRunViewModel) {
        models += model
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun guarded(body: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            body()
        } finally {
            models.forEach { it.viewModelScope.cancel() }
        }
    }

    @Test
    fun theFlowNamesEachPoseAndSavesAYogaSessionAtTheEnd() = guarded {
        val sessions = Sessions()
        val model = FlowViewModel(SaveGuidedSession(sessions, ids, clock), clock, "sun-salutation-a", "es")
        val cues = mutableListOf<GuidedCue>()
        backgroundScope.launch { model.cues.collect { cues += it } }
        stopAfter(model)
        runCurrent()
        assertEquals("mountain", cues.single().label)

        clock.nowMs += 8_100
        advanceTimeBy(200)
        assertEquals(listOf("mountain", "arms-up"), cues.map { it.label })
        assertEquals("arms-up", model.state.value.timer?.label)

        model.onIntent(GuidedIntent.Skip)
        advanceTimeBy(200)
        assertEquals("forward-fold", cues.last().label)

        clock.nowMs += 10 * 60_000
        advanceTimeBy(200)
        runCurrent()
        assertTrue(model.state.value.saved)
        assertEquals(Discipline.YOGA, sessions.sessions.single().discipline)
        assertEquals("Saludo al sol A", sessions.sessions.single().notes)
        assertEquals(BlockFormat.FLOW, sessions.blocks.single().format)
    }

    @Test
    fun breathingWaitsForStartAndFinishingEarlySavesTheTimeDone() = guarded {
        val sessions = Sessions()
        val model = BreathingViewModel(SaveGuidedSession(sessions, ids, clock), clock, "Respiración guiada")
        stopAfter(model)
        model.choose(BreathingPattern.RELAXING)
        runCurrent()
        assertEquals(null, model.state.value.timer)

        model.onIntent(GuidedIntent.Start)
        runCurrent()
        model.choose(BreathingPattern.BOX)
        assertEquals(BreathingPattern.RELAXING, model.choice.value.pattern)
        clock.nowMs += 5_000
        advanceTimeBy(200)
        assertEquals(SegmentKind.HOLD_IN, model.state.value.timer?.kind)

        model.onIntent(GuidedIntent.Finish)
        runCurrent()
        assertTrue(model.state.value.saved)
        assertEquals(Discipline.BREATHING, sessions.sessions.single().discipline)
        assertEquals(BlockResult.Time(5), sessions.blocks.single().result)
    }
}
