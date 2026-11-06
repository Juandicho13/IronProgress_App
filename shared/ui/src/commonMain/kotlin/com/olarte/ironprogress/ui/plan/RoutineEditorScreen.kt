package com.olarte.ironprogress.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.ExerciseQuery
import com.olarte.ironprogress.domain.catalog.ExerciseSearch
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.routine.RoutineExerciseId
import com.olarte.ironprogress.ui.components.EditorChoice
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.components.ForjaTextField
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.components.clearFocusOnTap
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.resources.editor_discard
import com.olarte.ironprogress.ui.resources.editor_error
import com.olarte.ironprogress.ui.resources.editor_keep
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.editor_save
import com.olarte.ironprogress.ui.resources.plan_add_block
import com.olarte.ironprogress.ui.resources.plan_add_exercise
import com.olarte.ironprogress.ui.resources.plan_add_set
import com.olarte.ironprogress.ui.resources.plan_block
import com.olarte.ironprogress.ui.resources.plan_down
import com.olarte.ironprogress.ui.resources.plan_edit
import com.olarte.ironprogress.ui.resources.plan_folder
import com.olarte.ironprogress.ui.resources.plan_invalid
import com.olarte.ironprogress.ui.resources.plan_name
import com.olarte.ironprogress.ui.resources.plan_none
import com.olarte.ironprogress.ui.resources.plan_notes
import com.olarte.ironprogress.ui.resources.plan_other_block
import com.olarte.ironprogress.ui.resources.plan_search
import com.olarte.ironprogress.ui.resources.plan_up
import com.olarte.ironprogress.ui.resources.routine_col_max
import com.olarte.ironprogress.ui.resources.routine_col_min
import com.olarte.ironprogress.ui.resources.routine_col_rir
import com.olarte.ironprogress.ui.resources.routine_col_set
import com.olarte.ironprogress.ui.resources.routine_new
import com.olarte.ironprogress.ui.resources.routine_no_results
import com.olarte.ironprogress.ui.resources.routine_remove_block
import com.olarte.ironprogress.ui.resources.routine_remove_exercise
import com.olarte.ironprogress.ui.resources.routine_remove_set
import com.olarte.ironprogress.ui.resources.workout_mode_title
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.ExerciseModeSheet
import com.olarte.ironprogress.ui.workout.summary
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoutineEditorRoute(id: String?, onClose: () -> Unit, setBack: ((() -> Unit)?) -> Unit) {
    val model = koinViewModel<RoutineEditorViewModel> { parametersOf(id) }
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.effects.collect { onClose() } }
    DisposableEffect(model) {
        setBack { model.onIntent(RoutineEditorIntent.Back) }
        onDispose { setBack(null) }
    }
    RoutineEditorScreen(state, model::onIntent, isNew = id == null)
}

/**
 * Editor de rutina (PANTALLAS 9, F1-08): barra superior con volver (pide descartar si hay cambios)
 * y Guardar a la derecha; datos de la rutina, un bloque por tarjeta y, en cada ejercicio, la tabla
 * de series con mínimo, máximo y RIR.
 */
