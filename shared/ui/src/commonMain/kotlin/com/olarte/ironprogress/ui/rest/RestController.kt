package com.olarte.ironprogress.ui.rest

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.hasRequiredMeasurements
import com.olarte.ironprogress.domain.settings.SettingsRepository
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.settings.forDisplay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Fase del descanso compartido entre la pantalla Entreno y la notificación en segundo plano. */
enum class RestPhase {
    IDLE,
    RESTING,
    READY,
}

/** Estado del descanso compartido (F1-14). */
data class RestSessionState(
    val phase: RestPhase = RestPhase.IDLE,
    val sessionId: SessionId? = null,
    val timer: RestTimer? = null,
    val readyAtMs: Long? = null,
    val alertOnReady: Boolean = false,
    val appInForeground: Boolean = true,
    val workoutScreenOpen: Boolean = false,
    val pendingPermissionSessionId: SessionId? = null,
) {
    /**
     * La notificación sale cuando hay un descanso activo o listo en una sesión en curso y la app
     * pasó a segundo plano o se apagó la pantalla (PANTALLAS 5.8).
     */
    val shouldShowBackgroundNotification: Boolean
        get() {
            val active = (phase == RestPhase.RESTING && timer != null) || phase == RestPhase.READY
            return !appInForeground && sessionId != null && active
        }
}

/** Plantillas de texto localizadas para armar la Live Update y la Now Bar sin depender de Android en commonMain. */
data class RestNotificationStrings(
    val defaultWorkoutName: String,
    val repsSuffix: String,
    val readyChip: String,
    val restingTitle: (exercise: String) -> String,
    val nextSetWithTarget: (setNumber: Int, totalSets: Int, target: String) -> String,
    val nextSetOnly: (setNumber: Int, totalSets: Int) -> String,
    val completedSetLine: (setNumber: Int, target: String) -> String,
    val readyTitle: (setNumber: Int, totalSets: Int) -> String,
    val readyTextWithTarget: (exercise: String, target: String) -> String,
)

/** Modelo listo para pintar en la notificación del sistema (Estado 1 Descansando y Estado 2 Listo). */
data class RestNotificationContent(
    val sessionId: SessionId,
    val phase: RestPhase,
    val ongoing: Boolean,
    val endsAtMs: Long?,
    val showChronometer: Boolean,
    val shortCriticalText: String?,
    val title: String,
    val collapsedText: String,
    val expandedText: String,
    val showAdjustAndSkipActions: Boolean,
    val showCompleteSetAction: Boolean,
    val nextSetId: WorkoutSetId?,
    val alertNow: Boolean,
)

/**
 * Coordinador en memoria del descanso (F1-14). Vive como singleton en Koin para que tanto
 * WorkoutViewModel como el servicio en primer plano de Android lean y modifiquen el mismo motor.
 */
class RestController {
    private val mutableState = MutableStateFlow(RestSessionState())
    val state: StateFlow<RestSessionState> = mutableState.asStateFlow()

    private val openWorkoutChannel = Channel<String>(Channel.BUFFERED)
    val openWorkoutRequests: Flow<String> = openWorkoutChannel.receiveAsFlow()

    private val askedPermissionSessions = mutableSetOf<SessionId>()

    fun startRest(sessionId: SessionId, timer: RestTimer) {
        val askPermission = sessionId !in askedPermissionSessions
        mutableState.update { current ->
            current.copy(
                phase = RestPhase.RESTING,
                sessionId = sessionId,
                timer = timer,
                readyAtMs = null,
                alertOnReady = false,
                pendingPermissionSessionId = if (askPermission) sessionId else current.pendingPermissionSessionId,
            )
        }
    }

    fun markNotificationPermissionHandled(sessionId: SessionId) {
        askedPermissionSessions += sessionId
        mutableState.update { current ->
            if (current.pendingPermissionSessionId == sessionId) {
                current.copy(pendingPermissionSessionId = null)
            } else {
                current
            }
        }
    }

