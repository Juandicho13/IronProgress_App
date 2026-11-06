package com.olarte.ironprogress.ui.interval

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.interval.IntervalPreset
import com.olarte.ironprogress.domain.interval.IntervalTemplate
import com.olarte.ironprogress.domain.voice.VoiceSettings
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorField
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.clearFocusOnTap
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.interval_alternate
import com.olarte.ironprogress.ui.resources.interval_cancel
import com.olarte.ironprogress.ui.resources.interval_countdown
import com.olarte.ironprogress.ui.resources.interval_countdown_hint
import com.olarte.ironprogress.ui.resources.interval_custom
import com.olarte.ironprogress.ui.resources.interval_default_name
import com.olarte.ironprogress.ui.resources.interval_duck
import com.olarte.ironprogress.ui.resources.interval_duck_hint
import com.olarte.ironprogress.ui.resources.interval_each_round
import com.olarte.ironprogress.ui.resources.interval_edit
import com.olarte.ironprogress.ui.resources.interval_error
import com.olarte.ironprogress.ui.resources.interval_exercises
import com.olarte.ironprogress.ui.resources.interval_name
import com.olarte.ironprogress.ui.resources.interval_new
import com.olarte.ironprogress.ui.resources.interval_next
import com.olarte.ironprogress.ui.resources.interval_next_hint
import com.olarte.ironprogress.ui.resources.interval_no_exercises
import com.olarte.ironprogress.ui.resources.interval_rest
import com.olarte.ironprogress.ui.resources.interval_rest_hint
import com.olarte.ironprogress.ui.resources.interval_rounds
import com.olarte.ironprogress.ui.resources.interval_save
import com.olarte.ironprogress.ui.resources.interval_search
import com.olarte.ironprogress.ui.resources.interval_start
import com.olarte.ironprogress.ui.resources.interval_total
import com.olarte.ironprogress.ui.resources.interval_voice
import com.olarte.ironprogress.ui.resources.interval_voice_on
import com.olarte.ironprogress.ui.resources.interval_voice_on_hint
import com.olarte.ironprogress.ui.resources.interval_work
import com.olarte.ironprogress.ui.resources.interval_work_hint
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun IntervalEditorRoute(id: String?, languageCode: String, onClose: () -> Unit, onRun: (String) -> Unit) {
    val defaultName = stringResource(Res.string.interval_default_name)
    val model = koinViewModel<IntervalEditorViewModel>(key = "interval-editor-${id ?: "new"}") { parametersOf(id, defaultName) }
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) {
        model.effects.collect { effect ->
            when (effect) {
                IntervalEditorEffect.Close -> onClose()
                is IntervalEditorEffect.Run -> onRun(effect.id)
            }
        }
    }
    IntervalEditorScreen(state, languageCode, editing = id != null, onIntent = model::onIntent, onCancel = onClose)
}

/** Crear intervalos (PANTALLAS 5.2). */
@Composable
fun IntervalEditorScreen(
    state: IntervalEditorState,
    languageCode: String,
    editing: Boolean,
    onIntent: (IntervalEditorIntent) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val type = ForjaTheme.typography
    val template = state.template
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().imePadding().clearFocusOnTap()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp5),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(Res.string.interval_cancel), colors.text2, onClick = onCancel)
            Text(
                stringResource(if (editing) Res.string.interval_edit else Res.string.interval_new),
                style = type.title3,
                color = colors.text1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = ForjaSpacing.esp2),
            )
            TextAction(stringResource(Res.string.interval_save), enabled = !state.saving) { onIntent(IntervalEditorIntent.Save) }
        }
        EditorField(stringResource(Res.string.interval_name), template.name) { onIntent(IntervalEditorIntent.Rename(it)) }

        PresetChips(template) { onIntent(IntervalEditorIntent.ChoosePreset(it)) }

        Column(
            Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp4),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.interval_each_round),
                    style = type.title3,
                    color = colors.text1,
                    modifier = Modifier.weight(1f),
                )
                ForjaNumber(clock(template.roundSeconds), type.figure, color = colors.text2)
            }
            ProportionBar(template)
            StepRow(
                stringResource(Res.string.interval_work),
                stringResource(Res.string.interval_work_hint),
                clock(template.workSeconds),
                canLower = template.workSeconds > IntervalTemplate.WORK_RANGE.first,
                canRaise = template.workSeconds < IntervalTemplate.WORK_RANGE.last,
                onLower = { onIntent(IntervalEditorIntent.Work(-IntervalTemplate.STEP)) },
                onRaise = { onIntent(IntervalEditorIntent.Work(IntervalTemplate.STEP)) },
            )
            StepRow(
                stringResource(Res.string.interval_rest),
                stringResource(Res.string.interval_rest_hint),
                clock(template.restSeconds),
                canLower = template.restSeconds > IntervalTemplate.REST_RANGE.first,
                canRaise = template.restSeconds < IntervalTemplate.REST_RANGE.last,
                onLower = { onIntent(IntervalEditorIntent.Rest(-IntervalTemplate.STEP)) },
                onRaise = { onIntent(IntervalEditorIntent.Rest(IntervalTemplate.STEP)) },
            )
            StepRow(
                stringResource(Res.string.interval_rounds),
                stringResource(Res.string.interval_total, clock(template.totalSeconds)),
                template.rounds.toString(),
                canLower = template.rounds > IntervalTemplate.ROUNDS_RANGE.first,
                canRaise = template.rounds < IntervalTemplate.ROUNDS_RANGE.last,
                onLower = { onIntent(IntervalEditorIntent.Rounds(-1)) },
                onRaise = { onIntent(IntervalEditorIntent.Rounds(1)) },
            )
        }

        ExercisePicker(state, languageCode) { onIntent(IntervalEditorIntent.ToggleExercise(it)) }

        VoiceSection(template.voice) { onIntent(IntervalEditorIntent.Voice(it)) }

        if (state.failed) Text(stringResource(Res.string.interval_error), style = type.body2, color = colors.danger)
        EditorAction(
            stringResource(Res.string.interval_start, clock(template.totalSeconds)),
            !state.saving && !state.loading,
            Modifier.fillMaxWidth(),
            ForjaButtonStyle.PRIMARY,
        ) {
            onIntent(IntervalEditorIntent.Start)
        }
        Spacer(Modifier.height(ForjaSpacing.esp8))
    }
}

