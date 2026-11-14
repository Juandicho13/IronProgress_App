package com.olarte.ironprogress.domain.body

import com.olarte.ironprogress.domain.common.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@JvmInline
value class BodyEntryId(val value: String)

/** Un registro de peso corporal (F2-09). La base guarda siempre kg. */
data class BodyWeight(val id: BodyEntryId, val measuredAt: Instant, val weightKg: Double)

/** Medidas del cuerpo que se pueden registrar. [CUSTOM] lleva su propio nombre. */
enum class BodyMeasure { WAIST, CHEST, ARM, THIGH, HIPS, CUSTOM }

/** Una medida en centímetros. Las propias ([BodyMeasure.CUSTOM]) llevan [customName]. */
data class BodyMeasurement(
    val id: BodyEntryId,
    val measuredAt: Instant,
    val kind: BodyMeasure,
    val valueCm: Double,
    val customName: String? = null,
) {
    /** Las medidas propias se agrupan por nombre; las fijas, por su tipo. */
    val key: MeasureKey get() = MeasureKey(kind, customName?.trim()?.takeIf { kind == BodyMeasure.CUSTOM })
}

data class MeasureKey(val kind: BodyMeasure, val customName: String?)

/**
 * Foto de progreso privada (F2-09). [fileName] es el archivo dentro del almacenamiento privado de
 * la app: nunca va a la galería ni a la copia de seguridad.
 */
data class ProgressPhoto(val id: BodyEntryId, val takenAt: Instant, val fileName: String)

/** Peso, medidas y fotos. Las listas salen de la más vieja a la más nueva. */
interface BodyRepository {
    fun observeWeights(): Flow<List<BodyWeight>>

    suspend fun saveWeight(weight: BodyWeight)

    suspend fun deleteWeight(id: BodyEntryId)

    fun observeMeasurements(): Flow<List<BodyMeasurement>>

    suspend fun saveMeasurement(measurement: BodyMeasurement)

    suspend fun deleteMeasurement(id: BodyEntryId)

    fun observePhotos(): Flow<List<ProgressPhoto>>

    suspend fun savePhoto(photo: ProgressPhoto)

    suspend fun deletePhoto(id: BodyEntryId)
}

/** Límites razonables para no guardar un dedo de más (780 kg o 7 cm de cintura). */
object BodyLimits {
    val WEIGHT_KG = 20.0..400.0
    val MEASURE_CM = 10.0..300.0
    const val CUSTOM_NAME_MAX = 40
}

/** Registra el peso de hoy. Responde false si está fuera de [BodyLimits.WEIGHT_KG]. */
class LogBodyWeight(private val repository: BodyRepository, private val clock: Clock, private val ids: IdGenerator) {
    suspend operator fun invoke(weightKg: Double): Boolean {
        if (weightKg.isNaN() || weightKg !in BodyLimits.WEIGHT_KG) return false
        repository.saveWeight(BodyWeight(BodyEntryId(ids.newId()), clock.now(), roundTo(weightKg, 100)))
        return true
    }
}

/**
 * Registra una medida de hoy. Responde false si está fuera de [BodyLimits.MEASURE_CM] o si es una
 * medida propia sin nombre.
 */
class LogBodyMeasurement(private val repository: BodyRepository, private val clock: Clock, private val ids: IdGenerator) {
    suspend operator fun invoke(kind: BodyMeasure, valueCm: Double, customName: String? = null): Boolean {
        if (valueCm.isNaN() || valueCm !in BodyLimits.MEASURE_CM) return false
        val name = customName?.trim()?.take(BodyLimits.CUSTOM_NAME_MAX)?.takeIf { it.isNotEmpty() }
        if (kind == BodyMeasure.CUSTOM && name == null) return false
        repository.saveMeasurement(
            BodyMeasurement(
                BodyEntryId(ids.newId()),
                clock.now(),
                kind,
                roundTo(valueCm, 10),
                name.takeIf { kind == BodyMeasure.CUSTOM },
            ),
        )
        return true
    }
}

/** Guarda la referencia a una foto ya copiada al almacenamiento privado. */
class AddProgressPhoto(private val repository: BodyRepository, private val clock: Clock, private val ids: IdGenerator) {
    suspend operator fun invoke(fileName: String): ProgressPhoto? {
        if (fileName.isBlank()) return null
        val photo = ProgressPhoto(BodyEntryId(ids.newId()), clock.now(), fileName)
        repository.savePhoto(photo)
        return photo
    }
}

/** La última medida de un tipo y cuánto cambió frente a la anterior. */
data class MeasureTrend(val key: MeasureKey, val latest: BodyMeasurement, val changeCm: Double?)

/** Cuentas del cuerpo sin pantalla: cambios y últimas medidas. */
object BodyTrends {
    /**
     * Cuánto cambió el peso en los últimos [days] días: el último registro menos el más reciente
     * que tenga al menos [days] días; si no hay uno tan viejo, el primero de todos. Null con menos
     * de dos registros.
     */
    fun weightChange(weights: List<BodyWeight>, now: Instant, days: Int = 30): Double? {
        if (weights.size < 2) return null
        val ordered = weights.sortedBy { it.measuredAt }
        val latest = ordered.last()
        val cutoff = now - days.days
        val base = ordered.lastOrNull { it.measuredAt <= cutoff } ?: ordered.first()
        if (base.id == latest.id) return null
        return roundTo(latest.weightKg - base.weightKg, 100)
    }

    /** Una fila por medida, en el orden de [BodyMeasure], con la propia al final y por nombre. */
    fun latestMeasures(measurements: List<BodyMeasurement>): List<MeasureTrend> = measurements.groupBy { it.key }.map { (key, items) ->
        val ordered = items.sortedBy { it.measuredAt }
        val latest = ordered.last()
        val previous = ordered.getOrNull(ordered.size - 2)
        MeasureTrend(key, latest, previous?.let { roundTo(latest.valueCm - it.valueCm, 10) })
    }.sortedWith(compareBy<MeasureTrend> { it.key.kind.ordinal }.thenBy { it.key.customName.orEmpty() })
}

private fun roundTo(value: Double, factor: Int): Double = kotlin.math.round(value * factor) / factor
