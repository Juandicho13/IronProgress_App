package com.olarte.ironprogress.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.time.TimeSource

/**
 * Lo que queda de un tramo, recalculado en cada cuadro. El motor de tiempo da [remainingMs] cada
 * cierto tiempo; entre una lectura y la siguiente se descuenta lo que pasó, así los anillos bajan
 * fluidos y no a saltos. Pausado, se queda quieto. Nunca cuenta solo: cada lectura nueva manda.
 */
@Composable
fun rememberSmoothRemainingMs(remainingMs: Long, running: Boolean): Long {
    val mark = remember(remainingMs) { TimeSource.Monotonic.markNow() }
    var value by remember { mutableLongStateOf(remainingMs) }
    LaunchedEffect(remainingMs, running) {
        value = remainingMs
        if (running) {
            while (true) {
                withFrameNanos { value = (remainingMs - mark.elapsedNow().inWholeMilliseconds).coerceAtLeast(0L) }
            }
        }
    }
    return value
}

/** Fracción que queda del tramo (1 lleno, 0 vacío), fluida en cada cuadro. */
@Composable
fun rememberSmoothFraction(remainingMs: Long, durationMs: Long, running: Boolean): Float {
    val left = rememberSmoothRemainingMs(remainingMs, running)
    return (left.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
}

/** Lo que va de un tramo, fluido en cada cuadro; nunca pasa de [limitMs]. */
@Composable
fun rememberSmoothElapsedMs(elapsedMs: Long, running: Boolean, limitMs: Long = Long.MAX_VALUE): Long {
    val mark = remember(elapsedMs) { TimeSource.Monotonic.markNow() }
    var value by remember { mutableLongStateOf(elapsedMs) }
    LaunchedEffect(elapsedMs, running, limitMs) {
        value = elapsedMs.coerceAtMost(limitMs)
        if (running) {
            while (true) {
                withFrameNanos { value = (elapsedMs + mark.elapsedNow().inWholeMilliseconds).coerceAtMost(limitMs) }
            }
        }
    }
    return value
}
