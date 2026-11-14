package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSet
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Qué se grafica en el detalle de un ejercicio (PANTALLAS 2.2). */
enum class ProgressMetric {
    /** 1RM estimado de la mejor serie de cada sesión ("Fuerza estimada" en el modo básico). */
    ESTIMATED_ONE_REP_MAX,

    /** La mejor serie de cada sesión: el mayor peso, con sus reps. Sin peso, las más reps. */
    BEST_SET,

    /** Peso por reps de todas las series de trabajo de la sesión. Sin peso, las reps totales. */
    VOLUME,
}

/** Rango de la gráfica: 3 meses, 6 meses, un año o todo. */
enum class ProgressRange(val days: Int?) {
    THREE_MONTHS(91),
    SIX_MONTHS(182),
    ONE_YEAR(365),
    ALL(null),
}

/**
 * Lo hecho en un ejercicio en una sesión. [bestSet] es la serie de más peso (a igual peso, la de
 * más reps); sin peso, la de más reps. [estimatedOneRepMax] es null si ninguna serie lo permite.
 */
data class ProgressPoint(
    val sessionId: SessionId,
    val at: Instant,
    val sets: List<WorkoutSet>,
    val bestSet: WorkoutSet,
    val estimatedOneRepMax: Double?,
    val volumeKg: Double,
    val totalReps: Int,
)

/** El progreso de un ejercicio: un punto por sesión, del más viejo al más nuevo. */
data class ExerciseProgress(val points: List<ProgressPoint>, val records: ExerciseRecords) {
    val isEmpty: Boolean get() = points.isEmpty()

    /** Si el ejercicio se ha hecho con peso alguna vez: si no, las gráficas usan reps. */
    val usesWeight: Boolean get() = points.any { point -> point.sets.any { (it.weightKg ?: 0.0) > 0 } }

    /** Las métricas que tienen datos: sin peso no hay 1RM estimado. */
    val metrics: List<ProgressMetric>
        get() = if (usesWeight && points.any { it.estimatedOneRepMax != null }) {
            ProgressMetric.entries
        } else {
            listOf(ProgressMetric.BEST_SET, ProgressMetric.VOLUME)
        }

    /** El valor de [metric] en un punto: kg con peso, reps sin él. Null si ese punto no lo tiene. */
    fun valueOf(point: ProgressPoint, metric: ProgressMetric): Double? = when (metric) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> point.estimatedOneRepMax
        ProgressMetric.BEST_SET -> if (usesWeight) point.bestSet.weightKg else point.bestSet.reps?.toDouble()
        ProgressMetric.VOLUME -> if (usesWeight) point.volumeKg else point.totalReps.toDouble()
    }

    /** Los puntos de [range] contados hacia atrás desde [now]. */
    fun inRange(range: ProgressRange, now: Instant): List<ProgressPoint> {
        val days = range.days ?: return points
        val from = now - days.days
        return points.filter { it.at >= from }
    }

    /**
     * El cambio de la métrica frente a hace unas 4 semanas: el último punto contra el último que
     * tenga 28 días o más antes que él. Null si no hay con qué comparar.
     */
    fun changeOverFourWeeks(metric: ProgressMetric): Double? {
        val latest = points.lastOrNull() ?: return null
        val now = valueOf(latest, metric) ?: return null
        val before = points.lastOrNull { it.at <= latest.at - 28.days }?.let { valueOf(it, metric) } ?: return null
        return now - before
    }

    /** El peso con el que más series se han hecho: el de "Más reps con 60 kg". */
    val mostUsedWeightKey: Double?
        get() = points.flatMap { it.sets }.filter { (it.weightKg ?: 0.0) > 0 }
            .groupingBy { Records.weightKey(it.weightKg) }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Double, Int>> { it.value }.thenBy { it.key })?.key

    companion object {
        /** Arma el progreso con la historia de un ejercicio (ver [Records.counts]). */
        fun of(history: List<ExerciseHistoryEntry>): ExerciseProgress {
            val entries = history.filter { Records.counts(it.set) }
            val points = entries.groupBy { it.sessionId }.map { (sessionId, items) ->
                val sets = items.sortedBy { it.at }.map { it.set }
                val best = sets.maxWith(compareBy<WorkoutSet> { it.weightKg ?: 0.0 }.thenBy { it.reps ?: 0 })
                ProgressPoint(
                    sessionId = sessionId,
                    at = items.maxOf { it.at },
                    sets = sets,
                    bestSet = best,
                    estimatedOneRepMax = sets.mapNotNull { Records.estimatedOneRepMax(it) }.maxOrNull(),
                    volumeKg = sets.sumOf { Records.volumeOf(it) },
                    totalReps = sets.sumOf { it.reps ?: 0 },
                )
            }.sortedBy { it.at }
            return ExerciseProgress(points, Records.bests(entries))
        }
    }
}
