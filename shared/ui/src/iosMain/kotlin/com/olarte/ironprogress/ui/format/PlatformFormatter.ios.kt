package com.olarte.ironprogress.ui.format

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun platformFormatter(languageTag: String): PlatformFormatter {
    val locale = NSLocale(localeIdentifier = languageTag.replace('-', '_'))
    return ApplePlatformFormatter(locale)
}

private class ApplePlatformFormatter(private val locale: NSLocale) : PlatformFormatter {

    override fun number(value: Double, maxFractionDigits: Int): String {
        val formatter = NSNumberFormatter()
        formatter.numberStyle = NSNumberFormatterDecimalStyle
        formatter.locale = locale
        formatter.minimumFractionDigits = 0u
        formatter.maximumFractionDigits = maxFractionDigits.toULong()
        formatter.usesGroupingSeparator = true
        return formatter.stringFromNumber(NSNumber(double = value)) ?: value.toString()
    }

    override fun date(epochMs: Long): String {
        val formatter = NSDateFormatter()
        formatter.locale = locale
        // Mismo esqueleto que Android: día, mes corto y año en el orden del idioma ("5 nov 2026").
        formatter.setLocalizedDateFormatFromTemplate("dMMMy")
        return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMs / 1_000.0))
    }
}
