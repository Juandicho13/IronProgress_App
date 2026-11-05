package com.olarte.ironprogress.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val LANGUAGES_KEY = "AppleLanguages"
private val systemLanguage: String = NSLocale.preferredLanguages.firstOrNull() as? String ?: "es"
private val LocalIosLanguage = staticCompositionLocalOf { systemLanguage }

/** Compose Resources lee AppleLanguages: cambiarlo y recomponer basta para los textos. */
@Composable
actual fun appLocaleProvider(languageTag: String?): ProvidedValue<*> {
    val defaults = NSUserDefaults.standardUserDefaults
    if (languageTag == null) defaults.removeObjectForKey(LANGUAGES_KEY) else defaults.setObject(listOf(languageTag), LANGUAGES_KEY)
    return LocalIosLanguage.provides(languageTag ?: systemLanguage)
}
