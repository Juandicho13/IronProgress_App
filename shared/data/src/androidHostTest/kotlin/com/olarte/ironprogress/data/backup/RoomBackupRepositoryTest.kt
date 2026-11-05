package com.olarte.ironprogress.data.backup

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.data.workout.RoomIntervalTemplateRepository
import com.olarte.ironprogress.data.workout.RoomSessionRepository
import com.olarte.ironprogress.domain.backup.RestoreResult
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.interval.IntervalTemplateId
import com.olarte.ironprogress.domain.interval.SaveIntervalResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RoomBackupRepositoryTest {

    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_700_000_000_000))
    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }
    private val opened = mutableListOf<IronProgressDatabase>()

    private fun newDatabase(): IronProgressDatabase = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
        factory = { IronProgressDatabaseConstructor.initialize() },
    ).buildIronProgressDatabase().also { opened += it }

    private suspend fun seeded(): IronProgressDatabase = newDatabase().also { RoomExerciseRepository(it.catalogDao()).ensureSeeded() }

    @AfterTest
    fun close() = opened.forEach { it.close() }

    private val template = IntervalTemplate(
        id = IntervalTemplateId("t1"),
        name = "Ráfagas, de 15",
        workSeconds = 15,
        restSeconds = 45,
        rounds = 2,
        exerciseIds = listOf(ExerciseId("push-up")),
    )

    private suspend fun IronProgressDatabase.count(sql: String): Long = useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            statement.step()
            statement.getLong(0)
        }
    }

    @Test
    fun aBackupRestoresEverythingOnAnotherPhone() = runTest {
        val first = seeded()
        val firstSessions = RoomSessionRepository(first.sessionDao(), clock)
        RoomIntervalTemplateRepository(first.stageOneDao(), clock).saveTemplate(template)
        val startedAt = clock.now
        clock.now = startedAt + template.totalSeconds.seconds
        val sessionId = SaveIntervalResult(firstSessions, ids, clock)(template, startedAt, listOf(11, 16))
        first.useWriterConnection { connection ->
            connection.executeSQL(
                "INSERT INTO exercises (id, name_es, name_en, tracking_type, equipment, movement_pattern, is_unilateral, " +
                    "preferred_unit, is_custom, is_archived, discipline, level, aliases) " +
                    "VALUES ('mio', 'Remo raro', 'Odd row', 'WEIGHT_REPS', 'DUMBBELL', 'HORIZONTAL_PULL', 0, NULL, 1, 0, 'STRENGTH', NULL, '')",
            )
            connection.executeSQL("UPDATE exercises SET is_archived = 1 WHERE id = 'push-up'")
        }
        val seedCount = first.count("SELECT COUNT(*) FROM exercises WHERE is_custom = 0")
        val backup = RoomBackupRepository(first, clock).createBackup()

        val second = newDatabase()
        val result = RoomBackupRepository(second, clock).restore(backup)

        assertIs<RestoreResult.Restored>(result)
        assertEquals(template, RoomIntervalTemplateRepository(second.stageOneDao(), clock).findTemplate(template.id))
        val restored = assertNotNull(RoomSessionRepository(second.sessionDao(), clock).findSession(sessionId))
        assertEquals(listOf(11, 16), restored.sets.map { it.reps })
        assertEquals(1, second.count("SELECT COUNT(*) FROM exercises WHERE id = 'mio' AND is_custom = 1"))
        assertEquals(1, second.count("SELECT is_archived FROM exercises WHERE id = 'push-up'"))
        assertEquals(seedCount, second.count("SELECT COUNT(*) FROM exercises WHERE is_custom = 0"))
        assertTrue(second.count("SELECT COUNT(*) FROM exercise_muscles WHERE exercise_id = 'push-up'") > 0)
        assertEquals(1, RoomSessionRepository(second.sessionDao(), clock).observeFinishedSessions().first().size)
    }

    @Test
    fun aBadFileChangesNothing() = runTest {
        val database = newDatabase()
        RoomIntervalTemplateRepository(database.stageOneDao(), clock).saveTemplate(template)
        val repository = RoomBackupRepository(database, clock)

        assertEquals(RestoreResult.NotABackup, repository.restore("no es json"))
        assertEquals(RestoreResult.NotABackup, repository.restore("""{"format":"otra"}"""))
        assertEquals(
            RestoreResult.TooNew,
            repository.restore("""{"format":"ironprogress-backup","version":1,"schemaVersion":999,"tables":{}}"""),
        )
        assertEquals(1, database.count("SELECT COUNT(*) FROM interval_templates"))
    }

    @Test
    fun csvHasOneRowPerDoneSetAndQuotesCommas() = runTest {
        val database = seeded()
        val sessions = RoomSessionRepository(database.sessionDao(), clock)
        val startedAt = clock.now
        clock.now = startedAt + template.totalSeconds.seconds
        SaveIntervalResult(sessions, ids, clock)(template, startedAt, listOf(11, 16))

        val lines = RoomBackupRepository(database, clock).exportSetsCsv("es").trim().lines()

        assertTrue(lines.first().startsWith("session_started_at,routine,exercise"))
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("Flexiones"), lines[1])
    }
}
