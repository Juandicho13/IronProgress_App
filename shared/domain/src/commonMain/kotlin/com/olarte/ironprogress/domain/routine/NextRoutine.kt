package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Instant

private const val MILLIS_PER_DAY = 86_400_000L
private const val DEFAULT_SECONDS_PER_SET = 45

/** Resultado de elegir la siguiente rutina en la rotación de una carpeta o de la biblioteca. */
data class NextRoutineSelection(
    val routine: Routine,
    val folder: Folder?,
    val positionInRotation: Int,
    val rotationSize: Int,
    val isActiveSession: Boolean = false,
)

/** Detalle completo de la rutina que toca hoy según la rotación. */
data class NextRoutine(
    val detail: RoutineDetail,
    val folder: Folder? = null,
    val positionInRotation: Int = 1,
    val rotationSize: Int = 1,
    val isActiveSession: Boolean = false,
)

/** Estado de uno de los siete días de la semana (lunes = 0 a domingo = 6). */
data class WeekDayStatus(val dayIndex: Int, val completed: Boolean, val isToday: Boolean)

/** Resumen real de la semana actual (lunes a domingo) y racha de semanas entrenadas. */
data class WeekProgress(val days: List<WeekDayStatus>, val trainedDaysCount: Int, val streakWeeks: Int)

/**
 * Ordena las rutinas respetando primero el orden de las carpetas y dejando al final las rutinas
 * sin carpeta; dentro de cada grupo conserva `sortOrder`.
 */
fun orderRoutinesForRotation(routines: List<Routine>, folders: List<Folder> = emptyList()): List<Routine> {
    val folderRank = folders
        .sortedBy { it.sortOrder }
        .mapIndexed { index, folder -> folder.id to index }
        .toMap()
    return routines.sortedWith(
        compareBy<Routine> { routine ->
            routine.folderId?.let { folderRank[it] } ?: folders.size
        }.thenBy { it.sortOrder },
    )
}

/**
 * Elige la siguiente rutina por rotación (RF-RUT-03):
 * 1. Si hay una sesión en curso de una rutina existente, devuelve esa rutina como activa.
 * 2. Si hay sesiones terminadas cuya rutina sigue existiendo, toma la más reciente y avanza
 *    a la siguiente dentro de su misma carpeta (o dentro de las rutinas sin carpeta), dando
 *    la vuelta al llegar al final.
 * 3. Si aún no hay sesiones terminadas válidas, propone la primera rutina según el orden
 *    de carpetas y rutinas.
 */
fun selectNextRoutine(
    routines: List<Routine>,
    folders: List<Folder> = emptyList(),
    finishedSessions: List<WorkoutSession> = emptyList(),
    activeRoutineId: RoutineId? = null,
): NextRoutineSelection? {
    if (routines.isEmpty()) return null

    val sortedFolders = folders.sortedBy { it.sortOrder }
    val folderById = sortedFolders.associateBy { it.id }
    val ordered = orderRoutinesForRotation(routines, sortedFolders)

    if (activeRoutineId != null) {
        val activeRoutine = ordered.firstOrNull { it.id == activeRoutineId }
        if (activeRoutine != null) {
            val group = ordered.filter { it.folderId == activeRoutine.folderId }
            val index = group.indexOfFirst { it.id == activeRoutine.id }
            return NextRoutineSelection(
                routine = activeRoutine,
                folder = activeRoutine.folderId?.let(folderById::get),
                positionInRotation = index + 1,
                rotationSize = group.size,
                isActiveSession = true,
            )
        }
    }

    val recentFinished = finishedSessions
        .filter { it.status == SessionStatus.FINISHED && it.routineId != null }
        .sortedWith(
            compareByDescending<WorkoutSession> { it.endedAt ?: it.startedAt }
                .thenByDescending { it.startedAt },
        )

    val lastRoutine = recentFinished.firstNotNullOfOrNull { session ->
        ordered.firstOrNull { it.id == session.routineId }
    }

    if (lastRoutine == null) {
        val first = ordered.first()
        val group = ordered.filter { it.folderId == first.folderId }
        return NextRoutineSelection(
            routine = first,
            folder = first.folderId?.let(folderById::get),
            positionInRotation = 1,
            rotationSize = group.size,
            isActiveSession = false,
        )
    }

    val group = ordered.filter { it.folderId == lastRoutine.folderId }
    val lastIndex = group.indexOfFirst { it.id == lastRoutine.id }
    val nextIndex = (lastIndex + 1) % group.size
    val nextRoutine = group[nextIndex]
    return NextRoutineSelection(
        routine = nextRoutine,
        folder = nextRoutine.folderId?.let(folderById::get),
        positionInRotation = nextIndex + 1,
        rotationSize = group.size,
        isActiveSession = false,
    )
}

