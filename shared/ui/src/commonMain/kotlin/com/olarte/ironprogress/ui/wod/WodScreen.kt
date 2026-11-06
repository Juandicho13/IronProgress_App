package com.olarte.ironprogress.ui.wod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.wod.Wod
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorField
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.interval.StepButton
import com.olarte.ironprogress.ui.interval.TextAction
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.interval_pause
import com.olarte.ironprogress.ui.resources.interval_resume
import com.olarte.ironprogress.ui.resources.interval_search
import com.olarte.ironprogress.ui.resources.wod_add_round
import com.olarte.ironprogress.ui.resources.wod_best
import com.olarte.ironprogress.ui.resources.wod_cap
import com.olarte.ironprogress.ui.resources.wod_close
import com.olarte.ironprogress.ui.resources.wod_discipline
import com.olarte.ironprogress.ui.resources.wod_finish
import com.olarte.ironprogress.ui.resources.wod_format_amrap
import com.olarte.ironprogress.ui.resources.wod_format_emom
import com.olarte.ironprogress.ui.resources.wod_format_for_time
import com.olarte.ironprogress.ui.resources.wod_format_rounds
import com.olarte.ironprogress.ui.resources.wod_load_hint
import com.olarte.ironprogress.ui.resources.wod_minute
import com.olarte.ironprogress.ui.resources.wod_minutes
import com.olarte.ironprogress.ui.resources.wod_minutes_amrap
import com.olarte.ironprogress.ui.resources.wod_minutes_emom
import com.olarte.ironprogress.ui.resources.wod_movements
import com.olarte.ironprogress.ui.resources.wod_movements_hint
import com.olarte.ironprogress.ui.resources.wod_name
import com.olarte.ironprogress.ui.resources.wod_no_cap
import com.olarte.ironprogress.ui.resources.wod_of
import com.olarte.ironprogress.ui.resources.wod_pace
import com.olarte.ironprogress.ui.resources.wod_remove
import com.olarte.ironprogress.ui.resources.wod_round
import com.olarte.ironprogress.ui.resources.wod_rounds_only
import com.olarte.ironprogress.ui.resources.wod_rounds_reps
import com.olarte.ironprogress.ui.resources.wod_save
import com.olarte.ironprogress.ui.resources.wod_save_failed
import com.olarte.ironprogress.ui.resources.wod_saved
import com.olarte.ironprogress.ui.resources.wod_start
import com.olarte.ironprogress.ui.resources.wod_tap_each
import com.olarte.ironprogress.ui.resources.wod_time
import com.olarte.ironprogress.ui.resources.wod_title
import com.olarte.ironprogress.ui.resources.wod_voice_hint
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.voice.mutedUnless
import com.olarte.ironprogress.ui.voice.rememberVoiceSpeaker
import com.olarte.ironprogress.ui.voice.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WodRoute(languageCode: String, onClose: () -> Unit) {
    val model = koinViewModel<WodViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    val speaker = rememberVoiceSpeaker().mutedUnless(LocalAppSettings.current.voice)
    LaunchedEffect(model, speaker) {
        model.voiceLines.collect { lines -> lines.forEach { speaker.speak(it.text { null }) } }
    }
    WodScreen(state, languageCode, model::onIntent, onClose)
}

/** Bloque funcional: armar y correr (PANTALLAS 7.4). */
@Composable
fun WodScreen(state: WodState, languageCode: String, onIntent: (WodIntent) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(Res.string.wod_close), ForjaTheme.colors.text2, onClick = onClose)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (state.phase == WodPhase.SETUP) stringResource(Res.string.wod_title) else title(state),
                    style = ForjaTheme.typography.title3,
                    color = ForjaTheme.colors.text1,
                )
                Text(stringResource(Res.string.wod_discipline), style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
            }
            Spacer(Modifier.size(48.dp))
        }
        when (state.phase) {
            WodPhase.SETUP -> Setup(state, languageCode, onIntent)
            else -> Running(state, languageCode, onIntent)
        }
        Spacer(Modifier.height(ForjaSpacing.esp8))
    }
}

@Composable
private fun title(state: WodState): String = when (state.format) {
    BlockFormat.AMRAP -> "AMRAP ${state.minutes} min"
    BlockFormat.EMOM -> "EMOM ${state.minutes} min"
    BlockFormat.FOR_TIME -> stringResource(Res.string.wod_format_for_time) + (state.minutes?.let { " · $it min" } ?: "")
    else -> stringResource(Res.string.wod_format_rounds)
}

