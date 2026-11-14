package com.olarte.ironprogress.ui.workout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.progression.SuggestionReason
import com.olarte.ironprogress.domain.progression.SuggestionRecord
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.suggest_apply
import com.olarte.ironprogress.ui.resources.suggest_apply_description
import com.olarte.ironprogress.ui.resources.suggest_first
import com.olarte.ironprogress.ui.resources.suggest_load
import com.olarte.ironprogress.ui.resources.suggest_reps
import com.olarte.ironprogress.ui.resources.why_add_rep
import com.olarte.ironprogress.ui.resources.why_adjustment
import com.olarte.ironprogress.ui.resources.why_aim_min
import com.olarte.ironprogress.ui.resources.why_at_top
import com.olarte.ironprogress.ui.resources.why_deload
import com.olarte.ironprogress.ui.resources.why_deload_sets
import com.olarte.ironprogress.ui.resources.why_first
import com.olarte.ironprogress.ui.resources.why_first_pro
import com.olarte.ironprogress.ui.resources.why_increase
import com.olarte.ironprogress.ui.resources.why_increase_pro
import com.olarte.ironprogress.ui.resources.why_previous_range
import com.olarte.ironprogress.ui.resources.why_previous_sets
import com.olarte.ironprogress.ui.resources.why_repeat_hard
import com.olarte.ironprogress.ui.resources.why_repeat_hard_pro
import com.olarte.ironprogress.ui.resources.why_young_wait
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/**
 * La sugerencia del motor para la serie actual (F2-02): qué toca hoy y su porqué en una línea. Si la
 * serie ya no coincide con la sugerencia, "Usar" la vuelve a poner. En modo básico la explicación no
 * habla de RIR; en Pro sí.
 */
@Composable
internal fun SuggestionLine(
    record: SuggestionRecord,
    matches: Boolean,
    pro: Boolean,
    unit: WeightUnit,
    enabled: Boolean,
    onApply: () -> Unit,
) {
    if (record.reason == SuggestionReason.SAME_AS_LAST) return
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val title = record.title(formats, unit)
    val why = record.explanation(pro, formats, unit)
    val canApply = !matches && (record.weightKg != null || record.reps != null)
    Row(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.emberVeil)
            .padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Icon(ForjaIcons.NorthEast, null, tint = colors.emberInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        Column(Modifier.weight(1f)) {
            Text(title, style = typography.label, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (why.isNotEmpty()) {
                Text(why, style = typography.caption, color = colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (canApply) {
            val description = stringResource(Res.string.suggest_apply_description)
            Box(
                Modifier.heightIn(min = ForjaMeasures.minTouch).clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onApply)
                    .semantics { contentDescription = description }
                    .padding(horizontal = ForjaSpacing.esp2),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.suggest_apply), style = typography.label, color = colors.emberInk, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SuggestionRecord.title(formats: Formats, unit: WeightUnit): String {
    val repsText = reps?.toString()
    val weight = weightKg
    return when {
        reason == SuggestionReason.FIRST_TIME && weight == null -> stringResource(Res.string.suggest_first)
        weight != null && repsText != null -> stringResource(Res.string.suggest_load, formats.weight(weight, unit), repsText)
        repsText != null -> stringResource(Res.string.suggest_reps, repsText)
        else -> stringResource(Res.string.suggest_first)
    }
}

/** La línea del porqué, con los números de la última vez cuando hay. */
@Composable
internal fun SuggestionRecord.explanation(pro: Boolean, formats: Formats, unit: WeightUnit): String = when (reason) {
    SuggestionReason.INCREASE -> {
        val previous = previousText()
        val rir = previousRir
        val weight = previousWeightKg
        if (pro && previous != null && rir != null && weight != null) {
            stringResource(Res.string.why_increase_pro, previous, formats.weight(weight, unit), rir)
        } else {
            stringResource(Res.string.why_increase)
        }
    }
    SuggestionReason.REPEAT_HARD -> {
        val rir = previousRir
        val top = repsMax
        if (pro && rir != null && top != null) {
            stringResource(Res.string.why_repeat_hard_pro, top, rir)
        } else {
            stringResource(Res.string.why_repeat_hard)
        }
    }
    SuggestionReason.ADD_REP -> stringResource(Res.string.why_add_rep, repsMax ?: reps ?: 0)
    SuggestionReason.AIM_MIN -> stringResource(Res.string.why_aim_min, repsMin ?: reps ?: 0)
    SuggestionReason.DELOAD -> stringResource(Res.string.why_deload, repsMin ?: reps ?: 0)
    SuggestionReason.FIRST_TIME -> stringResource(if (pro) Res.string.why_first_pro else Res.string.why_first)
    SuggestionReason.ADJUSTMENT_HOLD -> stringResource(Res.string.why_adjustment)
    SuggestionReason.YOUNG_WAIT -> stringResource(Res.string.why_young_wait)
    SuggestionReason.AT_TOP -> stringResource(Res.string.why_at_top)
    SuggestionReason.DELOAD_SETS -> stringResource(Res.string.why_deload_sets)
    SuggestionReason.SAME_AS_LAST -> ""
}

/** "3 × 12" o "3 series de 10 a 12"; null si no hay datos de la última vez. */
@Composable
private fun SuggestionRecord.previousText(): String? {
    val sets = previousSets ?: return null
    val low = previousMinReps ?: return null
    val high = previousMaxReps ?: low
    return if (low == high) {
        stringResource(Res.string.why_previous_sets, sets, low)
    } else {
        stringResource(Res.string.why_previous_range, sets, low, high)
    }
}
