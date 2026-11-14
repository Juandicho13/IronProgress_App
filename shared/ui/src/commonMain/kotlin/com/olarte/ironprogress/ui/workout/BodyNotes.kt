package com.olarte.ironprogress.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.domain.progress.RecoveryLevel
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.progress.midSentence
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.fb_change_add
import com.olarte.ironprogress.ui.resources.fb_change_remove
import com.olarte.ironprogress.ui.resources.pain_swap_hint
import com.olarte.ironprogress.ui.resources.pain_swap_title
import com.olarte.ironprogress.ui.resources.sore_0
import com.olarte.ironprogress.ui.resources.sore_1
import com.olarte.ironprogress.ui.resources.sore_2
import com.olarte.ironprogress.ui.resources.sore_3
import com.olarte.ironprogress.ui.resources.sore_apply
import com.olarte.ironprogress.ui.resources.sore_body
import com.olarte.ironprogress.ui.resources.sore_result_changes
import com.olarte.ironprogress.ui.resources.sore_result_dismiss
import com.olarte.ironprogress.ui.resources.sore_result_same
import com.olarte.ironprogress.ui.resources.sore_skip
import com.olarte.ironprogress.ui.resources.sore_title
import com.olarte.ironprogress.ui.resources.warm_add
import com.olarte.ironprogress.ui.resources.warm_hint
import com.olarte.ironprogress.ui.resources.warm_loaded
import com.olarte.ironprogress.ui.resources.warm_recovering
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Avisos tranquilos de Entreno que vienen del cuerpo (F2-06 y F2-14): las agujetas antes de la
 * primera serie, lo que cambió hoy por ellas, la propuesta de cambiar un ejercicio que molestó y el
 * calentamiento de más cuando el músculo sigue cargado. Ninguno es una alarma: superficies, hielo
 * para la recuperación y oro solo para las molestias.
 */

/** "¿Te quedan agujetas?": por músculo, Nada / Pocas / Bastantes / Muchas, y Omitir o Listo. */
@Composable
internal fun SorenessCard(
    questions: List<SessionFeedback>,
    answers: Map<Muscle, Int>,
    enabled: Boolean,
    onAnswer: (Muscle, Int) -> Unit,
    onApply: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
        ) {
            Text(stringResource(Res.string.sore_title), Modifier.semantics { heading() }, style = typography.title3, color = colors.text1)
            Text(stringResource(Res.string.sore_body), style = typography.caption, color = colors.text2, textAlign = TextAlign.Center)
        }
        questions.forEach { question ->
            Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                Text(question.muscle.label(), style = typography.label, color = colors.text1)
                ForjaSegmented(
                    options = listOf(0, 1, 2, 3),
                    selected = answers[question.muscle] ?: -1,
                    label = { sorenessLabel(it) },
                    enabled = enabled,
                ) { onAnswer(question.muscle, it) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            ForjaButton(
                stringResource(Res.string.sore_skip),
                Modifier.weight(1f),
                ForjaButtonStyle.SECONDARY,
                enabled = enabled,
                onClick = onSkip,
            )
            ForjaButton(
                stringResource(Res.string.sore_apply),
                Modifier.weight(1f),
                ForjaButtonStyle.TONAL,
                enabled = enabled,
                onClick = onApply,
            )
        }
    }
}

/** "Hoy: Pecho +1 serie" o "Hoy van las mismas series", con una X para cerrarlo. */
@Composable
internal fun SorenessResultNote(result: Map<Muscle, SetChange>, onDismiss: () -> Unit) {
    val colors = ForjaTheme.colors
    val parts = result.filterValues { it != SetChange.KEEP }.toList().mapIndexed { index, (muscle, change) ->
        stringResource(
            if (change == SetChange.ADD) Res.string.fb_change_add else Res.string.fb_change_remove,
            if (index == 0) muscle.label() else muscle.label().midSentence(),
        )
    }
    Row(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.iceVeil).padding(start = ForjaSpacing.esp4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (parts.isEmpty()) {
                stringResource(
                    Res.string.sore_result_same,
                )
            } else {
                stringResource(Res.string.sore_result_changes, parts.joinToString(", "))
            },
            Modifier.weight(1f).padding(vertical = ForjaSpacing.esp3),
            style = ForjaTheme.typography.label,
            color = colors.iceInk,
        )
        ForjaIconButton(
            ForjaIcons.Close,
            stringResource(Res.string.sore_result_dismiss),
            filled = false,
            tint = colors.iceInk,
            onClick = onDismiss,
        )
    }
}

/** Molestia fuerte la última vez con este ejercicio: abre el cambio por otro (F1-12). */
@Composable
internal fun PainSwapRow(enabled: Boolean, onSwap: () -> Unit, onDismiss: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    Row(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.goldVeil)
            .clickable(enabled = enabled, role = Role.Button, onClick = onSwap)
            .heightIn(min = ForjaMeasures.minTouch)
            .padding(start = ForjaSpacing.esp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Icon(ForjaMoreIcons.Info, null, tint = colors.goldInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        Column(Modifier.weight(1f).padding(vertical = ForjaSpacing.esp2)) {
            Text(stringResource(Res.string.pain_swap_title), style = typography.label, color = colors.text1, maxLines = 2)
            Text(stringResource(Res.string.pain_swap_hint), style = typography.caption, color = colors.text2, maxLines = 1)
        }
        ForjaIconButton(
            ForjaIcons.Close,
            stringResource(Res.string.sore_result_dismiss),
            filled = false,
            tint = colors.text2,
            onClick = onDismiss,
        )
    }
}

/** "Pecho aún se está recuperando" y "Te sugiero 4 series de aproximación", con Agregar. */
@Composable
internal fun WarmupNoteRow(note: RecoveryWarmupNote, enabled: Boolean, onAdd: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    Row(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.surface2)
            .padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Icon(ForjaIcons.MonitoringOutline, null, tint = colors.iceInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    if (note.level == RecoveryLevel.LOADED) Res.string.warm_loaded else Res.string.warm_recovering,
                    note.muscle.label(),
                ),
                style = typography.label,
                color = colors.text1,
                maxLines = 2,
            )
            Text(
                pluralStringResource(Res.plurals.warm_hint, note.sets, note.sets),
                style = typography.caption,
                color = colors.text2,
                maxLines = 2,
            )
        }
        ForjaButton(stringResource(Res.string.warm_add), style = ForjaButtonStyle.TONAL, enabled = enabled, onClick = onAdd)
    }
}

@Composable
private fun sorenessLabel(value: Int): String = stringResource(
    when (value) {
        0 -> Res.string.sore_0
        1 -> Res.string.sore_1
        2 -> Res.string.sore_2
        else -> Res.string.sore_3
    },
)
