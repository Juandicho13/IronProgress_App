package com.olarte.ironprogress.ui.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.progress.RecordHit
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.format_record_first
import com.olarte.ironprogress.ui.resources.format_record_new
import com.olarte.ironprogress.ui.resources.format_record_previous
import com.olarte.ironprogress.ui.resources.format_record_to_beat
import com.olarte.ironprogress.ui.resources.ic_trophy
import com.olarte.ironprogress.ui.resources.record_banner_a11y
import com.olarte.ironprogress.ui.resources.record_dismiss
import com.olarte.ironprogress.ui.resources.record_kind_heaviest
import com.olarte.ironprogress.ui.resources.record_kind_one_rm
import com.olarte.ironprogress.ui.resources.record_kind_reps
import com.olarte.ironprogress.ui.resources.record_kind_reps_with
import com.olarte.ironprogress.ui.resources.record_kind_strength
import com.olarte.ironprogress.ui.resources.record_kind_volume
import com.olarte.ironprogress.ui.resources.record_one_rm_info_body
import com.olarte.ironprogress.ui.resources.record_previous
import com.olarte.ironprogress.ui.resources.record_reps_value
import com.olarte.ironprogress.ui.resources.record_strength_info_body
import com.olarte.ironprogress.ui.resources.record_strength_info_title
import com.olarte.ironprogress.ui.resources.record_title
import com.olarte.ironprogress.ui.resources.summary_done
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Íconos de récords y progreso (F2-03 y F2-04), con las reglas de ForjaIcons. */
object ForjaRecordIcons {
    val Trophy: Painter
        @Composable get() = painterResource(Res.drawable.ic_trophy)
}

/**
 * Nombre de un récord. El 1RM estimado se llama "Fuerza estimada" en el modo básico y "1RM
 * estimado" en el Pro. "Más reps" lleva el peso cuando lo hay.
 */
@Composable
fun recordKindLabel(kind: RecordKind, weightKg: Double?, unit: WeightUnit, formats: Formats = rememberFormats()): String = when (kind) {
    RecordKind.ESTIMATED_ONE_REP_MAX -> strengthLabel()
    RecordKind.HEAVIEST_WEIGHT -> stringResource(Res.string.record_kind_heaviest)
    RecordKind.MOST_REPS -> weightKg?.takeIf { it > 0 }?.let { stringResource(Res.string.record_kind_reps_with, formats.weight(it, unit)) }
        ?: stringResource(Res.string.record_kind_reps)
    RecordKind.BEST_SESSION_VOLUME -> stringResource(Res.string.record_kind_volume)
}

/** "Fuerza estimada" en el modo básico, "1RM estimado" en el Pro. */
@Composable
fun strengthLabel(): String =
    stringResource(if (LocalAppSettings.current.proMode) Res.string.record_kind_one_rm else Res.string.record_kind_strength)

/** El valor de un récord con su unidad: kg o lb para el peso y el volumen, reps para las reps. */
@Composable
fun recordValue(kind: RecordKind, value: Double, unit: WeightUnit, formats: Formats = rememberFormats()): String = when (kind) {
    RecordKind.MOST_REPS -> pluralStringResource(Res.plurals.record_reps_value, value.toInt(), value.toInt())
    RecordKind.BEST_SESSION_VOLUME -> formats.volume(value, unit)
    else -> formats.weight(roundForDisplay(value, unit), unit)
}

/** El 1RM estimado sale con decimales largos: se muestra al medio kilo (o a la libra). */
fun roundForDisplay(kg: Double, unit: WeightUnit): Double = when (unit) {
    WeightUnit.KG -> kotlin.math.round(kg * 2) / 2
    WeightUnit.LB -> kotlin.math.round(kg * Formats.LB_PER_KG) / Formats.LB_PER_KG
}

/**
 * Aviso de récord en vivo (F2-03): una franja dorada dentro de la pantalla, justo debajo de la
 * barra de arriba del entreno o del descanso. Abre su espacio y empuja el contenido, así nunca tapa
 * un botón. Se anuncia a los lectores de pantalla y se cierra sola o al tocarla.
 */
@Composable
fun RecordBanner(hit: RecordHit?, exerciseName: String, unit: WeightUnit, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    // Guarda el último para que la salida se anime con el mismo texto.
    var shown by remember { mutableStateOf(hit) }
    if (hit != null) shown = hit
    AnimatedVisibility(
        visible = hit != null,
        modifier = modifier.fillMaxWidth(),
        enter = expandVertically(ForjaTheme.motion.spatial()) + fadeIn(ForjaTheme.motion.effect()),
        exit = shrinkVertically(ForjaTheme.motion.spatial()) + fadeOut(ForjaTheme.motion.effect()),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = ForjaSpacing.esp2), contentAlignment = Alignment.TopCenter) {
            shown?.let { RecordBannerContent(it, exerciseName, unit, onDismiss) }
        }
    }
}

