package com.olarte.ironprogress

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.ui.IronProgressApp
import com.olarte.ironprogress.ui.settings.SettingsGate

/**
 * Punto de entrada compartido de la interfaz. Koin ya arrancó antes (IronProgressKoin.start) y
 * Compose lo usa sin envolverlo en KoinApplication ni KoinContext: en Koin 4.1 eso sobra.
 */
@Composable
fun App() {
    // Idioma, tema y bienvenida (F1-25) envuelven toda la app.
    SettingsGate { IronProgressApp() }
}
