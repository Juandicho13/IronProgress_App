package com.olarte.ironprogress.ui.format

/**
 * Lo que cada plataforma formatea con sus reglas del idioma y la región: separador decimal y de
 * miles, y el nombre de los meses. Android usa java.text e iOS usa NSNumberFormatter y
 * NSDateFormatter (RNF-07).
 */
interface PlatformFormatter {
    /** [value] con hasta [maxFractionDigits] decimales, sin ceros de sobra: 62,5 y no 62,50. */
    fun number(value: Double, maxFractionDigits: Int): String

    /** Fecha corta del idioma: "4 nov 2026" en español, "Nov 4, 2026" en inglés. */
    fun date(epochMs: Long): String
}

/** [languageTag] en BCP 47, como "es-CO" o "en-US". */
expect fun platformFormatter(languageTag: String): PlatformFormatter
