package com.olarte.ironprogress.ui.rest

import app.cash.turbine.test
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Las fases del descanso salen en orden y una sola vez por cambio (F1-27, con Turbine). */
class RestControllerFlowTest {
    @Test
    fun phasesArriveInOrderWithoutRepeats() = runTest {
        val controller = RestController()
        controller.state.test {
            assertEquals(RestPhase.IDLE, awaitItem().phase)

            controller.startRest(SessionId("s"), RestTimer(WorkoutSetId("w"), 1_000_000, 90))
            val resting = awaitItem()
            assertEquals(RestPhase.RESTING, resting.phase)
            assertEquals(1_090_000, resting.timer?.endsAtMs(1_000_000))

            // Con la app abierta, al llegar a 0 vuelve a reposo sin pasar por Listo.
            controller.onRestCountdownFinished(1_090_000)
            assertEquals(RestPhase.IDLE, awaitItem().phase)

            // Con la app en segundo plano, queda en Listo hasta que se limpia.
            controller.startRest(SessionId("s"), RestTimer(WorkoutSetId("w2"), 2_000_000, 60))
            assertEquals(RestPhase.RESTING, awaitItem().phase)
            controller.setAppInForeground(false, 2_000_100)
            assertFalse(awaitItem().appInForeground)
            controller.onRestCountdownFinished(2_060_000)
            val ready = awaitItem()
            assertEquals(RestPhase.READY, ready.phase)
            assertEquals(2_060_000, ready.readyAtMs)

            controller.clearRest()
            val idle = awaitItem()
            assertEquals(RestPhase.IDLE, idle.phase)
            assertNull(idle.timer)
            expectNoEvents()
        }
    }
}
