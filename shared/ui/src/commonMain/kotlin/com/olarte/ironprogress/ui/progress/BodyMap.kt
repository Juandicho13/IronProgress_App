package com.olarte.ironprogress.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.MuscleRecovery
import com.olarte.ironprogress.domain.progress.Recovery
import com.olarte.ironprogress.domain.progress.RecoveryLevel
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rec_all_fresh
import com.olarte.ironprogress.ui.resources.rec_back
import com.olarte.ironprogress.ui.resources.rec_fresh
import com.olarte.ironprogress.ui.resources.rec_front
import com.olarte.ironprogress.ui.resources.rec_last_trained
import com.olarte.ironprogress.ui.resources.rec_level_fresh
import com.olarte.ironprogress.ui.resources.rec_level_loaded
import com.olarte.ironprogress.ui.resources.rec_level_recovering
import com.olarte.ironprogress.ui.resources.rec_loaded
import com.olarte.ironprogress.ui.resources.rec_many
import com.olarte.ironprogress.ui.resources.rec_map_description
import com.olarte.ironprogress.ui.resources.rec_none_recent
import com.olarte.ironprogress.ui.resources.rec_not_trained
import com.olarte.ironprogress.ui.resources.rec_one
import com.olarte.ironprogress.ui.resources.rec_pro_load
import com.olarte.ironprogress.ui.resources.rec_ready_days
import com.olarte.ironprogress.ui.resources.rec_ready_hours
import com.olarte.ironprogress.ui.resources.rec_ready_now
import com.olarte.ironprogress.ui.resources.rec_sore
import com.olarte.ironprogress.ui.resources.rec_tap_hint
import com.olarte.ironprogress.ui.resources.rec_title
import com.olarte.ironprogress.ui.resources.rec_two
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaColorPalette
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil
import kotlin.time.Instant

/*
 * Mapa corporal de recuperación (F2-08, PANTALLAS 2.1). Dibujo propio: siluetas de frente y de
 * espalda hechas con óvalos, rectángulos redondeados y polígonos sobre una caja de 100 × 200. Cada
 * músculo se pinta de heat0 (fresco) a heat4 (cargado) y se toca para ver su estado.
 */

/** Una figura en coordenadas de la caja de 100 × 200. */
internal sealed interface BodyShape {
    fun contains(x: Float, y: Float): Boolean

    fun DrawScope.draw(color: Color, sx: Float, sy: Float, stroke: Stroke? = null)

    data class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : BodyShape {
        override fun contains(x: Float, y: Float): Boolean {
            val dx = (x - cx) / rx
            val dy = (y - cy) / ry
            return dx * dx + dy * dy <= 1f
        }

        override fun DrawScope.draw(color: Color, sx: Float, sy: Float, stroke: Stroke?) {
            val topLeft = Offset((cx - rx) * sx, (cy - ry) * sy)
            val size = Size(rx * 2 * sx, ry * 2 * sy)
            if (stroke == null) drawOval(color, topLeft, size) else drawOval(color, topLeft, size, style = stroke)
        }
    }

    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float, val radius: Float) : BodyShape {
        override fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

        override fun DrawScope.draw(color: Color, sx: Float, sy: Float, stroke: Stroke?) {
            val topLeft = Offset(left * sx, top * sy)
            val size = Size((right - left) * sx, (bottom - top) * sy)
            val corner = CornerRadius(radius * sx, radius * sx)
            if (stroke == null) drawRoundRect(color, topLeft, size, corner) else drawRoundRect(color, topLeft, size, corner, style = stroke)
        }
    }

    data class Polygon(val points: List<Pair<Float, Float>>) : BodyShape {
        override fun contains(x: Float, y: Float): Boolean {
            var inside = false
            var j = points.lastIndex
            for (i in points.indices) {
                val (xi, yi) = points[i]
                val (xj, yj) = points[j]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
            return inside
        }

        override fun DrawScope.draw(color: Color, sx: Float, sy: Float, stroke: Stroke?) {
            val path = Path().apply {
                points.forEachIndexed { index, (x, y) -> if (index == 0) moveTo(x * sx, y * sy) else lineTo(x * sx, y * sy) }
                close()
            }
            if (stroke == null) drawPath(path, color) else drawPath(path, color, style = stroke)
        }
    }
}

