package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.session.SessionDetail
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Series por músculo de Progreso (PANTALLAS 2.1): cuentan las series de trabajo completadas en los
 * últimos 7 días (no la semana de calendario). El músculo primario suma 1 y el secundario 0,5.
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
                exercise.muscles.forEach { muscle ->
                    totals[muscle.muscle] = (totals[muscle.muscle] ?: 0.0) + done * muscle.role.setWeight
                }
            }
        }
        return totals.filterValues { it > 0.0 }
    }
}