    fun updateTimer(timer: RestTimer, nowMs: Long) {
        mutableState.update { current ->
            if (current.phase != RestPhase.RESTING) return@update current
            if (timer.at(nowMs).isFinished) {
                if (current.appInForeground) {
                    current.copy(phase = RestPhase.IDLE, timer = null, readyAtMs = null, alertOnReady = false)
                } else {
                    current.copy(phase = RestPhase.READY, timer = timer, readyAtMs = nowMs, alertOnReady = true)
                }
            } else {
                current.copy(timer = timer)
            }
        }
    }

    fun adjustFromNotification(nowMs: Long, deltaSeconds: Int) {
        val current = mutableState.value
        val timer = current.timer ?: return
        if (current.phase != RestPhase.RESTING) return
        updateTimer(timer.adjust(nowMs, deltaSeconds), nowMs)
    }

    /** Saltar desde la notificación termina el descanso con TimerEdit.Skip y pasa al Estado 2 sin sonido. */
    fun skipFromNotification(nowMs: Long) {
        mutableState.update { current ->
            val timer = current.timer ?: return@update current
            if (current.phase != RestPhase.RESTING) return@update current
            current.copy(
                phase = RestPhase.READY,
                timer = timer.skip(nowMs),
                readyAtMs = nowMs,
                alertOnReady = false,
            )
        }
    }

    fun skipFromUi(nowMs: Long) {
        mutableState.update { current ->
            val skipped = current.timer?.skip(nowMs)
            current.copy(
                phase = RestPhase.IDLE,
                timer = skipped?.takeIf { false },
                readyAtMs = null,
                alertOnReady = false,
            )
        }
    }

    fun onRestCountdownFinished(nowMs: Long) {
        mutableState.update { current ->
            if (current.phase != RestPhase.RESTING) return@update current
            if (current.appInForeground) {
                current.copy(phase = RestPhase.IDLE, timer = null, readyAtMs = null, alertOnReady = false)
            } else {
                val endsAt = current.timer?.endsAtMs(current.timer.startedAtMs) ?: nowMs
                current.copy(phase = RestPhase.READY, readyAtMs = endsAt, alertOnReady = true)
            }
        }
    }

    fun markReadyAlertDelivered() {
        mutableState.update { current ->
            if (current.alertOnReady) current.copy(alertOnReady = false) else current
        }
    }

    fun setAppInForeground(inForeground: Boolean, nowMs: Long) {
        mutableState.update { current ->
            if (inForeground) {
                val timerFinished = current.timer?.at(nowMs)?.isFinished == true
                if (current.phase == RestPhase.READY || timerFinished) {
                    current.copy(
                        appInForeground = true,
                        phase = RestPhase.IDLE,
                        timer = null,
                        readyAtMs = null,
                        alertOnReady = false,
                    )
                } else {
                    current.copy(appInForeground = true)
                }
            } else {
                current.copy(appInForeground = false)
            }
        }
    }

    fun setWorkoutScreenOpen(open: Boolean) {
        mutableState.update { it.copy(workoutScreenOpen = open) }
    }

    fun clearIfSet(setId: WorkoutSetId) {
        mutableState.update { current ->
            if (current.timer?.setId == setId) {
                current.copy(phase = RestPhase.IDLE, timer = null, readyAtMs = null, alertOnReady = false)
            } else {
                current
            }
        }
    }

    fun clearRest() {
        mutableState.update { current ->
            current.copy(
                phase = RestPhase.IDLE,
                sessionId = null,
                timer = null,
                readyAtMs = null,
                alertOnReady = false,
            )
        }
    }

    fun requestOpenWorkout(sessionId: String) {
        openWorkoutChannel.trySend(sessionId)
    }

    companion object {
        /** A los 10 minutos en Estado 2 deja de estar en curso y se puede descartar (PANTALLAS 5.8). */
        const val READY_TIMEOUT_MS = 10 * 60 * 1_000L
    }
}

