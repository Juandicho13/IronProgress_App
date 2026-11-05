package com.olarte.ironprogress.ui.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.format.appLanguage
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
import com.olarte.ironprogress.ui.resources.settings_back
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Copia de seguridad (F1-24), abierta desde Ajustes: Guardar copia es la acción principal;
 * Restaurar y Exportar a CSV son secundarias. Restaurar pide confirmar en danger.
 */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val model = koinViewModel<BackupViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(
        onSaved = { model.onIntent(BackupIntent.Saved(it)) },
        onOpened = { model.onIntent(BackupIntent.Opened(it)) },
    )
    LaunchedEffect(model, files) {
        model.saveRequests.collect { files.save(it.name, it.mimeType, it.content) }
    }
    val language = appLanguage()

    Column(Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding()) {
        ForjaTopBar(stringResource(Res.string.backup_section), onBack, stringResource(Res.string.settings_back))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            ForjaCard {
                ForjaNote(stringResource(Res.string.backup_hint))
                ForjaButton(stringResource(Res.string.backup_create), Modifier.fillMaxWidth(), enabled = !state.busy) {
                    model.onIntent(BackupIntent.CreateBackup)
                }
                ForjaButton(
                    stringResource(Res.string.backup_restore),
                    Modifier.fillMaxWidth(),
                    ForjaButtonStyle.SECONDARY,
                    enabled = !state.busy,
                ) { files.open() }
                ForjaButton(
                    stringResource(Res.string.backup_csv),
                    Modifier.fillMaxWidth(),
                    ForjaButtonStyle.SECONDARY,
                    enabled = !state.busy,
                ) { model.onIntent(BackupIntent.ExportCsv(language)) }
            }
            state.message?.let { message ->
                ForjaNote(stringResource(message.text()), error = message !in listOf(BackupMessage.SAVED, BackupMessage.RESTORED))
            }
        }
    }
    if (state.pendingRestore != null) {
        ForjaDialog(
            title = stringResource(Res.string.backup_confirm_title),
            text = stringResource(Res.string.backup_confirm_body),
            onDismiss = { model.onIntent(BackupIntent.CancelRestore) },
            confirmText = stringResource(Res.string.backup_restore_confirm),
            destructive = true,
            onConfirm = { model.onIntent(BackupIntent.ConfirmRestore) },
            dismissText = stringResource(Res.string.backup_cancel),
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
