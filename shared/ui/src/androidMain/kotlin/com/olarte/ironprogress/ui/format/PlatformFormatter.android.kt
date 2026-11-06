package com.olarte.ironprogress.ui.format

import android.text.format.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
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

    /** "5 nov 2026" o "Nov 5, 2026": día, mes corto y año en el orden del idioma. */
    override fun date(epochMs: Long): String =
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, DATE_SKELETON), locale).format(Date(epochMs))

    private companion object {
        const val DATE_SKELETON = "dMMMy"
    }
}
