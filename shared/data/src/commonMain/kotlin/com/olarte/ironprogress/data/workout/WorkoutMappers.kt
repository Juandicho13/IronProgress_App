package com.olarte.ironprogress.data.workout

import com.olarte.ironprogress.data.local.FolderEntity
import com.olarte.ironprogress.data.local.RoomJson
import com.olarte.ironprogress.data.local.RoutineEntity
import com.olarte.ironprogress.data.local.RoutineExerciseEntity
import com.olarte.ironprogress.data.local.SessionExerciseEntity
import com.olarte.ironprogress.data.local.SetTemplateEntity
import com.olarte.ironprogress.data.local.WorkoutBlockEntity
import com.olarte.ironprogress.data.local.WorkoutSessionEntity
import com.olarte.ironprogress.data.local.WorkoutSetEntity
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.CreatedBy
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineExercise
import com.olarte.ironprogress.domain.routine.RoutineExerciseId
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetTemplate
import com.olarte.ironprogress.domain.routine.SetTemplateId
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.Side
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.time.Instant

// Conversión entre filas de Room y modelos de dominio. created_at y updated_at los pone el
// repositorio; aquí solo se copian.

private const val CREATED_BY_USER = "USER"
private const val CREATED_BY_PRO = "PRO:"

internal fun CreatedBy.toColumn(): String = when (this) {
    CreatedBy.User -> CREATED_BY_USER
    is CreatedBy.Professional -> CREATED_BY_PRO + professionalId
}

internal fun createdByOf(column: String): CreatedBy =
    if (column.startsWith(CREATED_BY_PRO)) CreatedBy.Professional(column.removePrefix(CREATED_BY_PRO)) else CreatedBy.User

internal fun FolderEntity.toDomain() = Folder(FolderId(id), name, sortOrder)

internal fun Folder.toEntity(createdAt: Long, updatedAt: Long) =
    FolderEntity(id.value, name, sortOrder, createdAt = createdAt, updatedAt = updatedAt)

internal fun RoutineEntity.toDomain() = Routine(
    id = RoutineId(id),
    folderId = folderId?.let(::FolderId),
    name = name,
    notes = notes,
    restSeconds = restSeconds,
    sortOrder = sortOrder,
    createdBy = createdByOf(createdBy),
    sourceProgramVersionId = sourceProgramVersionId,
)

