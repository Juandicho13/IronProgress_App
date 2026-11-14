package com.olarte.ironprogress.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.Joint
import com.olarte.ironprogress.domain.feedback.PerceivedLoad
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.progress.midSentence
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.fb_change_add
import com.olarte.ironprogress.ui.resources.fb_change_remove
import com.olarte.ironprogress.ui.resources.fb_if_no_soreness
import com.olarte.ironprogress.ui.resources.fb_joint_elbow
import com.olarte.ironprogress.ui.resources.fb_joint_knee
import com.olarte.ironprogress.ui.resources.fb_joint_lower_back
import com.olarte.ironprogress.ui.resources.fb_joint_shoulder
import com.olarte.ironprogress.ui.resources.fb_joint_wrist
import com.olarte.ironprogress.ui.resources.fb_load_hard
import com.olarte.ironprogress.ui.resources.fb_load_light
import com.olarte.ironprogress.ui.resources.fb_load_right
import com.olarte.ironprogress.ui.resources.fb_next_same
import com.olarte.ironprogress.ui.resources.fb_next_week
import com.olarte.ironprogress.ui.resources.fb_pain
import com.olarte.ironprogress.ui.resources.fb_pain_mild
import com.olarte.ironprogress.ui.resources.fb_pain_none
import com.olarte.ironprogress.ui.resources.fb_pain_strong
import com.olarte.ironprogress.ui.resources.fb_pain_strong_note
import com.olarte.ironprogress.ui.resources.fb_pump
import com.olarte.ironprogress.ui.resources.fb_pump_0
import com.olarte.ironprogress.ui.resources.fb_pump_1
import com.olarte.ironprogress.ui.resources.fb_pump_2
import com.olarte.ironprogress.ui.resources.fb_pump_3
import com.olarte.ironprogress.ui.resources.fb_subtitle
import com.olarte.ironprogress.ui.resources.fb_title
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Lo que el Resumen puede hacer con el feedback. */
class FeedbackActions(
    val onLoad: (Muscle, PerceivedLoad) -> Unit,
    val onPump: (Muscle, Int) -> Unit,
    val onJoint: (Joint?) -> Unit,
    val onStrong: (Boolean) -> Unit,
    val onSave: () -> Unit,
)

/**
 * Tarjeta "¿Cuánto trabajo sintió cada músculo?" (PANTALLAS 1.6, F2-06): por músculo un segmentado
 * Poco / Justo / Mucho (y el bombeo en Pro), las molestias por articulación y, debajo, lo que
 * cambiaría la próxima semana. Todo empieza en "Justo" y "Ninguna".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedbackCard(form: FeedbackForm, pro: Boolean, actions: FeedbackActions) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaCard(spacing = ForjaSpacing.esp3) {
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            Text(stringResource(Res.string.fb_title), Modifier.semantics { heading() }, style = typography.title3, color = colors.text1)
            Text(stringResource(Res.string.fb_subtitle), style = typography.caption, color = colors.text2)
        }
        form.muscles.forEachIndexed { index, muscle ->
            if (index > 0) ForjaDivider(Modifier.padding(end = ForjaSpacing.esp4))
            Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                Text(muscle.label(), style = typography.label, color = colors.text1)
                ForjaSegmented(
                    options = PerceivedLoad.entries,
                    selected = form.loadOf(muscle),
                    label = { it.label() },
                    enabled = !form.saving,
                ) { actions.onLoad(muscle, it) }
                if (pro) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                        Text(stringResource(Res.string.fb_pump), style = typography.caption, color = colors.text2)
                        ForjaSegmented(
                            options = listOf(0, 1, 2, 3),
                            selected = form.pumps[muscle] ?: -1,
                            label = { pumpLabel(it) },
                            modifier = Modifier.weight(1f),
                            enabled = !form.saving,
                        ) { actions.onPump(muscle, it) }
                    }
                }
            }
        }
        ForjaDivider(Modifier.padding(end = ForjaSpacing.esp4))
        Text(stringResource(Res.string.fb_pain), style = typography.label, color = colors.text1)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            FeedbackChip(stringResource(Res.string.fb_pain_none), form.joints.isEmpty(), !form.saving) { actions.onJoint(null) }
            Joint.entries.forEach { joint ->
                FeedbackChip(joint.label(), joint in form.joints, !form.saving) { actions.onJoint(joint) }
            }
        }
        if (form.joints.isNotEmpty()) {
            ForjaSegmented(
                options = listOf(false, true),
                selected = form.strongPain,
                label = { stringResource(if (it) Res.string.fb_pain_strong else Res.string.fb_pain_mild) },
                enabled = !form.saving,
            ) { actions.onStrong(it) }
            if (form.strongPain) {
                Text(
                    stringResource(Res.string.fb_pain_strong_note),
                    Modifier.fillMaxWidth(),
                    style = typography.caption,
                    color = colors.goldInk,
                    textAlign = TextAlign.Center,
                )
            }
        }
        NextWeek(form.preview(pro))
    }
}

/** "La próxima semana: Pecho +1 serie, Tríceps −1 serie" o "mismas series". */
@Composable
private fun NextWeek(changes: Map<Muscle, SetChange>) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val parts = changes.filterValues { it != SetChange.KEEP }.toList().mapIndexed { index, (muscle, change) ->
        stringResource(
            if (change == SetChange.ADD) Res.string.fb_change_add else Res.string.fb_change_remove,
            if (index == 0) muscle.label() else muscle.label().midSentence(),
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.surface2).padding(ForjaSpacing.esp3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Text(
            if (parts.isEmpty()) {
                stringResource(
                    Res.string.fb_next_same,
                )
            } else {
                stringResource(Res.string.fb_next_week, parts.joinToString(", "))
            },
            style = typography.label,
            color = colors.text1,
            textAlign = TextAlign.Center,
        )
        if (changes.values.any { it == SetChange.ADD }) {
            Text(
                stringResource(Res.string.fb_if_no_soreness),
                style = typography.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Chip de 36 para las molestias: elegido en emberVeil con texto ember. */
@Composable
internal fun FeedbackChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Text(
        text,
        Modifier.clip(CircleShape)
            .background(if (selected) colors.emberVeil else colors.surface2)
            .toggleable(selected, enabled = enabled, role = Role.Checkbox) { onClick() }
            .heightIn(min = ForjaMeasures.minTouch - 12.dp)
            .padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
        style = ForjaTheme.typography.label,
        color = if (selected) colors.emberInk else colors.text1,
        maxLines = 1,
    )
}

@Composable
private fun PerceivedLoad.label(): String = stringResource(
    when (this) {
        PerceivedLoad.LIGHT -> Res.string.fb_load_light
        PerceivedLoad.RIGHT -> Res.string.fb_load_right
        PerceivedLoad.HARD -> Res.string.fb_load_hard
    },
)

@Composable
private fun pumpLabel(value: Int): String = stringResource(
    when (value) {
        0 -> Res.string.fb_pump_0
        1 -> Res.string.fb_pump_1
        2 -> Res.string.fb_pump_2
        else -> Res.string.fb_pump_3
    },
)

@Composable
internal fun Joint.label(): String = stringResource(
    when (this) {
        Joint.SHOULDER -> Res.string.fb_joint_shoulder
        Joint.ELBOW -> Res.string.fb_joint_elbow
        Joint.WRIST -> Res.string.fb_joint_wrist
        Joint.KNEE -> Res.string.fb_joint_knee
        Joint.LOWER_BACK -> Res.string.fb_joint_lower_back
    },
)
