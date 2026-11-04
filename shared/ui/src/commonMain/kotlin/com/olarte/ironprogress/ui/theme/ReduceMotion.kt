package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Composable

/** true cuando la persona activó Reducir movimiento (iOS) o quitó las animaciones (Android). */
@Composable
expect fun rememberReduceMotionEnabled(): Boolean
