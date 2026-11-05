package com.olarte.ironprogress.di

import android.content.Context
import com.olarte.ironprogress.data.local.createIronProgressDatabase
import com.olarte.ironprogress.ui.rest.RestBackgroundActions
import com.olarte.ironprogress.ui.rest.RestController
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/** Módulo de Koin de Android: la base con el contexto de la aplicación, sin koin-android. */
fun androidPlatformModule(context: Context): Module {
    val appContext = context.applicationContext
    return module {
        single { createIronProgressDatabase(appContext) }
    }
}

/**
 * Lo llama Application.onCreate, antes de cualquier Activity o servicio. No devuelve Koin para
 * que androidApp no necesite koin-core en su classpath.
 */
fun startIronProgress(context: Context) {
    IronProgressKoin.start(androidPlatformModule(context))
}

/** Devuelve el coordinador del descanso desde Koin sin exponer koin-core a androidApp. */
fun resolveRestController(): RestController = KoinPlatformTools.defaultContext().get().get()

/** Devuelve las acciones en segundo plano del descanso desde Koin sin exponer koin-core a androidApp. */
fun resolveRestBackgroundActions(): RestBackgroundActions = KoinPlatformTools.defaultContext().get().get()
