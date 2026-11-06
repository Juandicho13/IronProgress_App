package com.olarte.ironprogress.ui.discipline

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.discipline.GuidedFlow
import com.olarte.ironprogress.domain.time.TimerSegment
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.interval.clock
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.flow_breath_of
import com.olarte.ironprogress.ui.resources.flow_close
import com.olarte.ironprogress.ui.resources.flow_done
import com.olarte.ironprogress.ui.resources.flow_exhale
import com.olarte.ironprogress.ui.resources.flow_failed
import com.olarte.ironprogress.ui.resources.flow_inhale
import com.olarte.ironprogress.ui.resources.flow_last
import com.olarte.ironprogress.ui.resources.flow_left
import com.olarte.ironprogress.ui.resources.flow_next
import com.olarte.ironprogress.ui.resources.flow_next_breaths
import com.olarte.ironprogress.ui.resources.flow_next_one_breath
import com.olarte.ironprogress.ui.resources.flow_next_round
import com.olarte.ironprogress.ui.resources.flow_note
import com.olarte.ironprogress.ui.resources.flow_pause
import com.olarte.ironprogress.ui.resources.flow_resume
import com.olarte.ironprogress.ui.resources.flow_right
import com.olarte.ironprogress.ui.resources.flow_round
import com.olarte.ironprogress.ui.resources.flow_saved
import com.olarte.ironprogress.ui.resources.flow_seconds_left
import com.olarte.ironprogress.ui.resources.flow_skip
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.voice.mutedUnless
import com.olarte.ironprogress.ui.voice.rememberVoiceSpeaker
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FlowRoute(flowId: String, languageCode: String, onClose: () -> Unit) {
    val model = koinViewModel<FlowViewModel>(key = "flow-$flowId") { parametersOf(flowId, languageCode) }
    val state by model.state.collectAsStateWithLifecycle()
    val speaker = rememberVoiceSpeaker().mutedUnless(LocalAppSettings.current.voice)
    val haptics = ForjaTheme.haptics
    LaunchedEffect(model, speaker) {
        model.cues.collect { cue ->
            haptics.segmentTick()
            if (!model.state.value.muted) speaker.speak(poseSpeech(model.flow, cue.label, languageCode))
        }
    }
    FlowScreen(model.flow, state, languageCode, model::onIntent, onClose)
}

/** "Perro boca abajo", o "Estocada baja, lado izquierdo". */
private suspend fun poseSpeech(flow: GuidedFlow, label: String?, languageCode: String): String {
    val name = flow.poseFor(label)?.name?.resolve(languageCode).orEmpty()
    return when (label?.substringAfter(':', "")) {
        "left" -> name + ", " + getString(Res.string.flow_left)
        "right" -> name + ", " + getString(Res.string.flow_right)
        else -> name
    }
}

