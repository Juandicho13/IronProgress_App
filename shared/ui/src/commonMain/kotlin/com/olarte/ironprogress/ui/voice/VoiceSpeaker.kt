package com.olarte.ironprogress.ui.voice

import androidx.compose.runtime.Composable

/**
 * La voz del teléfono (F1-17). Por ahora todo sale con la voz del sistema; cuando exista la voz
 * grabada (HANDOFF 13.10), las cuentas y las frases fijas saldrán de composeResources/files/voice.
 */
interface VoiceSpeaker {
    /** Dice [text] después de lo que ya esté diciendo. Con [duckMusic], baja la música mientras habla. */
    fun speak(text: String, duckMusic: Boolean = true)

    /** Calla y descarta lo pendiente; la música vuelve a su volumen. */
    fun stop()

    /** Libera el motor de voz. Después de esto no se vuelve a usar. */
    fun release()
}

/** La voz del sistema, viva mientras la pantalla que la usa esté en composición. */
@Composable
expect fun rememberVoiceSpeaker(): VoiceSpeaker

private object SilentSpeaker : VoiceSpeaker {
    override fun speak(text: String, duckMusic: Boolean) = Unit

    override fun stop() = Unit

    override fun release() = Unit
}

/** Con la voz apagada en Ajustes (F1-25) no dice nada; el subtítulo sigue en pantalla. */
fun VoiceSpeaker.mutedUnless(enabled: Boolean): VoiceSpeaker = if (enabled) this else SilentSpeaker
