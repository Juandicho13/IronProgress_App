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
 * Voz del sistema con TextToSpeech. Según "Audio durante el entreno":
 * - MIX: no pide el foco de audio; la música sigue igual y la voz suena encima.
 * - DUCK: foco AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK; la música baja y vuelve al terminar la última frase.
 * - PAUSE: foco AUDIOFOCUS_GAIN_TRANSIENT; la música se pausa y sigue al terminar.
 * TextToSpeech habla en su propio hilo: speak() solo encola y vuelve enseguida.
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
    private val duckRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private val pauseRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .build()
    private val pendingUtterances = AtomicInteger(0)
    private val waiting = mutableListOf<Pair<String, MusicBehavior>>()
    private val nextId = AtomicInteger(0)

    @Volatile private var ready = false

    @Volatile private var heldFocus: AudioFocusRequest? = null
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
            waiting.forEach { (text, music) -> speak(text, music) }
            waiting.clear()
        }
    }

    override fun speak(text: String, music: MusicBehavior) {
        if (!ready) {
            synchronized(waiting) { waiting += text to music }
            return
        }
        val wanted = when (music) {
            MusicBehavior.MIX -> null
            MusicBehavior.DUCK -> duckRequest
            MusicBehavior.PAUSE -> pauseRequest
        }
        if (wanted != null && heldFocus !== wanted) {
            releaseFocus()
            if (audioManager.requestAudioFocus(wanted) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) heldFocus = wanted
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
        heldFocus?.let { audioManager.abandonAudioFocusRequest(it) }
        heldFocus = null
    }
}
