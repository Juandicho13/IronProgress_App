package com.olarte.ironprogress.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.ExerciseQuery
import com.olarte.ironprogress.domain.catalog.ExerciseSearch
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorChoice
import com.olarte.ironprogress.ui.components.EditorField
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
import com.olarte.ironprogress.ui.resources.plan_edit
import com.olarte.ironprogress.ui.resources.plan_folder
import com.olarte.ironprogress.ui.resources.plan_invalid
import com.olarte.ironprogress.ui.resources.plan_max
import com.olarte.ironprogress.ui.resources.plan_min
import com.olarte.ironprogress.ui.resources.plan_name
import com.olarte.ironprogress.ui.resources.plan_none
import com.olarte.ironprogress.ui.resources.plan_notes
import com.olarte.ironprogress.ui.resources.plan_other_block
import com.olarte.ironprogress.ui.resources.plan_remove
import com.olarte.ironprogress.ui.resources.plan_rest
import com.olarte.ironprogress.ui.resources.plan_rir
import com.olarte.ironprogress.ui.resources.plan_search
import com.olarte.ironprogress.ui.resources.plan_set
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
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
    RoutineEditorScreen(state, model::onIntent)
}

@Composable
fun RoutineEditorScreen(state: RoutineEditorState, onIntent: (RoutineEditorIntent) -> Unit) {
    var picking by remember { mutableStateOf<BlockId?>(null) }
    var search by remember { mutableStateOf("") }
    val language = Locale.current.language
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Text(stringResource(Res.string.plan_edit), style = ForjaTheme.typography.title1, color = ForjaTheme.colors.text1)
        EditorAction(stringResource(Res.string.editor_back), !state.saving) { onIntent(RoutineEditorIntent.Back) }
        if (state.loading) Text(stringResource(Res.string.editor_loading))
        if (state.failed) {
            Text(stringResource(Res.string.editor_error), color = ForjaTheme.colors.danger)
            EditorAction(stringResource(Res.string.editor_retry)) { onIntent(RoutineEditorIntent.Retry) }
        }
        state.detail?.let { detail ->
            EditorField(stringResource(Res.string.plan_name), state.name) { onIntent(RoutineEditorIntent.Name(it)) }
            EditorField(stringResource(Res.string.plan_notes), state.notes) { onIntent(RoutineEditorIntent.Notes(it)) }
            EditorChoice(
                stringResource(Res.string.plan_folder),
                detail.routine.folderId,
                listOf(null) + state.folders.map { it.id },
                { id ->
                    state.folders.find { it.id == id }?.name ?: stringResource(Res.string.plan_none)
                },
            ) { onIntent(RoutineEditorIntent.FolderSelect(it)) }
            detail.blocks.sortedBy { it.sortOrder }.forEachIndexed { index, block ->
                key(block.id) {
                    EditorCard {
                        if (block.format != BlockFormat.SETS) {
                            Text(stringResource(Res.string.plan_other_block), style = ForjaTheme.typography.body)
                        } else {
                            Text(stringResource(Res.string.plan_block, index + 1), style = ForjaTheme.typography.title3)
                            EditorAction(stringResource(Res.string.plan_remove), !state.saving) {
                                onIntent(RoutineEditorIntent.RemoveBlock(block.id))
                            }
                            detail.exercisesOf(block.id).forEachIndexed { position, exercise ->
                                key(exercise.id) {
                                    EditorCard {
                                        Text(
                                            state.catalog.find { it.id == exercise.exerciseId }?.name?.resolve(language)
                                                ?: exercise.exerciseId.value,
                                            style = ForjaTheme.typography.title3,
                                        )
                                        ReorderActions(position > 0, position < detail.exercisesOf(block.id).lastIndex, !state.saving) {
                                            onIntent(RoutineEditorIntent.MoveExercise(exercise.id, position + it))
                                        }
                                        EditorField(stringResource(Res.string.plan_notes), exercise.notes) {
                                            onIntent(RoutineEditorIntent.ExerciseNotes(exercise.id, it))
                                        }
                                        EditorField(stringResource(Res.string.plan_rest), state.rest[exercise.id].orEmpty()) {
                                            onIntent(RoutineEditorIntent.Rest(exercise.id, it))
                                        }
                                        detail.setsOf(exercise.id).forEachIndexed { setIndex, set ->
                                            val draft = state.sets.getValue(set.id)
                                            Text(stringResource(Res.string.plan_set, setIndex + 1), style = ForjaTheme.typography.label)
                                            EditorField(stringResource(Res.string.plan_min), draft.minimum) {
                                                onIntent(RoutineEditorIntent.ChangeSet(set.id, draft.copy(minimum = it)))
                                            }
                                            EditorField(stringResource(Res.string.plan_max), draft.maximum) {
                                                onIntent(RoutineEditorIntent.ChangeSet(set.id, draft.copy(maximum = it)))
                                            }
                                            EditorField(stringResource(Res.string.plan_rir), draft.rir) {
                                                onIntent(RoutineEditorIntent.ChangeSet(set.id, draft.copy(rir = it)))
                                            }
                                            EditorAction(stringResource(Res.string.plan_remove), !state.saving) {
                                                onIntent(RoutineEditorIntent.RemoveSet(set.id))
                                            }
                                        }
                                        EditorAction(stringResource(Res.string.plan_add_set), !state.saving) {
                                            onIntent(RoutineEditorIntent.AddSet(exercise.id))
                                        }
                                        EditorAction(stringResource(Res.string.plan_remove), !state.saving) {
                                            onIntent(RoutineEditorIntent.RemoveExercise(exercise.id))
                                        }
                                    }
                                }
                            }
                            EditorAction(stringResource(Res.string.plan_add_exercise), !state.saving) {
                                search = ""
                                picking = block.id
                            }
                        }
                    }
                }
            }
            EditorAction(stringResource(Res.string.plan_add_block), !state.saving) { onIntent(RoutineEditorIntent.AddBlock) }
            if (!state.valid) Text(stringResource(Res.string.plan_invalid), style = ForjaTheme.typography.body2)
            EditorAction(stringResource(Res.string.editor_save), state.valid && !state.saving) { onIntent(RoutineEditorIntent.Save) }
        }
        Spacer(Modifier.height(ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight))
    }
    if (picking != null) {
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(stringResource(Res.string.plan_add_exercise)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    EditorField(stringResource(Res.string.plan_search), search) { search = it }
                    ExerciseSearch.filter(state.catalog, ExerciseQuery(text = search), language).forEach { exercise ->
                        EditorAction(exercise.name.resolve(language)) {
                            picking?.let { onIntent(RoutineEditorIntent.AddExercise(it, exercise.id)) }
                            picking = null
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { onIntent(RoutineEditorIntent.Keep) },
            title = { Text(stringResource(Res.string.editor_discard)) },
            confirmButton = { EditorAction(stringResource(Res.string.editor_discard)) { onIntent(RoutineEditorIntent.Discard) } },
            dismissButton = { EditorAction(stringResource(Res.string.editor_keep)) { onIntent(RoutineEditorIntent.Keep) } },
        )
    }
}
