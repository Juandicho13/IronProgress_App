package com.olarte.ironprogress.ui.format

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

/**
 * El primero de los idiomas del iPhone ("es-CO"); si viene sin región se le pone la del teléfono
 * para que los números y las fechas usen la forma local.
 */
actual fun systemLanguageTag(): String {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String ?: "es"
    if (preferred.contains('-')) return preferred
    val region = NSLocale.currentLocale.countryCode
    return if (region.isNullOrBlank()) preferred else "$preferred-$region"
}

actual fun systemRegion(): String? = NSLocale.currentLocale.countryCode?.ifBlank { null }
