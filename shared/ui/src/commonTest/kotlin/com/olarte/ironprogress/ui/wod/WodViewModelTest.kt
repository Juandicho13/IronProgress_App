package com.olarte.ironprogress.ui.wod

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
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.wod.FindWodBest
import com.olarte.ironprogress.domain.wod.SaveWodResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WodViewModelTest {

    private class FakeClock(var nowMs: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
    }

    /** Guarda en memoria lo justo para el resultado y la marca anterior. */
    private class Sessions : SessionRepository {
        val finished = MutableStateFlow<List<WorkoutSession>>(emptyList())
        val blocks = mutableListOf<SessionBlock>()
        val sets = mutableListOf<WorkoutSet>()

        override fun observeActiveSession(): Flow<SessionDetail?> = flowOf(null)

        override suspend fun findSession(id: SessionId): SessionDetail? {
            val session = finished.value.firstOrNull { it.id == id } ?: return null
            return SessionDetail(session, blocks.filter { it.sessionId == id }, emptyList(), emptyList())
        }

        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = finished

        override suspend fun createSession(detail: SessionDetail) = detail

        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit

        override suspend fun saveSession(session: WorkoutSession) {
            finished.value = finished.value + session
        }

        override suspend fun saveBlock(block: SessionBlock) {
            blocks += block
        }

        override suspend fun saveExercise(exercise: SessionExercise) = Unit

        override suspend fun saveSet(set: WorkoutSet) {
            sets += set
        }

        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private fun exercise(id: String) = Exercise(
        ExerciseId(id),
        LocalizedText(id, id),
        TrackingType.REPS,
        Equipment.BODYWEIGHT,
        MovementPattern.OTHER,
        listOf(ExerciseMuscle(Muscle.QUADS, MuscleRole.PRIMARY)),
    )

    private val catalog = object : ExerciseRepository {
        override fun observeExercises(): Flow<List<Exercise>> = flowOf(listOf(exercise("burpee"), exercise("air-squat")))

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun findExercise(id: ExerciseId) = exercise(id.value)

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    @Test
    fun anAmrapRunsScoresAndSavesItsResultAndBecomesTheNextBest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var next = 0
            val ids = IdGenerator { "id-${next++}" }
            val sessions = Sessions()
            val clock = FakeClock(1_700_000_000_000)

            fun newModel() = WodViewModel(catalog, FindWodBest(sessions), SaveWodResult(sessions, ids, clock), clock)

            fun tickSeconds(model: WodViewModel, seconds: Int) = repeat(seconds * 10) {
                clock.nowMs += 100
                advanceTimeBy(100)
                runCurrent()
            }

            val model = newModel()
            runCurrent()
            model.onIntent(WodIntent.Name("Corto"))
            model.onIntent(WodIntent.Format(BlockFormat.AMRAP))
            repeat(11) { model.onIntent(WodIntent.Minutes(-1)) }
            model.onIntent(WodIntent.AddMovement(ExerciseId("burpee")))
            model.onIntent(WodIntent.AddMovement(ExerciseId("air-squat")))
            model.onIntent(WodIntent.Reps(1, 5))
            assertEquals(1, model.state.value.minutes)
            model.onIntent(WodIntent.Start)
            runCurrent()
            assertEquals(WodPhase.RUNNING, model.state.value.phase)

            // Dos rondas completas y un burpee: 2 + 10.
            repeat(5) { model.onIntent(WodIntent.Tick) }
            assertEquals("2 + 10", "${model.state.value.score.rounds} + ${model.state.value.score.reps(model.state.value.wod!!)}")
            tickSeconds(model, 61)
            assertEquals(WodPhase.FINISHED, model.state.value.phase)

            model.onIntent(WodIntent.Save)
            runCurrent()
            assertTrue(model.state.value.saved)
            val saved = sessions.finished.value.single()
            assertEquals(Discipline.FUNCTIONAL, saved.discipline)
            assertEquals(BlockResult.RoundsAndReps(2, 10), sessions.blocks.single().result)
            // 3 burpees (2 rondas y la parcial) y 2 sentadillas.
            assertEquals(5, sessions.sets.size)

            // La próxima vez con el mismo nombre aparece "Tu marca 2 + 10".
            val again = newModel()
            runCurrent()
            again.onIntent(WodIntent.Name("Corto"))
            again.onIntent(WodIntent.Format(BlockFormat.AMRAP))
            again.onIntent(WodIntent.AddMovement(ExerciseId("burpee")))
            again.onIntent(WodIntent.Start)
            runCurrent()
            assertEquals(BlockResult.RoundsAndReps(2, 10), again.state.value.best)
            again.onIntent(WodIntent.Finish)
            tickSeconds(again, 1)
            assertEquals(WodPhase.FINISHED, again.state.value.phase)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
