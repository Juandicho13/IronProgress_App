package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.session.SessionDetail
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Dónde queda un músculo frente al rango objetivo de series de la semana. */
enum class VolumeStatus { BELOW, IN_RANGE, ABOVE }

/**
 * Series efectivas por músculo (F2-07, HANDOFF 11.10 y PANTALLAS 2.1): cuentan las series de trabajo
 * completadas en los últimos 7 días, no la semana de calendario. Cada serie suma a un músculo su
 * porcentaje dividido por el del músculo principal del ejercicio: el principal suma 1 y en el press
 * banca el tríceps suma 25 ÷ 55 = 0,45. Sin porcentajes (ejercicios propios) el primario suma 1 y el
 * secundario 0,5.
 */
object MuscleSets {
    const val TARGET_MIN = 10
    const val TARGET_MAX = 20

    fun lastSevenDays(details: List<SessionDetail>, exercises: Map<ExerciseId, Exercise>, now: Instant): Map<Muscle, Double> =
        between(details, exercises, now - 7.days, now)

    fun between(details: List<SessionDetail>, exercises: Map<ExerciseId, Exercise>, from: Instant, to: Instant): Map<Muscle, Double> {
        val totals = mutableMapOf<Muscle, Double>()
        details.forEach { detail ->
            detail.exercises.forEach { sessionExercise ->
                val exercise = exercises[sessionExercise.exerciseId] ?: return@forEach
                val done = detail.setsOf(sessionExercise.id).count { set ->
                    val at = set.completedAt
                    at != null && at >= from && at <= to && set.setType.countsAsWorking
                }
                if (done == 0) return@forEach
                setWeights(exercise).forEach { (muscle, weight) ->
                    totals[muscle] = (totals[muscle] ?: 0.0) + done * weight
                }
            }
        }
        return totals.filterValues { it > 0.0 }
    }

    /**
     * Cuánto suma una serie de [exercise] a cada músculo. Con porcentajes: el del músculo entre el
     * mayor del ejercicio. Sin porcentaje en un músculo, el peso de su papel (1 o 0,5).
     */
    fun setWeights(exercise: Exercise): Map<Muscle, Double> {
        val top = exercise.muscles.mapNotNull { it.workShare }.maxOrNull()
        return exercise.muscles.associate { item ->
            val share = item.workShare
            item.muscle to if (top != null && share != null) share.toDouble() / top else item.role.setWeight
        }
    }

    /** Debajo, dentro o encima del rango de [TARGET_MIN] a [TARGET_MAX] series. */
    fun status(sets: Double): VolumeStatus = when {
        sets < TARGET_MIN -> VolumeStatus.BELOW
        sets > TARGET_MAX -> VolumeStatus.ABOVE
        else -> VolumeStatus.IN_RANGE
    }
}
