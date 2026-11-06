package com.olarte.ironprogress.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private var systemLocale: Locale? = null

@Suppress("DEPRECATION")
@Composable
actual fun appLocaleProvider(languageTag: String?): ProvidedValue<*> {
    val base = systemLocale ?: Locale.getDefault().also { systemLocale = it }
    val locale = languageTag?.let { Locale.forLanguageTag(it) } ?: base
    Locale.setDefault(locale)
    val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
    val resources = LocalContext.current.resources
    resources.updateConfiguration(configuration, resources.displayMetrics)
    return LocalConfiguration.provides(configuration)
}
