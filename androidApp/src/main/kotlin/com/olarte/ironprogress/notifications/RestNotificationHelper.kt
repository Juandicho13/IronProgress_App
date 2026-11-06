package com.olarte.ironprogress.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.olarte.ironprogress.MainActivity
import com.olarte.ironprogress.R
import com.olarte.ironprogress.session.RestForegroundService
import com.olarte.ironprogress.ui.rest.RestNotificationContent
import com.olarte.ironprogress.ui.rest.RestNotificationStrings

/**
 * Construye la notificación en curso del descanso y pide su promoción a Live Update / Now Bar en
 * Android 16+ cuando el sistema lo permite (PANTALLAS 5.8).
 */
object RestNotificationHelper {
    const val CHANNEL_ID = "forja_rest_channel"
    const val NOTIFICATION_ID = 1400

    private const val REQUEST_OPEN_WORKOUT = 1401
    private const val REQUEST_ADJUST_PLUS_15 = 1402
    private const val REQUEST_SKIP_REST = 1403
    private const val REQUEST_COMPLETE_SET = 1404

    fun canPostNotifications(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.rest_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            // El sonido y la vibración solo salen una vez al llegar a 0 (triggerReadyAlert).
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun strings(context: Context): RestNotificationStrings = RestNotificationStrings(
        defaultWorkoutName = context.getString(R.string.workout_free),
        repsSuffix = context.getString(R.string.rest_notification_reps_unit),
        readyChip = context.getString(R.string.rest_notification_ready_chip),
        restingTitle = { exercise -> context.getString(R.string.rest_notification_resting_title, exercise) },
        nextSetWithTarget = { setNumber, totalSets, target ->
            context.getString(R.string.rest_notification_next_set_with_target, setNumber, totalSets, target)
        },
        nextSetOnly = { setNumber, totalSets ->
            context.getString(R.string.rest_notification_next_set_only, setNumber, totalSets)
        },
        completedSetLine = { setNumber, target ->
            context.getString(R.string.rest_notification_completed_set_line, setNumber, target)
        },
        readyTitle = { setNumber, totalSets ->
            context.getString(R.string.rest_notification_ready_title, setNumber, totalSets)
        },
        readyTextWithTarget = { exercise, target ->
            context.getString(R.string.rest_notification_ready_text_with_target, exercise, target)
        },
    )

    fun buildNotification(context: Context, content: RestNotificationContent): Notification {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(content.title)
            .setContentText(content.collapsedText)
            .setStyle(Notification.BigTextStyle().bigText(content.expandedText))
            .setOngoing(content.ongoing)
            .setOnlyAlertOnce(true)
            .setAutoCancel(!content.ongoing)
            .setCategory(Notification.CATEGORY_WORKOUT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkoutPendingIntent(context, content.sessionId.value))

        val endsAtMs = content.endsAtMs
        if (content.showChronometer && endsAtMs != null) {
            builder
                .setWhen(endsAtMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder
                .setWhen(content.endsAtMs ?: System.currentTimeMillis())
                .setShowWhen(true)
                .setUsesChronometer(false)
                .setChronometerCountDown(false)
        }

        if (Build.VERSION.SDK_INT >= 36) {
            val promote = content.ongoing && manager?.canPostPromotedNotifications() == true
            builder.setRequestPromotedOngoing(promote)
            builder.setShortCriticalText(if (promote) content.shortCriticalText else null)
        }

        if (content.showAdjustAndSkipActions) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.rest_more),
                    serviceActionPendingIntent(context, RestForegroundService.ACTION_ADJUST_PLUS_15, REQUEST_ADJUST_PLUS_15),
                ).build(),
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.rest_notification_action_skip),
                    serviceActionPendingIntent(context, RestForegroundService.ACTION_SKIP_REST, REQUEST_SKIP_REST),
                ).build(),
            )
        }

        if (content.showCompleteSetAction) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.workout_complete),
                    serviceActionPendingIntent(context, RestForegroundService.ACTION_COMPLETE_NEXT_SET, REQUEST_COMPLETE_SET),
                ).build(),
            )
        }

        return builder.build()
    }

    /**
     * Suena y vibra una vez con el patrón del fin del descanso de Forja (0, 180, 90, 180 ms).
     * Si el teléfono está en silencio o en vibración, solo vibra (PANTALLAS 5.8).
     */
    fun triggerReadyAlert(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                val timings = longArrayOf(0L, 180L, 90L, 180L)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        }
        runCatching {
            val audioManager = context.getSystemService(AudioManager::class.java)
            if (audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (uri != null) {
                    RingtoneManager.getRingtone(context.applicationContext, uri)?.play()
                }
            }
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun openWorkoutPendingIntent(context: Context, sessionId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WORKOUT
            putExtra(MainActivity.EXTRA_SESSION_ID, sessionId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_WORKOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun serviceActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, RestForegroundService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
