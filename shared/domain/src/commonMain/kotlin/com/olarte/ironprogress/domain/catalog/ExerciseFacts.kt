package com.olarte.ironprogress.domain.catalog

import kotlinx.serialization.Serializable

/** Articulaciones que mira la adaptación (HANDOFF 10.3). Nuevas al final. */
@Serializable
enum class Joint { KNEE, HIP, SHOULDER, ELBOW, WRIST, LOWER_BACK }

@Serializable
enum class JointImpact { LOW, MEDIUM, HIGH }

/**
 * Qué tan exigente es un ejercicio (HANDOFF 11.10). [jointImpact] solo trae las articulaciones
 * con impacto medio o alto; las que faltan son bajo. [met] sale del Compendio de actividades
 * físicas 2024 y es null cuando no hay un código claro.
 */
data class ExerciseDemand(
    val overall: Int,
    val technique: Int,
    val spine: Int,
    val jointImpact: Map<Joint, JointImpact> = emptyMap(),
    val met: Double? = null,
) {
    init {
        require(overall in 1..5) { "La exigencia general va de 1 a 5" }
        require(technique in 1..5) { "La dificultad técnica va de 1 a 5" }
        require(spine in 0..3) { "La carga en la columna va de 0 a 3" }
        require(met == null || met > 0) { "El MET tiene que ser mayor que 0" }
    }

    fun impactOn(joint: Joint): JointImpact = jointImpact[joint] ?: JointImpact.LOW
}

/** La escalera de un ejercicio: el más fácil y el más difícil (11.10). */
data class ExerciseProgression(val easier: ExerciseId?, val harder: ExerciseId?)

/** De dónde salió un ejercicio y con qué licencia (RNF-16). */
data class ExerciseSource(val source: String, val license: String, val author: String? = null)

/** Todo lo del catálogo que no está en [Exercise]: lo usan la ficha y la adaptación. */
data class ExerciseFacts(val demand: ExerciseDemand?, val progression: ExerciseProgression?, val source: ExerciseSource?)
