package com.olarte.ironprogress.ui.interval

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.interval_breathe
import com.olarte.ironprogress.ui.resources.interval_close
import com.olarte.ironprogress.ui.resources.interval_default_hint
import com.olarte.ironprogress.ui.resources.interval_done
import com.olarte.ironprogress.ui.resources.interval_done_sub
import com.olarte.ironprogress.ui.resources.interval_how_many
import com.olarte.ironprogress.ui.resources.interval_how_many_plain
import com.olarte.ironprogress.ui.resources.interval_last_time
import com.olarte.ironprogress.ui.resources.interval_mute
import com.olarte.ironprogress.ui.resources.interval_next_up
import com.olarte.ironprogress.ui.resources.interval_not_found
import com.olarte.ironprogress.ui.resources.interval_of
import com.olarte.ironprogress.ui.resources.interval_pause
import com.olarte.ironprogress.ui.resources.interval_paused
import com.olarte.ironprogress.ui.resources.interval_phase_rest
import com.olarte.ironprogress.ui.resources.interval_phase_work
import com.olarte.ironprogress.ui.resources.interval_resume
import com.olarte.ironprogress.ui.resources.interval_round
import com.olarte.ironprogress.ui.resources.interval_saved
import com.olarte.ironprogress.ui.resources.interval_saving_failed
import com.olarte.ironprogress.ui.resources.interval_skip_rest
import com.olarte.ironprogress.ui.resources.interval_skip_work
import com.olarte.ironprogress.ui.resources.interval_summary_line
import com.olarte.ironprogress.ui.resources.interval_unmute
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.voice.mutedUnless
import com.olarte.ironprogress.ui.voice.rememberVoiceSpeaker
import com.olarte.ironprogress.ui.voice.text
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun IntervalRunRoute(id: String, languageCode: String, onClose: () -> Unit) {
    val model = koinViewModel<IntervalRunViewModel>(key = "interval-run-$id") { parametersOf(id, languageCode) }
    val state by model.state.collectAsStateWithLifecycle()
    val speaker = rememberVoiceSpeaker().mutedUnless(LocalAppSettings.current.voice)
    var subtitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(model, speaker) {
        model.voiceLines.collect { lines ->
            val names = model.state.value.names
            val duck = model.state.value.template?.voice?.duckMusic ?: true
            lines.forEach { line ->
                val text = line.text { names[it] }
                subtitle = text
                speaker.speak(text, duck)
            }
        }
    }
    // El subtítulo se va solo a los 2,6 s.
    LaunchedEffect(subtitle) {
        if (subtitle != null) {
            delay(SUBTITLE_MS)
            subtitle = null
        }
    }
    IntervalRunScreen(state, subtitle, model::onIntent, onClose)
}

/** Intervalos con voz (PANTALLAS 5.3). */
@Composable
fun IntervalRunScreen(state: IntervalRunState, subtitle: String?, onIntent: (IntervalRunIntent) -> Unit, onClose: () -> Unit) {
    val colors = ForjaTheme.colors
    val type = ForjaTheme.typography
    val template = state.template
    val timer = state.timer
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.notFound) {
            Text(stringResource(Res.string.interval_not_found), style = type.body, color = colors.text2)
            TextAction(stringResource(Res.string.interval_close), onClick = onClose)
            return@Column
        }
        if (template == null || timer == null) return@Column

        val elapsed = (timer.totalElapsedMs / 1000).toInt()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(Res.string.interval_close), colors.text2, onClick = onClose)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(template.name, style = type.title3, color = colors.text1)
                Text(
                    stringResource(Res.string.interval_of, clock(elapsed), clock(template.totalSeconds)),
                    style = type.caption,
                    color = colors.text2,
                )
            }
            TextAction(
                stringResource(if (state.muted) Res.string.interval_unmute else Res.string.interval_mute),
                colors.text2,
            ) { onIntent(IntervalRunIntent.ToggleMute) }
        }

        if (state.finished) {
            Finished(state)
            TextAction(stringResource(Res.string.interval_close), onClick = onClose)
            return@Column
        }

        RoundSegments(template.rounds, timer.round, timer.kind == SegmentKind.REST)
        Text(stringResource(Res.string.interval_round, timer.round, template.rounds), style = type.caption, color = colors.text2)

        val working = timer.kind == SegmentKind.WORK
        val phaseColor = if (working) colors.ember else colors.ice
        Text(
            stringResource(if (working) Res.string.interval_phase_work else Res.string.interval_phase_rest),
            modifier = Modifier.timerPhase(),
            style = type.label,
            color = phaseColor,
        )
        val exerciseName = timer.label?.let { state.names[it] }
        Text(
            if (working) exerciseName.orEmpty() else stringResource(Res.string.interval_breathe),
            style = type.title1,
            color = colors.text1,
        )

        Ring(timer, phaseColor)

        if (timer.isPaused) Text(stringResource(Res.string.interval_paused), style = type.body, color = colors.text2)
        if (subtitle != null) {
            Box(
                Modifier.height(40.dp).clip(CircleShape).background(colors.surface2).padding(horizontal = ForjaSpacing.esp4),
                contentAlignment = Alignment.Center,
            ) {
                Text(subtitle, style = type.body2, color = colors.text1)
            }
        }

        if (working) {
            timer.label?.let { id ->
                state.lastTime[id]?.let { last ->
                    Text(stringResource(Res.string.interval_last_time, last), style = type.body2, color = colors.text2)
                }
            }
        } else {
            state.roundToLog?.let { round -> RepsCard(state, round) { onIntent(IntervalRunIntent.LogReps(round, it)) } }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4)) {
            TextAction(
                stringResource(if (timer.isPaused) Res.string.interval_resume else Res.string.interval_pause),
                colors.text1,
            ) { onIntent(IntervalRunIntent.TogglePause) }
            TextAction(
                stringResource(if (working) Res.string.interval_skip_work else Res.string.interval_skip_rest),
                colors.text1,
                enabled = !timer.isPaused,
            ) { onIntent(IntervalRunIntent.Skip) }
        }
    }
}

