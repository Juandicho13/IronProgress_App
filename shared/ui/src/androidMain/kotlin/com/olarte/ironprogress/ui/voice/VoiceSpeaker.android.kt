package com.olarte.ironprogress.ui.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

@Composable
actual fun rememberVoiceSpeaker(): VoiceSpeaker {
    val context = LocalContext.current.applicationContext
    val speaker = remember(context) { AndroidVoiceSpeaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

/**
 * Voz del sistema con TextToSpeech. Mientras habla pide el foco de audio con atenuación
 * (AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK): la música baja y vuelve al terminar la última frase.
 * También la puede usar el servicio del entreno, que vive sin pantalla.
 */
class AndroidVoiceSpeaker(context: Context) :
    VoiceSpeaker,
    TextToSpeech.OnInitListener {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private val pendingUtterances = AtomicInteger(0)
    private val waiting = mutableListOf<Pair<String, Boolean>>()
    private val nextId = AtomicInteger(0)

    @Volatile private var ready = false

    @Volatile private var holdsFocus = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.getDefault()
        tts.setAudioAttributes(attributes)
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) = finishOne()

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finishOne()

                override fun onError(utteranceId: String?, errorCode: Int) = finishOne()
            },
        )
        ready = true
        synchronized(waiting) {
            waiting.forEach { (text, duck) -> speak(text, duck) }
            waiting.clear()
        }
    }

    override fun speak(text: String, duckMusic: Boolean) {
        if (!ready) {
            synchronized(waiting) { waiting += text to duckMusic }
            return
        }
        if (duckMusic && !holdsFocus) {
            holdsFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        pendingUtterances.incrementAndGet()
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "ironprogress-${nextId.incrementAndGet()}")
    }

    override fun stop() {
        synchronized(waiting) { waiting.clear() }
        if (ready) tts.stop()
        pendingUtterances.set(0)
        releaseFocus()
    }

    override fun release() {
        stop()
        tts.shutdown()
    }

    private fun finishOne() {
        if (pendingUtterances.decrementAndGet() <= 0) {
            pendingUtterances.set(0)
            releaseFocus()
        }
    }

    private fun releaseFocus() {
        if (holdsFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            holdsFocus = false
        }
    }
}