internal data class BodyRegion(val muscle: Muscle, val shapes: List<BodyShape>)

/** Las dos vistas del mapa. */
internal object BodyFigures {
    private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = BodyShape.Oval(cx, cy, rx, ry)

    private fun mirror(shape: BodyShape.Oval) = listOf(shape, shape.copy(cx = 100f - shape.cx))

    private fun mirror(points: List<Pair<Float, Float>>) =
        listOf(BodyShape.Polygon(points), BodyShape.Polygon(points.map { (x, y) -> (100f - x) to y }))

    /** Silueta común: cabeza, cuello, tronco, brazos, manos y piernas. */
    val silhouette: List<BodyShape> = listOf(
        oval(50f, 14f, 10f, 12f),
        BodyShape.Box(45f, 23f, 55f, 32f, 3f),
        BodyShape.Box(29f, 30f, 71f, 92f, 14f),
        BodyShape.Box(33f, 84f, 67f, 104f, 10f),
        BodyShape.Box(17f, 33f, 30f, 70f, 6f),
        BodyShape.Box(70f, 33f, 83f, 70f, 6f),
        BodyShape.Box(12f, 67f, 26f, 104f, 6f),
        BodyShape.Box(74f, 67f, 88f, 104f, 6f),
        oval(19f, 110f, 6f, 7f),
        oval(81f, 110f, 6f, 7f),
        BodyShape.Box(33f, 100f, 49.5f, 152f, 8f),
        BodyShape.Box(50.5f, 100f, 67f, 152f, 8f),
        BodyShape.Box(35f, 150f, 48.5f, 194f, 6f),
        BodyShape.Box(51.5f, 150f, 65f, 194f, 6f),
    )

    val front: List<BodyRegion> = listOf(
        BodyRegion(Muscle.SHOULDERS, mirror(oval(28f, 38f, 8f, 7f))),
        BodyRegion(Muscle.CHEST, mirror(oval(40.5f, 46f, 10f, 8f))),
        BodyRegion(Muscle.BICEPS, mirror(oval(23.5f, 54f, 5f, 11f))),
        BodyRegion(Muscle.FOREARMS, mirror(oval(19f, 84f, 5f, 13f))),
        BodyRegion(
            Muscle.ABS,
            listOf(57f, 66f, 75f).flatMap { top ->
                listOf(BodyShape.Box(43f, top, 49.5f, top + 8f, 2.5f), BodyShape.Box(50.5f, top, 57f, top + 8f, 2.5f))
            },
        ),
        BodyRegion(Muscle.QUADS, mirror(oval(41f, 123f, 7.5f, 21f))),
        BodyRegion(Muscle.CALVES, mirror(oval(41.5f, 168f, 5f, 15f))),
    )

    val back: List<BodyRegion> = listOf(
        BodyRegion(Muscle.TRAPS, listOf(BodyShape.Polygon(listOf(40f to 30f, 60f to 30f, 67f to 38f, 50f to 53f, 33f to 38f)))),
        BodyRegion(Muscle.SHOULDERS, mirror(oval(28f, 38f, 8f, 7f))),
        BodyRegion(Muscle.LATS, mirror(listOf(33f to 46f, 47.5f to 55f, 47.5f to 74f, 38f to 70f, 32f to 56f))),
        BodyRegion(Muscle.TRICEPS, mirror(oval(23.5f, 54f, 5f, 11f))),
        BodyRegion(Muscle.FOREARMS, mirror(oval(19f, 84f, 5f, 13f))),
        BodyRegion(Muscle.LOWER_BACK, listOf(BodyShape.Box(41f, 76f, 59f, 88f, 4f))),
        BodyRegion(Muscle.GLUTES, mirror(oval(42f, 98f, 8.5f, 8f))),
        BodyRegion(Muscle.HAMSTRINGS, mirror(oval(41f, 127f, 7f, 18f))),
        BodyRegion(Muscle.CALVES, mirror(oval(41.5f, 165f, 5.5f, 13f))),
    )

