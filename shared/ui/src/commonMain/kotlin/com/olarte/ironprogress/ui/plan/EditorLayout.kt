package com.olarte.ironprogress.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.plan_down
import com.olarte.ironprogress.ui.resources.plan_drag
import com.olarte.ironprogress.ui.resources.plan_up
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ForjaTheme.colors.surface1, ForjaRadius.shapeL).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        content = content,
    )
}

@Composable
fun ReorderActions(up: Boolean, down: Boolean, enabled: Boolean, move: (Int) -> Unit) {
    val currentMove by rememberUpdatedState(move)
    val canUp by rememberUpdatedState(up && enabled)
    val canDown by rememberUpdatedState(down && enabled)
    val threshold = with(LocalDensity.current) { ForjaMeasures.minTouch.toPx() }
    var dragged by remember { mutableStateOf(0f) }
    Text(
        stringResource(Res.string.plan_drag),
        style = ForjaTheme.typography.label,
        modifier = Modifier.fillMaxWidth().heightIn(min = ForjaMeasures.minTouch).pointerInput(Unit) {
            detectDragGesturesAfterLongPress(onDragStart = {
                dragged = 0f
            }, onDragCancel = { dragged = 0f }, onDragEnd = { dragged = 0f }) { change, amount ->
                change.consume()
                dragged += amount.y
                if (dragged > threshold && canDown) {
                    currentMove(1)
                    dragged = 0f
                }
                if (dragged < -threshold && canUp) {
                    currentMove(-1)
                    dragged = 0f
                }
            }
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        EditorAction(stringResource(Res.string.plan_up), up && enabled) { move(-1) }
        EditorAction(stringResource(Res.string.plan_down), down && enabled) { move(1) }
    }
}
