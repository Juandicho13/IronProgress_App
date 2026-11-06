package com.olarte.ironprogress.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeVoicePrompt
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

@Composable
actual fun rememberVoiceSpeaker(): VoiceSpeaker {
    val speaker = remember { AppleVoiceSpeaker() }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

/**
 * Voz del sistema con AVSpeechSynthesizer. Según "Audio durante el entreno":
 * - MIX: mixWithOthers; la música sigue igual.
 * - DUCK: duckOthers e interruptSpokenAudioAndMixWithOthers; la música baja mientras habla.
 * - PAUSE: sin opciones de mezcla; la música se pausa y, al callar, la sesión se desactiva avisando
 *   a las otras apps para que sigan.
 *
 * Activar y desactivar la sesión de audio bloquea el hilo que lo pide (a veces cientos de ms), y
 * eso congelaba las animaciones de Respiración, Flujo, Intervalos y el bloque funcional cada vez
 * que hablaba. Todo lo de audio va ahora en una cola propia en segundo plano: speak() solo encola y
 * vuelve enseguida, así la pantalla nunca espera a la voz.
 */
@OptIn(ExperimentalForeignApi::class)
class AppleVoiceSpeaker : VoiceSpeaker {
    private val synthesizer = AVSpeechSynthesizer()
    private val queue = dispatch_queue_create("com.olarte.ironprogress.voice", null)
    private val scope = MainScope()
    private var watcher: Job? = null
    private var configured: MusicBehavior? = null
    private var active = false

    override fun speak(text: String, music: MusicBehavior) {
        dispatch_async(queue) {
            val session = AVAudioSession.sharedInstance()
            if (configured != music) {
                if (active) deactivate()
                val options = when (music) {
                    MusicBehavior.MIX -> AVAudioSessionCategoryOptionMixWithOthers
                    MusicBehavior.DUCK ->
                        AVAudioSessionCategoryOptionDuckOthers or AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers
                    MusicBehavior.PAUSE -> 0uL
                }
                session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeVoicePrompt, options = options, error = null)
                configured = music
            }
            if (!active) {
                session.setActive(true, error = null)
                active = true
            }
            val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(NSLocale.currentLocale.languageCode)
            synthesizer.speakUtterance(utterance)
        }
        watchUntilSilent()
    }

    override fun stop() {
        dispatch_async(queue) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            deactivate()
        }
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    /** Sin delegado: revisa cada 250 ms si ya calló para devolver la música, sin bloquear la pantalla. */
    private fun watchUntilSilent() {
        watcher?.cancel()
        watcher = scope.launch {
            delay(400)
            while (synthesizer.speaking) delay(250)
            dispatch_async(queue) { if (!synthesizer.speaking) deactivate() }
        }
    }

    /** Solo desde la cola de audio. */
    private fun deactivate() {
        if (!active) return
        AVAudioSession.sharedInstance().setActive(
            false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
        active = false
    }
}
