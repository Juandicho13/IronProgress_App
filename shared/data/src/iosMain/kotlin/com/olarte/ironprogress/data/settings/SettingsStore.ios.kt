package com.olarte.ironprogress.data.settings

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Ajustes en la carpeta Documents de la app. Llámala una sola vez (single de Koin). */
@OptIn(ExperimentalForeignApi::class)
fun createSettingsRepository(): DataStoreSettingsRepository {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val dir = requireNotNull(documents?.path) { "No se encontró la carpeta Documents" }
    return DataStoreSettingsRepository.create("$dir/${DataStoreSettingsRepository.FILE_NAME}")
}
