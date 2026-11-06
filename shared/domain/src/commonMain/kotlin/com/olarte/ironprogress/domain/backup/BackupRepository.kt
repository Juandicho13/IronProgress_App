package com.olarte.ironprogress.domain.backup

/** Resultado de restaurar una copia (DAT-01). */
sealed interface RestoreResult {
    /** [rows] filas restauradas en total. */
    data class Restored(val rows: Int) : RestoreResult

    /** El archivo no es una copia de IronProgress o está dañado. Nada cambió. */
    data object NotABackup : RestoreResult

    /** La copia viene de una versión más nueva de la app. Nada cambió. */
    data object TooNew : RestoreResult
}

/**
 * Copia de seguridad y exportación (DAT-01). La copia es un JSON propio con todas las tablas del
 * usuario; restaurarla reemplaza lo que haya en el teléfono en una sola transacción.
 */
interface BackupRepository {
    /** JSON de la copia completa. Sirve también como exportación a JSON. */
    suspend fun createBackup(): String

    /** Reemplaza los datos del usuario por los de [json]. Si falla, no cambia nada. */
    suspend fun restore(json: String): RestoreResult

    /** Una fila por serie hecha, para abrir en una hoja de cálculo. [languageCode] elige los nombres. */
    suspend fun exportSetsCsv(languageCode: String): String
}
