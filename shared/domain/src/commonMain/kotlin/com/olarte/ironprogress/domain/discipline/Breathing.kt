package com.olarte.ironprogress.domain.discipline

import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.TimerPlans

/** Qué tan delicado es un patrón. Desde [CAUTION] se avisa antes de empezar (PANTALLAS 7.3). */
enum class BreathingRisk {
    LOW,

    /** Retención larga (7 s o más). */
    CAUTION,

    /** Hiperventilación o retenciones muy largas: llegan con el catálogo de F5. */
    HIGH,
}

/** Patrón de respiración: segundos de cada fase (0 se salta). */
data class BreathingPattern(val id: String, val inhale: Int, val holdIn: Int, val exhale: Int, val holdOut: Int, val risk: BreathingRisk) {
    init {
        require(inhale > 0 && exhale > 0 && holdIn >= 0 && holdOut >= 0)
    }

    val cycleSeconds: Int get() = inhale + holdIn + exhale + holdOut

    /** El aviso de seguridad sale como pregunta antes de empezar. */
    val warnsBeforeStart: Boolean get() = risk >= BreathingRisk.CAUTION

    /** Ciclos enteros que caben en [minutes], al menos uno; el último no se corta. */
    fun cyclesFor(minutes: Int): Int = ((minutes * 60 + cycleSeconds - 1) / cycleSeconds).coerceAtLeast(1)

    fun toPlan(minutes: Int): TimerPlan = TimerPlans.breathing(inhale, holdIn, exhale, holdOut, cyclesFor(minutes))

    companion object {
        val BOX = BreathingPattern("box", 4, 4, 4, 4, BreathingRisk.LOW)
        val RELAXING = BreathingPattern("relax-478", 4, 7, 8, 0, BreathingRisk.CAUTION)
        val COHERENT = BreathingPattern("coherent", 5, 0, 5, 0, BreathingRisk.LOW)
        val all = listOf(BOX, RELAXING, COHERENT)
        const val DEFAULT_MINUTES = 5
    }
}
