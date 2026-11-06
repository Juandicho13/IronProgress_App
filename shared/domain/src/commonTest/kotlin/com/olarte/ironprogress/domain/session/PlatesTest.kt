package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatesTest {

    private val kgPlates = WeightUnits.defaultPlates(WeightUnit.KG)

    @Test
    fun platesPerSideUseTheBiggestFirst() {
        assertEquals(PlateLoad(listOf(25.0, 15.0), 0.0), platesPerSide(100.0, 20.0, kgPlates))
        assertEquals(PlateLoad(listOf(20.0, 1.25), 0.0), platesPerSide(62.5, 20.0, kgPlates))
        assertEquals(PlateLoad(emptyList(), 0.0), platesPerSide(20.0, 20.0, kgPlates))
        assertEquals(PlateLoad(listOf(45.0, 45.0), 0.0), platesPerSide(225.0, 45.0, WeightUnits.defaultPlates(WeightUnit.LB)))
    }

    @Test
    fun whatCannotBeLoadedIsReported() {
        val load = platesPerSide(61.0, 20.0, kgPlates)
        assertEquals(listOf(20.0), load.perSide)
        assertEquals(0.5, load.remainder)
        assertTrue(!load.isExact)
    }

    @Test
    fun changingTheUnitOnlyChangesHowItLooks() {
        // 62,5 kg se ven como 137,8 lb; el valor guardado sigue en kg.
        val stored = 62.5
        val shown = WeightUnits.fromKg(stored, WeightUnit.LB)
        assertEquals(137.8, kotlin.math.round(shown * 10) / 10)
        assertEquals(stored, WeightUnits.toKg(shown, WeightUnit.LB), 1e-9)
        assertEquals(stored, WeightUnits.fromKg(stored, WeightUnit.KG))
    }

    @Test
    fun warmupRampsToTheWorkingWeight() {
        assertEquals(
            listOf(WarmupStep(20.0, 10), WarmupStep(50.0, 5), WarmupStep(70.0, 3), WarmupStep(85.0, 1)),
            warmupSets(100.0),
        )
        // 40 kg: 50 % es 20 (igual a la barra) y se salta.
        assertEquals(listOf(WarmupStep(20.0, 10), WarmupStep(27.5, 3), WarmupStep(32.5, 1)), warmupSets(40.0))
        assertTrue(warmupSets(20.0).isEmpty())
    }
}