    fun hit(regions: List<BodyRegion>, x: Float, y: Float): Muscle? =
        regions.lastOrNull { region -> region.shapes.any { it.contains(x, y) } }?.muscle
}

internal fun ForjaColorPalette.heat(level: Int): Color = when (level) {
    0 -> heat0
    1 -> heat1
    2 -> heat2
    3 -> heat3
    else -> heat4
}

/**
 * Tarjeta "Recuperación muscular": la frase de lo que sigue cargado, las dos siluetas, la leyenda de
 * Fresco a Cargado y el detalle del músculo tocado. El estado siempre va también en palabras.
 */
@Composable
internal fun RecoveryCard(recovery: Map<Muscle, MuscleRecovery>, nowEpochMs: Long, initialSelected: Muscle? = null) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    var selectedId by rememberSaveable { mutableStateOf(initialSelected?.id) }
    val selected = selectedId?.let { Muscle.fromId(it) }
    val description = mapDescription(recovery)
    val motion = ForjaTheme.motion
    ForjaCard {
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            Text(stringResource(Res.string.rec_title), Modifier.semantics { heading() }, style = typography.title3, color = colors.text1)
            Text(headline(recovery), style = typography.body2, color = colors.text2)
        }
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4, Alignment.CenterHorizontally),
        ) {
            Figure(stringResource(Res.string.rec_front), BodyFigures.front, recovery, selected, Modifier.weight(1f)) { selectedId = it?.id }
            Figure(stringResource(Res.string.rec_back), BodyFigures.back, recovery, selected, Modifier.weight(1f)) { selectedId = it?.id }
        }
        Legend()
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(motion.effectFast()) togetherWith fadeOut(motion.effectFast()) },
            label = "RecoveryDetail",
        ) { muscle ->
            val state = muscle?.let { recovery[it] }
            if (state == null) {
                Text(
                    stringResource(Res.string.rec_tap_hint),
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = ForjaSpacing.esp3),
                    style = typography.caption,
                    color = colors.text3,
                    textAlign = TextAlign.Center,
                )
            } else {
                MuscleDetail(state, nowEpochMs)
            }
        }
    }
}

@Composable
private fun Figure(
    caption: String,
    regions: List<BodyRegion>,
    recovery: Map<Muscle, MuscleRecovery>,
    selected: Muscle?,
    modifier: Modifier,
    onSelect: (Muscle?) -> Unit,
) {
    val colors = ForjaTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(0.5f).pointerInput(regions) {
                detectTapGestures { offset ->
                    val x = offset.x / size.width * 100f
                    val y = offset.y / size.height * 200f
                    onSelect(BodyFigures.hit(regions, x, y))
                }
            },
        ) {
            val sx = size.width / 100f
            val sy = size.height / 200f
            BodyFigures.silhouette.forEach { shape -> with(shape) { draw(colors.surface3, sx, sy) } }
            val gap = Stroke(width = 1.5.dp.toPx())
            regions.forEach { region ->
                val heat = recovery[region.muscle]?.heat ?: 0
                region.shapes.forEach { shape ->
                    with(shape) {
                        draw(colors.heat(heat), sx, sy)
                        draw(colors.surface1, sx, sy, gap)
                    }
                }
            }
            val ring = Stroke(width = 2.5.dp.toPx())
            regions.filter { it.muscle == selected }.forEach { region ->
                region.shapes.forEach { shape -> with(shape) { draw(colors.text1, sx, sy, ring) } }
            }
        }
        Text(caption, style = ForjaTheme.typography.caption, color = colors.text2)
    }
}

/** Cinco bloques de 22 × 10 de heat0 a heat4, con Fresco a la izquierda y Cargado a la derecha. */
@Composable
private fun Legend() {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(stringResource(Res.string.rec_fresh), style = typography.caption, color = colors.text2)
        Box(Modifier.width(ForjaSpacing.esp2))
        (0..4).forEach { level ->
            Box(Modifier.padding(horizontal = 1.dp).size(22.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(colors.heat(level)))
        }
        Box(Modifier.width(ForjaSpacing.esp2))
        Text(stringResource(Res.string.rec_loaded), style = typography.caption, color = colors.text2)
    }
}

