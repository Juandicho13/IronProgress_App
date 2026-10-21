package com.olarte.ironprogress.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object ForjaTheme {
    val colors: ForjaColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaColors.current

    val typography: ForjaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalForjaTypography.current
}

/**
 * Tema principal de IronProgress Forja.
 * Usa el tema oscuro por defecto ("Entrenar calienta. Descansar templa.").
 */
@Composable
fun ForjaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) ForjaDarkColors else ForjaLightColors
    val typography = createForjaTypography()

    CompositionLocalProvider(
        LocalForjaColors provides palette,
        LocalForjaTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = palette.toMaterialColorScheme(),
            content = content,
        )
    }
}

private fun ForjaColorPalette.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
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
}
