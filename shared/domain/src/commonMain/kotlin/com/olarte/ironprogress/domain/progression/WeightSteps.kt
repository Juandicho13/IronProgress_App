package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.session.WeightUnits
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Los pesos que de verdad se pueden armar con cada equipo, en la unidad con que se ve el ejercicio
 * (HANDOFF 11.2): barra con el par de discos más pequeño (2,5 kg o 5 lb), el siguiente par de
 * mancuernas y el siguiente escalón en máquinas y poleas. Todo se calcula en esa unidad para que en
 * lb suba de 5 en 5 y no de 2,2 en 2,2; quien guarda convierte a kg.
 */
object WeightSteps {
    private const val EPS = 1e-6

    private val DUMBBELLS_KG =
        (1..10).map { it.toDouble() } + (12..50 step 2).map { it.toDouble() } + (55..100 step 5).map { it.toDouble() }
    private val DUMBBELLS_LB = listOf(2.5, 5.0, 8.0, 10.0, 12.0) + (15..150 step 5).map { it.toDouble() }
    private val KETTLEBELLS_KG = listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 32.0, 36.0, 40.0, 44.0, 48.0)
    private val KETTLEBELLS_LB = (5..100 step 5).map { it.toDouble() }

    private val BAR_LOADED = setOf(Equipment.BARBELL, Equipment.EZ_BAR, Equipment.TRAP_BAR, Equipment.SMITH_MACHINE)

    /** Si el equipo se carga con discos a los dos lados de una barra. */
    fun isBarLoaded(equipment: Equipment): Boolean = equipment in BAR_LOADED

    /**
     * Salto de una rejilla regular. En modo joven las máquinas suben con el escalón más pequeño que
     * suele tener la placa adicional (2,5 kg o 5 lb), nunca con uno más grande (HANDOFF 11.11).
     */
    fun step(
        equipment: Equipment,
        unit: WeightUnit,
        young: Boolean = false,
        plates: List<Double> = WeightUnits.defaultPlates(unit),
    ): Double {
        val lb = unit == WeightUnit.LB
        return when {
            isBarLoaded(equipment) -> 2 * (plates.filter { it > 0 }.minOrNull() ?: if (lb) 2.5 else 1.25)
            equipment == Equipment.MACHINE && !young -> if (lb) 10.0 else 5.0
            else -> if (lb) 5.0 else 2.5
        }
    }

    /** Lista fija de pesos para equipos que no van en rejilla (mancuernas y pesas rusas); null si va en rejilla. */
    private fun ladderOf(equipment: Equipment, unit: WeightUnit): List<Double>? = when (equipment) {
        Equipment.DUMBBELL -> if (unit == WeightUnit.LB) DUMBBELLS_LB else DUMBBELLS_KG
        Equipment.KETTLEBELL -> if (unit == WeightUnit.LB) KETTLEBELLS_LB else KETTLEBELLS_KG
        else -> null
    }

    /** El siguiente peso que se puede armar por encima de [current]. */
    fun nextUp(current: Double, equipment: Equipment, unit: WeightUnit, young: Boolean = false): Double {
        ladderOf(equipment, unit)?.let { list ->
            return list.firstOrNull { it > current + EPS }
                ?: (list.last() + (list.last() - list[list.size - 2]))
        }
        val step = step(equipment, unit, young)
        return (floor(current / step + EPS) + 1) * step
    }

    /** El peso armable más alto que no pasa de [target]; nunca baja de [floorValue]. */
    fun roundDown(target: Double, equipment: Equipment, unit: WeightUnit, floorValue: Double = 0.0): Double {
        val rounded = ladderOf(equipment, unit)?.let { list -> list.lastOrNull { it <= target + EPS } ?: list.first() }
            ?: run {
                val step = step(equipment, unit)
                floor(target / step + EPS) * step
            }
        return rounded.coerceAtLeast(floorValue)
    }

    /** El peso armable más bajo que no queda por debajo de [target]. */
    fun roundUp(target: Double, equipment: Equipment, unit: WeightUnit): Double {
        ladderOf(equipment, unit)?.let { list -> return list.firstOrNull { it >= target - EPS } ?: list.last() }
        val step = step(equipment, unit)
        return ceil(target / step - EPS) * step
    }

    /** El peso armable inmediatamente por debajo de [current]; [floorValue] como mínimo. */
    fun nextDown(current: Double, equipment: Equipment, unit: WeightUnit, floorValue: Double = 0.0): Double {
        val below = ladderOf(equipment, unit)?.let { list -> list.lastOrNull { it < current - EPS } }
            ?: run {
                val step = step(equipment, unit)
                (ceil(current / step - EPS) - 1) * step
            }
        return below.coerceAtLeast(floorValue)
    }

    /** Lo mínimo que se puede cargar: la barra en los de barra y cero en el resto. */
    fun minimum(equipment: Equipment, unit: WeightUnit): Double = if (equipment == Equipment.BARBELL || equipment == Equipment.TRAP_BAR) {
        WeightUnits.defaultBar(unit)
    } else {
        0.0
    }
}
