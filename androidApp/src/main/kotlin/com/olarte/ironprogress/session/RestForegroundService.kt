package com.olarte.ironprogress.session

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.olarte.ironprogress.di.resolveRestBackgroundActions
import com.olarte.ironprogress.di.resolveRestController
import com.olarte.ironprogress.di.startIronProgress
import com.olarte.ironprogress.notifications.RestNotificationHelper
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.platformFormatter
import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.rest.RestPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano tipo `health` para mantener el descanso activo con la app cerrada y
 * actualizar la Live Update / Now Bar sin contar segundo a segundo (F1-14).
 */
class RestForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var transitionJob: Job? = null
    private var inForeground = false

    override fun onCreate() {
        super.onCreate()
        startIronProgress(applicationContext)
        RestNotificationHelper.ensureChannel(applicationContext)
        observeRestState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startIronProgress(applicationContext)
        val actions = resolveRestBackgroundActions()
        when (intent?.action) {
            ACTION_ADJUST_PLUS_15 -> {
                actions.adjustRest(15)
                serviceScope.launch { syncNotification() }
            }
            ACTION_SKIP_REST -> {
                actions.skipRest()
                serviceScope.launch { syncNotification() }
            }
            ACTION_COMPLETE_NEXT_SET -> {
                serviceScope.launch {
                    actions.completeNextSet()
                    syncNotification()
                }
            }
            ACTION_STOP -> {
                stopForegroundAndSelf(removeNotification = true)
                return START_NOT_STICKY
            }
            else -> {
                serviceScope.launch { syncNotification() }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        transitionJob?.cancel()
        stateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeRestState() {
        val controller = resolveRestController()
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            controller.state.collect {
                syncNotification()
            }
        }
    }

    private suspend fun syncNotification() {
        transitionJob?.cancel()
        if (!RestNotificationHelper.canPostNotifications(applicationContext)) {
            stopForegroundAndSelf(removeNotification = true)
            return
        }

        val controller = resolveRestController()
        val actions = resolveRestBackgroundActions()
        val locale = resources.configuration.locales.get(0)
        val languageCode = locale?.language ?: "es"
        val languageTag = locale?.toLanguageTag() ?: "es-CO"
        val formats = Formats(platformFormatter(languageTag))
        val strings = RestNotificationHelper.strings(applicationContext)

        val content = actions.buildCurrentNotification(
            languageCode = languageCode,
            formats = formats,
            strings = strings,
        )

        if (content == null) {
            stopForegroundAndSelf(removeNotification = true)
            return
        }

        val notification = RestNotificationHelper.buildNotification(applicationContext, content)
        val manager = getSystemService(NotificationManager::class.java)

        if (content.ongoing) {
            promoteToForeground(notification)
        } else {
            if (inForeground) {
                stopForeground(STOP_FOREGROUND_DETACH)
                inForeground = false
            }
            manager?.notify(RestNotificationHelper.NOTIFICATION_ID, notification)
            stopSelf()
            return
        }

        if (content.alertNow) {
            controller.markReadyAlertDelivered()
            RestNotificationHelper.triggerReadyAlert(applicationContext)
        }

        val nowMs = actions.nowMs()
        when (content.phase) {
            RestPhase.RESTING -> {
                val endsAtMs = content.endsAtMs ?: return
                val waitMs = (endsAtMs - nowMs).coerceAtLeast(0L)
                transitionJob = serviceScope.launch {
                    delay(waitMs)
                    controller.onRestCountdownFinished(actions.nowMs())
                    syncNotification()
                }
            }
            RestPhase.READY -> {
                val readyAtMs = content.endsAtMs ?: nowMs
                val remainingUntilExpiryMs = (readyAtMs + RestController.READY_TIMEOUT_MS - nowMs).coerceAtLeast(0L)
                if (remainingUntilExpiryMs > 0L) {
                    transitionJob = serviceScope.launch {
                        delay(remainingUntilExpiryMs)
                        syncNotification()
                    }
                }
            }
            RestPhase.IDLE -> stopForegroundAndSelf(removeNotification = true)
        }
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        if (!inForeground) {
            val started = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        RestNotificationHelper.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
                    )
                } else {
                    startForeground(RestNotificationHelper.NOTIFICATION_ID, notification)
                }
                true
            }.getOrDefault(false)
            if (started) {
                inForeground = true
            } else {
                manager?.notify(RestNotificationHelper.NOTIFICATION_ID, notification)
            }
        } else {
            manager?.notify(RestNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundAndSelf(removeNotification: Boolean) {
        transitionJob?.cancel()
        if (inForeground) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
            inForeground = false
        } else if (removeNotification) {
            RestNotificationHelper.cancel(applicationContext)
        }
        stopSelf()
    }

    companion object {
        const val ACTION_SYNC = "com.olarte.ironprogress.session.action.SYNC"
        const val ACTION_ADJUST_PLUS_15 = "com.olarte.ironprogress.session.action.ADJUST_PLUS_15"
        const val ACTION_SKIP_REST = "com.olarte.ironprogress.session.action.SKIP_REST"
        const val ACTION_COMPLETE_NEXT_SET = "com.olarte.ironprogress.session.action.COMPLETE_NEXT_SET"
        const val ACTION_STOP = "com.olarte.ironprogress.session.action.STOP"

        fun sync(context: Context, shouldRun: Boolean) {
            val appContext = context.applicationContext
            if (shouldRun && RestNotificationHelper.canPostNotifications(appContext)) {
                val intent = Intent(appContext, RestForegroundService::class.java).setAction(ACTION_SYNC)
                runCatching {
                    appContext.startForegroundService(intent)
                }.onFailure {
                    runCatching { appContext.startService(intent) }
                }
            } else {
                RestNotificationHelper.cancel(appContext)
                runCatching {
                    appContext.stopService(Intent(appContext, RestForegroundService::class.java))
                }
            }
        }
    }
}