@Composable
fun RoutineEditorScreen(state: RoutineEditorState, onIntent: (RoutineEditorIntent) -> Unit, isNew: Boolean = false) {
    var picking by remember { mutableStateOf<BlockId?>(null) }
    var search by remember { mutableStateOf("") }
    var modeFor by remember { mutableStateOf<RoutineExerciseId?>(null) }
    val language = appLanguage()
    val colors = ForjaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().imePadding().clearFocusOnTap()) {
        ForjaTopBar(
            title = stringResource(if (isNew) Res.string.routine_new else Res.string.plan_edit),
            onBack = { onIntent(RoutineEditorIntent.Back) },
            backDescription = stringResource(Res.string.editor_back),
            backIcon = ForjaIcons.Close,
        ) {
            ForjaButton(
                stringResource(Res.string.editor_save),
                style = ForjaButtonStyle.TONAL,
                enabled = state.valid && !state.saving,
            ) { onIntent(RoutineEditorIntent.Save) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            if (state.loading) ForjaNote(stringResource(Res.string.editor_loading))
            if (state.failed) {
                ForjaNote(stringResource(Res.string.editor_error), error = true)
                ForjaButton(stringResource(Res.string.editor_retry), style = ForjaButtonStyle.SECONDARY) {
                    onIntent(RoutineEditorIntent.Retry)
                }
            }
            state.detail?.let { detail ->
                ForjaCard {
                    ForjaTextField(stringResource(Res.string.plan_name), state.name, { onIntent(RoutineEditorIntent.Name(it)) })
                    ForjaTextField(
                        stringResource(Res.string.plan_notes),
                        state.notes,
                        { onIntent(RoutineEditorIntent.Notes(it)) },
                        singleLine = false,
                    )
                }
                EditorChoice(
                    stringResource(Res.string.plan_folder),
                    detail.routine.folderId,
                    listOf(null) + state.folders.map { it.id },
                    { id -> state.folders.find { it.id == id }?.name ?: stringResource(Res.string.plan_none) },
                ) { onIntent(RoutineEditorIntent.FolderSelect(it)) }

                detail.blocks.sortedBy { it.sortOrder }.forEachIndexed { index, block ->
                    key(block.id) {
                        ForjaCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (block.format == BlockFormat.SETS) {
                                        stringResource(Res.string.plan_block, index + 1)
                                    } else {
                                        stringResource(Res.string.plan_other_block)
                                    },
                                    Modifier.weight(1f),
                                    style = ForjaTheme.typography.title3,
                                    color = colors.text1,
                                )
                                if (block.format == BlockFormat.SETS) {
                                    ForjaIconButton(
                                        ForjaIcons.Close,
                                        stringResource(Res.string.routine_remove_block),
                                        enabled = !state.saving,
                                        filled = false,
                                        tint = colors.text2,
                                    ) { onIntent(RoutineEditorIntent.RemoveBlock(block.id)) }
                                }
                            }
                            if (block.format == BlockFormat.SETS) {
                                val exercises = detail.exercisesOf(block.id)
                                exercises.forEachIndexed { position, exercise ->
                                    key(exercise.id) {
                                        ExerciseEditor(
                                            name = state.catalog.find { it.id == exercise.exerciseId }?.name?.resolve(language)
                                                ?: exercise.exerciseId.value,
                                            notes = exercise.notes,
                                            modeText = ExerciseMode.of(
                                                exercise.supersetGroup,
                                                state.rest[exercise.id]?.toIntOrNull(),
                                                exercise.timedSeconds,
                                            ).summary(
                                                state.rest[exercise.id]?.toIntOrNull() ?: detail.routine.restSeconds,
                                                exercise.timedSeconds,
                                                detail.exercises.count {
                                                    it.supersetGroup != null &&
                                                        it.supersetGroup == exercise.supersetGroup
                                                },
                                            ),
                                            onMode = { modeFor = exercise.id },
                                            canUp = position > 0,
                                            canDown = position < exercises.lastIndex,
                                            enabled = !state.saving,
                                            onIntent = onIntent,
                                            exerciseId = exercise.id,
                                            position = position,
                                        ) {
                                            detail.setsOf(exercise.id).forEachIndexed { setIndex, set ->
                                                val draft = state.sets.getValue(set.id)
                                                SetRow(
                                                    number = setIndex + 1,
                                                    draft = draft,
                                                    enabled = !state.saving,
                                                    onChange = { onIntent(RoutineEditorIntent.ChangeSet(set.id, it)) },
                                                    onRemove = { onIntent(RoutineEditorIntent.RemoveSet(set.id)) },
                                                )
                                            }
                                        }
                                    }
                                }
                                ForjaButton(
                                    stringResource(Res.string.plan_add_exercise),
                                    Modifier.fillMaxWidth(),
                                    ForjaButtonStyle.TONAL,
                                    enabled = !state.saving,
                                    icon = ForjaIcons.Add,
                                ) {
                                    search = ""
                                    picking = block.id
                                }
                            }
                        }
                    }
                }
                ForjaButton(
                    stringResource(Res.string.plan_add_block),
                    Modifier.fillMaxWidth(),
                    ForjaButtonStyle.SECONDARY,
                    enabled = !state.saving,
                    icon = ForjaIcons.Add,
                ) { onIntent(RoutineEditorIntent.AddBlock) }
                if (!state.valid && state.dirty) ForjaNote(stringResource(Res.string.plan_invalid), error = true)
            }
            Spacer(Modifier.height(ForjaSpacing.esp8))
        }
    }

    if (picking != null) {
        ForjaSheet(stringResource(Res.string.plan_add_exercise), onDismiss = { picking = null }) {
            Box(Modifier.padding(horizontal = ForjaSpacing.esp5).padding(bottom = ForjaSpacing.esp2)) {
                ForjaTextField(stringResource(Res.string.plan_search), search, { search = it })
            }
            val results = ExerciseSearch.filter(state.catalog, ExerciseQuery(text = search), language)
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (results.isEmpty()) {
                    ForjaNote(stringResource(Res.string.routine_no_results), modifier = Modifier.padding(ForjaSpacing.esp5))
                }
                results.forEachIndexed { index, exercise ->
                    if (index > 0) ForjaDivider()
                    ForjaListRow(
                        title = exercise.name.resolve(language),
                        subtitle = exercise.primaryMuscles.map { it.label() }.joinToString(" · ").ifBlank { null },
                        chevron = false,
                        onClick = {
                            picking?.let { onIntent(RoutineEditorIntent.AddExercise(it, exercise.id)) }
                            picking = null
                        },
                    )
                }
            }
        }
    }
    modeFor?.let { id ->
        val detail = state.detail
        val exercise = detail?.exercises?.firstOrNull { it.id == id }
        if (detail != null && exercise != null) {
            val restValue = state.rest[id]?.toIntOrNull()
            val inBlock = detail.exercises.filter { it.blockId == exercise.blockId }.sortedBy { it.sortOrder }
            ExerciseModeSheet(
                ExerciseMode.of(exercise.supersetGroup, restValue, exercise.timedSeconds),
                restValue ?: detail.routine.restSeconds,
                exercise.timedSeconds,
                canGroup = inBlock.lastOrNull()?.id != id,
                onDismiss = { modeFor = null },
            ) { mode, seconds ->
                onIntent(RoutineEditorIntent.Mode(id, mode, seconds))
                modeFor = null
            }
        }
    }
    if (state.confirmDiscard) {
        ForjaDialog(
            title = stringResource(Res.string.editor_discard),
            onDismiss = { onIntent(RoutineEditorIntent.Keep) },
            confirmText = stringResource(Res.string.editor_discard),
            destructive = true,
            onConfirm = { onIntent(RoutineEditorIntent.Discard) },
            dismissText = stringResource(Res.string.editor_keep),
        )
    }
}

