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
import androidx.compose.ui.text.style.TextAlign
import com.olarte.ironprogress.domain.progression.Stagnation
import com.olarte.ironprogress.domain.progression.StagnationKind
import com.olarte.ironprogress.domain.progression.StagnationProposal
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.stagnation_below
import com.olarte.ironprogress.ui.resources.stagnation_body
import com.olarte.ironprogress.ui.resources.stagnation_deload
import com.olarte.ironprogress.ui.resources.stagnation_deload_hint
import com.olarte.ironprogress.ui.resources.stagnation_hint
import com.olarte.ironprogress.ui.resources.stagnation_later
import com.olarte.ironprogress.ui.resources.stagnation_plateau
import com.olarte.ironprogress.ui.resources.stagnation_range
import com.olarte.ironprogress.ui.resources.stagnation_range_hint
import com.olarte.ironprogress.ui.resources.stagnation_recovery
import com.olarte.ironprogress.ui.resources.stagnation_recovery_hint
import com.olarte.ironprogress.ui.resources.stagnation_substitute
import com.olarte.ironprogress.ui.resources.stagnation_substitute_hint
import com.olarte.ironprogress.ui.resources.stagnation_title
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Aviso tranquilo de estancamiento en el ejercicio actual (F2-05): una fila que abre las propuestas.
 * No es una alarma: el color es el de las superficies y el ícono, el de progreso.
 */
@Composable
internal fun StagnationRow(stagnation: Stagnation, enabled: Boolean, onOpen: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    Row(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onOpen)
            .heightIn(min = ForjaMeasures.minTouch)
            .padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Icon(ForjaIcons.MonitoringOutline, null, tint = colors.iceInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        Column(Modifier.weight(1f)) {
            Text(stagnation.headline(), style = typography.label, color = colors.text1, maxLines = 2)
            Text(stringResource(Res.string.stagnation_hint), style = typography.caption, color = colors.text2, maxLines = 1)
        }
        Icon(ForjaIcons.ChevronRight, null, tint = colors.text3, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
    }
}

@Composable
private fun Stagnation.headline(): String = when (kind) {
    StagnationKind.PLATEAU -> stringResource(Res.string.stagnation_plateau, sessions)
    StagnationKind.BELOW_MIN -> stringResource(Res.string.stagnation_below, minReps ?: 0)
}

/** Las propuestas en el orden de HANDOFF 11.3, en una hoja. */
@Composable
internal fun StagnationSheet(stagnation: Stagnation, onChoose: (StagnationProposal) -> Unit, onLater: () -> Unit, onDismiss: () -> Unit) {
    ForjaSheet(stringResource(Res.string.stagnation_title), onDismiss = onDismiss) {
        StagnationOptions(stagnation, onChoose, onLater)
    }
}

/** El contenido de la hoja, aparte para poder verlo en las capturas. */
@Composable
internal fun StagnationOptions(stagnation: Stagnation, onChoose: (StagnationProposal) -> Unit, onLater: () -> Unit) {
    val colors = ForjaTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = ForjaSpacing.esp4), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Text(
            stagnation.headline() + ". " + stringResource(Res.string.stagnation_body),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            style = ForjaTheme.typography.body2,
            color = colors.text2,
            textAlign = TextAlign.Center,
        )
        Column {
            stagnation.proposals.forEachIndexed { index, proposal ->
                if (index > 0) ForjaDivider()
                val (title, hint) = proposal.texts(stagnation)
                ForjaListRow(
                    "${index + 1}. $title",
                    subtitle = hint,
                    chevron = proposal != StagnationProposal.RECOVERY,
                    onClick = if (proposal == StagnationProposal.RECOVERY) null else ({ onChoose(proposal) }),
                )
            }
        }
        ForjaButton(
            stringResource(Res.string.stagnation_later),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            ForjaButtonStyle.TEXT,
            onClick = onLater,
        )
    }
}

@Composable
private fun StagnationProposal.texts(stagnation: Stagnation): Pair<String, String> = when (this) {
    StagnationProposal.RECOVERY -> stringResource(Res.string.stagnation_recovery) to stringResource(Res.string.stagnation_recovery_hint)
    StagnationProposal.DELOAD -> stringResource(Res.string.stagnation_deload) to stringResource(Res.string.stagnation_deload_hint)
    StagnationProposal.CHANGE_RANGE -> {
        val range = stagnation.newRange
        stringResource(Res.string.stagnation_range, range?.min ?: 0, range?.max ?: 0) to stringResource(Res.string.stagnation_range_hint)
    }
    StagnationProposal.SUBSTITUTE ->
        stringResource(Res.string.stagnation_substitute) to stringResource(Res.string.stagnation_substitute_hint)
}
