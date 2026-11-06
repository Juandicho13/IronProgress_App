package com.olarte.ironprogress.data.local

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Copia de la base antes de migrar (RNF-03). Si el archivo existe y su versión es menor que la
 * del código, lo copia a `ironprogress.db.v<versión>.bak` junto al original y devuelve esa ruta.
 * Si no hay nada que migrar devuelve null. Abrir y cerrar la conexión antes de copiar vacía el
 * WAL en el archivo principal, así la copia queda completa.
 */
fun backupBeforeMigration(
    databasePath: String,
    driver: SQLiteDriver = BundledSQLiteDriver(),
    targetVersion: Int = IronProgressDatabase.SCHEMA_VERSION,
): String? {
    if (!fileExists(databasePath)) return null
    val connection = driver.open(databasePath)
    val version = try {
        val statement = connection.prepare("PRAGMA user_version")
        try {
            if (statement.step()) statement.getLong(0).toInt() else 0
        } finally {
            statement.close()
        }
    } finally {
        connection.close()
    }
    if (version == 0 || version >= targetVersion) return null
    val backupPath = "$databasePath.v$version.bak"
    copyFile(databasePath, backupPath)
    return backupPath
}

internal expect fun fileExists(path: String): Boolean

/** Copia [from] en [to] y reemplaza [to] si ya existía. */
internal expect fun copyFile(from: String, to: String)
