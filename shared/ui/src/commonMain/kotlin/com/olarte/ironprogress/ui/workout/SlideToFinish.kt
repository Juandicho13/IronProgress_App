package com.olarte.ironprogress.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.theme.ForjaTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Deslizar para terminar (PANTALLAS 1.5): pista de 64 en surface3, pulgar de 56 en ember a 4 dp
 * del borde y estela emberVeil. Pasado el 85 % vibra y termina; si se suelta antes, vuelve.
 * Con lector de pantalla es un botón con su acción.
 */
@Composable
fun SlideToFinish(label: String, enabled: Boolean, onFinish: () -> Unit) {
    val colors = ForjaTheme.colors
    val haptics = ForjaTheme.haptics
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(64.dp).clip(CircleShape).background(colors.surface3)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (enabled) {
                    onClick {
                        onFinish()
                        true
                    }
                }
            },
    ) {
        val thumb = with(density) { 56.dp.toPx() }
        val inset = with(density) { 4.dp.toPx() }
        val max = (with(density) { maxWidth.toPx() } - thumb - inset * 2).coerceAtLeast(1f)
        Box(
            Modifier.padding(4.dp).fillMaxHeight()
                .width(with(density) { (offset.value + thumb).toDp() })
                .clip(CircleShape).background(colors.emberVeil),
        )
        Text(label, Modifier.align(Alignment.Center), style = ForjaTheme.typography.label, color = colors.text2)
        Box(
            Modifier.offset { IntOffset((inset + offset.value).roundToInt(), inset.roundToInt()) }
                .size(56.dp).clip(CircleShape).background(colors.ember)
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, max)) }
                    },
                    onDragStopped = {
                        if (offset.value >= max * 0.85f) {
                            haptics.slideThresholdReached()
                            offset.animateTo(max)
                            onFinish()
                            offset.snapTo(0f)
                        } else {
                            offset.animateTo(0f)
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ForjaIcons.ChevronRight, contentDescription = null, tint = colors.onEmber)
        }
    }
}
