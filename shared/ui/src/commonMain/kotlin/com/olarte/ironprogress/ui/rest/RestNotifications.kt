package com.olarte.ironprogress.ui.rest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Solo recibe cambios de fin; nunca se programa una notificación por cuadro. */
fun interface RestNotifications {
    fun replace(deadlineMs: Long?)
}

expect fun platformRestNotifications(): RestNotifications

internal interface RestNotificationBackend {
    suspend fun authorize(): Boolean
    suspend fun replace(deadlineMs: Long?)
    fun reportFailure(error: Exception)
}

/** Serializa las respuestas del sistema y descarta cambios viejos durante el permiso. */
internal class QueuedRestNotifications(scope: CoroutineScope, private val clock: Clock, private val backend: RestNotificationBackend) :
    RestNotifications {
    private data class Request(val deadlineMs: Long?)
    private val requests = Channel<Request>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                try {
                    if (request.deadlineMs == null) {
                        backend.replace(null)
                        continue
                    }
                    var latest = request
                    val authorized = backend.authorize()
                    while (true) {
                        latest = requests.tryReceive().getOrNull() ?: break
                    }
                    val deadline = latest.deadlineMs?.takeIf { authorized && it > clock.now().toEpochMilliseconds() }
                    backend.replace(deadline)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    backend.reportFailure(error)
                }
            }
        }
    }

    override fun replace(deadlineMs: Long?) {
        requests.trySend(Request(deadlineMs))
    }
}
