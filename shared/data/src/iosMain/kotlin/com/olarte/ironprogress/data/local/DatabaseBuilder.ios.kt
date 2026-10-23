package com.olarte.ironprogress.data.local

import androidx.room3.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Abre la base de IronProgress en la carpeta Documents de la app.
 * Llámala una sola vez (con Koin será un single) y comparte la instancia.
 */
fun createIronProgressDatabase(): IronProgressDatabase {
    val databasePath = documentsDirectory() + "/" + IronProgressDatabase.DATABASE_NAME
    return Room.databaseBuilder<IronProgressDatabase>(
        name = databasePath,
        factory = { IronProgressDatabaseConstructor.initialize() },
    ).buildIronProgressDatabase()
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documents?.path) { "No se encontró la carpeta Documents" }
}
