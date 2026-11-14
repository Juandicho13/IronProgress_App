package com.olarte.ironprogress.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.theme.ForjaTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Un punto de la gráfica: cuándo (ms) y cuánto, ya en la unidad que se muestra. */
data class ChartPoint(val epochMs: Long, val value: Double)

/** Marcas del eje vertical con números redondos: 3 o 4 líneas entre el mínimo y el máximo. */
internal fun niceTicks(min: Double, max: Double): List<Double> {
    var low = min
    var high = max
    if (high - low < 1e-9) {
        val pad = maxOf(abs(high) * 0.05, 1.0)
        low -= pad
        high += pad
    }
    val raw = (high - low) / 3
    val magnitude = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= raw }
    val first = floor(low / step) * step
    val last = ceil(high / step) * step
    val ticks = mutableListOf<Double>()
    var value = first
    while (value <= last + step / 2) {
        ticks += value
        value += step
    }
    return ticks
}

/**
 * Gráfica de línea de un ejercicio (PANTALLAS 2.2, con las reglas de dataviz): línea de 2 dp en
 * ember con área al 10 %, rejilla de 1 dp en `line`, ejes en text3, el punto del récord en oro con
 * su etiqueta y el último punto marcado. Tocar o arrastrar muestra la línea guía y el dato.
 * [recordIndex] marca el récord de siempre si está en lo que se ve.
 */
@Composable
fun ProgressChart(
    points: List<ChartPoint>,
    recordIndex: Int?,
    formatValue: (Double) -> String,
    formatTick: (Double) -> String,
    formatDate: (Long) -> String,
    recordLabel: (String) -> String,
    tooltipDetail: (Int) -> String?,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val reduceMotion = ForjaTheme.motion.reduceMotion
    val measurer = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val reveal = remember(points) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(points, reduceMotion) {
        if (reduceMotion) reveal.snapTo(1f) else reveal.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val axisStyle = typography.caption.copy(color = colors.text3)
    val labelStyle = typography.caption.copy(color = colors.text1)
    val tooltipValueStyle = typography.label.copy(color = colors.text1)
    val tooltipDateStyle = typography.caption.copy(color = colors.text2)

    Canvas(
        modifier.fillMaxWidth().height(height)
            .semantics { contentDescription = description }
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val nearest =
                        nearestIndex(
                            points,
                            offset.x,
                            size.width - RIGHT_PAD.toPx(),
                            gutter(points, measurer, axisStyle, formatTick, 20.dp.toPx()),
                        )
                    selected = if (selected == nearest) null else nearest
                }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        selected =
                            nearestIndex(
                                points,
                                offset.x,
                                size.width - RIGHT_PAD.toPx(),
                                gutter(points, measurer, axisStyle, formatTick, 20.dp.toPx()),
                            )
                    },
                ) { change, _ ->
                    selected =
                        nearestIndex(
                            points,
                            change.position.x,
                            size.width - RIGHT_PAD.toPx(),
                            gutter(points, measurer, axisStyle, formatTick, 20.dp.toPx()),
                        )
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val ticks = niceTicks(points.minOf { it.value }, points.maxOf { it.value })
        val left = gutter(points, measurer, axisStyle, formatTick, 20.dp.toPx())
        val bottomGutter = 22.dp.toPx()
        val topPad = 26.dp.toPx()
        val plot = PlotArea(left, topPad, size.width - RIGHT_PAD.toPx(), size.height - bottomGutter, ticks.first(), ticks.last(), points)

        // Rejilla y marcas del eje vertical.
        ticks.forEach { tick ->
            val y = plot.y(tick)
            drawLine(colors.line, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1.dp.toPx())
            val text = measurer.measure(formatTick(tick), axisStyle)
            drawText(text, topLeft = Offset(left - text.size.width - 8.dp.toPx(), y - text.size.height / 2f))
        }
        // Fechas: la primera, la del medio y la última, sin chocar.
        val dateIndexes = listOf(0, points.lastIndex / 2, points.lastIndex).distinct()
        var lastRight = Float.NEGATIVE_INFINITY
        dateIndexes.forEach { index ->
            val text = measurer.measure(formatDate(points[index].epochMs), axisStyle)
            val x = (plot.x(index) - text.size.width / 2f).clampIn(plot.left, size.width - text.size.width)
            if (x > lastRight + 6.dp.toPx()) {
                drawText(text, topLeft = Offset(x, plot.bottom + 6.dp.toPx()))
                lastRight = x + text.size.width
            }
        }

        clipRect(right = plot.left + (plot.right - plot.left) * reveal.value + 8.dp.toPx()) {
            if (points.size > 1) {
                val line = Path()
                val area = Path()
                points.indices.forEach { index ->
                    val p = Offset(plot.x(index), plot.y(points[index].value))
                    if (index == 0) {
                        line.moveTo(p.x, p.y)
                        area.moveTo(p.x, plot.bottom)
                        area.lineTo(p.x, p.y)
                    } else {
                        line.lineTo(p.x, p.y)
                        area.lineTo(p.x, p.y)
                    }
                }
                area.lineTo(plot.x(points.lastIndex), plot.bottom)
                area.close()
                drawPath(area, colors.ember.copy(alpha = 0.10f))
                drawPath(line, colors.ember, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Puntos chicos solo si son pocos; el último siempre, con anillo del color del fondo.
            if (points.size <= 16) {
                points.indices.forEach { index -> dot(Offset(plot.x(index), plot.y(points[index].value)), colors.ember, 3.dp.toPx()) }
            }
            dot(Offset(plot.x(points.lastIndex), plot.y(points.last().value)), colors.ember, 4.5.dp.toPx(), colors.surface1)
        }

        recordIndex?.takeIf { it in points.indices }?.let { index ->
            val center = Offset(plot.x(index), plot.y(points[index].value))
            dot(center, colors.gold, 5.dp.toPx(), colors.surface1)
            val text = measurer.measure(recordLabel(formatValue(points[index].value)), labelStyle)
            val x = (center.x - text.size.width / 2f).clampIn(plot.left, size.width - text.size.width)
            drawText(text, topLeft = Offset(x, (center.y - text.size.height - 8.dp.toPx()).coerceAtLeast(0f)))
        }

        selected?.takeIf { it in points.indices }?.let { index ->
            val x = plot.x(index)
            val center = Offset(x, plot.y(points[index].value))
            drawLine(colors.text3, Offset(x, plot.top), Offset(x, plot.bottom), strokeWidth = 1.dp.toPx())
            dot(center, colors.ember, 5.dp.toPx(), colors.surface1)
            val lines = listOfNotNull(formatValue(points[index].value), tooltipDetail(index), formatDate(points[index].epochMs))
            drawTooltip(measurer, lines, center, plot, tooltipValueStyle, tooltipDateStyle, colors.surface3)
        }
    }
}

private class PlotArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    private val min: Double,
    private val max: Double,
    private val points: List<ChartPoint>,
) {
    private val first = points.first().epochMs
    private val last = points.last().epochMs

    /** Posición en el tiempo: dos sesiones con una semana de diferencia quedan más lejos que dos seguidas. */
    fun x(index: Int): Float {
        if (points.size == 1 || last == first) return (left + right) / 2
        return left + (right - left) * ((points[index].epochMs - first).toFloat() / (last - first))
    }

    fun y(value: Double): Float = bottom - (bottom - top) * ((value - min) / (max - min)).toFloat()
}

