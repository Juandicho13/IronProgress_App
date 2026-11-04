package com.olarte.ironprogress.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espacios base de Forja (multiplos de 4 dp). */
@Immutable
object ForjaSpacing {
    val esp0_5: Dp = 2.dp
    val esp1: Dp = 4.dp
    val esp2: Dp = 8.dp
    val esp3: Dp = 12.dp
    val esp4: Dp = 16.dp
    val esp5: Dp = 20.dp
    val esp6: Dp = 24.dp
    val esp7: Dp = 28.dp
    val esp8: Dp = 32.dp
    val esp12: Dp = 48.dp
}

/** Radios de esquina de Forja. */
@Immutable
object ForjaRadius {
    val s: Dp = 14.dp
    val m: Dp = 20.dp
    val l: Dp = 28.dp
    val xl: Dp = 36.dp

    val shapeS = RoundedCornerShape(s)
    val shapeM = RoundedCornerShape(m)
    val shapeL = RoundedCornerShape(l)
    val shapeXlTop = RoundedCornerShape(topStart = xl, topEnd = xl)
    val shapeFull = CircleShape
}

/** Medidas fijas de controles tactiles de Forja. */
@Immutable
object ForjaMeasures {
    val minTouch: Dp = 48.dp
    val primaryButtonHeight: Dp = 56.dp
    val secondaryButtonHeight: Dp = 48.dp
    val setRowHeight: Dp = 56.dp
    val keyHeight: Dp = 60.dp
    val islandHeight: Dp = 64.dp
    val islandMargin: Dp = 12.dp
    val accessoryHeight: Dp = 56.dp
    val iconSize: Dp = 24.dp
    val inlineIconSize: Dp = 20.dp
    val restRingSize: Dp = 40.dp
    val glassBlur: Dp = 24.dp
}
