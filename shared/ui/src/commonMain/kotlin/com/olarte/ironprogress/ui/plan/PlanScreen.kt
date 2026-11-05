package com.olarte.ironprogress.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaLargeTitle
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaSectionLabel
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.components.ForjaTextField
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.catalog_exercise_count
import com.olarte.ironprogress.ui.resources.editor_error
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.editor_save
import com.olarte.ironprogress.ui.resources.plan_add_menu
import com.olarte.ironprogress.ui.resources.plan_confirm
import com.olarte.ironprogress.ui.resources.plan_copy_name
import com.olarte.ironprogress.ui.resources.plan_create
import com.olarte.ironprogress.ui.resources.plan_create_folder
import com.olarte.ironprogress.ui.resources.plan_delete
import com.olarte.ironprogress.ui.resources.plan_delete_body
import com.olarte.ironprogress.ui.resources.plan_down
import com.olarte.ironprogress.ui.resources.plan_duplicate
import com.olarte.ironprogress.ui.resources.plan_edit
import com.olarte.ironprogress.ui.resources.plan_empty
import com.olarte.ironprogress.ui.resources.plan_empty_body
import com.olarte.ironprogress.ui.resources.plan_folder
import com.olarte.ironprogress.ui.resources.plan_folder_delete
import com.olarte.ironprogress.ui.resources.plan_name
import com.olarte.ironprogress.ui.resources.plan_none
import com.olarte.ironprogress.ui.resources.plan_rename
import com.olarte.ironprogress.ui.resources.plan_routines
import com.olarte.ironprogress.ui.resources.plan_title
import com.olarte.ironprogress.ui.resources.plan_up
import com.olarte.ironprogress.ui.resources.ui_cancel
import com.olarte.ironprogress.ui.resources.ui_more
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

/** Lo que abre la hoja de acciones de Plan. */
private sealed interface PlanSheet {
    data object Create : PlanSheet
    data class RoutineActions(val routine: Routine) : PlanSheet
    data class FolderActions(val folder: Folder) : PlanSheet
}

/**
 * Plan (PANTALLAS 2.3 y 9, F1-08): título con el botón + (crear rutina o carpeta), las rutinas
 * agrupadas por carpeta en tarjetas de filas y, debajo, los otros formatos para entrenar. Cada
 * rutina se abre con un toque; duplicar, mover y borrar están en su hoja de acciones.
 */
@Composable
fun PlanScreen(state: PlanState, onIntent: (PlanIntent) -> Unit, onEdit: (String?) -> Unit, intervals: @Composable () -> Unit = {}) {
    var folderDialog by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<Folder?>(null) }
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<PlanIntent?>(null) }
    var sheet by remember { mutableStateOf<PlanSheet?>(null) }
    val colors = ForjaTheme.colors

    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Spacer(Modifier.height(ForjaSpacing.esp3))
        ForjaLargeTitle(stringResource(Res.string.plan_title)) {
            ForjaIconButton(ForjaIcons.Add, stringResource(Res.string.plan_add_menu), enabled = !state.busy) { sheet = PlanSheet.Create }
        }
        if (state.loading) ForjaNote(stringResource(Res.string.editor_loading))
        if (state.failed) {
            ForjaNote(stringResource(Res.string.editor_error), error = true)
            ForjaButton(stringResource(Res.string.editor_retry), style = ForjaButtonStyle.SECONDARY) { onIntent(PlanIntent.Retry) }
        }

        if (!state.loading && state.routines.isEmpty() && state.folders.isEmpty()) {
            ForjaEmptyState(stringResource(Res.string.plan_empty), stringResource(Res.string.plan_empty_body)) {
                ForjaButton(stringResource(Res.string.plan_create), enabled = !state.busy) { onEdit(null) }
            }
        } else if (!state.loading) {
            ForjaSectionLabel(stringResource(Res.string.plan_routines))
            // Rutinas sin carpeta primero, luego cada carpeta en su orden.
            val loose = state.routines.filter { routine -> state.folders.none { it.id == routine.folderId } }
            if (loose.isNotEmpty()) RoutineGroup(loose, state, onEdit) { sheet = PlanSheet.RoutineActions(it) }
            state.folders.forEach { item ->
                key(item.id) {
                    FolderHeader(item.name, !state.busy) { sheet = PlanSheet.FolderActions(item) }
                    val inside = state.routines.filter { it.folderId == item.id }
                    if (inside.isEmpty()) {
                        ForjaNote(stringResource(Res.string.plan_none), modifier = Modifier.padding(start = ForjaSpacing.esp1))
                    } else {
                        RoutineGroup(inside, state, onEdit) { sheet = PlanSheet.RoutineActions(it) }
                    }
                }
            }
        }

        intervals()
        Spacer(Modifier.height(ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight + ForjaSpacing.esp8))
    }

    when (val current = sheet) {
        PlanSheet.Create -> ForjaSheet(stringResource(Res.string.plan_add_menu), onDismiss = { sheet = null }) {
            ForjaListRow(stringResource(Res.string.plan_create), leading = ForjaIcons.Add, chevron = false, onClick = {
                sheet = null
                onEdit(null)
            })
            ForjaDivider()
            ForjaListRow(stringResource(Res.string.plan_create_folder), chevron = false, onClick = {
                sheet = null
                folder = null
                name = ""
                folderDialog = true
            })
        }
        is PlanSheet.RoutineActions -> RoutineActionsSheet(current.routine, state, onIntent, onEdit, onDelete = {
            deleting = PlanIntent.Delete(current.routine.id)
        }) { sheet = null }
        is PlanSheet.FolderActions -> {
            val index = state.folders.indexOfFirst { it.id == current.folder.id }
            ForjaSheet(current.folder.name, onDismiss = { sheet = null }) {
                ForjaListRow(stringResource(Res.string.plan_rename), chevron = false, onClick = {
                    sheet = null
                    folder = current.folder
                    name = current.folder.name
                    folderDialog = true
                })
                ForjaDivider()
                ForjaListRow(stringResource(Res.string.plan_up), chevron = false, enabled = index > 0, onClick = {
                    onIntent(PlanIntent.MoveFolder(current.folder.id, index - 1))
                    sheet = null
                })
                ForjaDivider()
                ForjaListRow(stringResource(Res.string.plan_down), chevron = false, enabled = index < state.folders.lastIndex, onClick = {
                    onIntent(PlanIntent.MoveFolder(current.folder.id, index + 1))
                    sheet = null
                })
                ForjaDivider()
                ForjaListRow(stringResource(Res.string.plan_folder_delete), titleColor = colors.danger, chevron = false, onClick = {
                    sheet = null
                    deleting = PlanIntent.FolderDelete(current.folder.id)
                })
            }
        }
        null -> Unit
    }

    if (folderDialog) {
        ForjaDialog(
            title = stringResource(if (folder == null) Res.string.plan_create_folder else Res.string.plan_folder),
            onDismiss = { folderDialog = false },
            confirmText = stringResource(Res.string.editor_save),
            confirmEnabled = name.isNotBlank() && !state.busy,
            onConfirm = {
                onIntent(PlanIntent.FolderSave(name, folder))
                folderDialog = false
            },
            dismissText = stringResource(Res.string.ui_cancel),
        ) {
            ForjaTextField(stringResource(Res.string.plan_name), name, { name = it })
        }
    }
    if (deleting != null) {
        ForjaDialog(
            title = stringResource(Res.string.plan_confirm),
            text = stringResource(Res.string.plan_delete_body),
            onDismiss = { deleting = null },
            confirmText = stringResource(Res.string.plan_delete),
            destructive = true,
            confirmEnabled = !state.busy,
            onConfirm = {
                deleting?.let(onIntent)
                deleting = null
            },
            dismissText = stringResource(Res.string.ui_cancel),
        )
    }
}

