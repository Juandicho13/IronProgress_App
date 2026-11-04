package com.olarte.ironprogress.ui.format

import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/** Corre el actual de Android (java.text) en la JVM del computador. */
class FormatsTest {

    private val spanish = Formats(platformFormatter("es-CO"))
    private val english = Formats(platformFormatter("en-US"))

    @Test
    fun weightsUseTheDecimalSeparatorOfTheLanguage() {
        assertEquals("62,5 kg", spanish.weight(62.5))
        assertEquals("62.5 kg", english.weight(62.5))
        assertEquals("1,25 kg", spanish.weight(1.25))
        assertEquals("100 kg", spanish.weight(100.0))
    }

    @Test
    fun volumesGroupThousands() {
        assertEquals("8.420 kg", spanish.volume(8_420.0))
        assertEquals("8,420 kg", english.volume(8_420.0))
        assertEquals("12.345 kg", spanish.volume(12_344.6))
    }

    @Test
    fun poundsAreOnlyForDisplay() {
        assertEquals("220.5 lb", english.weight(100.0, WeightUnit.LB))
        assertEquals("+22 lb", english.signedWeight(10.0, WeightUnit.LB))
        assertEquals("−20 kg", spanish.signedWeight(-20.0))
    }

    @Test
    fun durationsAndDistances() {
        assertEquals("1:12", Formats.duration(72))
        assertEquals("0:05", Formats.duration(5))
        assertEquals("1:02:05", Formats.duration(3_725))
        assertEquals("800 m", spanish.distance(800.0))
        assertEquals("5,2 km", spanish.distance(5_200.0))
        assertEquals("5.2 km", english.distance(5_200.0))
    }
}
