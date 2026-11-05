package com.olarte.ironprogress.live

import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.rest.RestNotificationContent
import com.olarte.ironprogress.ui.rest.RestPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Lo que muestra la Live Activity del entreno (F1-16, PANTALLAS 5.9). [endsAtEpochMs] es la hora
 * de fin del descanso: el sistema cuenta solo con Text(timerInterval:), así que la app no la
 * actualiza cada segundo. Es 0 cuando no hay cuenta atrás (estado Listo).
 */
data class WorkoutActivityState(
    val sessionId: String,
    val title: String,
    val detail: String,
    val endsAtEpochMs: Long,
    val ready: Boolean,
    val readyChip: String,
)

/**
 * La Live Activity vista desde Kotlin. Swift la implementa con ActivityKit y la pasa al arrancar
 * (IosStartupKt.startIronProgress(bridge:)). En Android no hay: la Live Update es de F1-14.
 */
interface WorkoutActivityBridge {
    fun show(state: WorkoutActivityState)

    fun end()
}

/** Sin Live Activity (Android y pruebas). */
object NoWorkoutActivity : WorkoutActivityBridge {
    override fun show(state: WorkoutActivityState) = Unit

    override fun end() = Unit
}

/**
 * Mantiene la Live Activity al día con el descanso. Solo reacciona a cambios de fase, de hora de
 * fin o de primer y segundo plano: en una sesión de 60 min son unas decenas de actualizaciones, no
 * una por segundo. [content] arma los textos (los mismos de la Live Update de Android).
 */
class WorkoutActivitySync(
    private val restController: RestController,
    private val bridge: WorkoutActivityBridge,
    private val content: suspend () -> RestNotificationContent?,
) {
    private var job: Job? = null
    private var showing = false

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            restController.state
                .map { Key(it.phase, it.sessionId?.value, it.timer?.startedAtMs, it.timer?.edits?.size, it.appInForeground) }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    /** Vuelve a pintar ahora; también lo llaman los botones después de su acción. */
    suspend fun refresh() {
        val current = content()
        if (current == null) {
            if (showing) bridge.end()
            showing = false
            return
        }
        bridge.show(current.toActivityState())
        showing = true
    }

    private data class Key(val phase: RestPhase, val session: String?, val started: Long?, val edits: Int?, val foreground: Boolean)
}

fun RestNotificationContent.toActivityState(): WorkoutActivityState = WorkoutActivityState(
    sessionId = sessionId.value,
    title = title,
    detail = collapsedText,
    endsAtEpochMs = if (phase == RestPhase.RESTING) endsAtMs ?: 0L else 0L,
    ready = phase == RestPhase.READY,
    readyChip = shortCriticalText.orEmpty(),
)
