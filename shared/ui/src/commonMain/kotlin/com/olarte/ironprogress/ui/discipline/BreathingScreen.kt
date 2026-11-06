package com.olarte.ironprogress.ui.discipline

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.interval.StepButton
import com.olarte.ironprogress.ui.interval.clock
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.breath_box
import com.olarte.ironprogress.ui.resources.breath_coherent
import com.olarte.ironprogress.ui.resources.breath_cycle_of
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
import com.olarte.ironprogress.ui.resources.flow_pause
import com.olarte.ironprogress.ui.resources.flow_resume
import com.olarte.ironprogress.ui.resources.guided_summary_cycles
import com.olarte.ironprogress.ui.resources.guided_summary_minutes
import com.olarte.ironprogress.ui.resources.guided_summary_pattern
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.voice.rememberWorkoutVoice
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeSource

@Composable
fun BreathingRoute(onClose: () -> Unit) {
    val label = stringResource(Res.string.breath_title)
    val model = koinViewModel<BreathingViewModel> { parametersOf(label) }
    val state by model.state.collectAsStateWithLifecycle()
    val choice by model.choice.collectAsStateWithLifecycle()
    val speaker = rememberWorkoutVoice()
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

/** Respiración guiada (PANTALLAS 7.3). Empezar arranca directo, sin pregunta previa. */
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
    val cycles = pattern.cyclesFor(choice.minutes)
    val totalMs = cycles * pattern.cycleSeconds * 1000L
    val leftMs = (totalMs - (timer?.totalElapsedMs ?: 0L)).coerceAtLeast(0)

    if (state.finished) {
        val done = when {
            timer == null -> 0
            timer.isFinished -> cycles
            else -> (timer.round - 1).coerceAtLeast(0)
        }
        GuidedSummary(
            stringResource(Res.string.breath_title),
            listOf(
                SummaryStat(stringResource(Res.string.guided_summary_minutes), summaryClock(timer?.totalElapsedMs ?: 0L)),
                SummaryStat(stringResource(Res.string.guided_summary_cycles), "$done"),
                SummaryStat(stringResource(Res.string.guided_summary_pattern), stringResource(pattern.labelRes())),
            ),
            state,
            onClose,
        )
        return
    }

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
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
        ) {
            if (!state.started) {
                ForjaSegmented(BreathingPattern.all, pattern, { stringResource(it.labelRes()) }, onSelect = onChoose)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepButton("−", choice.minutes > 1) { onMinutes(-1) }
                    Column(Modifier.widthIn(min = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ForjaNumber("${choice.minutes}", ForjaTheme.typography.largeFigure, textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.breath_minutes), style = ForjaTheme.typography.caption, color = colors.text2)
                    }
                    StepButton("+", choice.minutes < 30) { onMinutes(1) }
                }
            } else {
                Text(stringResource(pattern.labelRes()), style = ForjaTheme.typography.label, color = colors.ice)
            }

            // La figura ocupa todo el espacio que queda, centrada en vertical y en horizontal.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
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
            }

            when {
                state.started -> Text(
                    stringResource(Res.string.breath_cycle_of, (timer?.round ?: 1).coerceAtMost(cycles), cycles),
                    style = ForjaTheme.typography.title3,
                    color = colors.text1,
                    textAlign = TextAlign.Center,
                )
                else -> Text(
                    stringResource(Res.string.breath_saves),
                    style = ForjaTheme.typography.caption,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3)) {
            if (!state.started) {
                PillButton(stringResource(Res.string.breath_start), primary = true, modifier = Modifier.fillMaxWidth()) {
                    onIntent(GuidedIntent.Start)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        if (state.paused) ForjaIcons.PlayFilled else ForjaMoreIcons.Pause,
                        stringResource(if (state.paused) Res.string.flow_resume else Res.string.flow_pause),
                        size = ForjaMeasures.primaryButtonHeight,
                    ) { onIntent(GuidedIntent.TogglePause) }
                    PillButton(stringResource(Res.string.breath_finish), primary = true, modifier = Modifier.weight(1f)) {
                        onIntent(GuidedIntent.Finish)
                    }
                }
            }
        }
    }
}

/** Dónde empieza cada fase dentro del ciclo, en ms. */
private fun BreathingPattern.phaseOffsetMs(kind: SegmentKind): Long = 1000L * when (kind) {
    SegmentKind.INHALE -> 0
    SegmentKind.HOLD_IN -> inhale
    SegmentKind.EXHALE -> inhale + holdIn
    else -> inhale + holdIn + exhale
}

