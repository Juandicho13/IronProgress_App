package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/**
 * Acción de formulario. Por defecto es secundaria (surface3): la acción principal de cada pantalla
 * se pide con [style] = PRIMARY, para que no haya una pila de botones en brasa.
 */
@Composable
fun EditorAction(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    style: ForjaButtonStyle = ForjaButtonStyle.SECONDARY,
    onClick: () -> Unit,
) {
    ForjaButton(text, modifier, style, enabled, onClick = onClick)
}

@Composable
fun EditorField(label: String, value: String, onChange: (String) -> Unit) {
    ForjaTextField(label, value, onChange)
}

/**
 * Selector de una opción: fila con el valor actual que abre una hoja con las opciones. Con
 * [compact] es un chip "Equipo · Todo" para filtros en fila.
 */
@Composable
fun <T> EditorChoice(
    label: String,
    selected: T,
    options: List<T>,
    name: @Composable (T) -> String,
    compact: Boolean = false,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    if (compact) {
        Row(
            Modifier.heightIn(min = ForjaMeasures.minTouch).clip(CircleShape).background(ForjaTheme.colors.surface3)
                .clickable(role = Role.Button) { expanded = true }
                .padding(start = ForjaSpacing.esp4, end = ForjaSpacing.esp3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
        ) {
            Text("$label · ${name(selected)}", style = ForjaTheme.typography.label, color = ForjaTheme.colors.text1, maxLines = 1)
            Icon(
                ForjaIcons.ChevronRight,
                contentDescription = null,
                tint = ForjaTheme.colors.text2,
                modifier = Modifier.size(ForjaMeasures.inlineIconSize).rotate(90f),
            )
        }
    } else {
        ForjaListGroup {
            ForjaListRow(title = label, value = name(selected), onClick = { expanded = true })
        }
    }
    if (expanded) {
        ForjaSheet(label, onDismiss = { expanded = false }) {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                options.forEach { value ->
                    ForjaRadioRow(name(value), value == selected) {
                        onSelect(value)
                        expanded = false
                    }
                }
            }
        }
    }
}
