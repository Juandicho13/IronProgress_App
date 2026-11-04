package com.olarte.ironprogress.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

object ForjaTheme {
    val colors: ForjaColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaColors.current

    val typography: ForjaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaTypography.current

    val shapes: ForjaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaShapes.current

    val motion: ForjaMotionScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaMotion.current

    val haptics: ForjaHaptics
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaHaptics.current
}

/**
 * Tema principal de IronProgress Forja.
 * "Entrenar calienta. Descansar templa." Sigue el tema claro u oscuro y el ajuste de
 * Reducir movimiento del sistema, salvo que quien la llama pase otro valor.
 */
@Composable
fun ForjaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = rememberReduceMotionEnabled(),
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) ForjaDarkColors else ForjaLightColors
    val typography = rememberForjaTypography()
    val shapes = remember { ForjaShapes() }
    val motion = remember(reduceMotion) { ForjaMotionScheme(reduceMotion = reduceMotion) }
    val haptics = rememberForjaHaptics(enabled = hapticsEnabled)

    CompositionLocalProvider(
        LocalForjaColors provides palette,
        LocalForjaTypography provides typography,
        LocalForjaShapes provides shapes,
        LocalForjaMotion provides motion,
        LocalForjaHaptics provides haptics,
    ) {
        MaterialTheme(
            colorScheme = palette.toMaterialColorScheme(),
            content = content,
        )
    }
}

private fun ForjaColorPalette.toMaterialColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = ember,
        onPrimary = onEmber,
        primaryContainer = emberVeil,
        onPrimaryContainer = emberInk,
        secondary = ice,
        onSecondary = onEmber,
        secondaryContainer = iceVeil,
        onSecondaryContainer = iceInk,
        tertiary = gold,
        onTertiary = onGold,
        background = background,
        onBackground = text1,
        surface = surface1,
        onSurface = text1,
        surfaceVariant = surface3,
        onSurfaceVariant = text2,
        error = danger,
        onError = onDanger,
        outline = controlLine,
        outlineVariant = line,
    )
} else {
    lightColorScheme(
        primary = ember,
        onPrimary = onEmber,
        primaryContainer = emberVeil,
        onPrimaryContainer = emberInk,
        secondary = ice,
        onSecondary = surface1,
        secondaryContainer = iceVeil,
        onSecondaryContainer = iceInk,
        tertiary = gold,
        onTertiary = onGold,
        background = background,
        onBackground = text1,
        surface = surface1,
        onSurface = text1,
        surfaceVariant = surface3,
        onSurfaceVariant = text2,
        error = danger,
        onError = onDanger,
        outline = controlLine,
        outlineVariant = line,
    )
}