/**
 * Ejecuta las acciones de la notificación (+15 s, Saltar y Completar serie) y construye su
 * modelo leyendo Room y el motor de tiempo, incluso con la pantalla cerrada.
 */
class RestBackgroundActions(
    private val restController: RestController,
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val completeSet: CompleteSet,
    private val clock: Clock,
    /** Para "Ocultar peso corporal y calorías" (F1-25). Sin él, se muestra todo. */
    private val settings: SettingsRepository? = null,
) {
    private val actionMutex = Mutex()

    fun nowMs(): Long = clock.now().toEpochMilliseconds()

    fun adjustRest(deltaSeconds: Int) {
        restController.adjustFromNotification(nowMs(), deltaSeconds)
    }

    fun skipRest() {
        restController.skipFromNotification(nowMs())
    }

    suspend fun completeNextSet(): Boolean = actionMutex.withLock {
        val currentRest = restController.state.value
        val detail = sessions.observeActiveSession().first() ?: run {
            restController.clearRest()
            return false
        }
        if (detail.session.status != SessionStatus.IN_PROGRESS) {
            restController.clearRest()
            return false
        }
        val nextSet = findNextUncompletedSet(detail, currentRest.timer?.setId) ?: run {
            restController.clearRest()
            return false
        }
        val exercise = detail.exercises.firstOrNull { it.id == nextSet.sessionExerciseId } ?: return false
        val catalog = exercises.observeExercises().first().associateBy { it.id }
        val trackingType = catalog[exercise.exerciseId]?.trackingType ?: return false
        if (nextSet.isCompleted || !nextSet.hasRequiredMeasurements(trackingType)) {
            return false
        }

        val routine = detail.session.routineId?.let { routines.findRoutine(it) }
        val orderedSessionExercises = detail.exercises.sortedBy { it.sortOrder }
        val planned = routine?.blocks
            ?.sortedBy { it.sortOrder }
            ?.flatMap { routine.exercisesOf(it.id) }
            ?.getOrNull(orderedSessionExercises.indexOf(exercise))
        val restSeconds = planned?.takeIf { it.exerciseId == exercise.exerciseId }?.restSeconds
            ?: routine?.routine?.restSeconds
            ?: Routine.DEFAULT_REST_SECONDS

        val completed = completeSet(nextSet)
        val startedMs = requireNotNull(completed.completedAt).toEpochMilliseconds()
        if (restSeconds > 0) {
            restController.startRest(
                sessionId = detail.session.id,
                timer = RestTimer(completed.id, startedMs, restSeconds),
            )
        } else {
            restController.clearRest()
        }
        return true
    }

    suspend fun buildCurrentNotification(
        languageCode: String,
        formats: Formats,
        strings: RestNotificationStrings,
    ): RestNotificationContent? {
        val now = nowMs()
        val currentSnapshot = restController.state.value
        if (currentSnapshot.phase == RestPhase.RESTING && currentSnapshot.timer?.at(now)?.isFinished == true) {
            restController.onRestCountdownFinished(now)
        }
        val restState = restController.state.value
        if (!restState.shouldShowBackgroundNotification) return null

        val detail = sessions.observeActiveSession().first()
        if (detail == null || detail.session.status != SessionStatus.IN_PROGRESS || detail.session.id != restState.sessionId) {
            restController.clearRest()
            return null
        }
        val catalog = exercises.observeExercises().first().associateBy { it.id }
        val hide = settings?.settings?.first()?.hideBodyMetrics == true
        return buildRestNotificationContent(
            restState = restState,
            sessionDetail = detail.copy(sets = detail.sets.map { it.forDisplay(hide) }),
            catalog = catalog,
            nowMs = now,
            languageCode = languageCode,
            formats = formats,
            strings = strings,
        )
    }
}

