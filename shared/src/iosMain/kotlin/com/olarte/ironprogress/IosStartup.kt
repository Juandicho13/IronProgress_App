package com.olarte.ironprogress

import com.olarte.ironprogress.data.local.createIronProgressDatabase
import com.olarte.ironprogress.di.IronProgressKoin
import org.koin.dsl.module

/**
 * Arranque de iOS. Swift lo llama en el init de la App como IosStartupKt.startIronProgress(),
 * antes de crear la ventana; también corre cuando un App Intent abre la app sin pantalla.
 */
fun startIronProgress() {
    IronProgressKoin.start(
        module {
            single { createIronProgressDatabase() }
        },
    )
}