/** Flujo de yoga, pilates o movilidad (PANTALLAS 7.2). */
@Composable
fun FlowScreen(flow: GuidedFlow, state: GuidedRunState, languageCode: String, onIntent: (GuidedIntent) -> Unit, onClose: () -> Unit) {
    val colors = ForjaTheme.colors
    val plan = remember(flow) { flow.toPlan() }
    val timer = state.timer
    val index = timer?.segmentIndex ?: 0
    val segment = plan.segments[index]
    val pose = flow.poseFor(segment.label)
    val perRound = plan.segments.size / flow.rounds
    val elapsed = com.olarte.ironprogress.ui.components.rememberSmoothElapsedMs(
        timer?.elapsedInSegmentMs ?: 0L,
        running = timer != null && !timer.isPaused && !timer.isFinished,
        limitMs = timer?.segmentDurationMs ?: Long.MAX_VALUE,
    )

    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        GuidedTopBar(
            flow.name.resolve(languageCode),
            stringResource(Res.string.flow_round, segment.round, flow.rounds),
            state.muted,
            onClose,
        ) { onIntent(GuidedIntent.ToggleMute) }

        // Segmentos de las posturas de la ronda: hechas en hielo, la actual en text1.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(perRound) { i ->
                val position = index % perRound
                val color = when {
                    state.finished || i < position -> colors.ice
                    i == position -> colors.text1
                    else -> colors.surface3
                }
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(color))
            }
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(372f / 240f).clip(ForjaRadius.shapeL).background(colors.surface1)
                .background(Brush.radialGradient(listOf(colors.iceVeil, colors.surface1))),
        ) {
            Crossfade(targetState = pose?.figure, animationSpec = tween(450), label = "pose") { figure ->
                PoseFigureCanvas(PoseFigures.find(figure), colors.ice, colors.surface3, Modifier.fillMaxSize())
            }
        }

        if (state.finished) {
            Finished(state, onClose)
            return@Column
        }

        Column(
            Modifier.fillMaxWidth().semantics {
                liveRegion = LiveRegionMode.Polite
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                pose?.name?.resolve(languageCode).orEmpty(),
                style = ForjaTheme.typography.title1.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
                color = colors.text1,
                textAlign = TextAlign.Center,
            )
            pose?.sanskrit?.let { Text(it, style = ForjaTheme.typography.body.copy(fontStyle = FontStyle.Italic), color = colors.text2) }
        }

        val breaths = pose?.breaths
        if (breaths != null) {
            BreathGuide(elapsed)
            val current = (elapsed / BREATH_MS).toInt().coerceAtMost(breaths - 1)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Dots(breaths, current)
                Text(
                    stringResource(Res.string.flow_breath_of, current + 1, breaths),
                    style = ForjaTheme.typography.caption,
                    color = colors.text2,
                )
            }
        } else {
            ForjaNumber(
                stringResource(Res.string.flow_seconds_left, clock((timer?.displaySecondsLeft ?: 0L).toInt())),
                ForjaTheme.typography.largeFigure,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        InfoBox(stringResource(Res.string.flow_next), nextText(flow, plan.segments, index, languageCode), Modifier.fillMaxWidth())
        Text(stringResource(Res.string.flow_note), style = ForjaTheme.typography.caption, color = colors.text2)

        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                if (state.paused) ForjaIcons.PlayFilled else ForjaMoreIcons.Pause,
                stringResource(if (state.paused) Res.string.flow_resume else Res.string.flow_pause),
            ) { onIntent(GuidedIntent.TogglePause) }
            PillButton(stringResource(Res.string.flow_skip), primary = true, modifier = Modifier.weight(1f)) { onIntent(GuidedIntent.Skip) }
        }
    }
}

@Composable
private fun nextText(flow: GuidedFlow, segments: List<TimerSegment>, index: Int, languageCode: String): String {
    val next = segments.getOrNull(index + 1) ?: return stringResource(Res.string.flow_last)
    val pose = flow.poseFor(next.label)
    val name = pose?.name?.resolve(languageCode).orEmpty()
    val breaths = pose?.breaths
    return when {
        next.round != segments[index].round -> stringResource(Res.string.flow_next_round, name, next.round)
        breaths == 1 -> stringResource(Res.string.flow_next_one_breath, name)
        breaths != null -> stringResource(Res.string.flow_next_breaths, name, breaths)
        else -> name
    }
}

/**
 * Círculo de 64 que crece al inhalar y se encoge al exhalar, en ciclos de 8 s con curva suave.
 * Sale del tiempo del motor: con pausa se congela solo.
 */
@Composable
private fun BreathGuide(elapsedMs: Long) {
    val colors = ForjaTheme.colors
    val inCycle = (elapsedMs % BREATH_MS).toFloat() / BREATH_MS
    val inhaling = inCycle < 0.5f
    val progress = FastOutSlowInEasing.transform(if (inhaling) inCycle * 2 else (inCycle - 0.5f) * 2)
    val scale = if (inhaling) 0.55f + 0.45f * progress else 1f - 0.45f * progress
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).border(2.dp, colors.ice, CircleShape), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(64.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                    .clip(CircleShape).background(colors.ice.copy(alpha = 0.28f)),
            )
        }
        Text(
            stringResource(if (inhaling) Res.string.flow_inhale else Res.string.flow_exhale),
            style = ForjaTheme.typography.title1.copy(fontSize = 32.sp, lineHeight = 40.sp),
            color = colors.ice,
            modifier = Modifier.padding(start = ForjaSpacing.esp4),
        )
    }
}

@Composable
internal fun Finished(state: GuidedRunState, onClose: () -> Unit) {
    val colors = ForjaTheme.colors
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.flow_done), style = ForjaTheme.typography.title1, color = colors.text1)
        Text(
            stringResource(if (state.failed) Res.string.flow_failed else Res.string.flow_saved),
            style = ForjaTheme.typography.body,
            color = if (state.failed) colors.danger else colors.text2,
        )
        PillButton(stringResource(Res.string.flow_close), primary = true, modifier = Modifier.fillMaxWidth(), onClick = onClose)
    }
}

private const val BREATH_MS = GuidedFlow.BREATH_SECONDS * 1000L
