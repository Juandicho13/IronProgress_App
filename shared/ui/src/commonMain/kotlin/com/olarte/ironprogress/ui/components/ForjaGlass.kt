package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** Estado de Haze del contenido que pasa por detrás de la navegación. null: sin desenfoque. */
val LocalForjaHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Vidrio de Forja: desenfoque de 24 dp con el tinte `glass` (86 % en oscuro, 84 % en claro).
 * Va solo en la capa de navegación (isla, Buscar y accesorio). Sin HazeState queda el tinte
 * sólido, que ya cumple contraste AA. Se aplica después del clip de la forma.
 */
@Composable
fun Modifier.forjaGlass(tint: Color = ForjaTheme.colors.glass): Modifier {
    val hazeState = LocalForjaHazeState.current ?: return background(tint)
    val background = ForjaTheme.colors.background
    return hazeBlur(
        input = HazeInput.Sources(hazeState),
        style = HazeBlurStyle {
            backgroundColor(background)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            blurRadius(ForjaMeasures.glassBlur)
            noiseFactor(0.03f)
        },
    )
}
