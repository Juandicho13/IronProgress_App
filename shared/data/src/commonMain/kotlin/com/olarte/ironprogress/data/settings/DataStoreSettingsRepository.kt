package com.olarte.ironprogress.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

/**
 * Ajustes en DataStore (F1-25). Se guardan como un JSON en una sola clave: cambiar varios campos
 * es una escritura atómica y un campo nuevo toma su valor por defecto sin migración.
 * Un JSON dañado vuelve a los valores por defecto en vez de cerrar la app.
 */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.data
        .map { decode(it[KEY]) }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs[KEY] = json.encodeToString(AppSettings.serializer(), transform(decode(prefs[KEY]))) }
    }

    private fun decode(raw: String?): AppSettings =
        raw?.let { runCatching { json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() }
            ?: AppSettings()

    companion object {
        const val FILE_NAME = "ironprogress.preferences_pb"
        private val KEY = stringPreferencesKey("app_settings_v1")
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        /** Crea el DataStore en [path] (absoluta y terminada en .preferences_pb). Uno solo por archivo. */
        fun create(path: String): DataStoreSettingsRepository =
            DataStoreSettingsRepository(PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() }))
    }
}
