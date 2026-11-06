package com.olarte.ironprogress.ui.discipline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.discipline.BreathingPattern
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.TimerState
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.interval.StepButton
import com.olarte.ironprogress.ui.interval.clock
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.breath_box
import com.olarte.ironprogress.ui.resources.breath_coherent
import com.olarte.ironprogress.ui.resources.breath_confirm_cancel
import com.olarte.ironprogress.ui.resources.breath_confirm_ok
import com.olarte.ironprogress.ui.resources.breath_confirm_title
import com.olarte.ironprogress.ui.resources.breath_cycle
import com.olarte.ironprogress.ui.resources.breath_exhale
import com.olarte.ironprogress.ui.resources.breath_finish
import com.olarte.ironprogress.ui.resources.breath_hold
import com.olarte.ironprogress.ui.resources.breath_inhale
import com.olarte.ironprogress.ui.resources.breath_left
import com.olarte.ironprogress.ui.resources.breath_minutes
import com.olarte.ironprogress.ui.resources.breath_relax
import com.olarte.ironprogress.ui.resources.breath_saves
import com.olarte.ironprogress.ui.resources.breath_start
import com.olarte.ironprogress.ui.resources.breath_title
import com.olarte.ironprogress.ui.resources.breath_warning
import com.olarte.ironprogress.ui.resources.flow_pause
import com.olarte.ironprogress.ui.resources.flow_resume
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.voice.mutedUnless
import com.olarte.ironprogress.ui.voice.rememberVoiceSpeaker
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BreathingRoute(onClose: () -> Unit) {
    val label = stringResource(Res.string.breath_title)
    val model = koinViewModel<BreathingViewModel> { parametersOf(label) }
    val state by model.state.collectAsStateWithLifecycle()
    val choice by model.choice.collectAsStateWithLifecycle()
    val speaker = rememberVoiceSpeaker().mutedUnless(LocalAppSettings.current.voice)
    val haptics = ForjaTheme.haptics
    LaunchedEffect(model, speaker) {
        model.cues.collect { cue ->
            haptics.segmentTick()
            if (!model.state.value.muted) speaker.speak(getString(cue.kind.phaseText()))
        }
    }
    BreathingScreen(state, choice, model::onIntent, model::choose, model::changeMinutes, onClose)
}

private fun SegmentKind.phaseText(): StringResource = when (this) {
    SegmentKind.INHALE -> Res.string.breath_inhale
    SegmentKind.EXHALE -> Res.string.breath_exhale
    else -> Res.string.breath_hold
}

private fun BreathingPattern.labelRes(): StringResource = when (this) {
    BreathingPattern.RELAXING -> Res.string.breath_relax
    BreathingPattern.COHERENT -> Res.string.breath_coherent
    else -> Res.string.breath_box
}

/** Respiración guiada (PANTALLAS 7.3). */
@Composable
fun BreathingScreen(
    state: GuidedRunState,
    choice: BreathingChoice,
    onIntent: (GuidedIntent) -> Unit,
    onChoose: (BreathingPattern) -> Unit,
    onMinutes: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val timer = state.timer
    val pattern = choice.pattern
    val totalMs = pattern.cyclesFor(choice.minutes) * pattern.cycleSeconds * 1000L
    val leftMs = (totalMs - (timer?.totalElapsedMs ?: 0L)).coerceAtLeast(0)
    var confirming by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(colors.background)
            .background(Brush.radialGradient(listOf(colors.iceVeil, colors.background)))
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Box(Modifier.padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3)) {
            GuidedTopBar(
                stringResource(Res.string.breath_title),
                stringResource(Res.string.breath_left, clock(((leftMs + 999) / 1000).toInt())),
                state.muted,
                onClose,
            ) { onIntent(GuidedIntent.ToggleMute) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
        ) {
            if (!state.started) {
                ForjaSegmented(BreathingPattern.all, pattern, { stringResource(it.labelRes()) }, onSelect = onChoose)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.breath_minutes), style = ForjaTheme.typography.caption, color = colors.text2)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4)) {
                        StepButton("−", choice.minutes > 1) { onMinutes(-1) }
                        ForjaNumber(
                            "${choice.minutes}",
                            ForjaTheme.typography.largeFigure,
                            Modifier.widthIn(min = 64.dp),
                            textAlign = TextAlign.Center,
                        )
                        StepButton("+", choice.minutes < 30) { onMinutes(1) }
                    }
                }
            } else {
                Text(stringResource(pattern.labelRes()), style = ForjaTheme.typography.label, color = colors.ice)
            }

            Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                BreathFigure(pattern, timer)
                // Solo la fase es región viva: los segundos de abajo no se anuncian cada segundo.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource((timer?.kind ?: SegmentKind.INHALE).phaseText()),
                        modifier = Modifier.timerPhase(),
                        style = ForjaTheme.typography.title1.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = colors.ice,
                        maxLines = 1,
                    )
                    ForjaNumber(
                        "${timer?.displaySecondsLeft ?: pattern.inhale}",
                        ForjaTheme.typography.heroFigure.copy(fontSize = 72.sp, lineHeight = 76.sp),
                        Modifier.clearAndSetSemantics {},
                    )
                }
            }

            if (state.finished) {
                Finished(state, onClose)
                return@Column
            }

            if (state.started) {
                InfoBox(stringResource(Res.string.breath_cycle), "${timer?.round ?: 1}", Modifier.fillMaxWidth())
            } else {
                Text(
                    stringResource(Res.string.breath_saves),
                    style = ForjaTheme.typography.caption,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (!state.finished) {
            Box(Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3)) {
                if (!state.started) {
                    PillButton(stringResource(Res.string.breath_start), primary = true, modifier = Modifier.fillMaxWidth()) {
                        if (pattern.warnsBeforeStart) confirming = true else onIntent(GuidedIntent.Start)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3), verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(
                            if (state.paused) ForjaIcons.PlayFilled else ForjaMoreIcons.Pause,
                            stringResource(if (state.paused) Res.string.flow_resume else Res.string.flow_pause),
                        ) { onIntent(GuidedIntent.TogglePause) }
                        PillButton(stringResource(Res.string.breath_finish), primary = true, modifier = Modifier.weight(1f)) {
                            onIntent(GuidedIntent.Finish)
                        }
                    }
                }
            }
        }
    }

    if (confirming) {
        ForjaDialog(
            title = stringResource(Res.string.breath_confirm_title),
            text = stringResource(Res.string.breath_warning),
            onDismiss = { confirming = false },
            confirmText = stringResource(Res.string.breath_confirm_ok),
            onConfirm = {
                confirming = false
                onIntent(GuidedIntent.Start)
            },
            dismissText = stringResource(Res.string.breath_confirm_cancel),
        )
    }
}

