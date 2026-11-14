package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.SetField
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.WorkoutSet

/** Rango de reps de las series de trabajo y el RIR objetivo, tal como está en la rutina. */
data class RepRange(val min: Int? = null, val max: Int? = null, val targetRir: Int? = null) {
    init {
        require(min == null || min > 0) { "El mínimo del rango va desde 1" }
        require(max == null || max > 0) { "El tope del rango va desde 1" }
        require(min == null || max == null || min <= max) { "El mínimo no puede pasar del tope" }
    }

    /** Sin tope no hay doble progresión: solo se puede repetir lo de la última vez. */
    val hasTop: Boolean get() = max != null
}

/**
 * Lo que se hizo de un ejercicio en una sesión terminada: sus series de trabajo hechas, en orden.
 * [deload] marca las sesiones de descarga, que no cuentan para el estancamiento (11.3).
 */
data class PastPerformance(val sets: List<WorkoutSet>, val deload: Boolean = false) {
    val working: List<WorkoutSet> get() = sets.filter { it.isCompleted && it.setType.countsAsWorking }
}

/** Por qué el motor propone lo que propone. Cada uno tiene su línea de explicación en recursos. */
enum class SuggestionReason {
    /** Todas las series llegaron al tope con el RIR objetivo: sube un salto y vuelve al mínimo. */
    INCREASE,

    /** Llegaron al tope pero al fallo o por debajo del RIR objetivo: mismo peso, repetir. */
    REPEAT_HARD,

    /** Alguna serie quedó dentro del rango: mismo peso y una rep más en las que no llegaron. */
    ADD_REP,

    /** Una sesión por debajo del mínimo: mismo peso, buscar el mínimo. */
    AIM_MIN,

    /** Dos sesiones seguidas por debajo del mínimo: bajar entre 5 % y 10 % y volver al mínimo. */
    DELOAD,

    /** Sin historial: una serie de prueba con RIR 2 y proponer desde ahí. */
    FIRST_TIME,

    /** El ejercicio no tiene rango de reps o no se mide en reps: lo mismo que la última vez. */
    SAME_AS_LAST,

    /** Día con ajuste activo (11.6): no se sube peso. */
    ADJUSTMENT_HOLD,

    /** Modo joven: sube solo tras dos sesiones seguidas en el tope; falta una (11.11). */
    YOUNG_WAIT,

    /** Sin peso que subir y ya en el tope del rango, sin variante más difícil: mantener. */
    AT_TOP,
}

/** Lo que se hizo la última vez, para la línea de explicación ("3 × 12 con 60 kg y RIR 2"). */
data class PreviousSummary(val sets: Int, val minReps: Int, val maxReps: Int, val weightKg: Double?, val minRir: Int?)

/**
 * La propuesta para hoy. [reps] tiene una entrada por serie de trabajo planeada; [weightKg] es el
 * mismo para todas (null si el ejercicio no lleva peso o es la primera vez).
 */
data class Suggestion(
    val reason: SuggestionReason,
    val weightKg: Double?,
    val reps: List<Int?>,
    val range: RepRange,
    val previous: PreviousSummary? = null,
)

/** Todo lo que el motor necesita saber de un ejercicio para proponer la carga de hoy. */
data class ProgressionInput(
    val trackingType: TrackingType,
    val equipment: Equipment,
    /** Unidad con la que se ve el ejercicio; los saltos se calculan en ella. */
    val unit: WeightUnit = WeightUnit.KG,
    val range: RepRange = RepRange(),
    /** Cuántas series de trabajo trae hoy la rutina. */
    val plannedSets: Int = 1,
    /** De la sesión más reciente a la más antigua. */
    val history: List<PastPerformance> = emptyList(),
    /** Modo joven (13 a 17 años, 11.11): sin fallo, RIR 2 o más y subidas más lentas. */
    val young: Boolean = false,
    /** Día con ajuste activo (11.6): no subir peso. Llega en F4; mientras tanto siempre es false. */
    val adjustmentActive: Boolean = false,
)

/** Reglas del modo joven que no dependen de una sesión concreta (11.11). */
object YoungRules {
    const val MIN_TARGET_RIR = 2

    /** En modo joven no se ofrecen series al fallo. */
    fun allowedSetTypes(young: Boolean): List<SetType> = if (young) SetType.entries.filter { it != SetType.FAILURE } else SetType.entries

    /** El RIR objetivo que de verdad se usa: en modo joven nunca menos de 2. */
    fun effectiveTargetRir(target: Int?, young: Boolean): Int? = if (young) maxOf(target ?: MIN_TARGET_RIR, MIN_TARGET_RIR) else target

    /** En modo joven el 1RM estimado no se muestra. */
    fun showsOneRepMax(young: Boolean): Boolean = !young
}

/**
 * Doble progresión (HANDOFF 11.2). Funciones puras: la misma entrada da siempre la misma
 * propuesta, y cada propuesta lleva su porqué para explicarla en una línea.
 */
object DoubleProgression {
    /** Bajada al estancarse por debajo del mínimo: entre 5 % y 10 %, se toma 7,5 % y se redondea abajo. */
    const val DELOAD_FACTOR = 0.925

