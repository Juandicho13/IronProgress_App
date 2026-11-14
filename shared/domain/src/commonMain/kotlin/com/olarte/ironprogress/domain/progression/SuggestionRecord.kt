package com.olarte.ironprogress.domain.progression

import com.olarte.ironprogress.domain.session.WorkoutSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lo que el motor sugirió para una serie, guardado como JSON en `WorkoutSet.suggestion` (F2-02)
 * para mostrar su porqué y comparar después con lo que de verdad se hizo. Los nombres de
 * [SerialName] son estables: no se cambian.
 */
@Serializable
data class SuggestionRecord(
    @SerialName("why") val reason: SuggestionReason,
    @SerialName("w") val weightKg: Double? = null,
    @SerialName("r") val reps: Int? = null,
    @SerialName("min") val repsMin: Int? = null,
    @SerialName("max") val repsMax: Int? = null,
    @SerialName("rir") val targetRir: Int? = null,
    @SerialName("pn") val previousSets: Int? = null,
    @SerialName("pr") val previousMinReps: Int? = null,
    @SerialName("prx") val previousMaxReps: Int? = null,
    @SerialName("pw") val previousWeightKg: Double? = null,
    @SerialName("pir") val previousRir: Int? = null,
    /** Variante a la que propone pasar la escalera (F2-12). */
    @SerialName("to") val ladderTarget: String? = null,
    /** Serie de una descarga: esa sesión no cuenta para el estancamiento (F2-05). */
    @SerialName("dl") val deload: Boolean = false,
    @SerialName("v") val version: Int = 1,
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    /** Si [set] sigue tal como lo propuso el motor: nadie tocó el peso ni las reps. */
    fun matches(set: WorkoutSet): Boolean {
        val sameWeight = weightKg == null || set.weightKg?.let { kotlin.math.abs(it - weightKg) < 0.001 } == true
        val sameReps = reps == null || set.reps == reps
        return sameWeight && sameReps
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        /** Null si el texto no es un registro válido, por ejemplo uno de una versión futura. */
        fun decode(text: String?): SuggestionRecord? {
            if (text.isNullOrBlank()) return null
            return try {
                json.decodeFromString(serializer(), text)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** El registro de la serie [index] de las de trabajo planeadas hoy. */
fun Suggestion.recordFor(index: Int): SuggestionRecord = SuggestionRecord(
    reason = reason,
    weightKg = weightKg,
    reps = reps.getOrNull(index) ?: reps.lastOrNull(),
    repsMin = range.min,
    repsMax = range.max,
    targetRir = range.targetRir,
    previousSets = previous?.sets,
    previousMinReps = previous?.minReps,
    previousMaxReps = previous?.maxReps,
    previousWeightKg = previous?.weightKg,
    previousRir = previous?.minRir,
)

/** Lo que sugirió el motor para esta serie, si lo hay. */
val WorkoutSet.suggestionRecord: SuggestionRecord? get() = SuggestionRecord.decode(suggestion)

/**
 * Al completar una serie, la siguiente pendiente sigue el peso que de verdad se usó si nadie la
 * había tocado: si la sugerencia era 62,5 kg y se hizo con 60, la siguiente pasa a 60 y conserva
 * sus reps. Devuelve null si no hay nada que cambiar.
 */
fun carryWeightForward(completed: WorkoutSet, next: WorkoutSet): WorkoutSet? {
    if (next.isCompleted) return null
    val record = next.suggestionRecord ?: return null
    if (!record.matches(next)) return null
    val used = completed.weightKg ?: return null
    if (next.weightKg != null && kotlin.math.abs(next.weightKg - used) < 0.001) return null
    return next.copy(weightKg = used)
}
