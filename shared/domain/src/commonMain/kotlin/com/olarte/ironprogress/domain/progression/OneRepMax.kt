package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.session.WorkoutSet

/**
 * 1RM estimado con la fórmula de Epley sumando las reps en reserva (HANDOFF 11.1):
 * `1RM = peso × (1 + (reps + RIR) / 30)`. Sin RIR registrado se toma 0. Solo vale si
 * reps + RIR es 12 o menos; por encima la estimación ya no es fiable y no se calcula.
 */
object OneRepMax {
    const val MAX_REPS_IN_FORMULA = 12

    fun estimate(weightKg: Double, reps: Int, rir: Int? = null): Double? {
        if (!weightKg.isFinite() || weightKg <= 0 || reps <= 0) return null
        val total = reps + (rir ?: 0)
        if (total > MAX_REPS_IN_FORMULA) return null
        return weightKg * (1 + total / 30.0)
    }
}

/**
 * 1RM estimado de una serie de trabajo hecha. Solo en ejercicios donde el peso es la carga que se
 * levanta: en las dominadas asistidas el peso es ayuda y en los de solo reps no hay peso.
 */
fun WorkoutSet.estimatedOneRepMax(trackingType: TrackingType): Double? {
    if (!isCompleted || !setType.countsAsWorking) return null
    if (trackingType != TrackingType.WEIGHT_REPS && trackingType != TrackingType.WEIGHTED_BODYWEIGHT) return null
    return OneRepMax.estimate(weightKg ?: return null, reps ?: return null, rir)
}

/** El mejor 1RM estimado entre [sets]; null si ninguna serie permite calcularlo. */
fun List<WorkoutSet>.bestOneRepMax(trackingType: TrackingType): Double? = mapNotNull { it.estimatedOneRepMax(trackingType) }.maxOrNull()
