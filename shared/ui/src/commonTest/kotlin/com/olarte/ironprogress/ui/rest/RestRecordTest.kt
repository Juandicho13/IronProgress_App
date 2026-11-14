package com.olarte.ironprogress.ui.rest

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
import com.olarte.ironprogress.domain.progress.ExerciseHistoryEntry
import com.olarte.ironprogress.domain.progress.RecordHit
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.PlatformFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/** F2-15: el récord de la serie que abrió el descanso llega a la Live Update y a la Live Activity. */
class RestRecordTest {
    private val startMs = 1_760_000_000_000L
    private val bench = Exercise(
        id = ExerciseId("bench"),
        name = LocalizedText("Press banca con barra", "Barbell bench press"),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY)),
    )
    private val sessionId = SessionId("hoy")
    private val exerciseId = SessionExerciseId("ex")
    private val done =
        WorkoutSet(WorkoutSetId("s1"), exerciseId, 0, weightKg = 62.5, reps = 10, completedAt = Instant.fromEpochMilliseconds(startMs))
    private val next = WorkoutSet(WorkoutSetId("s2"), exerciseId, 1, weightKg = 62.5, reps = 10)
    private val detail = SessionDetail(
        WorkoutSession(sessionId, null, Discipline.STRENGTH, Instant.fromEpochMilliseconds(startMs - 60_000)),
        listOf(SessionBlock(SessionBlockId("b"), sessionId, 0, BlockFormat.SETS)),
        listOf(SessionExercise(exerciseId, sessionId, SessionBlockId("b"), bench.id, 0)),
        listOf(done, next),
    )
    private val strings = RestNotificationStrings(
        defaultWorkoutName = "Entreno libre",
        repsSuffix = "reps",
        readyChip = "Listo",
        restingTitle = { "Descanso · $it" },
        nextSetWithTarget = { n, total, target -> "Sigue: serie $n de $total · $target" },
        nextSetOnly = { n, total -> "Sigue: serie $n de $total" },
        completedSetLine = { n, target -> "Serie $n hecha: $target" },
        readyTitle = { n, total -> "A la serie $n de $total" },
        readyTextWithTarget = { ex, target -> "$ex · $target" },
        recordLine = { ex, value -> "Récord: $ex $value" },
    )
    private val formats = Formats(
        object : PlatformFormatter {
            override fun number(value: Double, maxFractionDigits: Int): String {
                val text = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                return text.replace('.', ',')
            }

            override fun date(epochMs: Long): String = epochMs.toString()
        },
    )

    private class Sessions(private val detail: SessionDetail, private val past: List<ExerciseHistoryEntry>) : SessionRepository {
        override fun observeActiveSession(): Flow<SessionDetail?> = MutableStateFlow(detail)
        override suspend fun findSession(id: SessionId): SessionDetail? = detail.takeIf { it.session.id == id }
        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = MutableStateFlow(emptyList())
        override suspend fun exerciseHistory(exerciseId: ExerciseId): List<ExerciseHistoryEntry> = past
        override suspend fun createSession(detail: SessionDetail): SessionDetail = detail
        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit
        override suspend fun saveSession(session: WorkoutSession) = Unit
        override suspend fun saveBlock(block: SessionBlock) = Unit
        override suspend fun saveExercise(exercise: SessionExercise) = Unit
        override suspend fun saveSet(set: WorkoutSet) = Unit
        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private inner class Exercises : ExerciseRepository {
        override fun observeExercises(): Flow<List<Exercise>> = MutableStateFlow(listOf(bench))
        override suspend fun findExercise(id: ExerciseId): Exercise? = bench.takeIf { it.id == id }
        override suspend fun saveCustomExercise(exercise: Exercise) = Unit
        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    private class Routines : RoutineRepository {
        override fun observeFolders(): Flow<List<Folder>> = MutableStateFlow(emptyList())
        override fun observeRoutines(): Flow<List<Routine>> = MutableStateFlow(emptyList())
        override suspend fun findRoutine(id: RoutineId): RoutineDetail? = null
        override suspend fun saveRoutine(detail: RoutineDetail) = Unit
        override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit
        override suspend fun reorderFolders(ids: List<FolderId>) = Unit
        override suspend fun deleteRoutine(id: RoutineId) = Unit
        override suspend fun saveFolder(folder: Folder) = Unit
        override suspend fun deleteFolder(id: FolderId) = Unit
    }

    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(startMs + 5_000)
    }

    private fun actions(past: List<ExerciseHistoryEntry>, controller: RestController): RestBackgroundActions {
        val sessions = Sessions(detail, past)
        return RestBackgroundActions(controller, sessions, Exercises(), Routines(), CompleteSet(sessions, clock), clock)
    }

    private val lastWeek = listOf(
        ExerciseHistoryEntry(
            SessionId("antes"),
            Instant.fromEpochMilliseconds(startMs - 7 * 86_400_000L),
            WorkoutSet(
                WorkoutSetId("old"),
                SessionExerciseId("old-ex"),
                0,
                weightKg = 60.0,
                reps = 10,
                completedAt = Instant.fromEpochMilliseconds(
                    startMs - 7 * 86_400_000L,
                ),
            ),
        ),
    )

    @Test
    fun theRestAfterARecordShowsItsLine() = runTest {
        val controller = RestController()
        controller.startRest(sessionId, RestTimer(done.id, startMs, 90))
        controller.setAppInForeground(false, startMs)

        val content = assertNotNull(actions(lastWeek, controller).buildCurrentNotification("es", formats, strings))

        // 62,5 × 10 bate la fuerza estimada de la semana pasada (60 × 10): 83,3 kg, al medio kilo.
        assertEquals("Récord: Press banca con barra 83,5 kg", content.recordText)
    }

    @Test
    fun withoutHistoryThereIsNoRecordLine() = runTest {
        val controller = RestController()
        controller.startRest(sessionId, RestTimer(done.id, startMs, 90))
        controller.setAppInForeground(false, startMs)

        val content = assertNotNull(actions(emptyList(), controller).buildCurrentNotification("es", formats, strings))

        assertNull(content.recordText)
    }

    @Test
    fun aRecordFromAnotherSetIsIgnoredAndRepsAreWritten() {
        val controller = RestController()
        controller.startRest(sessionId, RestTimer(done.id, startMs, 90))
        val other =
            RecordHit(
                bench.id, RecordKind.MOST_REPS, 12.0, 10.0, 62.5, 12,
                WorkoutSetId(
                    "otra",
                ),
                sessionId, Instant.fromEpochMilliseconds(startMs),
            )
        val build = { hit: RecordHit ->
            buildRestNotificationContent(
                controller.state.value,
                detail,
                mapOf(bench.id to bench),
                startMs + 1_000,
                "es",
                formats,
                strings,
                hit,
            )
        }

        assertNull(build(other)?.recordText)
        assertEquals("Récord: Press banca con barra 12 reps", build(other.copy(setId = done.id))?.recordText)
    }
}
