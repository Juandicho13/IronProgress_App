package com.olarte.ironprogress.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.olarte.ironprogress.data.local.createIronProgressDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo de Koin de Android: abre la base con el contexto de la aplicación,
 * que se toma de Compose para no depender de koin-android.
 */
@Composable
actual fun rememberPlatformModule(): Module {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        module {
            single { createIronProgressDatabase(appContext) }
        }
    }
}