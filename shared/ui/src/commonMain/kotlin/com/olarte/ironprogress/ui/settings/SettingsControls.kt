package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/** Etiqueta de sección de PANTALLAS 0: caption en mayúsculas, text2. */
@Composable
internal fun SectionLabel(text: String) {
    Text(text.uppercase(), style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
}

/** Fila de chips de una sola opción (o varias con [multi]); la elegida en emberVeil con borde. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceChips(options: List<T>, isSelected: (T) -> Boolean, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    val colors = ForjaTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        options.forEach { option ->
            val selected = isSelected(option)
            Box(
                Modifier.heightIn(min = ForjaMeasures.minTouch).clip(CircleShape)
                    .background(if (selected) colors.emberVeil else colors.surface2)
                    .then(if (selected) Modifier.border(1.dp, colors.ember, CircleShape) else Modifier)
                    .semantics { this.selected = selected }
                    .clickable(role = Role.RadioButton) { onPick(option) }
                    .padding(horizontal = ForjaSpacing.esp4),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = ForjaTheme.typography.label, color = if (selected) colors.ember else colors.text1)
            }
        }
    }
}

/** Fila con interruptor en una tarjeta; toda la fila responde al toque. */
@Composable
internal fun ToggleRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = ForjaTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = ForjaTheme.typography.body, color = colors.text1)
            hint?.let { Text(it, style = ForjaTheme.typography.caption, color = colors.text2) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.ember, checkedThumbColor = colors.onEmber),
        )
    }
}

/** Tarjeta surface1 de radio 28 y relleno 16 (PANTALLAS 0). */
@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(ForjaTheme.colors.surface1).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) { content() }
}

/** Barra superior: botón redondo de 48 a la izquierda y título centrado (17/22). */
@Composable
internal fun TopBar(title: String, icon: Painter, iconDescription: String, onIcon: () -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(ForjaMeasures.minTouch).clip(CircleShape).background(ForjaTheme.colors.surface3)
                .semantics { contentDescription = iconDescription }
                .clickable(role = Role.Button, onClick = onIcon),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ForjaTheme.colors.text1, modifier = Modifier.size(ForjaMeasures.iconSize))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, Modifier.semantics { heading() }, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
            subtitle?.let { Text(it, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2) }
        }
        Spacer(Modifier.size(ForjaMeasures.minTouch))
    }
}

/** Botón principal de PANTALLAS 0: alto 56, radio completo, ember. Uno por pantalla. */
@Composable
internal fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        modifier.fillMaxWidth().heightIn(min = ForjaMeasures.primaryButtonHeight).clip(CircleShape)
            .background(if (enabled) colors.ember else colors.surface3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.primaryButton, color = if (enabled) colors.onEmber else colors.text3)
    }
}