@Composable
private fun PresetChips(template: IntervalTemplate, onChoose: (IntervalPreset) -> Unit) {
    val current = template.preset
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Chip(stringResource(Res.string.interval_custom), selected = current == null, onClick = null)
        IntervalPreset.entries.forEach { preset ->
            val label = if (preset == IntervalPreset.TABATA) "Tabata 20/10" else "${preset.workSeconds}/${preset.restSeconds}"
            Chip(label, selected = current == preset) { onChoose(preset) }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: (() -> Unit)?) {
    val colors = ForjaTheme.colors
    Box(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (selected) colors.emberVeil else colors.surface2)
            .then(if (selected) Modifier.border(1.dp, colors.ember, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ForjaSpacing.esp4),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.label, color = if (selected) colors.ember else colors.text1)
    }
}

/** Barra de 10 dp: trabajo en ember y descanso en hielo, cada tramo tan ancho como su tiempo. */
@Composable
private fun ProportionBar(template: IntervalTemplate) {
    Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.weight(template.workSeconds.toFloat()).height(10.dp).clip(CircleShape).background(ForjaTheme.colors.ember))
        if (template.restSeconds > 0) {
            Box(Modifier.weight(template.restSeconds.toFloat()).height(10.dp).clip(CircleShape).background(ForjaTheme.colors.ice))
        }
    }
}

@Composable
private fun StepRow(
    title: String,
    hint: String,
    value: String,
    canLower: Boolean,
    canRaise: Boolean,
    onLower: () -> Unit,
    onRaise: () -> Unit,
) {
    val colors = ForjaTheme.colors
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = ForjaTheme.typography.body, color = colors.text1)
            Text(hint, style = ForjaTheme.typography.caption, color = colors.text2)
        }
        StepButton("−", canLower, onLower)
        ForjaNumber(
            value,
            ForjaTheme.typography.largeFigure,
            Modifier.width(104.dp).padding(horizontal = ForjaSpacing.esp1),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepButton("+", canRaise, onRaise)
    }
}

@Composable
private fun ExercisePicker(state: IntervalEditorState, languageCode: String, onToggle: (ExerciseId) -> Unit) {
    val colors = ForjaTheme.colors
    var query by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Text(stringResource(Res.string.interval_exercises), style = ForjaTheme.typography.title3, color = colors.text1)
        Text(
            if (state.selected.isEmpty()) {
                stringResource(Res.string.interval_no_exercises)
            } else {
                state.selected.joinToString(" · ") { it.name.resolve(languageCode) }
            },
            style = ForjaTheme.typography.body,
            color = colors.text1,
        )
        Text(stringResource(Res.string.interval_alternate), style = ForjaTheme.typography.caption, color = colors.text2)
        EditorField(stringResource(Res.string.interval_search), query) { query = it }
        val matches = state.exercises
            .filter { query.isBlank() || it.name.resolve(languageCode).contains(query.trim(), ignoreCase = true) }
            .take(if (query.isBlank()) 0 else MAX_RESULTS)
        matches.forEach { exercise ->
            val chosen = exercise.id in state.template.exerciseIds
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clip(ForjaRadius.shapeL)
                    .background(if (chosen) colors.emberVeil else colors.surface1)
                    .clickable { onToggle(exercise.id) }
                    .padding(horizontal = ForjaSpacing.esp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    exercise.name.resolve(languageCode),
                    style = ForjaTheme.typography.body,
                    color = if (chosen) colors.ember else colors.text1,
                    modifier = Modifier.weight(1f),
                )
                if (chosen) Text("✓", style = ForjaTheme.typography.body, color = colors.ember)
            }
        }
    }
}

@Composable
private fun VoiceSection(voice: VoiceSettings, onChange: (VoiceSettings) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(ForjaTheme.colors.surface1),
    ) {
        Text(
            stringResource(Res.string.interval_voice),
            style = ForjaTheme.typography.title3,
            color = ForjaTheme.colors.text1,
            modifier = Modifier.padding(ForjaSpacing.esp4),
        )
        SwitchRow(stringResource(Res.string.interval_voice_on), stringResource(Res.string.interval_voice_on_hint), voice.enabled) {
            onChange(voice.copy(enabled = it))
        }
        SwitchRow(
            stringResource(Res.string.interval_countdown),
            stringResource(Res.string.interval_countdown_hint),
            voice.countdown,
            voice.enabled,
        ) {
            onChange(voice.copy(countdown = it))
        }
        SwitchRow(
            stringResource(Res.string.interval_next),
            stringResource(Res.string.interval_next_hint),
            voice.saysWhatsNext,
            voice.enabled,
        ) {
            onChange(voice.copy(saysWhatsNext = it))
        }
        SwitchRow(stringResource(Res.string.interval_duck), stringResource(Res.string.interval_duck_hint), voice.duckMusic, voice.enabled) {
            onChange(voice.copy(duckMusic = it))
        }
    }
}

/** Fila de 56 dp; toda la fila es el interruptor. */
@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = ForjaTheme.typography.body, color = ForjaTheme.colors.text1)
            Text(hint, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private const val MAX_RESULTS = 8