/**
 * Cuadrada: el lado de la fase en hielo y un punto que recorre el borde a velocidad constante.
 * Las otras: un círculo que crece hasta 1 al inhalar, se queda al retener y baja a 0,55 al exhalar.
 * Todo sale del tiempo del motor, así la pausa lo congela.
 */
@Composable
private fun BreathFigure(pattern: BreathingPattern, timer: TimerState?) {
    val colors = ForjaTheme.colors
    val kind = timer?.kind ?: SegmentKind.INHALE
    val duration = timer?.segmentDurationMs ?: 1L
    val progress = if (timer == null) 0f else (timer.elapsedInSegmentMs.toFloat() / duration).coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxSize()) {
        val side = size.minDimension * 0.8f
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        if (pattern == BreathingPattern.BOX) {
            val stroke = 8.dp.toPx()
            val radius = 28.dp.toPx()
            drawRoundRect(colors.surface3, topLeft, Size(side, side), CornerRadius(radius), style = Stroke(stroke))
            val sideIndex = when (kind) {
                SegmentKind.INHALE -> 0
                SegmentKind.HOLD_IN -> 1
                SegmentKind.EXHALE -> 2
                else -> 3
            }
            // El camino arranca en el centro del lado de arriba a la izquierda y va en sentido horario;
            // cada fase recorre un cuarto del borde.
            val left = topLeft.x
            val top = topLeft.y
            val right = left + side
            val bottom = top + side
            val d = radius * 2
            // Sentido horario desde arriba a la izquierda: cada cuarto es un lado con su esquina.
            val outline = Path().apply {
                moveTo(left + radius, top)
                lineTo(right - radius, top)
                arcTo(Rect(right - d, top, right, top + d), -90f, 90f, false)
                lineTo(right, bottom - radius)
                arcTo(Rect(right - d, bottom - d, right, bottom), 0f, 90f, false)
                lineTo(left + radius, bottom)
                arcTo(Rect(left, bottom - d, left + d, bottom), 90f, 90f, false)
                lineTo(left, top + radius)
                arcTo(Rect(left, top, left + d, top + d), 180f, 90f, false)
                close()
            }
            val measure = PathMeasure().apply { setPath(outline, true) }
            val length = measure.length
            val start = length * sideIndex / 4f
            val end = start + length / 4f * progress
            val traced = Path()
            if (end > start) measure.getSegment(start, end, traced, true)
            drawPath(traced, colors.ice, style = Stroke(stroke, cap = StrokeCap.Round))
            val dot = measure.getPosition(end.coerceAtMost(length - 0.01f))
            drawCircle(colors.ice.copy(alpha = 0.35f), radius = 18.dp.toPx(), center = dot)
            drawCircle(colors.text1, radius = 9.dp.toPx(), center = dot)
        } else {
            val scale = when (kind) {
                SegmentKind.INHALE -> 0.55f + 0.45f * progress
                SegmentKind.HOLD_IN -> 1f
                SegmentKind.EXHALE -> 1f - 0.45f * progress
                else -> 0.55f
            }
            drawCircle(colors.iceVeil, radius = side / 2f * scale)
            drawCircle(colors.ice, radius = side / 2f * scale, style = Stroke(2.dp.toPx()))
        }
    }
}