    fun suggest(input: ProgressionInput): Suggestion {
        val range = input.range
        val planned = input.plannedSets.coerceAtLeast(1)
        val last = input.history.firstOrNull()?.working.orEmpty()
        val usesReps = SetField.REPS in input.trackingType.fields
        if (last.isEmpty()) {
            return Suggestion(SuggestionReason.FIRST_TIME, null, List(planned) { range.min ?: range.max }, range)
        }
        val previous = summary(last)
        val lastWeight = referenceWeight(last)
        if (!usesReps || !range.hasTop) {
            return Suggestion(
                SuggestionReason.SAME_AS_LAST,
                lastWeight,
                List(planned) {
                    last.getOrElse(it) { last.last() }.reps
                },
                range,
                previous,
            )
        }
        val top = range.max!!
        val min = range.min ?: top
        val targetRir = YoungRules.effectiveTargetRir(range.targetRir, input.young)
        val lastStatus = status(last, min, top, targetRir)
        val before = input.history.getOrNull(1)?.working.orEmpty().takeIf { it.isNotEmpty() }?.let { status(it, min, top, targetRir) }
        val hasWeight = SetField.WEIGHT in input.trackingType.fields && (lastWeight ?: 0.0) > 0

        fun keep(reason: SuggestionReason, reps: List<Int?>) = Suggestion(reason, lastWeight, reps, range, previous)
        val oneMore = List(planned) { index ->
            val done = last.getOrElse(index) { last.last() }.reps ?: min
            if (done >= top) top else (done + 1).coerceIn(min, top)
        }
        val atMin = List<Int?>(planned) { min }

        if (!hasWeight) {
            return when {
                lastStatus.allTop && lastStatus.rirOk -> keep(SuggestionReason.AT_TOP, List(planned) { top })
                lastStatus.anyBelowMin -> keep(SuggestionReason.AIM_MIN, oneMore)
                else -> keep(SuggestionReason.ADD_REP, oneMore)
            }
        }
        val weight = lastWeight!!
        val assisted = input.trackingType == TrackingType.ASSISTED_BODYWEIGHT
        return when {
            input.adjustmentActive -> keep(SuggestionReason.ADJUSTMENT_HOLD, oneMore)
            lastStatus.anyBelowMin && before?.anyBelowMin == true ->
                Suggestion(SuggestionReason.DELOAD, easier(weight, input, assisted), atMin, range, previous)
            lastStatus.allTop && !lastStatus.rirOk -> keep(SuggestionReason.REPEAT_HARD, List(planned) { top })
            lastStatus.allTop && input.young && !(before?.allTop == true && before.rirOk) ->
                keep(SuggestionReason.YOUNG_WAIT, List(planned) { top })
            lastStatus.allTop -> Suggestion(SuggestionReason.INCREASE, harder(weight, input, assisted), atMin, range, previous)
            lastStatus.anyBelowMin -> keep(SuggestionReason.AIM_MIN, oneMore)
            else -> keep(SuggestionReason.ADD_REP, oneMore)
        }
    }

    /** Cómo quedó una sesión frente al rango. */
    internal data class Status(val allTop: Boolean, val anyBelowMin: Boolean, val rirOk: Boolean)

    internal fun status(sets: List<WorkoutSet>, min: Int, top: Int, targetRir: Int?): Status = Status(
        allTop = sets.all { (it.reps ?: 0) >= top },
        anyBelowMin = sets.any { (it.reps ?: 0) < min },
        // Con RIR objetivo, cada serie tiene que dejar al menos esas reps. Sin objetivo, solo cuenta
        // como "al fallo" el RIR 0. Sin RIR anotado (modo básico) se da por bueno.
        rirOk = sets.all { set ->
            val rir = set.rir ?: return@all true
            if (targetRir != null) rir >= targetRir else rir > 0
        },
    )

    /** El peso de trabajo de la sesión: el más alto de sus series de trabajo. */
    internal fun referenceWeight(sets: List<WorkoutSet>): Double? = sets.mapNotNull { it.weightKg }.maxOrNull()

    internal fun summary(sets: List<WorkoutSet>): PreviousSummary {
        val reps = sets.mapNotNull { it.reps }
        return PreviousSummary(
            sets = sets.size,
            minReps = reps.minOrNull() ?: 0,
            maxReps = reps.maxOrNull() ?: 0,
            weightKg = referenceWeight(sets),
            minRir = sets.mapNotNull { it.rir }.minOrNull(),
        )
    }

    /** Un salto más difícil: más peso, o menos ayuda en las asistidas. En kg. */
    private fun harder(weightKg: Double, input: ProgressionInput, assisted: Boolean): Double {
        val shown = WeightUnits.fromKg(weightKg, input.unit)
        val next = if (assisted) {
            WeightSteps.nextDown(shown, input.equipment, input.unit)
        } else {
            WeightSteps.nextUp(shown, input.equipment, input.unit, input.young)
        }
        return WeightUnits.toKg(next, input.unit)
    }

    /** Bajar entre 5 % y 10 %: en las asistidas es más ayuda. Siempre al menos un salto. En kg. */
    private fun easier(weightKg: Double, input: ProgressionInput, assisted: Boolean): Double {
        val shown = WeightUnits.fromKg(weightKg, input.unit)
        val next = if (assisted) {
            maxOf(
                WeightSteps.roundUp(shown / DELOAD_FACTOR, input.equipment, input.unit),
                WeightSteps.nextUp(shown, input.equipment, input.unit),
            )
        } else {
            val floor = WeightSteps.minimum(input.equipment, input.unit).coerceAtMost(shown)
            val rounded = WeightSteps.roundDown(shown * DELOAD_FACTOR, input.equipment, input.unit, floor)
            if (rounded < shown - 1e-6) rounded else WeightSteps.nextDown(shown, input.equipment, input.unit, floor)
        }
        return WeightUnits.toKg(next, input.unit)
    }
}
