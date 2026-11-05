package com.olarte.ironprogress

import android.app.Application
import com.olarte.ironprogress.di.startIronProgress

/** Arranca Koin y la base antes de cualquier pantalla o servicio (F1-13). */
class IronProgressApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startIronProgress(this)
    }
}
