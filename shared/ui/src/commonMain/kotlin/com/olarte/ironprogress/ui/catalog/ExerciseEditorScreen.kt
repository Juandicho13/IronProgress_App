package com.olarte.ironprogress.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorChoice
import com.olarte.ironprogress.ui.components.EditorField
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.resources.editor_create
import com.olarte.ironprogress.ui.resources.editor_discard
import com.olarte.ironprogress.ui.resources.editor_discipline
import com.olarte.ironprogress.ui.resources.editor_edit
import com.olarte.ironprogress.ui.resources.editor_english
import com.olarte.ironprogress.ui.resources.editor_equipment
import com.olarte.ironprogress.ui.resources.editor_error
import com.olarte.ironprogress.ui.resources.editor_keep
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_pattern
import com.olarte.ironprogress.ui.resources.editor_primary
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.editor_save
import com.olarte.ironprogress.ui.resources.editor_secondary
import com.olarte.ironprogress.ui.resources.editor_spanish
import com.olarte.ironprogress.ui.resources.editor_tracking
import com.olarte.ironprogress.ui.resources.editor_unilateral
import com.olarte.ironprogress.ui.resources.editor_validation
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseEditorRoute(id: String?, onClose: () -> Unit, setBack: ((() -> Unit)?) -> Unit) {
    val model = koinViewModel<ExerciseEditorViewModel> { parametersOf(id) }
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.effects.collect { onClose() } }
    DisposableEffect(model) {
        setBack { model.onIntent(ExerciseEditorIntent.Back) }
        onDispose { setBack(null) }
    }
    ExerciseEditorScreen(state, model::onIntent, editing = id != null)
}

@Composable
fun ExerciseEditorScreen(state: ExerciseEditorState, onIntent: (ExerciseEditorIntent) -> Unit, editing: Boolean = false) {
    val draft = state.draft
    fun change(value: ExerciseDraft) = onIntent(ExerciseEditorIntent.Change(value))
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Text(
            stringResource(if (editing) Res.string.editor_edit else Res.string.editor_create),
            modifier = Modifier.semantics { heading() },
            style = ForjaTheme.typography.title1,
            color = ForjaTheme.colors.text1,
        )
        EditorAction(stringResource(Res.string.editor_back), !state.saving) { onIntent(ExerciseEditorIntent.Back) }
        if (state.loading) Text(stringResource(Res.string.editor_loading))
        if (state.failed) {
            Text(stringResource(Res.string.editor_error), color = ForjaTheme.colors.danger)
            EditorAction(stringResource(Res.string.editor_retry)) { onIntent(ExerciseEditorIntent.Retry) }
        }
        if (!state.loading) {
            EditorField(stringResource(Res.string.editor_spanish), draft.spanish) { change(draft.copy(spanish = it)) }
            EditorField(stringResource(Res.string.editor_english), draft.english) { change(draft.copy(english = it)) }
            EditorChoice(stringResource(Res.string.editor_equipment), draft.equipment, Equipment.entries, {
                it.label()
            }) { change(draft.copy(equipment = it)) }
            EditorChoice(stringResource(Res.string.editor_discipline), draft.discipline, Discipline.entries, {
                it.editorLabel()
            }) { change(draft.copy(discipline = it)) }
            EditorChoice(stringResource(Res.string.editor_pattern), draft.pattern, MovementPattern.entries, {
                it.editorLabel()
            }) { change(draft.copy(pattern = it)) }
            EditorChoice(stringResource(Res.string.editor_tracking), draft.tracking, TrackingType.entries, {
                it.editorLabel()
            }) { change(draft.copy(tracking = it)) }
            MuscleRole.entries.forEach { role ->
                Text(
                    stringResource(
                        if (role ==
                            MuscleRole.PRIMARY
                        ) {
                            Res.string.editor_primary
                        } else {
                            Res.string.editor_secondary
                        },
                    ),
                    style = ForjaTheme.typography.title3,
                )
                Muscle.entries.forEach { muscle ->
                    Row {
                        Checkbox(checked = draft.muscles.any { it.muscle == muscle && it.role == role }, onCheckedChange = { selected ->
                            change(
                                draft.copy(
                                    muscles =
                                    draft.muscles.filterNot { it.muscle == muscle } +
                                        if (selected) listOf(ExerciseMuscle(muscle, role)) else emptyList(),
                                ),
                            )
                        })
                        Text(muscle.label(), modifier = Modifier.padding(ForjaSpacing.esp3), style = ForjaTheme.typography.body)
                    }
                }
            }
            Row {
                Checkbox(draft.unilateral, { change(draft.copy(unilateral = it)) })
                Text(stringResource(Res.string.editor_unilateral), modifier = Modifier.padding(ForjaSpacing.esp3))
            }
            if (!draft.valid) Text(stringResource(Res.string.editor_validation), style = ForjaTheme.typography.body2)
            EditorAction(stringResource(Res.string.editor_save), draft.valid && !state.saving) { onIntent(ExerciseEditorIntent.Save) }
        }
        Spacer(Modifier.height(ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight))
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { onIntent(ExerciseEditorIntent.KeepEditing) },
            title = { Text(stringResource(Res.string.editor_discard)) },
            confirmButton = {
                TextButton(onClick = { onIntent(ExerciseEditorIntent.Discard) }) { Text(stringResource(Res.string.editor_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ExerciseEditorIntent.KeepEditing) }) { Text(stringResource(Res.string.editor_keep)) }
            },
        )
    }
}
