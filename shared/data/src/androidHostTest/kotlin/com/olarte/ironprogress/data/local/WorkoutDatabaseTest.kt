package com.olarte.ironprogress.data.local

import androidx.room3.Room
import com.olarte.ironprogress.domain.session.BlockResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tablas de la versión 2 contra una base Room real en memoria. */
class WorkoutDatabaseTest {

    private lateinit var database: IronProgressDatabase
    private val routines get() = database.routineDao()
    private val sessions get() = database.sessionDao()

    @BeforeTest
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
            factory = { IronProgressDatabaseConstructor.initialize() },
        ).buildIronProgressDatabase()
    }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun routine(id: String, folderId: String? = null) = RoutineEntity(
        id = id,
        folderId = folderId,
        name = "Rutina $id",
        notes = "",
        restSeconds = 120,
        sortOrder = 0,
        createdBy = "USER",
        sourceProgramVersionId = null,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun block(id: String, routineId: String? = null, sessionId: String? = null, result: String? = null) = WorkoutBlockEntity(
        id = id,
        routineId = routineId,
        sessionId = sessionId,
        sortOrder = 0,
        format = "SETS",
        rounds = null,
        workSeconds = null,
        restSeconds = null,
        timeCapSeconds = null,
        result = result,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun routineExercise(id: String, routineId: String, blockId: String, order: Int) = RoutineExerciseEntity(
        id = id,
        routineId = routineId,
        blockId = blockId,
        exerciseId = "back-squat",
        sortOrder = order,
        supersetGroup = null,
        restSeconds = null,
        unit = null,
        notes = "",
        createdAt = 1,
        updatedAt = 1,
    )

    private fun template(id: String, exerciseId: String) = SetTemplateEntity(
        id = id,
        routineExerciseId = exerciseId,
        sortOrder = 0,
        setType = "WORKING",
        repsMin = 8,
        repsMax = 12,
        targetRir = 2,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun session(id: String, status: String, startedAt: Long) = WorkoutSessionEntity(
        id = id,
        routineId = null,
        programDayId = null,
        gymProfileId = null,
        discipline = "STRENGTH",
        startedAt = startedAt,
        endedAt = if (status == "IN_PROGRESS") null else startedAt + 3_000_000,
        status = status,
        notes = "",
        adjustmentApplied = false,
        createdAt = startedAt,
        updatedAt = startedAt,
    )

    private fun set(id: String, exerciseId: String, order: Int, completedAt: Long?) = WorkoutSetEntity(
        id = id,
        sessionExerciseId = exerciseId,
        sortOrder = order,
        setType = "WORKING",
        weightKg = 100.0,
        reps = 5,
        durationSeconds = null,
        distanceMeters = null,
        calories = null,
        heightCm = null,
        breaths = null,
        watts = null,
        side = null,
        round = null,
        rir = 2,
        rpe = null,
        completedAt = completedAt,
        suggestion = null,
        endHeartRate = null,
        recoverySeconds = null,
        createdAt = 1,
        updatedAt = 1,
    )

    @Test
    fun replaceRoutineMarksWhatIsGoneAsDeleted() = runTest {
        routines.replaceRoutine(
            routine("r1"),
            listOf(block("b1", routineId = "r1")),
            listOf(routineExercise("e1", "r1", "b1", 0), routineExercise("e2", "r1", "b1", 1)),
            listOf(template("t1", "e1"), template("t2", "e2")),
            now = 10,
        )

        routines.replaceRoutine(
            routine("r1"),
            listOf(block("b1", routineId = "r1")),
            listOf(routineExercise("e1", "r1", "b1", 0)),
            listOf(template("t1", "e1")),
            now = 20,
        )

        assertEquals(listOf("e1"), routines.exercisesOfRoutine("r1").map { it.id })
        assertEquals(listOf("t1"), routines.setTemplatesOfRoutine("r1").map { it.id })
    }

    @Test
    fun deletingAFolderKeepsItsRoutines() = runTest {
        routines.upsertFolder(FolderEntity("f1", "Fuerza", 0, createdAt = 1, updatedAt = 1))
        routines.upsertRoutine(routine("r1", folderId = "f1"))

        routines.deleteFolder("f1", now = 5)

        assertTrue(routines.observeFolders().first().isEmpty())
        assertNull(routines.findRoutine("r1")?.folderId)
        routines.markRoutineDeleted("r1", now = 6)
        assertTrue(routines.observeRoutines().first().isEmpty())
    }

    @Test
    fun onlyTheNewestInProgressSessionIsActive() = runTest {
        sessions.upsertSession(session("old", "FINISHED", startedAt = 1_000))
        assertNull(sessions.observeActiveSession().first())

        sessions.upsertSession(session("now", "IN_PROGRESS", startedAt = 2_000))

        assertEquals("now", sessions.observeActiveSession().first()?.id)
        assertEquals(listOf("old"), sessions.observeFinishedSessions().first().map { it.id })
    }

    @Test
    fun setsAreReadInExerciseOrderAndSoftDeleted() = runTest {
        sessions.upsertSession(session("s1", "IN_PROGRESS", startedAt = 1_000))
        sessions.upsertBlock(block("sb1", sessionId = "s1"))
        sessions.upsertExercise(SessionExerciseEntity("x2", "s1", "sb1", "row", 1, null, "", createdAt = 1, updatedAt = 1))
        sessions.upsertExercise(SessionExerciseEntity("x1", "s1", "sb1", "bench", 0, null, "", createdAt = 1, updatedAt = 1))
        sessions.upsertSet(set("a", "x2", 0, completedAt = null))
        sessions.upsertSet(set("b", "x1", 1, completedAt = 1_500))
        sessions.upsertSet(set("c", "x1", 0, completedAt = 1_400))

        assertEquals(listOf("c", "b", "a"), sessions.setsOfSession("s1").map { it.id })

        sessions.markSetDeleted("b", now = 2_000)
        assertEquals(listOf("c", "a"), sessions.observeSetsOfSession("s1").first().map { it.id })
    }

    @Test
    fun recentSetsOnlyCountFinishedSessions() = runTest {
        sessions.upsertSession(session("done", "FINISHED", startedAt = 1_000))
        sessions.upsertSession(session("live", "IN_PROGRESS", startedAt = 9_000))
        sessions.upsertBlock(block("b-done", sessionId = "done"))
        sessions.upsertBlock(block("b-live", sessionId = "live"))
        sessions.upsertExercise(SessionExerciseEntity("x-done", "done", "b-done", "bench", 0, null, "", createdAt = 1, updatedAt = 1))
        sessions.upsertExercise(SessionExerciseEntity("x-live", "live", "b-live", "bench", 0, null, "", createdAt = 1, updatedAt = 1))
        sessions.upsertSet(set("old", "x-done", 0, completedAt = 1_200))
        sessions.upsertSet(set("new", "x-live", 0, completedAt = 9_200))

        assertEquals(listOf("old"), sessions.recentSetsOfExercise("bench", limit = 10).map { it.id })
    }

    @Test
    fun blockResultTravelsAsJson() = runTest {
        val results = listOf(
            BlockResult.Time(312),
            BlockResult.RoundsAndReps(rounds = 7, reps = 12),
            BlockResult.Rounds(10),
            BlockResult.RepsPerRound(listOf(20, 18, 17)),
            BlockResult.Completed(feeling = 4),
        )
        sessions.upsertSession(session("s1", "FINISHED", startedAt = 1_000))

        results.forEachIndexed { index, result ->
            sessions.upsertBlock(
                block("b$index", sessionId = "s1", result = RoomJson.encodeToString<BlockResult>(result)).copy(sortOrder = index),
            )
        }

        val read = sessions.blocksOfSession("s1").map { RoomJson.decodeFromString<BlockResult>(requireNotNull(it.result)) }
        assertEquals(results, read)
    }
}
