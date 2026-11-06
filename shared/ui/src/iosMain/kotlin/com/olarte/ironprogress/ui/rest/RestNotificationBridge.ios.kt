package com.olarte.ironprogress.ui.rest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun RestNotificationBridge(restController: RestController, isWorkoutScreenOpen: Boolean) {
    LaunchedEffect(isWorkoutScreenOpen) {
        restController.setWorkoutScreenOpen(isWorkoutScreenOpen)
    }
}
