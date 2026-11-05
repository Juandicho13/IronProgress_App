package com.olarte.ironprogress.di

import android.content.Context
import com.olarte.ironprogress.data.local.createIronProgressDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

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
