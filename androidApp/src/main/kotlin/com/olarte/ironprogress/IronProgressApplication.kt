package com.olarte.ironprogress

import android.app.Application
import com.olarte.ironprogress.di.resolveRestController
import com.olarte.ironprogress.di.startIronProgress
import com.olarte.ironprogress.session.RestForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Arranca Koin, la base y la supervisión del descanso en segundo plano (F1-13 y F1-14). */
class IronProgressApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        startIronProgress(this)
        val controller = resolveRestController()
        appScope.launch {
            controller.state
                .map { it.shouldShowBackgroundNotification }
                .distinctUntilChanged()
                .collect { shouldShow ->
                    RestForegroundService.sync(this@IronProgressApplication, shouldShow)
                }
        }
    }
}
