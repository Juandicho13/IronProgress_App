package com.olarte.ironprogress.data.workout

import androidx.room3.Room
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.SaveIntervalResult
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.voice.VoiceSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RoomIntervalTest {

    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private lateinit var database: IronProgressDatabase
    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_700_000_000_000))
    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }

    private val template = IntervalTemplate(
        id = IntervalTemplateId("t1"),
        name = "Ráfagas de 15",
        workSeconds = 15,
        restSeconds = 45,
        rounds = 4,
        exerciseIds = listOf(ExerciseId("burpee"), ExerciseId("kettlebell-swing")),
        voice = VoiceSettings(countdown = false),
    )

    @BeforeTest
    fun open() {
        database = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
            factory = { IronProgressDatabaseConstructor.initialize() },
        ).buildIronProgressDatabase()
    }

    @AfterTest
    fun close() {
        database.close()
    }

    @Test
    fun templatesRoundTripAndDelete() = runTest {
        val repository = RoomIntervalTemplateRepository(database.stageOneDao(), clock)

        repository.saveTemplate(template)

        assertEquals(template, repository.findTemplate(template.id))
        assertEquals(listOf(template), repository.observeTemplates().first())
        repository.deleteTemplate(template.id)
        assertNull(repository.findTemplate(template.id))
    }

    @Test
    fun aFinishedRunIsSavedAsAnHiitSessionWithRepsPerRound() = runTest {
        val sessions = RoomSessionRepository(database.sessionDao(), clock)
        val startedAt = clock.now
        clock.now = startedAt + template.totalSeconds.seconds

        val id = SaveIntervalResult(sessions, ids, clock)(template, startedAt, listOf(11, 16, 10, null))

        val saved = assertNotNull(sessions.findSession(id))
        assertEquals(Discipline.HIIT, saved.session.discipline)
        assertEquals(BlockResult.RepsPerRound(listOf(11, 16, 10, 0)), saved.blocks.single().result)
        assertEquals(listOf("burpee", "kettlebell-swing"), saved.exercises.map { it.exerciseId.value })
        assertEquals(listOf(1, 2, 3, 4), saved.sets.mapNotNull { it.round }.sorted())
        assertEquals(listOf(11, 10), saved.setsOf(saved.exercises.first().id).map { it.reps })
        // La última vez de burpee para la próxima: la serie más reciente de esta sesión.
        assertEquals(listOf(11, 10), sessions.previousSets(ExerciseId("burpee")).map { it.reps })
    }
}
