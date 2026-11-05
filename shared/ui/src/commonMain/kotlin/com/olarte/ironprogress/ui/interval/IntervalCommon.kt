package com.olarte.ironprogress.ui.interval

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/** "0:15", "9:15": los tiempos de intervalos siempre en minutos y segundos. */
internal fun clock(seconds: Int): String = Formats.duration(seconds.toLong())

/** Botón de texto de 48 dp de alto, en el color dado; se atenúa al 40 % si no está activo. */
@Composable
internal fun TextAction(text: String, color: Color = ForjaTheme.colors.ember, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(minWidth = ForjaMeasures.minTouch, minHeight = ForjaMeasures.minTouch)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = ForjaSpacing.esp3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.label, color = color)
    }
}

/** Botón redondo de 48 en surface3 para los pasos de - y +. */
@Composable
internal fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(minWidth = ForjaMeasures.minTouch, minHeight = ForjaMeasures.minTouch)
            .clip(CircleShape)
            .background(ForjaTheme.colors.surface3)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
    }
}
