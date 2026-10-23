package com.olarte.ironprogress.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.olarte.ironprogress.data.local.createIronProgressDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo de Koin de iOS: abre la base en la carpeta Documents de la app.
 */
@Composable
actual fun rememberPlatformModule(): Module =
    remember {
        module {
            single { createIronProgressDatabase() }
        }
    }