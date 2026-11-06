package com.olarte.ironprogress.ui.rest

import kotlin.test.Test
import kotlin.test.assertEquals

class RestNotificationTextsTest {
    @Test
    fun fillsNumberedPlaceholders() {
        assertEquals("Sigue: serie 3 de 4 · 62,5 kg × 10", fill("Sigue: serie %1\$d de %2\$d · %3\$s", 3, 4, "62,5 kg × 10"))
        assertEquals("Descanso · Press banca", fill("Descanso · %1\$s", "Press banca"))
    }
}
