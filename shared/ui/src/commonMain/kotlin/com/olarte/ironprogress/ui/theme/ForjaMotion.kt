package com.olarte.ironprogress.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Esquema de resortes del sistema de diseno Forja con soporte para Reducir movimiento.
 * Convive con el objeto [ForjaMotion] definido en ForjaMetrics.kt.
 */
@Immutable
data class ForjaMotionScheme(
    val reduceMotion: Boolean = false,
) {
    /** Resorte espacial rapido (0.6 / 800): presionar botones, chips e indicador de pestana. */
    fun <T> spatialFast(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            reducedMotionTween()
        } else {
            spring(
                dampingRatio = ForjaMotionTokens.SPATIAL_FAST_DAMPING,
                stiffness = ForjaMotionTokens.SPATIAL_FAST_STIFFNESS,
                visibilityThreshold = visibilityThreshold,
            )
        }

    /** Resorte espacial estandar (0.8 / 380): transformaciones de la isla, hojas y tarjetas. */
    fun <T> spatial(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            reducedMotionTween()
        } else {
            spring(
                dampingRatio = ForjaMotionTokens.SPATIAL_DAMPING,
                stiffness = ForjaMotionTokens.SPATIAL_STIFFNESS,
                visibilityThreshold = visibilityThreshold,
            )
        }

    /** Resorte espacial lento (0.8 / 200): transiciones de pantalla completa y resumen. */
    fun <T> spatialSlow(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            reducedMotionTween()
        } else {
            spring(
                dampingRatio = ForjaMotionTokens.SPATIAL_SLOW_DAMPING,
                stiffness = ForjaMotionTokens.SPATIAL_SLOW_STIFFNESS,
                visibilityThreshold = visibilityThreshold,
            )
        }

    /** Resorte de efecto rapido (1.0 / 3800): fundidos de contenido dentro de la isla. */
    fun <T> effectFast(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = ForjaMotionTokens.EFFECT_DAMPING,
            stiffness = ForjaMotionTokens.EFFECT_FAST_STIFFNESS,
            visibilityThreshold = visibilityThreshold,
        )

    /** Resorte de efecto estandar (1.0 / 1600): cambios de color, relleno de iconos y opacidad. */
    fun <T> effect(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = ForjaMotionTokens.EFFECT_DAMPING,
            stiffness = ForjaMotionTokens.EFFECT_STIFFNESS,
            visibilityThreshold = visibilityThreshold,
        )

    /** Resorte de efecto lento (1.0 / 800): fundidos grandes como el velo de una hoja. */
    fun <T> effectSlow(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = ForjaMotionTokens.EFFECT_DAMPING,
            stiffness = ForjaMotionTokens.EFFECT_SLOW_STIFFNESS,
            visibilityThreshold = visibilityThreshold,
        )

    private fun <T> reducedMotionTween(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = ForjaMotionTokens.REDUCED_MOTION_DURATION_MS,
            easing = FastOutSlowInEasing,
        )
}

object ForjaMotionTokens {
    const val SPATIAL_FAST_DAMPING = 0.6f
    const val SPATIAL_FAST_STIFFNESS = 800f

    const val SPATIAL_DAMPING = 0.8f
    const val SPATIAL_STIFFNESS = 380f

    const val SPATIAL_SLOW_DAMPING = 0.8f
    const val SPATIAL_SLOW_STIFFNESS = 200f

    const val EFFECT_DAMPING = 1.0f
    const val EFFECT_FAST_STIFFNESS = 3800f
    const val EFFECT_STIFFNESS = 1600f
    const val EFFECT_SLOW_STIFFNESS = 800f

    const val REDUCED_MOTION_DURATION_MS = 150

    const val BUTTON_PRESSED_SCALE = 0.97f
    const val CHIP_PRESSED_SCALE = 0.95f
}

val LocalForjaMotion = staticCompositionLocalOf { ForjaMotionScheme() }

/**
 * Aplica la compresion tactil de Forja al presionar un control (0.97f en botones, 0.95f en chips).
 */
fun Modifier.forjaPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = ForjaMotionTokens.BUTTON_PRESSED_SCALE,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val motion = LocalForjaMotion.current
    val targetScale = if (isPressed && !motion.reduceMotion) pressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = motion.spatialFast(),
        label = "ForjaPressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}