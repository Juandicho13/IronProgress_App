package com.olarte.ironprogress

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.di.rememberPlatformModule
import com.olarte.ironprogress.di.sharedAppModule
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.ui.IronProgressApp
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

/**
 * Punto de entrada compartido de la interfaz.
 * Inicializa Koin con Room 3 e inyecta los repositorios en [IronProgressApp].
 */
@Composable
fun App() {
    val platformModule = rememberPlatformModule()
    KoinApplication(
        application = {
            modules(platformModule, sharedAppModule)
        },
    ) {
        val exerciseRepository: ExerciseRepository = koinInject()
        IronProgressApp(exerciseRepository = exerciseRepository)
    }
}