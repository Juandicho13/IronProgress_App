package com.olarte.ironprogress.data.workout

import androidx.room3.Room
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.CreateRoutine
import com.olarte.ironprogress.domain.routine.CreatedBy
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.addExercise
import com.olarte.ironprogress.domain.routine.groupAsSuperset
import com.olarte.ironprogress.domain.routine.removeExercise
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.EndSession
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.Side
import com.olarte.ironprogress.domain.session.StartSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Repositorios de rutinas y sesiones contra una base Room real en memoria. */
class RoomWorkoutRepositoryTest {

    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private lateinit var database: IronProgressDatabase
    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_700_000_000_000))
    private lateinit var routines: RoomRoutineRepository
    private lateinit var sessions: RoomSessionRepository
    private var nextId = 0
    private val ids = IdGenerator { "id-${nextId++}" }

    @BeforeTest
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
            factory = { IronProgressDatabaseConstructor.initialize() },
        ).buildIronProgressDatabase()
        routines = RoomRoutineRepository(database.routineDao(), clock)
        sessions = RoomSessionRepository(database.sessionDao(), clock)
    }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun duplicatesReordersAndDeletesWithoutChangingTheOriginal() = runTest {
        val edit = com.olarte.ironprogress.domain.routine.EditRoutines(routines, ids)
        edit.folder("Gimnasio")
        edit.folder("Casa")
        val folders = routines.observeFolders().first()
        edit.moveFolder(folders.last().id, 0)
        assertEquals(folders.last().id, routines.observeFolders().first().first().id)
        val original = edit.draft("Torso", folders.first().id).let {
            it.addExercise(ids, it.blocks.single().id, ExerciseId("barbell-row"))
        }
        edit.save(original)
        val copy = edit.duplicate(original.routine.id, "Torso copia")
        edit.move(copy.routine.id, 0)
        assertEquals(copy.routine.id, routines.observeRoutines().first().first().id)
        assertEquals(original.exercises, routines.findRoutine(original.routine.id)!!.exercises)
        assertEquals(copy.setTemplates, routines.findRoutine(copy.routine.id)!!.setTemplates)
        routines.deleteFolder(folders.first().id)
        assertTrue(routines.observeRoutines().first().all { it.folderId == null })
        routines.deleteRoutine(copy.routine.id)
        assertNull(routines.findRoutine(copy.routine.id))
        assertNotNull(routines.findRoutine(original.routine.id))
    }

    @Test
    fun aRoutineRoundTripsWithBlocksSupersetsAndTemplates() = runTest {
        routines.saveFolder(Folder(FolderId("f1"), "Fuerza", 0))
        var detail = CreateRoutine(routines, ids)("Torso A", FolderId("f1"))
        val block = detail.blocks.single().id
        detail = detail
            .addExercise(ids, block, ExerciseId("barbell-bench-press"), sets = 4)
            .addExercise(ids, block, ExerciseId("barbell-row"))
            .addExercise(ids, block, ExerciseId("plank"), sets = 2, repsMin = null, repsMax = null, targetRir = null)
        detail = detail.groupAsSuperset(detail.exercises.take(2).map { it.id })
        routines.saveRoutine(detail)

        val read = assertNotNull(routines.findRoutine(detail.routine.id))

        assertEquals(detail.routine.copy(), read.routine)
        assertEquals(CreatedBy.User, read.routine.createdBy)
        assertEquals(detail.blocks, read.blocks)
        assertEquals(detail.exercises.sortedBy { it.sortOrder }, read.exercises)
        assertEquals(detail.setTemplates.toSet(), read.setTemplates.toSet())
        assertEquals(listOf(2, 1), read.supersetsOf(block).map { it.size })
        assertEquals(listOf("Torso A"), routines.observeRoutines().first().map { it.name })
    }

    @Test
    fun removingAnExerciseAndSavingAgainDropsItsSets() = runTest {
        var detail = CreateRoutine(routines, ids)("Pierna")
        val block = detail.blocks.single().id
        detail = detail
            .addExercise(ids, block, ExerciseId("back-squat"))
            .addExercise(ids, block, ExerciseId("leg-press"))
        routines.saveRoutine(detail)

        routines.saveRoutine(detail.removeExercise(detail.exercises.first().id))

        val read = assertNotNull(routines.findRoutine(detail.routine.id))
        assertEquals(listOf("leg-press"), read.exercises.map { it.exerciseId.value })
        assertEquals(3, read.setTemplates.size)
    }

    @Test
    fun deletedRoutinesAndFoldersDisappear() = runTest {
        routines.saveFolder(Folder(FolderId("f1"), "Fuerza", 0))
        val detail = CreateRoutine(routines, ids)("Torso", FolderId("f1"))

        routines.deleteFolder(FolderId("f1"))
        assertNull(routines.findRoutine(detail.routine.id)?.routine?.folderId)
        assertTrue(routines.observeFolders().first().isEmpty())

        routines.deleteRoutine(detail.routine.id)
        assertNull(routines.findRoutine(detail.routine.id))
    }

    @Test
    fun aWholeSessionFromARoutineIsSavedSetBySet() = runTest {
        var routine = CreateRoutine(routines, ids)("Torso")
        routine = routine.addExercise(ids, routine.blocks.single().id, ExerciseId("barbell-bench-press"), sets = 2)
        routines.saveRoutine(routine)

        val started = StartSession(sessions, ids, clock)(routine)
        assertEquals(started.session.id, sessions.observeActiveSession().first()?.session?.id)

        clock.now += 3.minutes
        val complete = CompleteSet(sessions, clock)
        complete(started.sets[0].copy(weightKg = 80.0, reps = 8, rir = 2))
        complete(started.sets[1].copy(weightKg = 80.0, reps = 7, rir = 1, side = Side.LEFT))

        val live = assertNotNull(sessions.observeActiveSession().first())
        assertEquals(2, live.workingSetsDone)
        assertEquals(1_200.0, live.volumeKg)
        assertEquals(Side.LEFT, live.sets[1].side)

        sessions.saveBlock(live.blocks.single().copy(result = BlockResult.Completed(feeling = 4)))
        clock.now += 40.minutes
        EndSession(sessions, clock)(live.session)

        assertNull(sessions.observeActiveSession().first())
        val finished = sessions.observeFinishedSessions().first().single()
        assertEquals(SessionStatus.FINISHED, finished.status)
        assertEquals(clock.now, finished.endedAt)
        val saved = assertNotNull(sessions.findSession(finished.id))
        assertEquals(BlockResult.Completed(feeling = 4), saved.blocks.single().result)
        assertEquals(started.session.startedAt + 3.minutes, saved.sets.first().completedAt)
    }

    @Test
    fun aDeletedSetIsNotReadBack() = runTest {
        val started = StartSession(sessions, ids, clock)(
            CreateRoutine(routines, ids)("Brazo").let { it.addExercise(ids, it.blocks.single().id, ExerciseId("curl"), sets = 3) },
        )

        sessions.deleteSet(started.sets[1].id)

        assertEquals(2, assertNotNull(sessions.findSession(started.session.id)).sets.size)
    }
}