@Composable
private fun RecordBannerContent(hit: RecordHit, exerciseName: String, unit: WeightUnit, onDismiss: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val kind = recordKindLabel(hit.kind, hit.weightKg, unit, formats)
    val value = recordValue(hit.kind, hit.value, unit, formats)
    val previous = stringResource(Res.string.record_previous, recordValue(hit.kind, hit.previous, unit, formats))
    val title = stringResource(Res.string.record_title)
    val announcement = stringResource(Res.string.record_banner_a11y, "$kind $value", exerciseName)
    val dismiss = stringResource(Res.string.record_dismiss)
    Row(
        Modifier.padding(horizontal = ForjaSpacing.esp5).widthIn(max = 520.dp).fillMaxWidth()
            .heightIn(min = ForjaMeasures.minTouch + ForjaSpacing.esp4)
            .clip(CircleShape).background(colors.goldVeil)
            .clickable(onClickLabel = dismiss, onClick = onDismiss)
            .clearAndSetSemantics {
                contentDescription = announcement
                liveRegion = LiveRegionMode.Polite
                role = Role.Button
            }
            .padding(start = ForjaSpacing.esp2, end = ForjaSpacing.esp5, top = ForjaSpacing.esp2, bottom = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Box(Modifier.size(ForjaMeasures.minTouch).clip(CircleShape).background(colors.gold), contentAlignment = Alignment.Center) {
            Icon(ForjaRecordIcons.Trophy, contentDescription = null, tint = colors.onGold, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "$title · $exerciseName",
                style = typography.caption,
                color = colors.goldInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(kind, style = typography.body2, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            ForjaNumber(value, typography.smallFigure, color = colors.text1)
            Text(previous, style = typography.caption, color = colors.text2, maxLines = 1)
        }
    }
}

/** Hoja que explica la fuerza estimada en palabras simples (y la fórmula en el modo Pro). */
@Composable
fun StrengthInfoSheet(onDismiss: () -> Unit) {
    val pro = LocalAppSettings.current.proMode
    ForjaSheet(stringResource(Res.string.record_strength_info_title), onDismiss) {
        Text(
            stringResource(Res.string.record_strength_info_body),
            style = ForjaTheme.typography.body,
            color = ForjaTheme.colors.text1,
        )
        if (pro) {
            Text(stringResource(Res.string.record_one_rm_info_body), style = ForjaTheme.typography.body2, color = ForjaTheme.colors.text2)
        }
        ForjaButton(
            stringResource(Res.string.summary_done),
            Modifier.fillMaxWidth(),
            style = ForjaButtonStyle.SECONDARY,
            onClick = onDismiss,
        )
    }
}

/**
 * Resultado de un bloque con tiempo frente a su marca (F2-13): en oro con el trofeo si es récord;
 * si no, la marca anterior, o que es la primera vez.
 */
@Composable
fun FormatResultNote(isRecord: Boolean, previous: String?, modifier: Modifier = Modifier) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    if (isRecord) {
        Row(
            modifier.fillMaxWidth().clip(CircleShape).background(colors.goldVeil)
                .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3, Alignment.CenterHorizontally),
        ) {
            Icon(ForjaRecordIcons.Trophy, contentDescription = null, tint = colors.goldInk, modifier = Modifier.size(22.dp))
            Column {
                Text(stringResource(Res.string.format_record_new), style = typography.label, color = colors.text1)
                if (previous != null) {
                    Text(stringResource(Res.string.format_record_previous, previous), style = typography.caption, color = colors.text2)
                }
            }
        }
    } else {
        Text(
            if (previous != null) {
                stringResource(Res.string.format_record_previous, previous)
            } else {
                stringResource(Res.string.format_record_first)
            },
            modifier.fillMaxWidth(),
            style = typography.body2,
            color = colors.text2,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Marca a superar: 8 + 12" con el trofeo, antes de empezar un bloque que ya tiene marca. */
@Composable
fun FormatBestToBeat(best: String, modifier: Modifier = Modifier) {
    val colors = ForjaTheme.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2, Alignment.CenterHorizontally),
    ) {
        Icon(ForjaRecordIcons.Trophy, contentDescription = null, tint = colors.goldInk, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.format_record_to_beat, best), style = ForjaTheme.typography.body2, color = colors.text1)
    }
}
