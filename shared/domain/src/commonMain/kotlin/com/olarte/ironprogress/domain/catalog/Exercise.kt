package com.olarte.ironprogress.domain.catalog

import kotlin.jvm.JvmInline

/**
 * Identificador estable de un ejercicio. En el catálogo base es un slug fijo
 * ("barbell-bench-press"); en los ejercicios propios será un UUID v7.
 */
@JvmInline
value class ExerciseId(val value: String)

/** Un texto en los dos idiomas de la app. */
data class LocalizedText(val es: String, val en: String) {

    /** El texto para [languageCode] ("es", "es-CO", "en"...). Cualquier idioma distinto del español recibe inglés. */
    fun resolve(languageCode: String): String = if (languageCode.lowercase().startsWith("es")) es else en
}

/** Cuánto cuenta una serie para un músculo: el primario suma 1 y el secundario 0,5. */
enum class MuscleRole(val setWeight: Double) {
    PRIMARY(1.0),
    SECONDARY(0.5),
}

/**
 * Un músculo de un ejercicio. [workShare] es el porcentaje del trabajo (los de un ejercicio suman
 * 100); null en los ejercicios propios que no lo tienen, que siguen con la regla de [role].
 */
data class ExerciseMuscle(val muscle: Muscle, val role: MuscleRole, val workShare: Int? = null) {
    init {
        require(workShare == null || workShare in 1..100) { "El porcentaje va de 1 a 100" }
    }
}

data class Exercise(
    val id: ExerciseId,
    val name: LocalizedText,
    val trackingType: TrackingType,
    val equipment: Equipment,
    val pattern: MovementPattern,
    val muscles: List<ExerciseMuscle>,
    /** Se registra por lado (izquierdo y derecho), como el remo con mancuerna. */
    val unilateral: Boolean = false,
    /** Unidad para mostrar este ejercicio; null usa la preferencia general. Todo se guarda en kilos. */
    val preferredUnit: WeightUnit? = null,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val discipline: Discipline = Discipline.STRENGTH,
    /** null mientras el catálogo no lo marque (F1-06). */
    val level: ExerciseLevel? = null,
    /** Otros nombres con los que se busca, como el sánscrito en yoga. */
    val aliases: List<String> = emptyList(),
) {
    init {
        require(pattern == MovementPattern.CARDIO || muscles.any { it.role == MuscleRole.PRIMARY }) {
            "El ejercicio ${id.value} necesita al menos un músculo primario"
        }
        require(muscles.distinctBy { it.muscle }.size == muscles.size) {
            "El ejercicio ${id.value} repite un músculo"
        }
    }

    val primaryMuscles: List<Muscle>
        get() = muscles.filter { it.role == MuscleRole.PRIMARY }.map { it.muscle }

    val secondaryMuscles: List<Muscle>
        get() = muscles.filter { it.role == MuscleRole.SECONDARY }.map { it.muscle }

    /** Cuánto suma una serie de este ejercicio a [muscle]: 1, 0,5 o 0 si no lo trabaja. */
    fun setWeightFor(muscle: Muscle): Double = muscles.firstOrNull { it.muscle == muscle }?.role?.setWeight ?: 0.0
}

/** Nivel de un ejercicio en el catálogo. */
enum class ExerciseLevel { BEGINNER, INTERMEDIATE, ADVANCED }
