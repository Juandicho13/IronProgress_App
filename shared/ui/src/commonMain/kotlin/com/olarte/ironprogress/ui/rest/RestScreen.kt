package com.olarte.ironprogress.ui.rest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rest_expand
import com.olarte.ironprogress.ui.resources.rest_less
import com.olarte.ironprogress.ui.resources.rest_minimize
import com.olarte.ironprogress.ui.resources.rest_more
import com.olarte.ironprogress.ui.resources.rest_next
import com.olarte.ironprogress.ui.resources.rest_skip
import com.olarte.ironprogress.ui.resources.rest_swipe
import com.olarte.ironprogress.ui.resources.rest_title
import com.olarte.ironprogress.ui.resources.rest_total
import com.olarte.ironprogress.ui.resources.workout_undo
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.WorkoutIntent
import com.olarte.ironprogress.ui.workout.WorkoutState
import org.jetbrains.compose.resources.stringResource

@Composable
fun RestBar(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val rest = state.rest ?: return
    val timer = rest.at(state.restNowMs)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onIntent(WorkoutIntent.AdjustRest(-15)) }) { Text(stringResource(Res.string.rest_less)) }
        Column(
            Modifier.clickable(role = Role.Button, onClickLabel = stringResource(Res.string.rest_expand)) {
                onIntent(WorkoutIntent.ExpandRest)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RestRing((timer.remainingInSegmentMs ?: 0).toFloat() / (timer.segmentDurationMs ?: 1).coerceAtLeast(1), false)
            Text(Formats.duration(timer.displaySecondsLeft ?: 0), color = ForjaTheme.colors.ice, style = ForjaTheme.typography.smallFigure)
        }
        TextButton(onClick = { onIntent(WorkoutIntent.AdjustRest(15)) }) { Text(stringResource(Res.string.rest_more)) }
        TextButton(onClick = { onIntent(WorkoutIntent.SkipRest) }) { Text(stringResource(Res.string.rest_skip)) }
    }
}

@Composable
fun RestScreen(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val rest = state.rest ?: return
    val timer = rest.at(state.restNowMs)
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val detail = state.session
    val next = detail?.sets?.firstOrNull { !it.isCompleted && it.id != rest.setId }
    val exercise = detail?.exercises?.firstOrNull { it.id == next?.sessionExerciseId }
    val name = state.catalog[exercise?.exerciseId]?.name?.resolve(appLanguage())
    val formats = rememberFormats()
    var dragOffset by remember(rest.setId) { mutableFloatStateOf(0f) }
    val dismiss = remember(onIntent) { { onIntent(WorkoutIntent.MinimizeRest) } }
    Column(
        Modifier.fillMaxSize().graphicsLayer {
            translationY = dragOffset
        }.background(colors.background).statusBarsPadding().navigationBarsPadding().pointerInput(rest.setId) {
            var distance = 0f
            detectVerticalDragGestures(onDragStart = { distance = 0f }, onVerticalDrag = { change, amount ->
                if (amount > 0 || distance > 0) {
                    distance += amount
                    dragOffset = distance.coerceAtLeast(0f)
                    change.consume()
                }
            }, onDragCancel = { dragOffset = 0f }, onDragEnd = {
                if (distance > ForjaMeasures.minTouch.toPx()) dismiss()
                dragOffset = 0f
            })
        }.verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        TextButton(onClick = dismiss) { Text(stringResource(Res.string.rest_minimize)) }
        Text(
            state.routineName ?: stringResource(Res.string.rest_title),
            modifier = Modifier.timerPhase(),
            color = colors.ice,
            style = typography.title2,
        )
        Box(contentAlignment = Alignment.Center) {
            RestRing((timer.remainingInSegmentMs ?: 0).toFloat() / (timer.segmentDurationMs ?: 1).coerceAtLeast(1), true)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Formats.duration(timer.displaySecondsLeft ?: 0), style = typography.heroFigure, color = colors.text1)
                Text(
                    stringResource(Res.string.rest_total, Formats.duration((timer.segmentDurationMs ?: 0) / 1000)),
                    style = typography.body2,
                    color = colors.text2,
                )
            }
        }
        if (name != null) {
            Text(stringResource(Res.string.rest_next), style = typography.caption, color = colors.text2)
            Text(name, style = typography.title2, color = colors.text1)
            next?.let {
                Text(
                    listOfNotNull(
                        it.weightKg?.let { value ->
                            formats.weight(value)
                        },
                        it.reps?.let { value ->
                            formats.number(value.toDouble(), 0)
                        },
                    ).joinToString(" × "),
                    style = typography.figure,
                    color = colors.text1,
                )
            }
        }
        Text(stringResource(Res.string.rest_swipe), style = typography.caption, color = colors.text3)
        RestBar(state, onIntent)
        if (state.undoSetId !=
            null
        ) {
            TextButton(onClick = { onIntent(WorkoutIntent.Undo) }) { Text(stringResource(Res.string.workout_undo)) }
        }
    }
}

@Composable
private fun RestRing(progress: Float, large: Boolean) {
    val colors = ForjaTheme.colors
    Canvas(Modifier.size(if (large) 300.dp else ForjaMeasures.restRingSize).clearAndSetSemantics {}) {
        val stroke = if (large) 12.dp.toPx() else 3.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val origin = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(colors.surface3, -90f, 360f, false, origin, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(colors.ice, -90f, 360f * progress.coerceIn(0f, 1f), false, origin, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
