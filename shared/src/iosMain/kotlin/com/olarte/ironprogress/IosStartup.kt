package com.olarte.ironprogress

import androidx.compose.ui.text.intl.Locale
import com.olarte.ironprogress.data.local.createIronProgressDatabase
import com.olarte.ironprogress.di.IronProgressKoin
import com.olarte.ironprogress.live.NoWorkoutActivity
import com.olarte.ironprogress.live.WorkoutActivityBridge
import com.olarte.ironprogress.live.WorkoutActivitySync
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.platformFormatter
import com.olarte.ironprogress.ui.rest.RestBackgroundActions
import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.rest.loadRestNotificationStrings
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.dsl.module
import kotlin.time.Clock

private val scope = MainScope()
private var activitySync: WorkoutActivitySync? = null

/**
 * Arranque de iOS. Swift lo llama en el init de la App como IosStartupKt.startIronProgress(),
 * antes de crear la ventana; también corre cuando un App Intent abre la app sin pantalla.
 */
fun startIronProgress() {
    startIronProgress(NoWorkoutActivity)
}

/** Igual, con la Live Activity que Swift implementa con ActivityKit (F1-16). */
fun startIronProgress(bridge: WorkoutActivityBridge) {
    val koin = IronProgressKoin.start(
        module {
            single { createIronProgressDatabase() }
        },
    )
    if (activitySync != null) return
    val actions = koin.get<RestBackgroundActions>()
    val language = Locale.current.toLanguageTag()
    val sync = WorkoutActivitySync(koin.get(), bridge) {
        actions.buildCurrentNotification(Locale.current.language, Formats(platformFormatter(language)), loadRestNotificationStrings())
    }
    activitySync = sync
    sync.start(scope)
}

/** SwiftUI avisa con scenePhase si la app está a la vista: la Live Activity solo sale fuera de ella. */
fun setAppInForeground(active: Boolean) {
    val koin = IronProgressKoin.start(module { single { createIronProgressDatabase() } })
    koin.get<RestController>().setAppInForeground(active, Clock.System.now().toEpochMilliseconds())
}

/**
 * Los botones de la Live Activity (App Intents). [action] es "plus", "minus", "skip" o "complete".
 * Corre en el proceso de la app, aunque esté cerrada: por eso arranca Koin si hace falta.
 */
fun runWorkoutAction(action: String, completion: () -> Unit) {
    val koin = IronProgressKoin.start(module { single { createIronProgressDatabase() } })
    val actions = koin.get<RestBackgroundActions>()
    scope.launch {
        try {
            when (action) {
                "plus" -> actions.adjustRest(15)
                "minus" -> actions.adjustRest(-15)
                "skip" -> actions.skipRest()
                "complete" -> actions.completeNextSet()
            }
            activitySync?.refresh()
        } finally {
            completion()
        }
    }
}
