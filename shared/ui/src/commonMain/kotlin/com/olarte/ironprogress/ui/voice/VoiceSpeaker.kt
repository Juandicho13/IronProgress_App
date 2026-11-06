package com.olarte.ironprogress.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import com.olarte.ironprogress.ui.settings.LocalAppSettings

/** Qué hace la voz con la música del teléfono mientras habla. */
enum class MusicBehavior {
    /** La música sigue igual y la voz se mezcla encima. */
    MIX,

    /** La música baja mientras habla y vuelve al terminar. */
    DUCK,

    /** La música se pausa mientras habla y sigue al terminar. */
    PAUSE,
}

/** El ajuste "Audio durante el entreno" en lo que entiende la voz; null es que no habla. */
fun WorkoutAudio.musicBehavior(): MusicBehavior? = when (this) {
    WorkoutAudio.KEEP_VOLUME -> MusicBehavior.MIX
    WorkoutAudio.DUCK -> MusicBehavior.DUCK
    WorkoutAudio.PAUSE_MUSIC -> MusicBehavior.PAUSE
    WorkoutAudio.NO_VOICE -> null
}

/**
 * La voz del teléfono (F1-17). Por ahora todo sale con la voz del sistema; cuando exista la voz
 * grabada (HANDOFF 13.10), las cuentas y las frases fijas saldrán de composeResources/files/voice.
 * [speak] nunca bloquea: encola y vuelve enseguida, así las animaciones no se detienen al hablar.
 */
interface VoiceSpeaker {
    /** Dice [text] después de lo que ya esté diciendo, tratando la música según [music]. */
    fun speak(text: String, music: MusicBehavior = MusicBehavior.MIX)

    /** Calla y descarta lo pendiente; la música vuelve a como estaba. */
    fun stop()

    /** Libera el motor de voz. Después de esto no se vuelve a usar. */
    fun release()
}

/** La voz del sistema, viva mientras la pantalla que la usa esté en composición. */
@Composable
expect fun rememberVoiceSpeaker(): VoiceSpeaker

private object SilentSpeaker : VoiceSpeaker {
    override fun speak(text: String, music: MusicBehavior) = Unit

    override fun stop() = Unit

    override fun release() = Unit
}

private class AudioSettingSpeaker(private val inner: VoiceSpeaker, private val music: MusicBehavior) : VoiceSpeaker {
    override fun speak(text: String, music: MusicBehavior) = inner.speak(text, this.music)

    override fun stop() = inner.stop()

    override fun release() = inner.release()
}

/**
 * Aplica "Audio durante el entreno" de Ajustes: con "Sin voz" no dice nada (el subtítulo sigue en
 * pantalla) y con las otras opciones trata la música como se eligió, sin importar lo que pida la pantalla.
 */
fun VoiceSpeaker.withAudio(audio: WorkoutAudio): VoiceSpeaker {
    val music = audio.musicBehavior() ?: return SilentSpeaker
    return AudioSettingSpeaker(this, music)
}

/** La voz del sistema con el ajuste vigente de Ajustes; la misma instancia mientras el ajuste no cambie. */
@Composable
fun rememberWorkoutVoice(): VoiceSpeaker {
    val base = rememberVoiceSpeaker()
    val audio = LocalAppSettings.current.workoutAudio
    return remember(base, audio) { base.withAudio(audio) }
}
