package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.session.CivilDate
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionStatus
import kotlin.time.Instant

enum class PeriodKind { MONTH, YEAR }

/** Un mes (con [month] de 1 a 12) o un año entero del calendario local. */
data class Period(val kind: PeriodKind, val year: Int, val month: Int = 1) {
    init {
        require(month in 1..12) { "month fuera de 1..12" }
    }

    val firstDay: CivilDate get() = if (kind == PeriodKind.MONTH) CivilDate(year, month, 1) else CivilDate(year, 1, 1)

    val lastDay: CivilDate
        get() = if (kind == PeriodKind.MONTH) CivilDate(year, month, CivilDate.daysInMonth(year, month)) else CivilDate(year, 12, 31)

    fun contains(epochDay: Long): Boolean = epochDay in firstDay.toEpochDay()..lastDay.toEpochDay()

    fun previous(): Period = when (kind) {
        PeriodKind.MONTH -> if (month == 1) copy(year = year - 1, month = 12) else copy(month = month - 1)
        PeriodKind.YEAR -> copy(year = year - 1)
    }

    fun next(): Period = when (kind) {
        PeriodKind.MONTH -> if (month == 12) copy(year = year + 1, month = 1) else copy(month = month + 1)
        PeriodKind.YEAR -> copy(year = year + 1)
    }

    companion object {
        fun monthOf(epochDay: Long): Period = CivilDate.fromEpochDay(epochDay).let { Period(PeriodKind.MONTH, it.year, it.month) }

        fun yearOf(epochDay: Long): Period = Period(PeriodKind.YEAR, CivilDate.fromEpochDay(epochDay).year)
    }
}

/** Cuentas básicas de un periodo. */
data class PeriodStats(val sessions: Int, val totalSeconds: Long, val volumeKg: Double, val trainedDays: Int) {
    companion object {
        val EMPTY = PeriodStats(0, 0, 0.0, 0)
    }
}

/** El ejercicio con más series de trabajo en el periodo. */
data class FavouriteExercise(val exerciseId: ExerciseId, val sets: Int)

/**
 * Resumen de un mes o un año (F2-10). [previous] son las cuentas del periodo anterior para comparar.
 * [records] trae un récord por ejercicio y tipo: el último (el mejor) que se batió en el periodo.
 */
data class PeriodSummary(
    val period: Period,
    val stats: PeriodStats,
    val previous: PeriodStats,
    val records: List<RecordHit>,
    val topMuscles: List<Pair<Muscle, Double>>,
    val favourite: FavouriteExercise?,
    val bestStreakWeeks: Int,
) {
    val isEmpty: Boolean get() = stats.sessions == 0
}

/** Resúmenes por periodo sin pantalla. Las fechas se cuentan en días civiles locales con [epochDay]. */
object PeriodSummaries {
    const val TOP_MUSCLES = 3

    fun summarize(
        details: List<SessionDetail>,
        exercises: Map<ExerciseId, Exercise>,
        period: Period,
        epochDay: (Instant) -> Long,
    ): PeriodSummary {
        val finished = details.filter { it.session.status == SessionStatus.FINISHED }.sortedBy { it.session.startedAt }
        val dayOf = { detail: SessionDetail -> epochDay(detail.session.endedAt ?: detail.session.startedAt) }
        val inPeriod = finished.filter { period.contains(dayOf(it)) }
        val previousPeriod = period.previous()
        return PeriodSummary(
            period = period,
            stats = stats(inPeriod, dayOf),
            previous = stats(finished.filter { previousPeriod.contains(dayOf(it)) }, dayOf),
            records = records(finished, inPeriod.map { it.session.id }.toSet()),
            topMuscles = muscles(inPeriod, exercises).toList()
                .sortedWith(compareByDescending<Pair<Muscle, Double>> { it.second }.thenBy { it.first.ordinal })
                .take(TOP_MUSCLES),
            favourite = favourite(inPeriod),
            bestStreakWeeks = bestStreak(inPeriod.map(dayOf).toSet(), period),
        )
    }