/** Duración estimada de la rutina en minutos a partir de sus bloques, series y descansos. */
fun RoutineDetail.estimatedDurationMinutes(): Int {
    if (blocks.isEmpty() && exercises.isEmpty()) return 0
    val totalSeconds = blocks.sumOf { block ->
        when (block.format) {
            BlockFormat.AMRAP, BlockFormat.FOR_TIME -> block.timeCapSeconds ?: 0
            BlockFormat.EMOM -> (block.rounds ?: 0) * 60
            BlockFormat.INTERVALS, BlockFormat.ROUNDS -> {
                val rounds = block.rounds ?: 0
                val work = block.workSeconds ?: 0
                val rest = block.restSeconds ?: 0
                if (rounds > 0) rounds * work + (rounds - 1).coerceAtLeast(0) * rest else 0
            }
            BlockFormat.SETS, BlockFormat.FLOW, BlockFormat.BREATHING -> {
                exercisesOf(block.id).sumOf { exercise ->
                    val setsCount = setsOf(exercise.id).size
                    val restSeconds = exercise.restSeconds ?: routine.restSeconds
                    setsCount * (DEFAULT_SECONDS_PER_SET + restSeconds)
                }
            }
        }
    }
    return if (totalSeconds <= 0) 0 else ((totalSeconds + 59) / 60).coerceAtLeast(1)
}

/** Calcula los días entrenados en la semana actual (lunes a domingo) y la racha de semanas. */
fun buildWeekProgress(
    finishedSessions: List<WorkoutSession>,
    now: Instant,
    epochDay: (Instant) -> Long = { it.toEpochMilliseconds().floorDiv(MILLIS_PER_DAY) },
): WeekProgress {
    val todayEpochDay = epochDay(now)
    // 1970-01-01 fue jueves (índice 3 cuando lunes = 0 y domingo = 6).
    val todayDayOfWeek = (todayEpochDay + 3L).mod(7)
    val weekStartEpochDay = todayEpochDay - todayDayOfWeek

    val trainedEpochDays = finishedSessions
        .filter { it.status == SessionStatus.FINISHED && (it.endedAt ?: it.startedAt) <= now }
        .map { epochDay(it.endedAt ?: it.startedAt) }
        .toSet()

    val days = (0..6).map { dayIndex ->
        val dayEpoch = weekStartEpochDay + dayIndex
        WeekDayStatus(
            dayIndex = dayIndex,
            completed = dayEpoch in trainedEpochDays,
            isToday = dayIndex == todayDayOfWeek,
        )
    }

    fun hasSessionInWeek(weekOffset: Int): Boolean {
        val start = weekStartEpochDay - weekOffset * 7L
        return (0L..6L).any { offset -> (start + offset) in trainedEpochDays }
    }

    var streakWeeks = 0
    var offset = if (hasSessionInWeek(0)) 0 else 1
    while (hasSessionInWeek(offset)) {
        streakWeeks += 1
        offset += 1
    }

    return WeekProgress(
        days = days,
        trainedDaysCount = days.count { it.completed },
        streakWeeks = streakWeeks,
    )
}

/** Observa la siguiente rutina que toca según la rotación y carga su detalle completo. */
class ObserveNextRoutine(private val routineRepository: RoutineRepository, private val sessionRepository: SessionRepository) {
    operator fun invoke(): Flow<NextRoutine?> = combine(
        routineRepository.observeRoutines(),
        routineRepository.observeFolders(),
        sessionRepository.observeFinishedSessions(),
        sessionRepository.observeActiveSession(),
    ) { routines, folders, finishedSessions, activeSession ->
        val selection = selectNextRoutine(
            routines = routines,
            folders = folders,
            finishedSessions = finishedSessions,
            activeRoutineId = activeSession?.session?.routineId,
        ) ?: return@combine null
        val detail = routineRepository.findRoutine(selection.routine.id) ?: return@combine null
        NextRoutine(
            detail = detail,
            folder = selection.folder,
            positionInRotation = selection.positionInRotation,
            rotationSize = selection.rotationSize,
            isActiveSession = selection.isActiveSession,
        )
    }
}
