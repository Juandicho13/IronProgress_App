package com.olarte.ironprogress.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberForjaHaptics(enabled: Boolean): ForjaHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, enabled) {
        if (!enabled) {
            NoOpForjaHaptics
        } else {
            AndroidForjaHaptics(view = view, context = context.applicationContext)
        }
    }
}

private class AndroidForjaHaptics(
    private val view: View,
    private val context: Context,
) : ForjaHaptics {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun setCompleted() {
        performConfirmOrFallback()
    }

    override fun segmentTick() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performWithFallback(constant, HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun keyboardTap() {
        performWithFallback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }

    override fun restCountdownTick() {
        performWithFallback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }

    override fun restFinished() {
        // Patron del sistema de diseno Forja: 0 ms espera, 180 ms vibra, 90 ms pausa, 180 ms vibra.
        val vibratedWithWaveform = runCatching {
            val currentVibrator = vibrator
            if (currentVibrator != null && currentVibrator.hasVibrator()) {
                val timings = longArrayOf(0L, 180L, 90L, 180L)
                val effect = VibrationEffect.createWaveform(timings, -1)
                currentVibrator.vibrate(effect)
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!vibratedWithWaveform) {
            performConfirmOrFallback()
        }
    }

    override fun personalRecord() {
        // CONFIRM dos veces con 120 ms de pausa.
        performConfirmOrFallback()
        view.postDelayed(
            { performConfirmOrFallback() },
            PERSONAL_RECORD_SECOND_PULSE_DELAY_MS,
        )
    }

    override fun slideThresholdReached() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performWithFallback(constant, HapticFeedbackConstants.LONG_PRESS)
    }

    override fun actionRejected() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performWithFallback(constant, HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun performConfirmOrFallback() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performWithFallback(constant, HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun performWithFallback(primaryConstant: Int, fallbackConstant: Int) {
        val performed = view.performHapticFeedback(primaryConstant)
        if (!performed && primaryConstant != fallbackConstant) {
            view.performHapticFeedback(fallbackConstant)
        }
    }

    private companion object {
        const val PERSONAL_RECORD_SECOND_PULSE_DELAY_MS = 120L
    }
}