package com.olarte.ironprogress.ui.rest

import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rest_notification_body
import com.olarte.ironprogress.ui.resources.rest_notification_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSLog
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Clock

// Vive fuera del ViewModel: cerrar la pantalla no borra el aviso entregado a iOS.
private val notifications by lazy {
    QueuedRestNotifications(CoroutineScope(SupervisorJob() + Dispatchers.Main), Clock.System, IosRestNotificationBackend())
}

actual fun platformRestNotifications(): RestNotifications = notifications

private class IosRestNotificationBackend : RestNotificationBackend {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val identifier = "ironprogress.rest.finished"

    override suspend fun authorize(): Boolean = suspendCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, error ->
            if (error != null) {
                continuation.resumeWithException(IllegalStateException(error.localizedDescription))
            } else {
                continuation.resume(granted)
            }
        }
    }

    override suspend fun replace(deadlineMs: Long?) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(identifier))
        if (deadlineMs == null) return
        val content = UNMutableNotificationContent().apply {
            setTitle(getString(Res.string.rest_notification_title))
            setBody(getString(Res.string.rest_notification_body))
            setSound(UNNotificationSound.defaultSound())
        }
        // El permiso y los recursos pueden tardar: conserva el fin absoluto del motor.
        val remaining = (deadlineMs - Clock.System.now().toEpochMilliseconds()) / 1000.0
        if (remaining <= 0) return
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(remaining, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
        suspendCoroutine<Unit> { continuation ->
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override fun reportFailure(error: Exception) {
        NSLog("No se pudo programar el aviso de descanso: %@", error.message.orEmpty())
    }
}