/** Elige la serie siguiente que toca después del descanso actual. */
fun findNextUncompletedSet(detail: SessionDetail, completedSetId: WorkoutSetId?): WorkoutSet? {
    val orderedExercises = detail.exercises.sortedBy { it.sortOrder }
    val completedSet = completedSetId?.let { id -> detail.sets.firstOrNull { it.id == id } }
    if (completedSet != null) {
        val sameExerciseNext = detail.setsOf(completedSet.sessionExerciseId)
            .firstOrNull { !it.isCompleted && it.id != completedSetId }
        if (sameExerciseNext != null) return sameExerciseNext
    }
    return orderedExercises.firstNotNullOfOrNull { exercise ->
        detail.setsOf(exercise.id).firstOrNull { !it.isCompleted && it.id != completedSetId }
    }
}

/**
 * Formatea las medidas de una serie para la notificación (PANTALLAS 5.8):
 * "{peso} × {reps}" con el formato local de F1-05; si no tiene peso, solo las reps;
 * si es por tiempo, la duración.
 */
fun formatSetSummary(set: WorkoutSet, trackingType: TrackingType?, formats: Formats, repsSuffix: String): String? {
    val weight = set.weightKg?.takeIf { it > 0.0 || (trackingType == TrackingType.WEIGHT_REPS && set.reps != null) }
    val reps = set.reps?.takeIf { it > 0 }
    val duration = set.durationSeconds?.takeIf { it > 0 }
    val distance = set.distanceMeters?.takeIf { it > 0.0 }
    val watts = set.watts?.takeIf { it > 0 }
    val calories = set.calories?.takeIf { it > 0 }
    val height = set.heightCm?.takeIf { it > 0.0 }
    val breaths = set.breaths?.takeIf { it > 0 }

    return when {
        weight != null && reps != null -> "${formats.weight(weight)} × ${formats.number(reps.toDouble(), 0)}"
        reps != null && height != null -> "${formats.number(height, 1)} cm × ${formats.number(reps.toDouble(), 0)}"
        reps != null -> "${formats.number(reps.toDouble(), 0)} $repsSuffix"
        distance != null && duration != null -> "${formats.distance(distance)} · ${Formats.duration(duration.toLong())}"
        watts != null && duration != null -> "${formats.number(watts.toDouble(), 0)} W · ${Formats.duration(duration.toLong())}"
        duration != null -> Formats.duration(duration.toLong())
        weight != null && distance != null -> "${formats.weight(weight)} · ${formats.distance(distance)}"
        distance != null -> formats.distance(distance)
        calories != null -> "${formats.number(calories.toDouble(), 0)} kcal"
        breaths != null -> formats.number(breaths.toDouble(), 0)
        else -> null
    }
}

