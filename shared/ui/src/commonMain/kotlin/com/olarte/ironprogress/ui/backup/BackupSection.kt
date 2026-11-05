package com.olarte.ironprogress.ui.backup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.backup_cancel
import com.olarte.ironprogress.ui.resources.backup_confirm_body
import com.olarte.ironprogress.ui.resources.backup_confirm_title
import com.olarte.ironprogress.ui.resources.backup_create
import com.olarte.ironprogress.ui.resources.backup_csv
import com.olarte.ironprogress.ui.resources.backup_hint
import com.olarte.ironprogress.ui.resources.backup_msg_not_backup
import com.olarte.ironprogress.ui.resources.backup_msg_read_failed
import com.olarte.ironprogress.ui.resources.backup_msg_restored
import com.olarte.ironprogress.ui.resources.backup_msg_save_failed
import com.olarte.ironprogress.ui.resources.backup_msg_saved
import com.olarte.ironprogress.ui.resources.backup_msg_too_new
import com.olarte.ironprogress.ui.resources.backup_restore
import com.olarte.ironprogress.ui.resources.backup_restore_confirm
import com.olarte.ironprogress.ui.resources.backup_section
import com.olarte.ironprogress.ui.settings.SectionLabel
import com.olarte.ironprogress.ui.settings.SettingsCard
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sección Copia de seguridad de Ajustes (F1-24). */
@Composable
fun BackupSection() {
    val model = koinViewModel<BackupViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(
        onSaved = { model.onIntent(BackupIntent.Saved(it)) },
        onOpened = { model.onIntent(BackupIntent.Opened(it)) },
    )
    LaunchedEffect(model, files) {
        model.saveRequests.collect { files.save(it.name, it.mimeType, it.content) }
    }
    val language = Locale.current.language

    SectionLabel(stringResource(Res.string.backup_section))
    SettingsCard {
        Text(stringResource(Res.string.backup_hint), style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
        EditorAction(stringResource(Res.string.backup_create), enabled = !state.busy) { model.onIntent(BackupIntent.CreateBackup) }
        EditorAction(stringResource(Res.string.backup_restore), enabled = !state.busy) { files.open() }
        EditorAction(stringResource(Res.string.backup_csv), enabled = !state.busy) { model.onIntent(BackupIntent.ExportCsv(language)) }
        state.message?.let { message ->
            val color = if (message in
                listOf(BackupMessage.SAVED, BackupMessage.RESTORED)
            ) {
                ForjaTheme.colors.text1
            } else {
                ForjaTheme.colors.danger
            }
            Text(stringResource(message.text()), style = ForjaTheme.typography.body, color = color)
        }
    }
    if (state.pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { model.onIntent(BackupIntent.CancelRestore) },
            title = { Text(stringResource(Res.string.backup_confirm_title), style = ForjaTheme.typography.title3) },
            text = { Text(stringResource(Res.string.backup_confirm_body), style = ForjaTheme.typography.body) },
            confirmButton = {
                TextButton(onClick = { model.onIntent(BackupIntent.ConfirmRestore) }) {
                    Text(stringResource(Res.string.backup_restore_confirm), color = ForjaTheme.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { model.onIntent(BackupIntent.CancelRestore) }) { Text(stringResource(Res.string.backup_cancel)) }
            },
        )
    }
}

private fun BackupMessage.text() = when (this) {
    BackupMessage.SAVED -> Res.string.backup_msg_saved
    BackupMessage.SAVE_FAILED -> Res.string.backup_msg_save_failed
    BackupMessage.RESTORED -> Res.string.backup_msg_restored
    BackupMessage.NOT_A_BACKUP -> Res.string.backup_msg_not_backup
    BackupMessage.TOO_NEW -> Res.string.backup_msg_too_new
    BackupMessage.READ_FAILED -> Res.string.backup_msg_read_failed
}