@Composable
private fun Setup(state: WodState, languageCode: String, onIntent: (WodIntent) -> Unit) {
    val colors = ForjaTheme.colors
    EditorField(stringResource(Res.string.wod_name), state.name) { onIntent(WodIntent.Name(it)) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        listOf(
            BlockFormat.AMRAP to Res.string.wod_format_amrap,
            BlockFormat.EMOM to Res.string.wod_format_emom,
            BlockFormat.FOR_TIME to Res.string.wod_format_for_time,
            BlockFormat.ROUNDS to Res.string.wod_format_rounds,
        ).forEach { (format, label) ->
            val selected = state.format == format
            Box(
                Modifier.height(40.dp).clip(CircleShape)
                    .background(if (selected) colors.emberVeil else colors.surface2)
                    .then(if (selected) Modifier.border(1.dp, colors.ember, CircleShape) else Modifier)
                    .clickable { onIntent(WodIntent.Format(format)) }
                    .padding(horizontal = ForjaSpacing.esp4),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(label), style = ForjaTheme.typography.label, color = if (selected) colors.ember else colors.text1)
            }
        }
    }
    if (state.format != BlockFormat.ROUNDS) {
        val hint = when (state.format) {
            BlockFormat.AMRAP -> stringResource(Res.string.wod_minutes_amrap)
            BlockFormat.EMOM -> stringResource(Res.string.wod_minutes_emom)
            else -> stringResource(Res.string.wod_cap)
        }
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.wod_minutes), style = ForjaTheme.typography.body, color = colors.text1)
                Text(hint, style = ForjaTheme.typography.caption, color = colors.text2)
            }
            val lowest = if (state.format == BlockFormat.FOR_TIME) 0 else 1
            StepButton("−", (state.minutes ?: 0) > lowest) { onIntent(WodIntent.Minutes(-1)) }
            ForjaNumber(
                state.minutes?.toString() ?: stringResource(Res.string.wod_no_cap),
                ForjaTheme.typography.largeFigure,
                Modifier.widthIn(min = 64.dp).padding(horizontal = ForjaSpacing.esp2),
                textAlign = TextAlign.Center,
            )
            StepButton("+", (state.minutes ?: 0) < Wod.MAX_MINUTES) { onIntent(WodIntent.Minutes(1)) }
        }
    }
    Text(stringResource(Res.string.wod_movements), style = ForjaTheme.typography.title3, color = colors.text1)
    Text(stringResource(Res.string.wod_movements_hint), style = ForjaTheme.typography.caption, color = colors.text2)
    state.movements.forEachIndexed { index, movement ->
        Column(
            Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp3),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.exerciseName(movement.exerciseId, languageCode),
                    style = ForjaTheme.typography.body,
                    color = colors.text1,
                    modifier = Modifier.weight(1f),
                )
                TextAction(stringResource(Res.string.wod_remove), colors.text2) { onIntent(WodIntent.RemoveMovement(index)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                StepButton("−", movement.reps > 1) { onIntent(WodIntent.Reps(index, -1)) }
                ForjaNumber(
                    "${movement.reps}",
                    ForjaTheme.typography.largeFigure,
                    Modifier.widthIn(min = 56.dp),
                    textAlign = TextAlign.Center,
                )
                StepButton("+", true) { onIntent(WodIntent.Reps(index, 1)) }
            }
            EditorField(stringResource(Res.string.wod_load_hint), movement.load.orEmpty()) { onIntent(WodIntent.Load(index, it)) }
        }
    }
    var query by remember { mutableStateOf("") }
    EditorField(stringResource(Res.string.interval_search), query) { query = it }
    if (query.isNotBlank()) {
        val matches = state.exercises.filter { it.name.resolve(languageCode).contains(query.trim(), ignoreCase = true) }
        matches.take(MAX_RESULTS).forEach { exercise ->
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clip(ForjaRadius.shapeL).background(colors.surface1)
                    .clickable {
                        onIntent(WodIntent.AddMovement(exercise.id))
                        query = ""
                    }
                    .padding(horizontal = ForjaSpacing.esp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(exercise.name.resolve(languageCode), style = ForjaTheme.typography.body, color = colors.text1)
            }
        }
    }
    EditorAction(stringResource(Res.string.wod_start), state.canStart, Modifier.fillMaxWidth(), ForjaButtonStyle.PRIMARY) {
        onIntent(WodIntent.Start)
    }
}