@Composable
private fun MuscleDetail(state: MuscleRecovery, nowEpochMs: Long) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val pro = LocalAppSettings.current.proMode
    Column(
        Modifier.fillMaxWidth().padding(
            top = ForjaSpacing.esp2,
        ).clip(ForjaRadius.shapeM).background(colors.surface2).padding(ForjaSpacing.esp3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.heat(state.heat)))
            Text(state.muscle.label() + " · " + state.level.label(), style = typography.label, color = colors.text1)
        }
        Text(readyText(state, nowEpochMs), style = typography.body2, color = colors.text2, textAlign = TextAlign.Center)
        if (state.sore) Text(stringResource(Res.string.rec_sore), style = typography.caption, color = colors.goldInk)
        Text(
            state.lastTrainedAt?.let { stringResource(Res.string.rec_last_trained, formats.date(it.toEpochMilliseconds())) }
                ?: stringResource(Res.string.rec_not_trained),
            style = typography.caption,
            color = colors.text3,
            textAlign = TextAlign.Center,
        )
        if (pro) {
            Text(
                stringResource(Res.string.rec_pro_load, formats.number(state.fatigue, 1)),
                style = typography.caption,
                color = colors.text3,
            )
        }
    }
}

@Composable
internal fun RecoveryLevel.label(): String = stringResource(
    when (this) {
        RecoveryLevel.FRESH -> Res.string.rec_level_fresh
        RecoveryLevel.RECOVERING -> Res.string.rec_level_recovering
        RecoveryLevel.LOADED -> Res.string.rec_level_loaded
    },
)

/** "Se recupera en unas 18 h", "en unos 2 días" o "Listo para entrenar". */
@Composable
private fun readyText(state: MuscleRecovery, nowEpochMs: Long): String {
    val ready = state.readyAt?.takeIf { it > Instant.fromEpochMilliseconds(nowEpochMs) }
        ?: return stringResource(Res.string.rec_ready_now)
    val hours = ceil((ready.toEpochMilliseconds() - nowEpochMs) / 3_600_000.0).toInt().coerceAtLeast(1)
    return if (hours < 24) {
        pluralStringResource(Res.plurals.rec_ready_hours, hours, hours)
    } else {
        val days = ceil(hours / 24.0).toInt()
        pluralStringResource(Res.plurals.rec_ready_days, days, days)
    }
}

/** La frase de arriba: qué sigue cargado, o que todo está listo. */
@Composable
private fun headline(recovery: Map<Muscle, MuscleRecovery>): String {
    val pending = Recovery.notReady(recovery)
    return when {
        recovery.values.all { it.fatigue < 0.5 && !it.sore } -> stringResource(Res.string.rec_none_recent)
        pending.isEmpty() -> stringResource(Res.string.rec_all_fresh)
        pending.size == 1 -> stringResource(Res.string.rec_one, pending[0].muscle.label())
        pending.size == 2 -> stringResource(Res.string.rec_two, pending[0].muscle.label(), pending[1].muscle.label().midSentence())
        else -> stringResource(Res.string.rec_many, pending[0].muscle.label(), pending[1].muscle.label().midSentence(), pending.size - 2)
    }
}

/** Un nombre de músculo dentro de la frase va en minúscula: "Pecho y tríceps". */
internal fun String.midSentence(): String = replaceFirstChar { it.lowercase() }

/** Para lectores de pantalla: cada músculo con su estado en palabras. */
@Composable
private fun mapDescription(recovery: Map<Muscle, MuscleRecovery>): String {
    val parts = Muscle.entries.mapNotNull { muscle -> recovery[muscle]?.let { "${muscle.label()}: ${it.level.label()}" } }
    return stringResource(Res.string.rec_map_description, parts.joinToString(". "))
}
