package com.olarte.ironprogress.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupFiles(onSaved: (Boolean?) -> Unit, onOpened: (String?) -> Unit): BackupFiles {
    val context = LocalContext.current
    val saved by rememberUpdatedState(onSaved)
    val opened by rememberUpdatedState(onOpened)
    var pending by remember { mutableStateOf<String?>(null) }

    // El tipo de archivo de CreateDocument se fija al crear el lanzador; el de JSON y el de CSV
    // van en lanzadores distintos para que el sistema proponga la extensión correcta.
    fun write(uri: Uri?) {
        val content = pending
        pending = null
        if (uri == null || content == null) return saved(null)
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.encodeToByteArray()) } != null
        }.getOrDefault(false)
        saved(ok)
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { write(it) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { write(it) }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val text = uri?.let {
            runCatching { context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes().decodeToString() } }.getOrNull()
        }
        opened(text)
    }

    return remember {
        object : BackupFiles {
            override fun save(suggestedName: String, mimeType: String, content: String) {
                pending = content
                if (mimeType == "text/csv") createCsv.launch(suggestedName) else createJson.launch(suggestedName)
            }

            override fun open() {
                openDocument.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            }
        }
    }
}
