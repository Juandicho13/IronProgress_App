package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.ui.theme.ForjaTheme

/**
 * Número de Forja (tiempo, kg, reps, rondas): siempre en una línea. Si no cabe, se achica hasta
 * [minSize] en vez de partirse como "0:4" arriba y "0" abajo.
 */
@Composable
fun ForjaNumber(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = ForjaTheme.colors.text1,
    textAlign: TextAlign? = null,
    minSize: TextUnit = 12.sp,
) {
    val max = if (style.fontSize.isSp) style.fontSize else 16.sp
    Text(
        text,
        modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize.coerceAtMost(max), maxFontSize = max, stepSize = 1.sp),
    )
}

private fun TextUnit.coerceAtMost(other: TextUnit): TextUnit = if (value > other.value) other else this
