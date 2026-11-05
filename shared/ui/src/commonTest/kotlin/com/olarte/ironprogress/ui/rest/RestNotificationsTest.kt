package com.olarte.ironprogress.ui.rest

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RestNotificationsTest {
    private class Backend : RestNotificationBackend {
        var permission = CompletableDeferred(true)
        var addition: CompletableDeferred<Unit>? = null
        val deadlines = mutableListOf<Long?>()
        var authorizations = 0
        var failures = 0
        var fail = false
        override suspend fun authorize(): Boolean {
            authorizations++
            return permission.await()
        }
        override suspend fun replace(deadlineMs: Long?) {
            if (fail) error("Fallo del sistema")
            addition?.await()
            deadlines += deadlineMs
        }
        override fun reportFailure(error: Exception) {
            failures++
        }
    }

    private class FakeClock(var millis: Long = 1000) : Clock {
        override fun now() = Instant.fromEpochMilliseconds(millis)
    }

    @Test
    fun permissionDelayKeepsOnlyLatestAbsoluteDeadline() = runTest {
        val backend = Backend().apply { permission = CompletableDeferred() }
        val clock = FakeClock()
        val notifications = QueuedRestNotifications(backgroundScope, clock, backend)
        notifications.replace(121000)
        runCurrent()
        notifications.replace(136000)
        notifications.replace(151000)
        clock.millis = 31000
        backend.permission.complete(true)
        runCurrent()
        assertEquals(listOf<Long?>(151000L), backend.deadlines)
    }

    @Test
    fun skipWhilePermissionIsOpenCannotResurrectTheRest() = runTest {
        val backend = Backend().apply { permission = CompletableDeferred() }
        val notifications = QueuedRestNotifications(backgroundScope, FakeClock(), backend)
        notifications.replace(121000)
        runCurrent()
        notifications.replace(null)
        backend.permission.complete(true)
        runCurrent()
        assertEquals(listOf<Long?>(null), backend.deadlines)
    }

    @Test
    fun deniedAndExpiredRequestsDoNotScheduleAnAlert() = runTest {
        val backend = Backend().apply { permission = CompletableDeferred(false) }
        val clock = FakeClock()
        val notifications = QueuedRestNotifications(backgroundScope, clock, backend)
        notifications.replace(121000)
        runCurrent()
        backend.permission = CompletableDeferred(true)
        clock.millis = 121000
        notifications.replace(121000)
        runCurrent()
        assertEquals(listOf<Long?>(null, null), backend.deadlines)
    }

    @Test
    fun skipWaitsForAnInFlightAdditionAndThenRemovesIt() = runTest {
        val backend = Backend().apply { addition = CompletableDeferred() }
        val notifications = QueuedRestNotifications(backgroundScope, FakeClock(), backend)
        notifications.replace(121000)
        runCurrent()
        notifications.replace(null)
        runCurrent()
        assertTrue(backend.deadlines.isEmpty())
        backend.addition!!.complete(Unit)
        runCurrent()
        assertEquals(listOf(121000L, null), backend.deadlines)
        assertEquals(1, backend.authorizations)
    }

    @Test
    fun failureDoesNotStopSubsequentSchedulingAndStartupDoesNotCancelAnything() = runTest {
        val backend = Backend()
        val notifications = QueuedRestNotifications(backgroundScope, FakeClock(), backend)
        runCurrent()
        assertTrue(backend.deadlines.isEmpty())
        backend.fail = true
        notifications.replace(121000)
        runCurrent()
        assertEquals(1, backend.failures)
        backend.fail = false
        notifications.replace(136000)
        runCurrent()
        assertEquals(listOf<Long?>(136000L), backend.deadlines)
    }
}
