package com.olarte.ironprogress.data.settings

import android.content.Context

/** Ajustes en la carpeta privada de la app. Llámala una sola vez (single de Koin). */
fun createSettingsRepository(context: Context): DataStoreSettingsRepository =
    DataStoreSettingsRepository.create(context.applicationContext.filesDir.resolve(DataStoreSettingsRepository.FILE_NAME).absolutePath)
