package com.olarte.ironprogress.data.local

import kotlinx.serialization.json.Json

/**
 * JSON de las columnas de texto que guardan listas u objetos (HANDOFF 10.3). Ignora claves
 * desconocidas para que una versión vieja de la app lea lo que escribió una más nueva.
 */
internal val RoomJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