    /** El primer mes con un entreno terminado, para no dejar ir más atrás. */
    fun firstMonth(details: List<SessionDetail>, epochDay: (Instant) -> Long): Period? = details
        .filter { it.session.status == SessionStatus.FINISHED }
        .minOfOrNull { epochDay(it.session.endedAt ?: it.session.startedAt) }
        ?.let { Period.monthOf(it) }

    private fun stats(details: List<SessionDetail>, dayOf: (SessionDetail) -> Long): PeriodStats = PeriodStats(
        sessions = details.size,
        totalSeconds = details.sumOf { detail ->
            val end = detail.session.endedAt ?: return@sumOf 0L
            (end - detail.session.startedAt).inWholeSeconds.coerceAtLeast(0)
        },
        volumeKg = details.sumOf { it.volumeKg },
        trainedDays = details.map(dayOf).toSet().size,
    )

    /** Repite la historia en orden y se queda con los récords batidos en las sesiones del periodo. */
    private fun records(ordered: List<SessionDetail>, inPeriod: Set<com.olarte.ironprogress.domain.session.SessionId>): List<RecordHit> {
        if (inPeriod.isEmpty()) return emptyList()
        val history = mutableMapOf<ExerciseId, MutableList<ExerciseHistoryEntry>>()
        val best = linkedMapOf<Pair<ExerciseId, RecordKind>, RecordHit>()
        ordered.forEach { detail ->
            detail.exercises.map { it.exerciseId }.distinct().forEach { exerciseId ->
                val entries = detail.historyOf(exerciseId)
                if (detail.session.id in inPeriod) {
                    Records.replay(exerciseId, history[exerciseId].orEmpty(), entries).forEach { hit ->
                        best[exerciseId to hit.kind] = hit
                    }
                }
                history.getOrPut(exerciseId) { mutableListOf() } += entries
            }
        }
        return best.values.sortedWith(compareBy<RecordHit> { Records.priority.indexOf(it.kind) }.thenByDescending { it.at })
    }

    private fun muscles(details: List<SessionDetail>, exercises: Map<ExerciseId, Exercise>): Map<Muscle, Double> {
        val totals = mutableMapOf<Muscle, Double>()
        details.forEach { detail ->
            detail.exercises.forEach { sessionExercise ->
                val exercise = exercises[sessionExercise.exerciseId] ?: return@forEach
                val done = detail.setsOf(sessionExercise.id).count { it.isCompleted && it.setType.countsAsWorking }
                if (done == 0) return@forEach
                MuscleSets.setWeights(exercise).forEach { (muscle, weight) -> totals[muscle] = (totals[muscle] ?: 0.0) + done * weight }
            }
        }
        return totals.filterValues { it > 0.0 }
    }

    private fun favourite(details: List<SessionDetail>): FavouriteExercise? {
        val sets = mutableMapOf<ExerciseId, Int>()
        val volume = mutableMapOf<ExerciseId, Double>()
        details.forEach { detail ->
            detail.exercises.forEach { sessionExercise ->
                val done = detail.setsOf(sessionExercise.id).filter { it.isCompleted && it.setType.countsAsWorking }
                if (done.isEmpty()) return@forEach
                sets[sessionExercise.exerciseId] = (sets[sessionExercise.exerciseId] ?: 0) + done.size
                volume[sessionExercise.exerciseId] = (volume[sessionExercise.exerciseId] ?: 0.0) +
                    done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            }
        }
        val top = sets.entries.maxWithOrNull(compareBy<Map.Entry<ExerciseId, Int>> { it.value }.thenBy { volume[it.key] ?: 0.0 })
        return top?.let { FavouriteExercise(it.key, it.value) }
    }

    /** Semanas seguidas (de lunes a domingo) con al menos un entreno, dentro del periodo. */
    private fun bestStreak(days: Set<Long>, period: Period): Int {
        if (days.isEmpty()) return 0
        val weeks = days.map { weekOf(it) }.toSet()
        var best = 0
        var run = 0
        for (week in weekOf(period.firstDay.toEpochDay())..weekOf(period.lastDay.toEpochDay())) {
            run = if (week in weeks) run + 1 else 0
            best = maxOf(best, run)
        }
        return best
    }

    /** Número de semana contando desde el lunes 29 de diciembre de 1969. */
    private fun weekOf(epochDay: Long): Long = (epochDay + 3).floorDiv(7L)
}
