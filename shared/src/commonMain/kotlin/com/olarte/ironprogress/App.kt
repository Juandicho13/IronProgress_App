package com.olarte.ironprogress

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.di.rememberPlatformModule
import com.olarte.ironprogress.di.sharedAppModule
import com.olarte.ironprogress.ui.IronProgressApp
import com.olarte.ironprogress.ui.di.uiModule
import org.koin.compose.KoinApplication

/**
 * Punto de entrada compartido de la interfaz.
 * Inicializa Koin con Room 3, los casos de uso y los ViewModels (uiModule).
 */
@Composable
fun App() {
    val platformModule = rememberPlatformModule()
    KoinApplication(
        application = {
            modules(platformModule, sharedAppModule, uiModule)
        },
    ) {
        IronProgressApp()
    }
}