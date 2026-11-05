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

@Composable
actual fun rememberVoiceSpeaker(): VoiceSpeaker {
    val speaker = remember { AppleVoiceSpeaker() }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

/**
 * Voz del sistema con AVSpeechSynthesizer. La sesión de audio usa duckOthers: la música baja
 * mientras habla y, al callar, la sesión se desactiva avisando a las otras apps para que suba.
 */
@OptIn(ExperimentalForeignApi::class)
class AppleVoiceSpeaker : VoiceSpeaker {
    private val synthesizer = AVSpeechSynthesizer()
    private val scope = MainScope()
    private var watcher: Job? = null

    override fun speak(text: String, duckMusic: Boolean) {
        val session = AVAudioSession.sharedInstance()
        val options = if (duckMusic) AVAudioSessionCategoryOptionDuckOthers else AVAudioSessionCategoryOptionMixWithOthers
        session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeVoicePrompt, options = options, error = null)
        session.setActive(true, error = null)
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(NSLocale.currentLocale.languageCode)
        synthesizer.speakUtterance(utterance)
        watchUntilSilent()
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        deactivate()
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    /** Sin delegado: revisa cada 200 ms si ya calló para devolver la música. */
    private fun watchUntilSilent() {
        watcher?.cancel()
        watcher = scope.launch {
            delay(200)
            while (synthesizer.speaking) delay(200)
            deactivate()
        }
    }

    private fun deactivate() {
        AVAudioSession.sharedInstance().setActive(
            false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
    }
}
