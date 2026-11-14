package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.TrackingType
import kotlin.math.roundToInt

/** Por qué se considera estancado un ejercicio (HANDOFF 11.3). */
enum class StagnationKind {
    /** El mejor 1RM estimado (o las mejores reps sin peso) no mejora más de 1 % en 4 sesiones. */
    PLATEAU,

    /** Dos sesiones seguidas con alguna serie por debajo del mínimo del rango. */
    BELOW_MIN,
}

/** Lo que se propone, en el orden de 11.3. */
enum class StagnationProposal {
    /** Revisar la recuperación: solo si hubo días bajos (llega con el ajuste de F4). */
    RECOVERY,

    /** Descarga: 40 % menos series con el mismo peso. */
    DELOAD,

    /** Cambiar el rango de reps. */
    CHANGE_RANGE,

    /** Cambiar por un sustituto. */
    SUBSTITUTE,
}

data class Stagnation(
    val kind: StagnationKind,
    /** Cuántas sesiones mira la regla: 4 en la meseta, 2 bajo el mínimo. */
    val sessions: Int,
    val proposals: List<StagnationProposal>,
    /** El rango que se propone si se elige cambiarlo; null si no hay rango que cambiar. */
    val newRange: RepRange?,
    /** El mínimo del rango actual, para explicarlo ("por debajo de 8 reps"). */
    val minReps: Int?,
)

/** Detección de estancamiento y propuestas (F2-05). Funciones puras. */
object StagnationDetector {
    const val WINDOW = 4
    const val MIN_GAIN = 0.01
    const val DELOAD_SHARE = 0.4

    /**
     * [history] va de la sesión más reciente a la más antigua; las descargas no cuentan. Devuelve
     * null si no hay estancamiento. [canChangeRange] es falso en un entreno libre, donde no hay rango
     * guardado que cambiar.
     */
    fun detect(
        history: List<PastPerformance>,
        trackingType: TrackingType,
        range: RepRange,
        lowRecoveryDays: Boolean = false,
        canChangeRange: Boolean = true,
    ): Stagnation? {
        val counted = history.filter { !it.deload && it.working.isNotEmpty() }
        val min = range.min ?: range.max
        val belowMin = min != null && counted.size >= 2 && counted.take(2).all { session -> session.working.any { (it.reps ?: 0) < min } }
        val kind = when {
            belowMin -> StagnationKind.BELOW_MIN
            isPlateau(counted, trackingType) -> StagnationKind.PLATEAU
            else -> return null
        }
        val newRange = if (canChangeRange && range.hasTop) proposedRange(range) else null
        val proposals = buildList {
            if (lowRecoveryDays) add(StagnationProposal.RECOVERY)
            add(StagnationProposal.DELOAD)
            if (newRange != null) add(StagnationProposal.CHANGE_RANGE)
            add(StagnationProposal.SUBSTITUTE)
        }
        return Stagnation(kind, if (kind == StagnationKind.PLATEAU) WINDOW else 2, proposals, newRange, min)
    }

    /**
     * Meseta: en las últimas [WINDOW] sesiones, ninguna supera a la más vieja de ellas en más de
     * [MIN_GAIN]. Con peso se mira el mejor 1RM estimado; sin peso, las mejores reps.
     */
    internal fun isPlateau(counted: List<PastPerformance>, trackingType: TrackingType): Boolean {
        val metrics = counted.mapNotNull { metric(it, trackingType) }
        if (metrics.size < WINDOW) return false
        val window = metrics.take(WINDOW).reversed()
        val base = window.first()
        return window.drop(1).maxOrNull()?.let { it <= base * (1 + MIN_GAIN) } == true
    }

    private fun metric(session: PastPerformance, trackingType: TrackingType): Double? =
        session.working.bestOneRepMax(trackingType) ?: session.working.mapNotNull { it.reps }.maxOrNull()?.toDouble()?.takeIf { it > 0 }

    /** Si el rango es de 9 reps o más se baja a 5 a 8 (más fuerza); si no, se sube a 10 a 15. */
    fun proposedRange(range: RepRange): RepRange {
        val top = range.max ?: return range
        return if (top > 8) RepRange(5, 8, range.targetRir) else RepRange(10, 15, range.targetRir)
    }

    /** Series de trabajo que quedan en una descarga: 40 % menos, al menos una. */
    fun deloadSetCount(total: Int): Int = (total - (total * DELOAD_SHARE).roundToInt()).coerceAtLeast(1)
}

/**
 * Aplica una descarga a las series de trabajo de un ejercicio en la sesión (F2-05): quita del final
 * las pendientes que sobran para quedar con el 60 % y marca las que quedan como descarga, así esa
 * sesión no cuenta para el estancamiento. Devuelve las series a borrar y las que hay que guardar.
 */
fun deload(sets: List<com.olarte.ironprogress.domain.session.WorkoutSet>): DeloadChange {
    val working = sets.filter { it.setType.countsAsWorking }.sortedBy { it.sortOrder }
    val pending = working.filter { !it.isCompleted }
    val keep = StagnationDetector.deloadSetCount(working.size)
    val leaveOne = if (working.none { it.isCompleted }) 1 else 0
    val remove = pending.takeLast((working.size - keep).coerceIn(0, (pending.size - leaveOne).coerceAtLeast(0)))
    val removed = remove.map { it.id }.toSet()
    val marked = working.filter { it.id !in removed }.map { set ->
        val record =
            set.suggestionRecord?.copy(reason = SuggestionReason.DELOAD_SETS, weightKg = set.weightKg, reps = set.reps, deload = true)
                ?: SuggestionRecord(SuggestionReason.DELOAD_SETS, set.weightKg, set.reps, deload = true)
        set.copy(suggestion = record.encode())
    }
    return DeloadChange(remove.map { it.id }, marked)
}

data class DeloadChange(
    val remove: List<com.olarte.ironprogress.domain.session.WorkoutSetId>,
    val save: List<com.olarte.ironprogress.domain.session.WorkoutSet>,
)
