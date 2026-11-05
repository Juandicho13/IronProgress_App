package com.olarte.ironprogress.ui.interval

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
import com.olarte.ironprogress.domain.interval.IntervalPreset
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.IntervalTemplateRepository
import com.olarte.ironprogress.domain.interval.SaveIntervalResult
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.voice.VoiceLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
class IntervalViewModelTest {

    private class FakeClock(var nowMs: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
    }

    private class Templates : IntervalTemplateRepository {
        val saved = MutableStateFlow<Map<IntervalTemplateId, IntervalTemplate>>(emptyMap())

        override fun observeTemplates(): Flow<List<IntervalTemplate>> = saved.map { it.values.toList() }

        override suspend fun findTemplate(id: IntervalTemplateId) = saved.value[id]

        override suspend fun saveTemplate(template: IntervalTemplate) {
            saved.value = saved.value + (template.id to template)
        }

        override suspend fun deleteTemplate(id: IntervalTemplateId) {
            saved.value = saved.value - id
        }
    }

    private class Sessions : SessionRepository {
        val sessions = mutableListOf<WorkoutSession>()
        val blocks = mutableListOf<SessionBlock>()
        val sets = mutableListOf<WorkoutSet>()

        override fun observeActiveSession(): Flow<SessionDetail?> = flowOf(null)

        override suspend fun findSession(id: SessionId): SessionDetail? = null

        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())

        override suspend fun createSession(detail: SessionDetail) = detail

        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit

        override suspend fun previousSets(exerciseId: ExerciseId): List<WorkoutSet> {
            if (exerciseId.value != "burpee") return emptyList()
            return listOf(WorkoutSet(WorkoutSetId("old"), SessionExerciseId("x"), 0, reps = 11))
        }

        override suspend fun saveSession(session: WorkoutSession) {
            sessions += session
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

    private val catalog = object : ExerciseRepository {
        private val burpee = Exercise(
            ExerciseId("burpee"),
            LocalizedText("Burpees", "Burpees"),
            TrackingType.REPS,
            Equipment.BODYWEIGHT,
            MovementPattern.OTHER,
            listOf(ExerciseMuscle(Muscle.QUADS, MuscleRole.PRIMARY)),
        )

        override fun observeExercises(): Flow<List<Exercise>> = flowOf(listOf(burpee))

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun findExercise(id: ExerciseId) = burpee.takeIf { it.id == id }

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }

    @Test
    fun editorStepsStayInsideTheLimitsAndStartSavesFirst() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val templates = Templates()
            val model = IntervalEditorViewModel(templates, catalog, ids, null, "Mis intervalos")
            val effects = mutableListOf<IntervalEditorEffect>()
            val job = launch { model.effects.collect { effects += it } }
            runCurrent()

            model.onIntent(IntervalEditorIntent.ChoosePreset(IntervalPreset.TABATA))
            repeat(5) { model.onIntent(IntervalEditorIntent.Work(-5)) }
            model.onIntent(IntervalEditorIntent.Rounds(100))
            model.onIntent(IntervalEditorIntent.ToggleExercise(ExerciseId("burpee")))
            assertEquals(5, model.state.value.template.workSeconds)
            assertEquals(50, model.state.value.template.rounds)
            assertEquals(listOf("Burpees"), model.state.value.selected.map { it.name.es })

            model.onIntent(IntervalEditorIntent.Start)
            runCurrent()
            val id = model.state.value.template.id
            assertEquals(model.state.value.template, templates.saved.value[id])
            assertEquals(listOf<IntervalEditorEffect>(IntervalEditorEffect.Run(id.value)), effects)
            job.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aRunSpeaksLogsRepsAndSavesTheResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val templates = Templates()
            val template = IntervalTemplate(
                IntervalTemplateId("t"),
                "Corto",
                workSeconds = 10,
                restSeconds = 10,
                rounds = 2,
                exerciseIds = listOf(ExerciseId("burpee")),
            )
            templates.saveTemplate(template)
            val sessions = Sessions()
            val clock = FakeClock(1_700_000_000_000)
            val model = IntervalRunViewModel(templates, catalog, sessions, SaveIntervalResult(sessions, ids, clock), clock, "t", "es")
            val spoken = mutableListOf<VoiceLine>()
            val job = launch { model.voiceLines.collect { spoken += it } }
            runCurrent()

            assertEquals(11, model.state.value.lastTime["burpee"])
            assertEquals(VoiceLine.Go("burpee"), spoken.first())

            // A los 12 s está en el descanso de la ronda 1 y anota 13 reps.
            repeat(120) {
                clock.nowMs += 100
                advanceTimeBy(100)
                runCurrent()
            }
            assertEquals(SegmentKind.REST, model.state.value.timer?.kind)
            assertEquals(1, model.state.value.roundToLog)
            model.onIntent(IntervalRunIntent.LogReps(1, 13))

            // Termina a los 30 s. La ronda 2 no se anotó: se guarda 13, como la ronda anterior.
            repeat(200) {
                clock.nowMs += 100
                advanceTimeBy(100)
                runCurrent()
            }
            assertTrue(model.state.value.finished)
            assertTrue(model.state.value.saved)
            assertEquals(VoiceLine.Done, spoken.last())
            assertEquals(BlockResult.RepsPerRound(listOf(13, 13)), sessions.blocks.single().result)
            assertEquals(2, sessions.sets.size)
            job.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
