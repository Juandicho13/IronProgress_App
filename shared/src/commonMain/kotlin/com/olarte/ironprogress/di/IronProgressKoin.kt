package com.olarte.ironprogress.di

import com.olarte.ironprogress.ui.di.uiModule
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

/**
 * Arranque de Koin antes de cualquier pantalla (F1-13). Android lo llama en
 * Application.onCreate y iOS al iniciar la app, así el servicio del descanso y los botones de
 * la Live Activity, que corren sin pantalla, ya encuentran la base y los repositorios.
 * Llamarlo dos veces devuelve el mismo Koin: no arranca otro.
 */
object IronProgressKoin {

    fun start(platformModule: Module): Koin {
        KoinPlatformTools.defaultContext().getOrNull()?.let { return it }
        return startKoin {
            modules(platformModule, sharedAppModule, uiModule)
        }.koin
    }
}
