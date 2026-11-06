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

/** Región del teléfono ("CO", "ES") o null si no hay. */
expect fun systemRegion(): String?

/**
 * Etiqueta para "Del teléfono": el idioma en que de verdad salieron los textos (values o
 * values-es) más la región del teléfono. Así fechas y números nunca quedan en otro idioma que
 * los textos. En iPhone, NSLocale.preferredLanguages podía no coincidir con lo que Compose
 * Resources eligió y Hoy mostraba "NOV 5, 2026" con todo lo demás en español.
 */
fun resolvedLanguageTag(resolvedLanguage: String, region: String?): String =
    if (region.isNullOrBlank()) resolvedLanguage else "$resolvedLanguage-$region"

@Composable
@ReadOnlyComposable
fun appLanguageTag(): String = LocalAppLanguageTag.current ?: systemLanguageTag()

/** Solo el idioma ("es" o "en") para los textos del catálogo. */
@Composable
@ReadOnlyComposable
fun appLanguage(): String = appLanguageTag().substringBefore('-').lowercase()
