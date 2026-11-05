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
import com.olarte.ironprogress.domain.time.TimerEvent
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.PlatformFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class RestTimerTest {
    @Test
    fun twentyRestsKeepExactTimeWithIrregularFramesAndAdjustments() {
        repeat(20) { index ->
            val start = 100_000L * (index + 1)
            val rest = RestTimer(WorkoutSetId("set-$index"), start, 120)
                .adjust(start + 1234, 15).adjust(start + 5689, -15)
            listOf(0L, 17, 1234, 5701, 38941, 119999).forEach { offset ->
                val added = if (offset in 1234..5688) 15000 else 0
                assertEquals(120000L + added - offset, rest.at(start + offset).remainingInSegmentMs)
                assertFalse(rest.at(start + offset).isFinished)
            }
            assertTrue(rest.at(start + 120000).isFinished)
            assertEquals(listOf(TimerEvent.Finished), rest.events(start + 119999, start + 120000))
            assertTrue(rest.events(start + 120000, start + 120001).isEmpty())
        }
    }

    @Test
    fun reducingBelowElapsedFinishesWithoutNegativeTime() {
        val rest = RestTimer(WorkoutSetId("set"), 1000, 10).adjust(9000, -15)
        assertEquals(0L, rest.at(9000).remainingInSegmentMs)
        assertTrue(rest.at(9000).isFinished)
    }

    @Test
    fun backgroundNotificationBuildsRestingAndReadyStatesAndHandlesActions() = runTest {
        val startMs = 1_760_000_000_000L
        val clock = FakeClock(Instant.fromEpochMilliseconds(startMs))
        val sessionId = SessionId("session-1")
        val blockId = SessionBlockId("block-1")
        val exerciseId = SessionExerciseId("ex-1")
        val set1 = WorkoutSet(
            id = WorkoutSetId("set-1"),
            sessionExerciseId = exerciseId,
            sortOrder = 0,
            weightKg = 62.5,
            reps = 10,
            completedAt = Instant.fromEpochMilliseconds(startMs),
        )
        val set2 = WorkoutSet(
            id = WorkoutSetId("set-2"),
            sessionExerciseId = exerciseId,
            sortOrder = 1,
            weightKg = 62.5,
            reps = 10,
        )
        val set3 = WorkoutSet(
            id = WorkoutSetId("set-3"),
            sessionExerciseId = exerciseId,
            sortOrder = 2,
            weightKg = null,
            reps = null,
        )
        val detail = SessionDetail(
            session = WorkoutSession(
                id = sessionId,
                routineId = null,
                discipline = Discipline.STRENGTH,
                startedAt = Instant.fromEpochMilliseconds(startMs - 60_000L),
            ),
            blocks = listOf(SessionBlock(blockId, sessionId, 0, BlockFormat.SETS)),
            exercises = listOf(SessionExercise(exerciseId, sessionId, blockId, ExerciseId("bench"), 0)),
            sets = listOf(set1, set2, set3),
        )

        val sessions = FakeSessions(detail)
        val exercises = FakeExercises()
        val routines = FakeRoutines()
        val controller = RestController()
        val actions = RestBackgroundActions(
            restController = controller,
            sessions = sessions,
            exercises = exercises,
            routines = routines,
            completeSet = CompleteSet(sessions, clock),
            clock = clock,
        )
        val esFormats = Formats(SpanishTestFormatter)
        val esStrings = RestNotificationStrings(
            defaultWorkoutName = "Entreno libre",
            repsSuffix = "reps",
            readyChip = "Listo",
            restingTitle = { "Descanso · $it" },
            nextSetWithTarget = { n, total, target -> "Sigue: serie $n de $total · $target" },
            nextSetOnly = { n, total -> "Sigue: serie $n de $total" },
            completedSetLine = { n, target -> "Serie $n hecha: $target" },
            readyTitle = { n, total -> "A la serie $n de $total" },
            readyTextWithTarget = { ex, target -> "$ex · $target" },
        )

        controller.startRest(sessionId, RestTimer(set1.id, startMs, 120))
        assertEquals(sessionId, controller.state.value.pendingPermissionSessionId)
        controller.markNotificationPermissionHandled(sessionId)
        assertNull(controller.state.value.pendingPermissionSessionId)

        // Con la app en primer plano no se muestra la notificación.
        assertNull(actions.buildCurrentNotification("es", esFormats, esStrings))

        // Al pasar a segundo plano sale Estado 1 · Descansando con el instante de fin exacto.
        controller.setAppInForeground(false, startMs)
        val restingContent = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertEquals(RestPhase.RESTING, restingContent.phase)
        assertTrue(restingContent.ongoing)
        assertTrue(restingContent.showChronometer)
        assertNull(restingContent.shortCriticalText)
        assertEquals(startMs + 120_000L, restingContent.endsAtMs)
        assertEquals("Descanso · Press banca con barra", restingContent.title)
        assertEquals("Sigue: serie 2 de 3 · 62,5 kg × 10", restingContent.collapsedText)
        assertEquals("Sigue: serie 2 de 3 · 62,5 kg × 10\nSerie 1 hecha: 62,5 kg × 10", restingContent.expandedText)
        assertTrue(restingContent.showAdjustAndSkipActions)
        assertFalse(restingContent.showCompleteSetAction)

        // +15 s desde la notificación suma 15.000 ms al instante de fin.
        clock.current = Instant.fromEpochMilliseconds(startMs + 10_000L)
        actions.adjustRest(15)
        val adjustedContent = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertEquals(startMs + 135_000L, adjustedContent.endsAtMs)

        // Saltar termina el descanso y pasa a Estado 2 · Listo sin sonido (alertNow = false).
        clock.current = Instant.fromEpochMilliseconds(startMs + 20_000L)
        actions.skipRest()
        val skippedReady = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertEquals(RestPhase.READY, skippedReady.phase)
        assertEquals("Listo", skippedReady.shortCriticalText)
        assertEquals("A la serie 2 de 3", skippedReady.title)
        assertEquals("Press banca con barra · 62,5 kg × 10", skippedReady.collapsedText)
        assertFalse(skippedReady.alertNow)
        assertTrue(skippedReady.showCompleteSetAction)

        // Completar serie desde la notificación guarda la serie 2 y empieza el descanso siguiente.
        clock.current = Instant.fromEpochMilliseconds(startMs + 25_000L)
        assertTrue(actions.completeNextSet())
        assertTrue(sessions.current.sets.first { it.id == set2.id }.isCompleted)

        val nextRest = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertEquals(RestPhase.RESTING, nextRest.phase)
        assertEquals("Sigue: serie 3 de 3", nextRest.collapsedText)
        assertEquals("Sigue: serie 3 de 3\nSerie 2 hecha: 62,5 kg × 10", nextRest.expandedText)

        // Cuando el descanso llega a 0 naturalmente, pasa a Estado 2 con alerta una vez;
        // como la serie 3 no tiene peso ni reps propuestos, el botón Completar serie no sale.
        clock.current = Instant.fromEpochMilliseconds(startMs + 25_000L + 120_000L)
        val naturalReady = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertEquals(RestPhase.READY, naturalReady.phase)
        assertTrue(naturalReady.alertNow)
        assertFalse(naturalReady.showCompleteSetAction)
        controller.markReadyAlertDelivered()

        // A los 10 minutos deja de estar en curso (ongoing = false).
        clock.current = Instant.fromEpochMilliseconds(startMs + 25_000L + 120_000L + RestController.READY_TIMEOUT_MS)
        val expiredReady = assertNotNull(actions.buildCurrentNotification("es", esFormats, esStrings))
        assertFalse(expiredReady.ongoing)
    }

    private class FakeClock(var current: Instant) : Clock {
        override fun now(): Instant = current
    }

    private object SpanishTestFormatter : PlatformFormatter {
        override fun number(value: Double, maxFractionDigits: Int): String {
            val scale = 10.0.pow(maxFractionDigits)
            val rounded = (value * scale).roundToLong() / scale
            return if (rounded % 1.0 == 0.0) {
                rounded.toLong().toString()
            } else {
                rounded.toString().replace('.', ',')
            }
        }

        override fun date(epochMs: Long): String = epochMs.toString()
    }

    private class FakeSessions(initial: SessionDetail) : SessionRepository {
        private val flow = MutableStateFlow<SessionDetail?>(initial)
        val current: SessionDetail get() = flow.value!!

        override fun observeActiveSession(): Flow<SessionDetail?> = flow

        override suspend fun findSession(id: SessionId): SessionDetail? = flow.value?.takeIf { it.session.id == id }

        override fun observeFinishedSessions(): Flow<List<WorkoutSession>> = flow.map { emptyList() }

        override suspend fun createSession(detail: SessionDetail): SessionDetail {
            flow.value = detail
            return detail
        }

        override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) = Unit

        override suspend fun saveSession(session: WorkoutSession) {
            flow.value = flow.value?.copy(session = session)
        }

        override suspend fun saveBlock(block: SessionBlock) = Unit

        override suspend fun saveExercise(exercise: SessionExercise) = Unit

        override suspend fun saveSet(set: WorkoutSet) {
            val detail = flow.value ?: return
            flow.value = detail.copy(sets = detail.sets.map { if (it.id == set.id) set else it })
        }

        override suspend fun deleteSet(id: WorkoutSetId) = Unit
    }

    private class FakeExercises : ExerciseRepository {
        private val bench = Exercise(
            id = ExerciseId("bench"),
            name = LocalizedText("Press banca con barra", "Barbell bench press"),
            trackingType = TrackingType.WEIGHT_REPS,
            equipment = Equipment.BARBELL,
            pattern = MovementPattern.HORIZONTAL_PUSH,
            muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY)),
        )

        override fun observeExercises(): Flow<List<Exercise>> = MutableStateFlow(listOf(bench))

        override suspend fun findExercise(id: ExerciseId): Exercise? = bench.takeIf { it.id == id }

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    private class FakeRoutines : RoutineRepository {
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
}
