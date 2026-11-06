package com.olarte.ironprogress.live

import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.rest.RestNotificationContent
import com.olarte.ironprogress.ui.rest.RestPhase
import com.olarte.ironprogress.ui.rest.RestTimer
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutActivitySyncTest {

    private class Recorder : WorkoutActivityBridge {
        val shown = mutableListOf<WorkoutActivityState>()
        var ended = 0

        override fun show(state: WorkoutActivityState) {
            shown += state
        }

        override fun end() {
            ended++
        }
    }

    private fun content(phase: RestPhase, endsAt: Long?) = RestNotificationContent(
        sessionId = SessionId("s"),
        phase = phase,
        ongoing = true,
        endsAtMs = endsAt,
        showChronometer = phase == RestPhase.RESTING,
        shortCriticalText = if (phase == RestPhase.READY) "Listo" else null,
        title = "Descanso · Press banca",
        collapsedText = "Sigue: serie 3 de 4 · 62,5 kg × 10",
        expandedText = "",
        showAdjustAndSkipActions = phase == RestPhase.RESTING,
        showCompleteSetAction = phase == RestPhase.READY,
        nextSetId = null,
        alertNow = false,
    )

    @Test
    fun updatesOnlyOnPhaseChangesNotEverySecond() = runTest {
        val controller = RestController()
        val bridge = Recorder()
        var current: RestNotificationContent? = null
        val sync = WorkoutActivitySync(controller, bridge) { current }
        sync.start(backgroundScope)
        runCurrent()

        // Descanso de 90 s con la app en segundo plano.
        current = content(RestPhase.RESTING, 1_000_090_000)
        controller.startRest(SessionId("s"), RestTimer(WorkoutSetId("w"), 1_000_000_000, 90))
        controller.setAppInForeground(false, 1_000_000_100)
        runCurrent()
        val afterStart = bridge.shown.size
        assertTrue(afterStart >= 1)
        assertEquals(1_000_090_000, bridge.shown.last().endsAtEpochMs)
        assertEquals("Descanso · Press banca", bridge.shown.last().title)

        // Pasa el tiempo sin cambios de fase: nada nuevo.
        runCurrent()
        assertEquals(afterStart, bridge.shown.size)

        // Llega a 0: Listo, sin cuenta atrás.
        current = content(RestPhase.READY, null)
        controller.onRestCountdownFinished(1_000_090_000)
        runCurrent()
        assertTrue(bridge.shown.last().ready)
        assertEquals(0, bridge.shown.last().endsAtEpochMs)

        // Se termina la sesión: la Live Activity se va.
        current = null
        controller.clearRest()
        runCurrent()
        assertEquals(1, bridge.ended)
    }
}
