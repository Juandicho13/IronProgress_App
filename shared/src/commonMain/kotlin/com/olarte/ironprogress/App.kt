package com.olarte.ironprogress

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.ui.IronProgressApp

/**
 * Punto de entrada compartido de la interfaz. Koin ya arrancó antes (IronProgressKoin.start) y
 * Compose lo usa sin envolverlo en KoinApplication ni KoinContext: en Koin 4.1 eso sobra.
 */
@Composable
fun App() {
    IronProgressApp()
}
