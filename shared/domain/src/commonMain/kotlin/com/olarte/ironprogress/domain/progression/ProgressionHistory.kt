package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseProgression
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.SetTemplate
import com.olarte.ironprogress.domain.session.SessionRepository

/** Las últimas veces que se hizo un ejercicio, con las descargas marcadas, de la más reciente a la más antigua. */
suspend fun SessionRepository.performances(exerciseId: ExerciseId, limit: Int = 8): List<PastPerformance> =
    recentPerformances(exerciseId, limit).map { sets -> PastPerformance(sets, deload = sets.any { it.suggestionRecord?.deload == true }) }

/** El rango de las series de trabajo de una rutina: el de la primera que lo tenga. */
fun List<SetTemplate>.workingRange(): RepRange {
    val first = filter { it.setType.countsAsWorking }.firstOrNull { it.repsMin != null || it.repsMax != null }
        ?: return RepRange(targetRir = firstOrNull { it.setType.countsAsWorking }?.targetRir)
    val min = first.repsMin?.takeIf { it > 0 }
    val max = first.repsMax?.takeIf { it > 0 }
    val low = if (min != null && max != null) minOf(min, max) else min
    val high = if (min != null && max != null) maxOf(min, max) else max
    return RepRange(low, high, first.targetRir?.takeIf { it >= 0 })
}

/**
 * La sugerencia de hoy para un ejercicio de una rutina (F2-02). Null cuando no aplica: series por
 * tiempo o ejercicios que no se miden en reps.
 */
fun suggestToday(
    exercise: Exercise,
    templates: List<SetTemplate>,
    history: List<PastPerformance>,
    unit: WeightUnit,
    timed: Boolean = false,
    young: Boolean = false,
    ladder: ExerciseProgression? = null,
): Suggestion? {
    if (timed || SetField.REPS !in exercise.trackingType.fields) return null
    val working = templates.count { it.setType.countsAsWorking }
    if (working == 0) return null
    return DoubleProgression.suggest(
        ProgressionInput(
            trackingType = exercise.trackingType,
            equipment = exercise.equipment,
            unit = unit,
            range = templates.workingRange(),
            plannedSets = working,
            history = history,
            young = young,
            ladder = ladder,
        ),
    )
}
