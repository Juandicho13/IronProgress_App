package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

/**
 * Formas de Forja que el tema entrega a toda la app.
 * Usan los radios de [ForjaRadius]: 14, 20, 28, 36 y completo.
 */
@Immutable
data class ForjaShapes(
    // Campos, teclas y chips pequenos (radio 14).
    val small: Shape = ForjaRadius.shapeS,
    // Tarjetas compactas, filas de serie y baldosas (radio 20).
    val medium: Shape = ForjaRadius.shapeM,
    // Tarjetas grandes (radio 28).
    val large: Shape = ForjaRadius.shapeL,
    // Hojas inferiores: solo las esquinas de arriba (radio 36).
    val sheet: Shape = ForjaRadius.shapeXlTop,
    // Botones, pildoras, la isla y los circulos.
    val full: Shape = ForjaRadius.shapeFull,
)

// Se lee con ForjaTheme.shapes; ForjaTheme la provee con LocalForjaShapes.
val LocalForjaShapes = staticCompositionLocalOf { ForjaShapes() }