@Composable
private fun ExerciseEditor(
    name: String,
    notes: String,
    modeText: String,
    onMode: () -> Unit,
    canUp: Boolean,
    canDown: Boolean,
    enabled: Boolean,
    onIntent: (RoutineEditorIntent) -> Unit,
    exerciseId: RoutineExerciseId,
    position: Int,
    sets: @Composable () -> Unit,
) {
    val colors = ForjaTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(colors.background).padding(ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), style = ForjaTheme.typography.body, color = colors.text1)
            ForjaIconButton(
                ForjaIcons.ChevronRight,
                stringResource(Res.string.plan_up),
                enabled = enabled && canUp,
                filled = false,
                tint = colors.text2,
                rotation = -90f,
            ) { onIntent(RoutineEditorIntent.MoveExercise(exerciseId, position - 1)) }
            ForjaIconButton(
                ForjaIcons.ChevronRight,
                stringResource(Res.string.plan_down),
                enabled = enabled && canDown,
                filled = false,
                tint = colors.text2,
                rotation = 90f,
            ) { onIntent(RoutineEditorIntent.MoveExercise(exerciseId, position + 1)) }
            ForjaIconButton(
                ForjaIcons.Close,
                stringResource(Res.string.routine_remove_exercise),
                enabled = enabled,
                filled = false,
                tint = colors.text2,
            ) { onIntent(RoutineEditorIntent.RemoveExercise(exerciseId)) }
        }
        // Encabezado de la tabla de series.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ColumnLabel(stringResource(Res.string.routine_col_set), Modifier.width(40.dp))
            ColumnLabel(stringResource(Res.string.routine_col_min), Modifier.weight(1f))
            ColumnLabel(stringResource(Res.string.routine_col_max), Modifier.weight(1f))
            // RIR solo en modo Pro; en el básico la tabla es serie, mínimo y máximo.
            if (com.olarte.ironprogress.ui.settings.LocalAppSettings.current.proMode) {
                ColumnLabel(stringResource(Res.string.routine_col_rir), Modifier.weight(1f))
            }
            Spacer(Modifier.width(ForjaMeasures.minTouch))
        }
        sets()
        ForjaButton(
            stringResource(Res.string.plan_add_set),
            style = ForjaButtonStyle.TEXT,
            enabled = enabled,
            icon = ForjaIcons.Add,
        ) { onIntent(RoutineEditorIntent.AddSet(exerciseId)) }
        ForjaListRow(
            stringResource(Res.string.workout_mode_title),
            value = modeText,
            enabled = enabled,
            onClick = onMode,
        )
        ForjaTextField(stringResource(Res.string.plan_notes), notes, { onIntent(RoutineEditorIntent.ExerciseNotes(exerciseId, it)) })
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    Text(text, modifier, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(number: Int, draft: SetDraft, enabled: Boolean, onChange: (SetDraft) -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            number.toString(),
            Modifier.width(40.dp),
            style = ForjaTheme.typography.smallFigure,
            color = ForjaTheme.colors.text1,
            textAlign = TextAlign.Center,
        )
        NumberCell(draft.minimum, stringResource(Res.string.routine_col_min), enabled, Modifier.weight(1f)) {
            onChange(draft.copy(minimum = it))
        }
        NumberCell(draft.maximum, stringResource(Res.string.routine_col_max), enabled, Modifier.weight(1f)) {
            onChange(draft.copy(maximum = it))
        }
        if (com.olarte.ironprogress.ui.settings.LocalAppSettings.current.proMode) {
            NumberCell(draft.rir, stringResource(Res.string.routine_col_rir), enabled, Modifier.weight(1f)) {
                onChange(draft.copy(rir = it))
            }
        }
        ForjaIconButton(
            ForjaIcons.Remove,
            stringResource(Res.string.routine_remove_set, number),
            enabled = enabled,
            filled = false,
            tint = ForjaTheme.colors.text3,
            onClick = onRemove,
        )
    }
}

/** Celda numérica compacta de 48 en surface3, con el número centrado. */
@Composable
private fun NumberCell(value: String, description: String, enabled: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    val colors = ForjaTheme.colors
    val valid = value.isEmpty() || value.all { it.isDigit() }
    BasicTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(3)) },
        enabled = enabled,
        singleLine = true,
        textStyle = ForjaTheme.typography.smallFigure.copy(color = colors.text1, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.ember),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = com.olarte.ironprogress.ui.components.dismissingKeyboardActions(),
        modifier = modifier.semantics { contentDescription = description },
        decorationBox = { field ->
            Box(
                Modifier.fillMaxWidth().height(ForjaMeasures.minTouch).clip(ForjaRadius.shapeS)
                    .background(if (valid) colors.surface3 else colors.danger.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { field() }
        },
    )
}
