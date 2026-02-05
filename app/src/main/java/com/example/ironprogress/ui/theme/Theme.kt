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

/**
 * Definición del Esquema de Color Oscuro ("Dark Gym").
 *
 * Mapea los colores de marca definidos en [Color.kt] a los roles semánticos
 * de Material Design 3. Se prioriza el contraste alto (Rojo/Negro)
 * para entornos de gimnasio con poca luz.
 */
private val DarkColorScheme = darkColorScheme(
    primary = IronRed,
    onPrimary = Color.White,
    primaryContainer = IronRedContainer,
    onPrimaryContainer = IronRed,

    // Se reutiliza el color primario como secundario para mantener
    // una identidad visual monocromática y agresiva.
    secondary = IronRed,
    onSecondary = Color.White,

    // Usado para elementos destacados especiales o funciones Premium.
    tertiary = IronGold,

    background = IronBlack,
    onBackground = IronWhite,

    surface = IronSurface,
    onSurface = IronWhite,

    surfaceVariant = IronSurfaceVariant,
    onSurfaceVariant = IronGreyText,

    error = Color(0xFFCF6679)
)

/**
 * Componente principal del Tema de IronProgress.
 *
 * Este Composable debe envolver la jerarquía de UI raíz de la aplicación.
 * Se encarga de aplicar los esquemas de color, tipografía y configuración
 * de las barras del sistema (Status/Navigation bars).
 *
 * @param darkTheme Indica si el sistema está en modo oscuro (por defecto true/system).
 * @param dynamicColor Si es true, usa colores dinámicos de Android 12+.
 * Se establece en `false` por defecto para forzar la identidad de marca.
 */
@Composable
fun IronProgressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado para mantener consistencia de marca
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Por decisión de diseño, la app siempre utiliza el esquema oscuro
        // independientemente de la configuración del sistema.
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Configuración de Barras del Sistema (Inmersión)
            // Se configuran en negro puro para fundirse con el fondo de la app.
            window.statusBarColor = IronBlack.toArgb()
            window.navigationBarColor = IronBlack.toArgb()

            // Asegura que los iconos de la barra de estado sean claros (para fondo oscuro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}