package com.olarte.ironprogress.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Accesibilidad de los cronómetros (F1-27). TalkBack y VoiceOver anuncian una región viva cada vez
 * que su texto cambia, así que solo se marca la etiqueta de fase (Trabajo, Descanso, Inhala, la
 * postura, el minuto del EMOM), que cambia por fase. La cifra que cambia cada segundo queda fuera
 * de cualquier región viva: se lee al tocarla y nunca se anuncia sola.
 */
fun Modifier.timerPhase(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
