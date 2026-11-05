package com.olarte.ironprogress.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorField
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.catalog_exercise_count
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.resources.editor_edit
import com.olarte.ironprogress.ui.resources.editor_error
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.editor_save
import com.olarte.ironprogress.ui.resources.plan_confirm
import com.olarte.ironprogress.ui.resources.plan_copy_name
import com.olarte.ironprogress.ui.resources.plan_create
import com.olarte.ironprogress.ui.resources.plan_create_folder
import com.olarte.ironprogress.ui.resources.plan_delete
import com.olarte.ironprogress.ui.resources.plan_duplicate
import com.olarte.ironprogress.ui.resources.plan_empty
import com.olarte.ironprogress.ui.resources.plan_folder
import com.olarte.ironprogress.ui.resources.plan_folder_delete
import com.olarte.ironprogress.ui.resources.plan_name
import com.olarte.ironprogress.ui.resources.plan_none
import com.olarte.ironprogress.ui.resources.plan_title
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlanRoute(onEdit: (String?) -> Unit, intervals: @Composable () -> Unit = {}) {
    val model = koinViewModel<PlanViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    PlanScreen(state, model::onIntent, onEdit, intervals)
}

@Composable
fun PlanScreen(state: PlanState, onIntent: (PlanIntent) -> Unit, onEdit: (String?) -> Unit, intervals: @Composable () -> Unit = {}) {
    var folderDialog by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<Folder?>(null) }
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<PlanIntent?>(null) }
    Column(
        Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Text(
            stringResource(Res.string.plan_title),
            Modifier.semantics { heading() },
            style = ForjaTheme.typography.title1,
            color = ForjaTheme.colors.text1,
        )
        intervals()
        EditorAction(stringResource(Res.string.plan_create), !state.busy) { onEdit(null) }
        EditorAction(stringResource(Res.string.plan_create_folder), !state.busy) {
            folder = null
            name = ""
            folderDialog = true
        }
        if (state.loading) Text(stringResource(Res.string.editor_loading))
        if (state.failed) {
            Text(stringResource(Res.string.editor_error), color = ForjaTheme.colors.danger)
            EditorAction(stringResource(Res.string.editor_retry)) { onIntent(PlanIntent.Retry) }
        }
        state.folders.forEachIndexed { folderIndex, item ->
            key(item.id) {
                EditorCard {
                    Text(item.name, style = ForjaTheme.typography.title3)
                    ReorderActions(folderIndex > 0, folderIndex < state.folders.lastIndex, !state.busy) {
                        onIntent(
                            PlanIntent.MoveFolder(
                                item.id,
                                folderIndex + it,
                            ),
                        )
                    }
                    EditorAction(stringResource(Res.string.editor_edit), !state.busy) {
                        folder = item
                        name = item.name
                        folderDialog = true
                    }
                    EditorAction(stringResource(Res.string.plan_folder_delete), !state.busy) { deleting = PlanIntent.FolderDelete(item.id) }
                }
            }
        }
        if (!state.loading && state.routines.isEmpty()) Text(stringResource(Res.string.plan_empty), style = ForjaTheme.typography.body)
        val ordered = state.routines.sortedBy { routine ->
            state.folders.indexOfFirst { it.id == routine.folderId }.let { if (it < 0) state.folders.size else it }
        }
        ordered.forEachIndexed { index, routine ->
            key(routine.id) {
                val copyName = stringResource(Res.string.plan_copy_name, routine.name)
                EditorCard {
                    Text(routine.name, style = ForjaTheme.typography.title3)
                    val count = state.counts[routine.id] ?: 0
                    Text(pluralStringResource(Res.plurals.catalog_exercise_count, count, count), style = ForjaTheme.typography.body2)
                    Text(state.folders.find { it.id == routine.folderId }?.name ?: stringResource(Res.string.plan_none))
                    ReorderActions(
                        index > 0 && ordered[index - 1].folderId == routine.folderId,
                        index < ordered.lastIndex && ordered[index + 1].folderId == routine.folderId,
                        enabled = !state.busy,
                    ) { delta ->
                        onIntent(
                            PlanIntent.Move(
                                routine.id,
                                state.routines.indexOfFirst { it.id == ordered[index + delta].id },
                            ),
                        )
                    }
                    EditorAction(stringResource(Res.string.editor_edit), !state.busy) { onEdit(routine.id.value) }
                    EditorAction(stringResource(Res.string.plan_duplicate), !state.busy) {
                        onIntent(PlanIntent.Duplicate(routine.id, copyName))
                    }
                    EditorAction(stringResource(Res.string.plan_delete), !state.busy) { deleting = PlanIntent.Delete(routine.id) }
                }
            }
        }
        Spacer(Modifier.height(ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight))
    }
    if (folderDialog) {
        AlertDialog(
            onDismissRequest = { folderDialog = false },
            title = { Text(stringResource(Res.string.plan_folder)) },
            text = { EditorField(stringResource(Res.string.plan_name), name) { name = it } },
            confirmButton = {
                EditorAction(stringResource(Res.string.editor_save), name.isNotBlank() && !state.busy) {
                    onIntent(PlanIntent.FolderSave(name, folder))
                    folderDialog = false
                }
            },
        )
    }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.plan_confirm)) },
            confirmButton = {
                EditorAction(stringResource(Res.string.plan_delete), !state.busy) {
                    deleting?.let(onIntent)
                    deleting = null
                }
            },
            dismissButton = { EditorAction(stringResource(Res.string.editor_back)) { deleting = null } },
        )
    }
}