@Composable
private fun FolderHeader(name: String, enabled: Boolean, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ForjaSectionLabel(name, Modifier.weight(1f))
        ForjaIconButton(
            ForjaIcons.MoreVert,
            stringResource(Res.string.ui_more),
            enabled = enabled,
            filled = false,
            tint = ForjaTheme.colors.text2,
        ) {
            onMore()
        }
    }
}

@Composable
private fun RoutineGroup(routines: List<Routine>, state: PlanState, onEdit: (String?) -> Unit, onMore: (Routine) -> Unit) {
    ForjaListGroup {
        routines.forEachIndexed { index, routine ->
            key(routine.id) {
                if (index > 0) ForjaDivider()
                val count = state.counts[routine.id] ?: 0
                ForjaListRow(
                    title = routine.name,
                    subtitle = pluralStringResource(Res.plurals.catalog_exercise_count, count, count),
                    enabled = !state.busy,
                    chevron = false,
                    onClick = { onEdit(routine.id.value) },
                    trailing = {
                        ForjaIconButton(
                            ForjaIcons.MoreVert,
                            stringResource(Res.string.ui_more),
                            enabled = !state.busy,
                            filled = false,
                            tint = ForjaTheme.colors.text2,
                        ) { onMore(routine) }
                    },
                )
            }
        }
    }
}

@Composable
private fun RoutineActionsSheet(
    routine: Routine,
    state: PlanState,
    onIntent: (PlanIntent) -> Unit,
    onEdit: (String?) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    // Mismo orden que la lista: solo se mueve dentro de su carpeta.
    val siblings = state.routines.filter { it.folderId == routine.folderId }
    val position = siblings.indexOfFirst { it.id == routine.id }
    val copyName = stringResource(Res.string.plan_copy_name, routine.name)
    fun moveTo(target: Routine) {
        onIntent(PlanIntent.Move(routine.id, state.routines.indexOfFirst { it.id == target.id }))
        onClose()
    }
    ForjaSheet(routine.name, onDismiss = onClose) {
        ForjaListRow(stringResource(Res.string.plan_edit), chevron = false, onClick = {
            onClose()
            onEdit(routine.id.value)
        })
        ForjaDivider()
        ForjaListRow(stringResource(Res.string.plan_duplicate), chevron = false, onClick = {
            onIntent(PlanIntent.Duplicate(routine.id, copyName))
            onClose()
        })
        ForjaDivider()
        ForjaListRow(stringResource(Res.string.plan_up), chevron = false, enabled = position > 0, onClick = {
            moveTo(siblings[position - 1])
        })
        ForjaDivider()
        ForjaListRow(
            stringResource(Res.string.plan_down),
            chevron = false,
            enabled = position in 0 until siblings.lastIndex,
            onClick = { moveTo(siblings[position + 1]) },
        )
        ForjaDivider()
        ForjaListRow(stringResource(Res.string.plan_delete), titleColor = ForjaTheme.colors.danger, chevron = false, onClick = {
            onClose()
            onDelete()
        })
    }
}
