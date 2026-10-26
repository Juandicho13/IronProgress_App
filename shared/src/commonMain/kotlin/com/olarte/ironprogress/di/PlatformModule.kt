package com.olarte.ironprogress.di

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * Provee el modulo Koin especifico de cada plataforma (Android o iOS)
 * con la instancia configurada de [com.olarte.ironprogress.data.local.IronProgressDatabase].
 */
@Composable
expect fun rememberPlatformModule(): Module