internal fun Routine.toEntity(createdAt: Long, updatedAt: Long) = RoutineEntity(
    id = id.value,
    folderId = folderId?.value,
    name = name,
    notes = notes,
    restSeconds = restSeconds,
    sortOrder = sortOrder,
    createdBy = createdBy.toColumn(),
    sourceProgramVersionId = sourceProgramVersionId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun WorkoutBlockEntity.toRoutineBlock() = WorkoutBlock(
    id = BlockId(id),
    sortOrder = sortOrder,
    format = BlockFormat.valueOf(format),
    rounds = rounds,
    workSeconds = workSeconds,
    restSeconds = restSeconds,
    timeCapSeconds = timeCapSeconds,
)

internal fun WorkoutBlock.toEntity(routineId: RoutineId, createdAt: Long, updatedAt: Long) = WorkoutBlockEntity(
    id = id.value,
    routineId = routineId.value,
    sessionId = null,
    sortOrder = sortOrder,
    format = format.name,
    rounds = rounds,
    workSeconds = workSeconds,
    restSeconds = restSeconds,
    timeCapSeconds = timeCapSeconds,
    result = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id = RoutineExerciseId(id),
    blockId = BlockId(blockId),
    exerciseId = ExerciseId(exerciseId),
    sortOrder = sortOrder,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    unit = unit?.let { WeightUnit.valueOf(it) },
    notes = notes,
    timedSeconds = timedSeconds,
)

internal fun RoutineExercise.toEntity(routineId: RoutineId, createdAt: Long, updatedAt: Long) = RoutineExerciseEntity(
    id = id.value,
    routineId = routineId.value,
    blockId = blockId.value,
    exerciseId = exerciseId.value,
    sortOrder = sortOrder,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    unit = unit?.name,
    notes = notes,
    timedSeconds = timedSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SetTemplateEntity.toDomain() = SetTemplate(
    id = SetTemplateId(id),
    routineExerciseId = RoutineExerciseId(routineExerciseId),
    sortOrder = sortOrder,
    setType = SetType.valueOf(setType),
    repsMin = repsMin,
    repsMax = repsMax,
    targetRir = targetRir,
)

internal fun SetTemplate.toEntity(createdAt: Long, updatedAt: Long) = SetTemplateEntity(
    id = id.value,
    routineExerciseId = routineExerciseId.value,
    sortOrder = sortOrder,
    setType = setType.name,
    repsMin = repsMin,
    repsMax = repsMax,
    targetRir = targetRir,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = SessionId(id),
    routineId = routineId?.let(::RoutineId),
    discipline = Discipline.valueOf(discipline),
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
    status = SessionStatus.valueOf(status),
    notes = notes,
    adjustmentApplied = adjustmentApplied,
    programDayId = programDayId,
    gymProfileId = gymProfileId,
)

internal fun WorkoutSession.toEntity(createdAt: Long, updatedAt: Long) = WorkoutSessionEntity(
    id = id.value,
    routineId = routineId?.value,
    programDayId = programDayId,
    gymProfileId = gymProfileId,
    discipline = discipline.name,
    startedAt = startedAt.toEpochMilliseconds(),
    endedAt = endedAt?.toEpochMilliseconds(),
    status = status.name,
    notes = notes,
    adjustmentApplied = adjustmentApplied,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun WorkoutBlockEntity.toSessionBlock() = SessionBlock(
    id = SessionBlockId(id),
    sessionId = SessionId(requireNotNull(sessionId) { "El bloque $id no es de una sesión" }),
    sortOrder = sortOrder,
    format = BlockFormat.valueOf(format),
    result = result?.let { RoomJson.decodeFromString<BlockResult>(it) },
)

internal fun SessionBlock.toEntity(createdAt: Long, updatedAt: Long) = WorkoutBlockEntity(
    id = id.value,
    routineId = null,
    sessionId = sessionId.value,
    sortOrder = sortOrder,
    format = format.name,
    rounds = null,
    workSeconds = null,
    restSeconds = null,
    timeCapSeconds = null,
    result = result?.let { RoomJson.encodeToString<BlockResult>(it) },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SessionExerciseEntity.toDomain() = SessionExercise(
    id = SessionExerciseId(id),
    sessionId = SessionId(sessionId),
    blockId = SessionBlockId(blockId),
    exerciseId = ExerciseId(exerciseId),
    sortOrder = sortOrder,
    supersetGroup = supersetGroup,
    notes = notes,
    restSeconds = restSeconds,
    timedSeconds = timedSeconds,
)

internal fun SessionExercise.toEntity(createdAt: Long, updatedAt: Long) = SessionExerciseEntity(
    id = id.value,
    sessionId = sessionId.value,
    blockId = blockId.value,
    exerciseId = exerciseId.value,
    sortOrder = sortOrder,
    supersetGroup = supersetGroup,
    notes = notes,
    restSeconds = restSeconds,
    timedSeconds = timedSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = WorkoutSetId(id),
    sessionExerciseId = SessionExerciseId(sessionExerciseId),
    sortOrder = sortOrder,
    setType = SetType.valueOf(setType),
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    calories = calories,
    heightCm = heightCm,
    breaths = breaths,
    watts = watts,
    side = side?.let { Side.valueOf(it) },
    round = round,
    rir = rir,
    rpe = rpe,
    completedAt = completedAt?.let(Instant::fromEpochMilliseconds),
    suggestion = suggestion,
    endHeartRate = endHeartRate,
    recoverySeconds = recoverySeconds,
    notes = notes,
)

internal fun WorkoutSet.toEntity(createdAt: Long, updatedAt: Long) = WorkoutSetEntity(
    id = id.value,
    sessionExerciseId = sessionExerciseId.value,
    sortOrder = sortOrder,
    setType = setType.name,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    calories = calories,
    heightCm = heightCm,
    breaths = breaths,
    watts = watts,
    side = side?.name,
    round = round,
    rir = rir,
    rpe = rpe,
    completedAt = completedAt?.toEpochMilliseconds(),
    suggestion = suggestion,
    endHeartRate = endHeartRate,
    recoverySeconds = recoverySeconds,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
