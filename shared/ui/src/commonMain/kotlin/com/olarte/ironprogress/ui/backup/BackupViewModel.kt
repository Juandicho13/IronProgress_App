package com.olarte.ironprogress.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.backup.BackupRepository
import com.olarte.ironprogress.domain.backup.RestoreResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class BackupMessage {
    SAVED,
    SAVE_FAILED,
    RESTORED,
    NOT_A_BACKUP,
    TOO_NEW,
    READ_FAILED,
}

data class BackupState(
    val busy: Boolean = false,
    val message: BackupMessage? = null,
    /** Copia leída que espera la confirmación: restaurar reemplaza todo lo del teléfono. */
    val pendingRestore: String? = null,
)

sealed interface BackupIntent {
    data object CreateBackup : BackupIntent

    data class ExportCsv(val languageCode: String) : BackupIntent

    /** null: la persona canceló el selector. */
    data class Saved(val ok: Boolean?) : BackupIntent

    data class Opened(val content: String?) : BackupIntent

    data object ConfirmRestore : BackupIntent

    data object CancelRestore : BackupIntent
}

/** Lo que el selector de archivos tiene que guardar. */
data class SaveRequest(val name: String, val mimeType: String, val content: String)

/** Copia de seguridad y exportación en Ajustes (DAT-01). */
class BackupViewModel(private val backups: BackupRepository, private val clock: Clock) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private val saves = Channel<SaveRequest>(Channel.BUFFERED)
    val saveRequests: Flow<SaveRequest> = saves.receiveAsFlow()

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.CreateBackup -> produce { SaveRequest("ironprogress-${today()}.json", "application/json", backups.createBackup()) }
            is BackupIntent.ExportCsv -> produce {
                SaveRequest("ironprogress-series-${today()}.csv", "text/csv", backups.exportSetsCsv(intent.languageCode))
            }
            is BackupIntent.Saved -> _state.update {
                val message = when (intent.ok) {
                    true -> BackupMessage.SAVED
                    false -> BackupMessage.SAVE_FAILED
                    null -> null
                }
                it.copy(busy = false, message = message)
            }
            is BackupIntent.Opened -> _state.update {
                if (intent.content == null) {
                    it.copy(busy = false, message = null)
                } else {
                    it.copy(busy = false, message = null, pendingRestore = intent.content)
                }
            }
            BackupIntent.CancelRestore -> _state.update { it.copy(pendingRestore = null) }
            BackupIntent.ConfirmRestore -> restore()
        }
    }

    private fun produce(build: suspend () -> SaveRequest) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val request = runCatching { build() }.getOrNull()
            if (request == null) {
                _state.update { it.copy(busy = false, message = BackupMessage.SAVE_FAILED) }
            } else {
                saves.send(request)
            }
        }
    }

    private fun restore() {
        val json = _state.value.pendingRestore ?: return
        _state.update { it.copy(busy = true, pendingRestore = null, message = null) }
        viewModelScope.launch {
            val message = when (runCatching { backups.restore(json) }.getOrNull()) {
                is RestoreResult.Restored -> BackupMessage.RESTORED
                RestoreResult.TooNew -> BackupMessage.TOO_NEW
                else -> BackupMessage.NOT_A_BACKUP
            }
            _state.update { it.copy(busy = false, message = message) }
        }
    }

    private fun today(): String = clock.now().toString().take(10)
}
