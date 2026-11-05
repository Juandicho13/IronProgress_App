package com.olarte.ironprogress.ui.backup

import androidx.compose.runtime.Composable

/**
 * Selector de archivos del sistema (F1-24): en Android el de SAF, en iPhone el de Archivos.
 * La persona elige dónde guardar y qué abrir; la app no pide permisos de almacenamiento.
 */
interface BackupFiles {
    /** Pide un lugar y guarda [content]. Responde true si quedó guardado, false si falló y null si se canceló. */
    fun save(suggestedName: String, mimeType: String, content: String)

    /** Pide un archivo y lo devuelve como texto, o null si se canceló o no se pudo leer. */
    fun open()
}

@Composable
expect fun rememberBackupFiles(onSaved: (Boolean?) -> Unit, onOpened: (String?) -> Unit): BackupFiles
