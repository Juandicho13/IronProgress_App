package com.olarte.ironprogress.ui.discipline

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Figuras propias de Forja para las posturas (PANTALLAS 7.2): un solo grosor de trazo, perfil
 * mirando a la izquierda y el suelo siempre a la misma altura. Coordenadas en un lienzo de
 * 372 × 240 (la tarjeta) que se escala al tamaño real. Dibujo propio, sin licencias ajenas.
 */
internal class PoseFigure(val head: Offset, val lines: List<List<Offset>>)

private fun p(x: Int, y: Int) = Offset(x.toFloat(), y.toFloat())

internal object PoseFigures {
    const val WIDTH = 372f
    const val GROUND_Y = 222f
    const val STROKE = 16f
    const val HEAD_RADIUS = 14f

    private val figures: Map<String, PoseFigure> = mapOf(
        "mountain" to PoseFigure(
            p(186, 44),
            listOf(listOf(p(186, 64), p(186, 140)), listOf(p(180, 212), p(186, 140), p(192, 212)), listOf(p(186, 82), p(182, 140))),
        ),
        "arms-up" to PoseFigure(
            p(186, 64),
            listOf(listOf(p(186, 84), p(186, 146)), listOf(p(180, 212), p(186, 146), p(192, 212)), listOf(p(186, 92), p(180, 22))),
        ),
        "forward-fold" to PoseFigure(
            p(170, 196),
            listOf(listOf(p(204, 212), p(206, 120)), listOf(p(206, 120), p(180, 176)), listOf(p(184, 160), p(190, 212))),
        ),
        "half-fold" to PoseFigure(
            p(128, 118),
            listOf(listOf(p(212, 212), p(214, 120)), listOf(p(214, 120), p(146, 124)), listOf(p(160, 126), p(166, 212))),
        ),
        "plank" to PoseFigure(
            p(84, 138),
            listOf(listOf(p(102, 150), p(300, 200)), listOf(p(116, 154), p(116, 212)), listOf(p(300, 200), p(312, 212))),
        ),
        "up-dog" to PoseFigure(
            p(112, 92),
            listOf(listOf(p(312, 210), p(190, 204)), listOf(p(190, 204), p(124, 112)), listOf(p(130, 128), p(136, 212))),
        ),
        "down-dog" to PoseFigure(
            p(132, 176),
            listOf(listOf(p(92, 212), p(190, 92)), listOf(p(190, 92), p(286, 212))),
        ),
        "cat-cow" to PoseFigure(
            p(98, 124),
            listOf(listOf(p(120, 212), p(120, 142)), listOf(p(116, 140), p(240, 136)), listOf(p(240, 136), p(244, 212), p(300, 212))),
        ),
        "low-lunge" to PoseFigure(
            p(186, 62),
            listOf(
                listOf(p(108, 212), p(126, 158), p(204, 162)),
                listOf(p(204, 162), p(270, 212), p(336, 212)),
                listOf(p(204, 162), p(190, 82)),
                listOf(p(192, 90), p(176, 22)),
            ),
        ),
        "hundred" to PoseFigure(
            p(152, 168),
            listOf(listOf(p(250, 204), p(170, 182)), listOf(p(250, 204), p(320, 136)), listOf(p(184, 188), p(258, 186))),
        ),
        "bridge" to PoseFigure(
            p(82, 204),
            listOf(listOf(p(100, 210), p(210, 144)), listOf(p(210, 144), p(272, 150), p(290, 212)), listOf(p(112, 212), p(196, 212))),
        ),
        "child" to PoseFigure(
            p(134, 202),
            listOf(listOf(p(206, 212), p(272, 212), p(262, 172)), listOf(p(262, 172), p(152, 200)), listOf(p(150, 208), p(64, 210))),
        ),
    )

    fun find(id: String?): PoseFigure? = figures[id]
}

/** Dibuja [figure] en hielo sobre una línea de suelo en [ground]. */
@Composable
internal fun PoseFigureCanvas(figure: PoseFigure?, color: Color, ground: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = size.width / PoseFigures.WIDTH
        val offsetY = (size.height - 240f * scale) / 2f
        fun Offset.scaled() = Offset(x * scale, y * scale + offsetY)
        val stroke = Stroke(width = PoseFigures.STROKE * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(
            ground,
            Offset(24f * scale, PoseFigures.GROUND_Y * scale + offsetY),
            Offset((PoseFigures.WIDTH - 24f) * scale, PoseFigures.GROUND_Y * scale + offsetY),
            strokeWidth = 4f * scale,
            cap = StrokeCap.Round,
        )
        figure ?: return@Canvas
        figure.lines.forEach { points ->
            val path = Path().apply {
                val first = points.first().scaled()
                moveTo(first.x, first.y)
                points.drop(1).forEach {
                    val q = it.scaled()
                    lineTo(q.x, q.y)
                }
            }
            drawPath(path, color, style = stroke)
        }
        drawCircle(color, radius = PoseFigures.HEAD_RADIUS * scale, center = figure.head.scaled())
    }
}
