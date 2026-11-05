package com.olarte.ironprogress.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Idioma con el que se ve la app, en BCP 47 ("es-CO", "en-US"): el elegido en Ajustes o, si no hay,
 * el primero de los idiomas preferidos del teléfono. En iPhone no se usa Locale.current porque
 * sigue al idioma del paquete de la app y no al de Ajustes, y salían fechas en inglés.
 */
val LocalAppLanguageTag = staticCompositionLocalOf<String?> { null }

/** Idioma preferido del teléfono con su región, como "es-CO". */
expect fun systemLanguageTag(): String

@Composable
@ReadOnlyComposable
fun appLanguageTag(): String = LocalAppLanguageTag.current ?: systemLanguageTag()

/** Solo el idioma ("es" o "en") para los textos del catálogo. */
@Composable
@ReadOnlyComposable
fun appLanguage(): String = appLanguageTag().substringBefore('-').lowercase()
