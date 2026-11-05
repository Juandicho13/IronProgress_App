package com.olarte.ironprogress.ui.discipline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.flow_close
import com.olarte.ironprogress.ui.resources.flow_voice_off
import com.olarte.ironprogress.ui.resources.flow_voice_on
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Botón redondo de 48 con ícono de 24 (PANTALLAS 0). */
@Composable
internal fun RoundIconButton(icon: Painter, description: String, background: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(ForjaMeasures.minTouch).clip(CircleShape)
            .then(if (background) Modifier.background(ForjaTheme.colors.surface3) else Modifier)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ForjaTheme.colors.text1, modifier = Modifier.size(ForjaMeasures.iconSize))
    }
}

/** Barra superior de Flujo y Respiración: minimizar, título con detalle y la voz. */
@Composable
internal fun GuidedTopBar(title: String, subtitle: String?, muted: Boolean, onClose: () -> Unit, onToggleVoice: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(ForjaMoreIcons.ChevronLeft, stringResource(Res.string.flow_close), onClick = onClose)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
            subtitle?.let { Text(it, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2) }
        }
        RoundIconButton(
            if (muted) ForjaMoreIcons.VolumeOff else ForjaMoreIcons.VolumeUp,
            stringResource(if (muted) Res.string.flow_voice_off else Res.string.flow_voice_on),
            onClick = onToggleVoice,
        )
    }
}

/** Botón de texto de 48 de alto en surface3 (secundario) o ember (principal). */
@Composable
internal fun PillButton(text: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        modifier.heightIn(min = if (primary) ForjaMeasures.primaryButtonHeight else ForjaMeasures.secondaryButtonHeight)
            .clip(CircleShape).background(if (primary) colors.ember else colors.surface3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (primary) ForjaTheme.typography.primaryButton else ForjaTheme.typography.secondaryButton,
            color = if (primary) colors.onEmber else colors.text1,
        )
    }
}

/** Caja pequeña con etiqueta y valor, en surface1. */
@Composable
internal fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(ForjaRadius.shapeM)
            .background(ForjaTheme.colors.surface1).padding(16.dp),
    ) {
        Text(label.uppercase(), style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
        Text(value, style = ForjaTheme.typography.figure, color = ForjaTheme.colors.text1)
    }
}

/** Fila de puntos: hechos en hielo y el actual con anillo. */
@Composable
internal fun Dots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val colors = ForjaTheme.colors
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .then(
                        when {
                            index < current -> Modifier.background(colors.ice)
                            index == current -> Modifier.border(2.dp, colors.ice, CircleShape)
                            else -> Modifier.background(colors.surface3)
                        },
                    ),
            )
        }
    }
}