@Composable
private fun RoundSegments(rounds: Int, current: Int, resting: Boolean) {
    val colors = ForjaTheme.colors
    Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (round in 1..rounds) {
            val done = round < current || (round == current && resting)
            val color = when {
                done -> colors.ember
                round == current -> colors.text1
                else -> colors.surface3
            }
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(color))
        }
    }
}

/** Anillo de 300 dp (grosor 14) que se vacía con el tramo; en los últimos 5 s de trabajo, el número solo. */
@Composable
private fun Ring(timer: com.olarte.ironprogress.domain.time.TimerState, color: Color) {
    val colors = ForjaTheme.colors
    val duration = timer.segmentDurationMs ?: 1L
    val left = timer.remainingInSegmentMs ?: 0L
    val target = (left.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, tween(durationMillis = 120, easing = LinearEasing))
    val seconds = timer.displaySecondsLeft?.toInt() ?: 0
    val track = colors.surface3
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (timer.kind == SegmentKind.WORK && seconds in 1..5) {
            ForjaNumber(
                seconds.toString(),
                ForjaTheme.typography.heroFigure.copy(fontSize = 156.sp, lineHeight = 160.sp, fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
            )
        } else {
            ForjaNumber(
                clock(seconds),
                ForjaTheme.typography.heroFigure,
                Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RepsCard(state: IntervalRunState, round: Int, onPick: (Int) -> Unit) {
    val colors = ForjaTheme.colors
    val template = state.template ?: return
    val exerciseId = template.exerciseFor(round)?.value
    val name = exerciseId?.let { state.names[it] }
    val guess = state.guessFor(round)
    val chosen = state.reps.getOrNull(round - 1)
    val last = exerciseId?.let { state.lastTime[it] } ?: guess
    Column(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Text(
            if (name != null) stringResource(Res.string.interval_how_many, name) else stringResource(Res.string.interval_how_many_plain),
            style = ForjaTheme.typography.body,
            color = colors.text1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ((last - 2).coerceAtLeast(0)..(last + 2)).forEach { value ->
                val selected = chosen == value
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (selected) colors.ember else colors.surface3)
                        .then(if (value == last && !selected) Modifier.border(2.dp, colors.ember, CircleShape) else Modifier)
                        .clickable { onPick(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(value.toString(), style = ForjaTheme.typography.label, color = if (selected) colors.onEmber else colors.text1)
                }
            }
        }
        if (chosen == null) {
            Text(stringResource(Res.string.interval_default_hint, guess), style = ForjaTheme.typography.caption, color = colors.text2)
        }
        template.exerciseFor(round + 1)?.value?.let { next ->
            state.names[next]?.let {
                Text(stringResource(Res.string.interval_next_up, it), style = ForjaTheme.typography.body2, color = colors.text2)
            }
        }
    }
}

@Composable
private fun Finished(state: IntervalRunState) {
    val colors = ForjaTheme.colors
    val template = state.template ?: return
    Box(Modifier.size(220.dp).clip(CircleShape).border(14.dp, colors.gold, CircleShape), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.interval_done), style = ForjaTheme.typography.title1, color = colors.gold)
    }
    Text(
        stringResource(Res.string.interval_done_sub, template.rounds, clock(template.totalSeconds)),
        style = ForjaTheme.typography.body,
        color = colors.text2,
    )
    template.exerciseIds.distinct().forEach { id ->
        val rounds = (1..template.rounds).filter { template.exerciseFor(it) == id }
        val total = rounds.sumOf { state.reps.getOrNull(it - 1) ?: 0 }
        Text(
            stringResource(Res.string.interval_summary_line, state.names[id.value] ?: id.value, total, rounds.size),
            style = ForjaTheme.typography.body,
            color = colors.text1,
        )
    }
    when {
        state.saved -> Text(stringResource(Res.string.interval_saved), style = ForjaTheme.typography.caption, color = colors.text2)
        state.failed -> Text(
            stringResource(Res.string.interval_saving_failed),
            style = ForjaTheme.typography.caption,
            color = colors.danger,
        )
    }
}

private const val SUBTITLE_MS = 2_600L