/**
 * Lo que va del ciclo, en ms, recalculado en cada cuadro. El motor da la lectura cada 100 ms; entre
 * lecturas se suma lo que pasó y se da la vuelta al llegar al final del ciclo, así la figura nunca
 * se detiene en los cambios de fase ni espera a la siguiente lectura.
 */
@Composable
private fun rememberCycleMs(readingMs: Long, cycleMs: Long, running: Boolean): Long {
    val mark = remember(readingMs) { TimeSource.Monotonic.markNow() }
    var value by remember { mutableLongStateOf(readingMs) }
    LaunchedEffect(readingMs, cycleMs, running) {
        value = readingMs % cycleMs
        if (running) {
            while (true) {
                withFrameNanos { value = (readingMs + mark.elapsedNow().inWholeMilliseconds) % cycleMs }
            }
        }
    }
    return value
}

/**
 * Cuadrada: un punto da una vuelta completa al borde en cada ciclo (arriba inhala, derecha retén,
 * abajo exhala, izquierda retén) y deja el recorrido pintado detrás, como un anillo que se llena.
 * Las otras: un círculo que crece al inhalar, se queda al retener y baja al exhalar, con suavizado,
 * y un anillo fino alrededor que se llena en cada ciclo. Todo sale del tiempo del motor, así la pausa
 * lo congela, y se dibuja en cada cuadro.
 */
@Composable
private fun BreathFigure(pattern: BreathingPattern, timer: TimerState?) {
    val colors = ForjaTheme.colors
    val cycleMs = pattern.cycleSeconds * 1000L
    val reading = timer?.let { pattern.phaseOffsetMs(it.kind) + it.elapsedInSegmentMs } ?: 0L
    val running = timer != null && !timer.isPaused && !timer.isFinished
    val t = rememberCycleMs(reading, cycleMs, running)
    val lap = if (timer == null) 0f else (t.toFloat() / cycleMs).coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 8.dp.toPx()
        if (pattern == BreathingPattern.BOX) {
            val side = size.minDimension * 0.84f
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val radius = 32.dp.toPx()
            drawRoundRect(colors.surface3, topLeft, Size(side, side), CornerRadius(radius), style = Stroke(stroke))
            val left = topLeft.x
            val top = topLeft.y
            val right = left + side
            val bottom = top + side
            val d = radius * 2
            // Sentido horario desde la esquina de arriba a la izquierda: cada cuarto es un lado con su esquina.
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
            val end = length * lap
            val traced = Path()
            if (end > 0f) measure.getSegment(0f, end, traced, true)
            drawPath(traced, colors.ice, style = Stroke(stroke, cap = StrokeCap.Round))
            val dot = measure.getPosition(end.coerceIn(0f, length - 0.01f))
            drawCircle(colors.ice.copy(alpha = 0.35f), radius = 18.dp.toPx(), center = dot)
            drawCircle(colors.text1, radius = 9.dp.toPx(), center = dot)
        } else {
            val ringRadius = size.minDimension / 2f - stroke
            val maxRadius = ringRadius - 14.dp.toPx()
            val inMs = pattern.inhale * 1000f
            val holdInMs = pattern.holdIn * 1000f
            val outMs = pattern.exhale * 1000f
            val ms = t.toFloat()
            val ease = FastOutSlowInEasing
            val scale = if (timer == null) {
                MIN_SCALE
            } else {
                when {
                    ms < inMs -> MIN_SCALE + (1f - MIN_SCALE) * ease.transform(ms / inMs)
                    ms < inMs + holdInMs -> 1f
                    ms < inMs + holdInMs + outMs -> 1f - (1f - MIN_SCALE) * ease.transform((ms - inMs - holdInMs) / outMs)
                    else -> MIN_SCALE
                }
            }
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(colors.surface3, radius = ringRadius, center = center, style = Stroke(stroke / 2f))
            if (lap > 0f) {
                drawArc(
                    colors.ice,
                    startAngle = -90f,
                    sweepAngle = 360f * lap,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(stroke / 2f, cap = StrokeCap.Round),
                )
                val angle = (-90.0 + 360.0 * lap) * PI / 180.0
                val dot = Offset(center.x + ringRadius * cos(angle).toFloat(), center.y + ringRadius * sin(angle).toFloat())
                drawCircle(colors.text1, radius = 6.dp.toPx(), center = dot)
            }
            drawCircle(colors.iceVeil, radius = maxRadius * scale, center = center)
            drawCircle(colors.ice, radius = maxRadius * scale, center = center, style = Stroke(2.dp.toPx()))
        }
    }
}

private const val MIN_SCALE = 0.55f
