package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlin.math.floor
import kotlin.math.round

/** Conversión entre kg y la unidad elegida. En disco todo va en kg (SES-10). */
object WeightUnits {
    const val LB_PER_KG = 2.2046226218

    fun fromKg(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) kg * LB_PER_KG else kg

    fun toKg(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) value / LB_PER_KG else value

    /** Barra olímpica: 20 kg o 45 lb. */
    fun defaultBar(unit: WeightUnit): Double = if (unit == WeightUnit.LB) 45.0 else 20.0

    /** Discos de un gimnasio común, de mayor a menor. */
    fun defaultPlates(unit: WeightUnit): List<Double> = if (unit == WeightUnit.LB) {
        listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    } else {
        listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    }
}

/**
 * Discos por lado para un peso total, en la unidad de [total]. [remainder] es lo que no se puede
 * armar con esos discos (por lado); cero si sale exacto.
 */
data class PlateLoad(val perSide: List<Double>, val remainder: Double) {
    val isExact: Boolean get() = remainder < 0.001
}

/** Reparte con el disco más grande primero (SES-09). Si el peso no llega a la barra, no hay discos. */
fun platesPerSide(total: Double, bar: Double, plates: List<Double>): PlateLoad {
    require(total.isFinite() && bar >= 0) { "Peso inválido" }
    var side = (total - bar) / 2
    if (side <= 0) return PlateLoad(emptyList(), 0.0)
    val used = mutableListOf<Double>()
    for (plate in plates.filter { it > 0 }.sortedDescending()) {
        val count = floor(side / plate + 1e-9).toInt()
        repeat(count) { used += plate }
        side -= count * plate
    }
    return PlateLoad(used, round(side * 1000) / 1000)
}

/** Una serie de calentamiento sugerida, en kg. */
data class WarmupStep(val weightKg: Double, val reps: Int)

/**
 * Calentamiento hacia [workKg]: barra × 10 y luego 50 % × 5, 70 % × 3 y 85 % × 1, redondeado
 * hacia abajo al salto de [stepKg]. Se saltan los pasos que no superan al anterior. Sin peso de
 * trabajo por encima de la barra no hay calentamiento.
 */
fun warmupSets(workKg: Double, barKg: Double = 20.0, stepKg: Double = 2.5): List<WarmupStep> {
    if (workKg <= barKg) return emptyList()
    val steps = mutableListOf(WarmupStep(barKg, 10))
    listOf(0.5 to 5, 0.7 to 3, 0.85 to 1).forEach { (share, reps) ->
        val kg = floor(workKg * share / stepKg + 1e-9) * stepKg
        if (kg > steps.last().weightKg && kg < workKg) steps += WarmupStep(kg, reps)
    }
    return steps
}
