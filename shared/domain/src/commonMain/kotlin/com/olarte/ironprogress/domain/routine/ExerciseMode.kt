package com.olarte.ironprogress.domain.routine

/**
 * Cómo se hace un ejercicio, elegido por ejercicio en la rutina y en la sesión.
 *
 * No se guarda como columna propia: sale de los campos que ya existen. Superserie es
 * `supersetGroup`, por tiempo es `timedSeconds`, sin descanso es `restSeconds = 0` y con
 * descanso es todo lo demás (null usa el descanso de la rutina).
 */
enum class ExerciseMode {
    /** Series con descanso entre una y otra. */
    REST,

    /** Series seguidas, sin descanso. */
    NO_REST,

    /** Cada serie dura un tiempo fijo, con cuenta atrás. */
    TIMED,

    /** Superserie con el siguiente ejercicio: el descanso llega al final de la vuelta. */
    SUPERSET,
    ;

    companion object {
        const val DEFAULT_TIMED_SECONDS = 30

        fun of(supersetGroup: Int?, restSeconds: Int?, timedSeconds: Int?): ExerciseMode = when {
            supersetGroup != null -> SUPERSET
            timedSeconds != null -> TIMED
            restSeconds == 0 -> NO_REST
            else -> REST
        }
    }
}

/** Valores que guarda cada modo. `restSeconds` null usa el descanso de la rutina. */
data class ExerciseModeValues(val restSeconds: Int?, val timedSeconds: Int?)

/**
 * Campos que quedan al pasar a [mode]. Superserie no toca descanso ni tiempo: el grupo se arma
 * aparte con groupWithNext.
 */
fun ExerciseMode.values(currentRest: Int?, currentTimed: Int?, restSeconds: Int? = null, timedSeconds: Int? = null): ExerciseModeValues =
    when (this) {
        ExerciseMode.REST -> ExerciseModeValues(restSeconds ?: currentRest?.takeIf { it > 0 }, null)
        ExerciseMode.NO_REST -> ExerciseModeValues(0, null)
        ExerciseMode.TIMED -> ExerciseModeValues(
            restSeconds ?: currentRest,
            (timedSeconds ?: currentTimed ?: ExerciseMode.DEFAULT_TIMED_SECONDS).coerceIn(5, 3_600),
        )
        ExerciseMode.SUPERSET -> ExerciseModeValues(currentRest, null)
    }
