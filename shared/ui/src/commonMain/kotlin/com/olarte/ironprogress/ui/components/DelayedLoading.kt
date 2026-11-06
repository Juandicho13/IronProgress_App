package com.olarte.ironprogress.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Espera antes de mostrar "Cargando": una carga corta no hace parpadear la pantalla. */
const val LOADING_DELAY_MS = 300L

/** Muestra [content] solo si [loading] sigue activo después de [LOADING_DELAY_MS]. */
@Composable
fun DelayedLoading(loading: Boolean, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        show = false
        if (loading) {
            delay(LOADING_DELAY_MS)
            show = true
        }
    }
    if (loading && show) content()
}