@Composable
private fun Running(state: WodState, languageCode: String, onIntent: (WodIntent) -> Unit) {
    val colors = ForjaTheme.colors
    val wod = state.wod ?: return
    val timer = state.timer ?: return
    val total = wod.minutes?.let { it * 60 }
    val left = total?.let { (it - state.elapsedSeconds).coerceAtLeast(0) }
    val shownSeconds = if (wod.countsDown) (timer.remainingInSegmentMs?.let { ((it + 999) / 1000).toInt() } ?: 0) else state.elapsedSeconds
    val ending = wod.countsDown && left != null && left <= LAST_SECONDS
    val finished = state.phase == WodPhase.FINISHED

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ForjaNumber(
            if (finished) stringResource(Res.string.wod_time) else Formats.duration(shownSeconds.toLong()),
            ForjaTheme.typography.heroFigure.copy(fontSize = 96.sp, lineHeight = 100.sp),
            color = if (ending || finished) colors.ember else colors.text1,
        )
        when {
            wod.format == BlockFormat.EMOM -> Text(
                stringResource(Res.string.wod_minute, timer.round, wod.minutes!!),
                modifier = Modifier.timerPhase(),
                style = ForjaTheme.typography.body2,
                color = colors.text2,
            )
            total != null -> Text(
                stringResource(Res.string.wod_of, Formats.duration(total.toLong())),
                style = ForjaTheme.typography.body2,
                color = colors.text2,
            )
        }
    }
    if (total != null) {
        val fraction = (state.elapsedSeconds.toFloat() / total).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.surface3)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(colors.ember))
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.wod_rounds_reps), style = ForjaTheme.typography.caption, color = colors.text2)
            ForjaNumber(score(wod, state), ForjaTheme.typography.heroFigure.copy(fontSize = 56.sp, lineHeight = 60.sp))
        }
        Column(horizontalAlignment = Alignment.End) {
            state.best?.let {
                Text(stringResource(Res.string.wod_best, mark(it)), style = ForjaTheme.typography.body2, color = colors.text2)
            }
            state.score.paceRounds(wod, state.elapsedSeconds.toLong())?.takeIf { !finished }?.let {
                Text(stringResource(Res.string.wod_pace, it), style = ForjaTheme.typography.body2, color = colors.ember)
            }
        }
    }

    if (!finished) {
        Column(
            Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp3),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
        ) {
            Text(stringResource(Res.string.wod_round, state.score.rounds + 1), style = ForjaTheme.typography.title3, color = colors.text1)
            Text(stringResource(Res.string.wod_tap_each), style = ForjaTheme.typography.caption, color = colors.text2)
            wod.movements.forEachIndexed { index, movement ->
                val done = index < state.score.movementsDone
                val next = index == state.score.movementsDone
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).clip(ForjaRadius.shapeL)
                        .background(if (next) colors.surface2 else colors.surface1)
                        .clickable(enabled = next) { onIntent(WodIntent.Tick) }
                        .padding(horizontal = ForjaSpacing.esp3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .then(if (done) Modifier.background(colors.ember) else Modifier)
                            .border(2.dp, if (done || next) colors.ember else colors.activeControl, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Text("✓", style = ForjaTheme.typography.label, color = colors.onEmber)
                    }
                    ForjaNumber("${movement.reps}", ForjaTheme.typography.largeFigure)
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.exerciseName(movement.exerciseId, languageCode),
                            style = ForjaTheme.typography.body,
                            color = colors.text1,
                        )
                        movement.load?.let { Text(it, style = ForjaTheme.typography.caption, color = colors.text2) }
                    }
                }
            }
        }
        Text(stringResource(Res.string.wod_voice_hint), style = ForjaTheme.typography.caption, color = colors.text3)
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3), verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                stringResource(if (timer.isPaused) Res.string.interval_resume else Res.string.interval_pause),
                colors.text1,
            ) { onIntent(WodIntent.TogglePause) }
            EditorAction(stringResource(Res.string.wod_add_round)) { onIntent(WodIntent.AddRound) }
            if (!wod.countsDown) TextAction(stringResource(Res.string.wod_finish), colors.text1) { onIntent(WodIntent.Finish) }
        }
    } else {
        when {
            state.saved -> Text(stringResource(Res.string.wod_saved), style = ForjaTheme.typography.body2, color = colors.text2)
            else -> {
                if (state.failed) {
                    Text(stringResource(Res.string.wod_save_failed), style = ForjaTheme.typography.body2, color = colors.danger)
                }
                EditorAction(
                    stringResource(Res.string.wod_save, mark(state.score.result(wod, state.elapsedSeconds))),
                    !state.saving,
                    Modifier.fillMaxWidth(),
                    ForjaButtonStyle.PRIMARY,
                ) {
                    onIntent(WodIntent.Save)
                }
            }
        }
    }
}

@Composable
private fun score(wod: Wod, state: WodState): String = when (wod.format) {
    BlockFormat.EMOM -> stringResource(Res.string.wod_rounds_only, state.score.rounds)
    else -> "${state.score.rounds} + ${state.score.reps(wod)}"
}

@Composable
private fun mark(result: BlockResult): String = when (result) {
    is BlockResult.RoundsAndReps -> "${result.rounds} + ${result.reps}"
    is BlockResult.Rounds -> stringResource(Res.string.wod_rounds_only, result.rounds)
    is BlockResult.Time -> Formats.duration(result.seconds.toLong())
    else -> ""
}

private const val LAST_SECONDS = 10
private const val MAX_RESULTS = 8
