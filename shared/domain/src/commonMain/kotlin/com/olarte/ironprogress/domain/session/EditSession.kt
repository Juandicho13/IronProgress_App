package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat

/** Copia únicamente medidas, nunca identidad, nota ni estado de completado. */
fun WorkoutSet.copyMeasurements(previous: WorkoutSet): WorkoutSet = copy(
    weightKg = previous.weightKg,
    reps = previous.reps,
    durationSeconds = previous.durationSeconds,
    distanceMeters = previous.distanceMeters,
    calories = previous.calories,
    heightCm = previous.heightCm,
    breaths = previous.breaths,
    watts = previous.watts,
    rir = previous.rir,
    rpe = previous.rpe,
)

/**
 * Rellena solo las medidas vacías con las de [source] (la serie anterior o la última vez). Lo que
 * ya tiene la serie, como las reps objetivo de la rutina, se queda; RIR y RPE no se copian.
 */
fun WorkoutSet.fillMissingFrom(source: WorkoutSet): WorkoutSet = copy(
    weightKg = weightKg ?: source.weightKg,
    reps = reps ?: source.reps,
    durationSeconds = durationSeconds ?: source.durationSeconds,
    distanceMeters = distanceMeters ?: source.distanceMeters,
    calories = calories ?: source.calories,
    heightCm = heightCm ?: source.heightCm,
    breaths = breaths ?: source.breaths,
    watts = watts ?: source.watts,
)

/** La serie equivalente conserva el tipo y su posición dentro de ese tipo. */
fun equivalentPreviousSet(set: WorkoutSet, current: List<WorkoutSet>, previous: List<WorkoutSet>): WorkoutSet? {
    val index = current.sortedBy { it.sortOrder }.filter { it.setType == set.setType }.indexOfFirst { it.id == set.id }
    return previous.filter { it.setType == set.setType }.getOrNull(index)
}

/** No convierte campos vacíos en ceros ni permite completar medidas inválidas. */
fun WorkoutSet.hasRequiredMeasurements(type: TrackingType): Boolean = type.fields.all { field ->
    when (field) {
        SetField.WEIGHT -> weightKg?.let { it.isFinite() && it >= 0 } == true
        SetField.REPS -> (reps ?: 0) > 0
        SetField.DURATION -> (durationSeconds ?: 0) > 0
        SetField.DISTANCE -> distanceMeters?.let { it.isFinite() && it > 0 } == true
        SetField.CALORIES -> (calories ?: 0) > 0
        SetField.HEIGHT -> heightCm?.let { it.isFinite() && it > 0 } == true
        SetField.BREATHS -> (breaths ?: 0) > 0
        SetField.WATTS -> (watts ?: 0) > 0
    }
}

/** Agrega el primer registro de un ejercicio a una sesión libre o de rutina. */
class AddSessionExercise(private val repository: SessionRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(sessionId: SessionId, exerciseId: ExerciseId): SessionExerciseId {
        val detail = requireNotNull(repository.findSession(sessionId))
        require(detail.session.status == SessionStatus.IN_PROGRESS)
        val block = detail.blocks.lastOrNull { it.format == BlockFormat.SETS }
            ?: SessionBlock(SessionBlockId(ids.newId()), sessionId, detail.blocks.size, BlockFormat.SETS)
        val exercise = SessionExercise(
            SessionExerciseId(ids.newId()),
            sessionId,
            block.id,
            exerciseId,
            (detail.exercises.maxOfOrNull { it.sortOrder } ?: -1) + 1,
        )
        val previous = repository.previousSets(exerciseId).firstOrNull { it.setType.countsAsWorking }
        val set = WorkoutSet(WorkoutSetId(ids.newId()), exercise.id, 0).let {
            if (previous == null) it else it.copyMeasurements(previous)
        }
        repository.appendExercise(block, exercise, listOf(set))
        return exercise.id
    }
}