/** Construye el modelo exacto de la Live Update / Now Bar para Estado 1 y Estado 2 (PANTALLAS 5.8). */
fun buildRestNotificationContent(
    restState: RestSessionState,
    sessionDetail: SessionDetail,
    catalog: Map<ExerciseId, Exercise>,
    nowMs: Long,
    languageCode: String,
    formats: Formats,
    strings: RestNotificationStrings,
): RestNotificationContent? {
    val sessionId = restState.sessionId ?: return null
    if (restState.phase == RestPhase.IDLE) return null

    val orderedExercises = sessionDetail.exercises.sortedBy { it.sortOrder }
    val completedSet = restState.timer?.setId?.let { id -> sessionDetail.sets.firstOrNull { it.id == id } }
    val nextSet = findNextUncompletedSet(sessionDetail, restState.timer?.setId)

    val completedExercise = completedSet?.let { cs -> orderedExercises.firstOrNull { it.id == cs.sessionExerciseId } }
    val nextExercise = nextSet?.let { ns -> orderedExercises.firstOrNull { it.id == ns.sessionExerciseId } }
    val displayExercise = nextExercise ?: completedExercise ?: orderedExercises.firstOrNull()
    val restingExercise = completedExercise ?: nextExercise ?: orderedExercises.firstOrNull()

    val displayExerciseName = displayExercise?.let {
        catalog[it.exerciseId]?.name?.resolve(languageCode) ?: it.exerciseId.value
    } ?: strings.defaultWorkoutName

    val restingExerciseName = restingExercise?.let {
        catalog[it.exerciseId]?.name?.resolve(languageCode) ?: it.exerciseId.value
    } ?: strings.defaultWorkoutName

    val nextExerciseSets = displayExercise?.let { sessionDetail.setsOf(it.id) }.orEmpty()
    val nextSetNumber = if (nextSet != null) {
        val index = nextExerciseSets.indexOfFirst { it.id == nextSet.id }
        if (index >= 0) index + 1 else nextSet.sortOrder + 1
    } else {
        nextExerciseSets.size.coerceAtLeast(1)
    }
    val totalSets = nextExerciseSets.size.coerceAtLeast(nextSetNumber)

    val nextTrackingType = displayExercise?.let { catalog[it.exerciseId]?.trackingType }
    val nextSummary = nextSet?.let { formatSetSummary(it, nextTrackingType, formats, strings.repsSuffix) }
    val canCompleteNextSet = nextSet != null &&
        nextTrackingType != null &&
        !nextSet.isCompleted &&
        nextSet.hasRequiredMeasurements(nextTrackingType)

    val completedLine = if (completedSet != null && completedExercise != null) {
        val compSets = sessionDetail.setsOf(completedExercise.id)
        val compNumber = compSets.indexOfFirst { it.id == completedSet.id }.let {
            if (it >= 0) it + 1 else completedSet.sortOrder + 1
        }
        val compTracking = catalog[completedExercise.exerciseId]?.trackingType
        val compSummary = formatSetSummary(completedSet, compTracking, formats, strings.repsSuffix)
        compSummary?.let { strings.completedSetLine(compNumber, it) }
    } else {
        null
    }

    return when (restState.phase) {
        RestPhase.RESTING -> {
            val timer = restState.timer ?: return null
            val endsAtMs = timer.endsAtMs(nowMs) ?: return null
            val collapsed = if (nextSummary != null) {
                strings.nextSetWithTarget(nextSetNumber, totalSets, nextSummary)
            } else {
                strings.nextSetOnly(nextSetNumber, totalSets)
            }
            val expanded = if (completedLine != null) "$collapsed\n$completedLine" else collapsed
            RestNotificationContent(
                sessionId = sessionId,
                phase = RestPhase.RESTING,
                ongoing = true,
                endsAtMs = endsAtMs,
                showChronometer = true,
                shortCriticalText = null,
                title = strings.restingTitle(restingExerciseName),
                collapsedText = collapsed,
                expandedText = expanded,
                showAdjustAndSkipActions = true,
                showCompleteSetAction = false,
                nextSetId = nextSet?.id,
                alertNow = false,
            )
        }
        RestPhase.READY -> {
            val readyAtMs = restState.readyAtMs ?: nowMs
            val isExpired = nowMs - readyAtMs >= RestController.READY_TIMEOUT_MS
            val text = if (nextSummary != null) {
                strings.readyTextWithTarget(displayExerciseName, nextSummary)
            } else {
                displayExerciseName
            }
            RestNotificationContent(
                sessionId = sessionId,
                phase = RestPhase.READY,
                ongoing = !isExpired,
                endsAtMs = readyAtMs,
                showChronometer = false,
                shortCriticalText = strings.readyChip,
                title = strings.readyTitle(nextSetNumber, totalSets),
                collapsedText = text,
                expandedText = text,
                showAdjustAndSkipActions = false,
                showCompleteSetAction = canCompleteNextSet,
                nextSetId = nextSet?.id,
                alertNow = restState.alertOnReady,
            )
        }
        RestPhase.IDLE -> null
    }
}