/** Ancho a la izquierda para las marcas del eje: la más ancha más [extra]. */
private fun gutter(
    points: List<ChartPoint>,
    measurer: TextMeasurer,
    style: TextStyle,
    formatTick: (Double) -> String,
    extra: Float,
): Float {
    if (points.isEmpty()) return 0f
    val ticks = niceTicks(points.minOf { it.value }, points.maxOf { it.value })
    return ticks.maxOf { measurer.measure(formatTick(it), style).size.width }.toFloat() + extra
}

/** El punto más cercano a [x] entre [left] y [right], con la misma escala de tiempo que el dibujo. */
private fun nearestIndex(points: List<ChartPoint>, x: Float, right: Float, left: Float): Int? {
    if (points.isEmpty()) return null
    if (points.size == 1) return 0
    val first = points.first().epochMs
    val last = points.last().epochMs
    if (last == first) return 0
    return points.indices.minBy { index ->
        val px = left + (right - left) * ((points[index].epochMs - first).toFloat() / (last - first))
        abs(px - x)
    }
}

/** Margen derecho de la gráfica. */
private val RIGHT_PAD = 8.dp

/** Como coerceIn, pero si no cabe se queda en [min] en vez de fallar. */
private fun Float.clampIn(min: Float, max: Float): Float = if (max < min) min else coerceIn(min, max)

private fun DrawScope.dot(center: Offset, color: Color, radius: Float, ring: Color? = null) {
    if (ring != null) drawCircle(ring, radius + 2.dp.toPx(), center)
    drawCircle(color, radius, center)
}

/** El dato elegido: la primera línea (el valor) resaltada; el detalle y la fecha debajo. */
private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    lines: List<String>,
    center: Offset,
    plot: PlotArea,
    valueStyle: TextStyle,
    dateStyle: TextStyle,
    background: Color,
) {
    val texts = lines.mapIndexed { index, line -> measurer.measure(line, if (index == 0) valueStyle else dateStyle) }
    val pad = 10.dp.toPx()
    val width = texts.maxOf { it.size.width } + pad * 2
    val height = texts.sumOf { it.size.height } + pad * 2
    // Arriba del punto si cabe; si no, abajo. Nunca se sale de la gráfica por los lados.
    val x = (center.x - width / 2).clampIn(plot.left, plot.right - width)
    val above = center.y - height - 12.dp.toPx()
    val y = if (above >= 0f) above else (center.y + 12.dp.toPx()).coerceAtMost(size.height - height)
    drawRoundRect(background, Offset(x, y), Size(width, height), CornerRadius(12.dp.toPx()))
    var top = y + pad
    texts.forEach { text ->
        drawText(text, topLeft = Offset(x + pad, top))
        top += text.size.height
    }
}
