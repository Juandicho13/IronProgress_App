package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Contrato de retroalimentacion haptica del sistema Forja.
 * Solo vibra cuando confirma una accion o avisa de algo que el usuario no esta mirando.
 * Cambiar de pestana en la isla o hacer scroll nunca vibra.
 */
@Stable
interface ForjaHaptics {
    /** Completar una serie: CONFIRM en Android, impacto medio en iOS. */
    fun setCompleted()

    /** Ajustar un valor arrastrando o cambiar un selector segmentado: SEGMENT_TICK / seleccion. */
    fun segmentTick()

    /** Tecla del teclado numerico propio: KEYBOARD_TAP en Android, impacto suave en iOS. */
    fun keyboardTap()

    /** Ultimos 3 segundos del descanso (un tic por segundo): CLOCK_TICK / seleccion. */
    fun restCountdownTick()

    /** Fin del descanso: patron 0, 180, 90, 180 ms en Android, notificacion de exito en iOS. */
    fun restFinished()

    /** Record personal: CONFIRM dos veces con 120 ms de pausa en Android, exito en iOS. */
    fun personalRecord()

    /** Pasar el umbral de Deslizar para terminar: GESTURE_THRESHOLD_ACTIVATE / impacto rigido. */
    fun slideThresholdReached()

    /** Accion rechazada o error: REJECT en Android, notificacion de error en iOS. */
    fun actionRejected()
}

/**
 * Implementacion silenciosa para pruebas o cuando el usuario desactiva la vibracion en ajustes.
 */
object NoOpForjaHaptics : ForjaHaptics {
    override fun setCompleted() = Unit
    override fun segmentTick() = Unit
    override fun keyboardTap() = Unit
    override fun restCountdownTick() = Unit
    override fun restFinished() = Unit
    override fun personalRecord() = Unit
    override fun slideThresholdReached() = Unit
    override fun actionRejected() = Unit
}

val LocalForjaHaptics = staticCompositionLocalOf<ForjaHaptics> { NoOpForjaHaptics }

/**
 * Proveedor multiplataforma de [ForjaHaptics] conectado a los motores nativos de Android e iOS.
 */
@Composable
expect fun rememberForjaHaptics(enabled: Boolean = true): ForjaHaptics