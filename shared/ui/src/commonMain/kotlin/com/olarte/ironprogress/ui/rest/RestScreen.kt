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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rest_edit_next
import com.olarte.ironprogress.ui.resources.rest_expand
import com.olarte.ironprogress.ui.resources.rest_less
import com.olarte.ironprogress.ui.resources.rest_minimize
import com.olarte.ironprogress.ui.resources.rest_more
import com.olarte.ironprogress.ui.resources.rest_next
import com.olarte.ironprogress.ui.resources.rest_reps_label
import com.olarte.ironprogress.ui.resources.rest_skip_short
import com.olarte.ironprogress.ui.resources.rest_swipe
import com.olarte.ironprogress.ui.resources.rest_title
import com.olarte.ironprogress.ui.resources.rest_total
import com.olarte.ironprogress.ui.resources.workout_free
import com.olarte.ironprogress.ui.resources.workout_undo
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.WorkoutIntent
import com.olarte.ironprogress.ui.workout.WorkoutState
import org.jetbrains.compose.resources.stringResource

/**
 * Barra del descanso minimizado al pie de Entreno: anillo pequeño con el tiempo a la izquierda (abre
 * el descanso completo) y los tres controles a la derecha, del mismo alto.
 */
@Composable
fun RestBar(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val rest = state.rest ?: return
    val timer = rest.at(state.restNowMs)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Box(
            Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = stringResource(Res.string.rest_expand)) {
                onIntent(WorkoutIntent.ExpandRest)
            },
            contentAlignment = Alignment.Center,
        ) {
            RestRing((timer.remainingInSegmentMs ?: 0).toFloat() / (timer.segmentDurationMs ?: 1).coerceAtLeast(1), false)
            Text(
                Formats.duration(timer.displaySecondsLeft ?: 0),
                color = ForjaTheme.colors.ice,
                style = ForjaTheme.typography.label,
                maxLines = 1,
                softWrap = false,
            )
        }
        RestControls(onIntent, Modifier.weight(1f))
    }
}

/** -15 s, Saltar descanso y +15 s en una sola fila centrada; los tres con el mismo alto. */
@Composable
fun RestControls(onIntent: (WorkoutIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        ForjaButton(stringResource(Res.string.rest_less), Modifier.weight(1f), ForjaButtonStyle.SECONDARY) {
            onIntent(WorkoutIntent.AdjustRest(-15))
        }
        ForjaButton(stringResource(Res.string.rest_skip_short), Modifier.weight(1.3f), ForjaButtonStyle.TONAL) {
            onIntent(WorkoutIntent.SkipRest)
        }
        ForjaButton(stringResource(Res.string.rest_more), Modifier.weight(1f), ForjaButtonStyle.SECONDARY) {
            onIntent(WorkoutIntent.AdjustRest(15))
        }
    }
}

@Composable
fun RestScreen(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val rest = state.rest ?: return
    val timer = rest.at(state.restNowMs)
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val detail = state.session
    // La siguiente es la que toca en el ejercicio elegido; si ya no le quedan, la primera pendiente.
    val next = state.selectedExerciseId?.let { id -> detail?.setsOf(id)?.firstOrNull { !it.isCompleted && it.id != rest.setId } }
        ?: detail?.exercises?.sortedBy { it.sortOrder }?.firstNotNullOfOrNull { exercise ->
            detail.setsOf(exercise.id).firstOrNull { !it.isCompleted && it.id != rest.setId }
        }
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
        },
    ) {
        ForjaTopBar(
            title = state.routineName ?: stringResource(Res.string.workout_free),
            onBack = dismiss,
            backDescription = stringResource(Res.string.rest_minimize),
            backIcon = ForjaMoreIcons.ChevronLeft,
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4, Alignment.CenterVertically),
        ) {
            Text(
                stringResource(Res.string.rest_title),
                modifier = Modifier.timerPhase(),
                color = colors.ice,
                style = typography.title2,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RestRing((timer.remainingInSegmentMs ?: 0).toFloat() / (timer.segmentDurationMs ?: 1).coerceAtLeast(1), true)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Formats.duration(timer.displaySecondsLeft ?: 0),
                        modifier = Modifier.clearAndSetSemantics {},
                        style = typography.heroFigure,
                        color = colors.text1,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        stringResource(Res.string.rest_total, Formats.duration((timer.segmentDurationMs ?: 0) / 1000)),
                        style = typography.body2,
                        color = colors.text2,
                        maxLines = 1,
                    )
                }
            }
            if (name != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.rest_next), style = typography.caption, color = colors.text2)
                    Text(name, style = typography.title3, color = colors.text1, textAlign = TextAlign.Center)
                    next?.let { set ->
                        // Peso y reps de la siguiente serie: se tocan para cambiarlos sin salir del descanso.
                        Row(
                            Modifier.padding(top = ForjaSpacing.esp2),
                            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2, Alignment.CenterHorizontally),
                        ) {
                            val unit = state.unitOf(set.sessionExerciseId)
                            NextValue(
                                set.weightKg?.let { value -> formats.weight(value, unit).substringBeforeLast(' ') } ?: "-",
                                if (unit == com.olarte.ironprogress.domain.catalog.WeightUnit.LB) "lb" else "kg",
                            ) { onIntent(WorkoutIntent.OpenKeyboard(set.id, com.olarte.ironprogress.ui.workout.WorkoutField.WEIGHT)) }
                            NextValue(
                                set.reps?.let { value -> formats.number(value.toDouble(), 0) } ?: "-",
                                stringResource(Res.string.rest_reps_label),
                            ) { onIntent(WorkoutIntent.OpenKeyboard(set.id, com.olarte.ironprogress.ui.workout.WorkoutField.REPS)) }
                        }
                        Text(stringResource(Res.string.rest_edit_next), style = typography.caption, color = colors.text3)
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            RestControls(onIntent)
            if (state.undoSetId != null) {
                ForjaButton(stringResource(Res.string.workout_undo), style = ForjaButtonStyle.TEXT) { onIntent(WorkoutIntent.Undo) }
            }
            Text(stringResource(Res.string.rest_swipe), style = typography.caption, color = colors.text3, textAlign = TextAlign.Center)
        }
    }
}

/** Un número de la siguiente serie, grande y tocable, con su unidad debajo. */
@Composable
private fun NextValue(value: String, unit: String, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Column(
        Modifier.widthIn(min = 112.dp).clip(ForjaRadius.shapeM).background(colors.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = ForjaTheme.typography.figure, color = colors.text1, maxLines = 1, softWrap = false)
        Text(unit, style = ForjaTheme.typography.caption, color = colors.text2, maxLines = 1)
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
