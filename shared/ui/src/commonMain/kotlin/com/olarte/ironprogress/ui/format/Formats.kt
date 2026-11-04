package com.olarte.ironprogress.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Formato de lo que se muestra: pesos, volúmenes, duraciones, distancias y fechas. Todo se guarda
 * en kg, metros y segundos; aquí solo cambia cómo se ve. Ninguna pantalla arma un número a mano.
 */
class Formats(private val platform: PlatformFormatter) {

    /** "62,5 kg" o "137,8 lb". Hasta dos decimales para los discos de 1,25. */
    fun weight(kg: Double, unit: WeightUnit = WeightUnit.KG): String = when (unit) {
        WeightUnit.KG -> platform.number(kg, maxFractionDigits = 2) + " kg"
        WeightUnit.LB -> platform.number(kg * LB_PER_KG, maxFractionDigits = 1) + " lb"
    }

    /** Lastre en dominadas o fondos: "+10 kg"; asistencia: "−20 kg". */
    fun signedWeight(kg: Double, unit: WeightUnit = WeightUnit.KG): String = when {
        kg > 0 -> "+" + weight(kg, unit)
        kg < 0 -> "−" + weight(abs(kg), unit)
        else -> weight(0.0, unit)
    }

    /** Volumen de una sesión, sin decimales: "8.420 kg". */
    fun volume(kg: Double, unit: WeightUnit = WeightUnit.KG): String = when (unit) {
        WeightUnit.KG -> platform.number(kg.roundToLong().toDouble(), maxFractionDigits = 0) + " kg"
        WeightUnit.LB -> platform.number((kg * LB_PER_KG).roundToLong().toDouble(), maxFractionDigits = 0) + " lb"
    }

    /** Número sin unidad con los separadores del idioma. */
    fun number(value: Double, maxFractionDigits: Int = 1): String = platform.number(value, maxFractionDigits)

    /** "800 m" por debajo de 1 km; desde ahí "5,2 km". */
    fun distance(meters: Double): String = if (meters < 1_000) {
        platform.number(meters.roundToLong().toDouble(), maxFractionDigits = 0) + " m"
    } else {
        platform.number(meters / 1_000, maxFractionDigits = 2) + " km"
    }

    fun date(epochMs: Long): String = platform.date(epochMs)

    companion object {
        const val LB_PER_KG = 2.2046226218

        /** "1:12" o "1:02:05", igual en todos los idiomas. */
        fun duration(totalSeconds: Long): String {
            val seconds = totalSeconds.coerceAtLeast(0)
            val h = seconds / 3_600
            val m = (seconds % 3_600) / 60
            val s = seconds % 60
            return if (h > 0) "$h:${m.twoDigits()}:${s.twoDigits()}" else "$m:${s.twoDigits()}"
        }

        private fun Long.twoDigits(): String = toString().padStart(2, '0')
    }
}

/** Formatos del idioma y la región del teléfono, recordados mientras no cambien. */
@Composable
fun rememberFormats(): Formats {
    val tag = Locale.current.toLanguageTag()
    return remember(tag) { Formats(platformFormatter(tag)) }
}
