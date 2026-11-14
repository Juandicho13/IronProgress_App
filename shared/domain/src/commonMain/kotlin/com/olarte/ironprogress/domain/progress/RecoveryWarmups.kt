package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExerciseId

/**
 * Calentamiento según la recuperación (F2-14, PRG-09). Si un músculo del día sigue en ámbar o en
 * rojo, el primer ejercicio que lo trabaja como principal lleva más series de aproximación: una
 * suave más en ámbar y dos en rojo ([com.olarte.ironprogress.domain.session.warmupSets]).
 */
object RecoveryWarmups {
    fun extraLight(level: RecoveryLevel): Int = when (level) {
        RecoveryLevel.FRESH -> 0
        RecoveryLevel.RECOVERING -> 1
        RecoveryLevel.LOADED -> 2
    }

    /**
     * Ejercicio de [detail] que lleva el aviso, con el músculo más cargado que lo pide. Solo
     * ejercicios con peso, sin calentamiento todavía y sin series hechas; cada músculo avisa una vez,
     * en el primero que lo trabaja.
     */
    fun targets(
        detail: SessionDetail,
        exercises: Map<ExerciseId, Exercise>,
        recovery: Map<Muscle, MuscleRecovery>,
    ): Map<SessionExerciseId, MuscleRecovery> {
        val ordered = detail.exercises.sortedBy { it.sortOrder }
        val result = mutableMapOf<SessionExerciseId, MuscleRecovery>()
        Recovery.notReady(recovery).forEach { state ->
            val first = ordered.firstOrNull { state.muscle in exercises[it.exerciseId]?.primaryMuscles.orEmpty() } ?: return@forEach
            val sets = detail.setsOf(first.id)
            val eligible = sets.none { it.setType == SetType.WARMUP || it.isCompleted } &&
                sets.any { it.setType.countsAsWorking && (it.weightKg ?: 0.0) > 0.0 }
            if (eligible && first.id !in result) result[first.id] = state
        }
        return result
    }
}
