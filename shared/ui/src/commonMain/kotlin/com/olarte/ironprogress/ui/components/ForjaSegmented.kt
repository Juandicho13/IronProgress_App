package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/**
 * Control segmentado de ancho completo: todas las opciones con el mismo ancho, la elegida en
 * surface1 sobre la pista surface3. Para 2 a 4 opciones cortas.
 */
@Composable
fun <T> ForjaSegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    val colors = ForjaTheme.colors
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(colors.surface3).padding(ForjaSpacing.esp1).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier.weight(1f).heightIn(min = ForjaMeasures.minTouch - ForjaSpacing.esp2).clip(CircleShape)
                    .background(if (isSelected) colors.surface1 else colors.surface3)
                    .selectable(isSelected, enabled = enabled, role = Role.Tab) { onSelect(option) }
                    .padding(horizontal = ForjaSpacing.esp2),
                contentAlignment = Alignment.Center,
            ) {
                ForjaNumber(
                    label(option),
                    ForjaTheme.typography.label,
                    color = if (isSelected) colors.text1 else colors.text2,
                    textAlign = TextAlign.Center,
                    minSize = 10.sp,
                )
            }
        }
    }
}
