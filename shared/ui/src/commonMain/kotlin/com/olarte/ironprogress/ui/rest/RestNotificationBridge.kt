package com.olarte.ironprogress.ui.rest

import androidx.compose.runtime.Composable

/**
 * Sincroniza la visibilidad de la pantalla Entreno y el permiso POST_NOTIFICATIONS con el
 * controlador del descanso (F1-14).
 */
@Composable
expect fun RestNotificationBridge(restController: RestController, isWorkoutScreenOpen: Boolean)
