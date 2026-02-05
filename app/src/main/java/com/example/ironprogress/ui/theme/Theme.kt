package com.example.ironprogress.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definimos SOLAMENTE el esquema oscuro (Dark Gym Style)
private val DarkColorScheme = darkColorScheme(
    primary = IronRed,
    onPrimary = Color.White,
    primaryContainer = IronRedContainer,
    onPrimaryContainer = IronRed,

    secondary = IronRed, // Usamos rojo también como secundario para consistencia
    onSecondary = Color.White,

    tertiary = IronGold, // Dorado para detalles especiales

    background = IronBlack,
    onBackground = IronWhite,

    surface = IronSurface,
    onSurface = IronWhite,

    surfaceVariant = IronSurfaceVariant,
    onSurfaceVariant = IronGreyText,

    error = Color(0xFFCF6679)
)

@Composable
fun IronProgressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color está disponible en Android 12+
    // Lo pondremos en FALSE para forzar NUESTROS colores rojo/negro y no los del sistema del usuario
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Forzamos siempre el tema oscuro por diseño "Dark Gym"
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barra de estado negra para que se funda con la app
            window.statusBarColor = IronBlack.toArgb()
            window.navigationBarColor = IronBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}