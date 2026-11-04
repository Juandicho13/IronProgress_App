package com.olarte.ironprogress.ui.format

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

actual fun platformFormatter(languageTag: String): PlatformFormatter = JavaPlatformFormatter(Locale.forLanguageTag(languageTag))

private class JavaPlatformFormatter(private val locale: Locale) : PlatformFormatter {

    override fun number(value: Double, maxFractionDigits: Int): String {
        // NumberFormat no es seguro entre hilos: uno nuevo por llamada.
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
            isGroupingUsed = true
        }
        return format.format(value)
    }

    override fun date(epochMs: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(epochMs))
}
