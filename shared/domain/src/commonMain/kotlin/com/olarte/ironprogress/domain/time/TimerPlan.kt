package com.olarte.ironprogress.domain.time

/** Qué pasa en un tramo del reloj. La voz, la vibración y el color salen de aquí. */
enum class SegmentKind {
    WARMUP,
    WORK,
    REST,
    COOLDOWN,

    /** Una postura de yoga, pilates o movilidad. */
    POSE,
    INHALE,
    HOLD_IN,
    EXHALE,
    HOLD_OUT,

    /** "Por tiempo" con límite: cuenta hacia arriba y termina al llegar al límite. */
    CAP,

    /** Cuenta hacia arriba sin límite (rondas): termina cuando la persona lo dice. */
    OPEN,
}

/**
 * Un tramo del plan. [durationMs] null significa abierto: cuenta hacia arriba y solo puede
 * ser el último tramo. [round] y [set] empiezan en 1; [label] es el id del ejercicio o la postura.
 */
data class TimerSegment(val kind: SegmentKind, val durationMs: Long?, val round: Int = 1, val set: Int = 1, val label: String? = null) {
    init {
        require(durationMs == null || durationMs >= 0) { "Un tramo no puede durar menos de 0 ms" }
    }
}

/** El plan completo de un bloque: la lista de tramos en orden. */
data class TimerPlan(val segments: List<TimerSegment>) {
    init {
        require(segments.isNotEmpty()) { "El plan necesita al menos un tramo" }
        require(segments.dropLast(1).all { it.durationMs != null }) { "Solo el último tramo puede ser abierto" }
    }

    /** Duración total si ningún tramo es abierto; null si el último cuenta hacia arriba. */
    val totalDurationMs: Long?
        get() = if (segments.last().durationMs == null) null else segments.sumOf { it.durationMs ?: 0L }
}

/** Lo que la persona hace sobre el reloj, con el instante (ms desde época) en que lo hizo. */
sealed interface TimerEdit {
    val atMs: Long

    data class Pause(override val atMs: Long) : TimerEdit

    data class Resume(override val atMs: Long) : TimerEdit

    /** Suma o resta tiempo al tramo en curso (más o menos 15 s del descanso). */
    data class Adjust(override val atMs: Long, val deltaMs: Long) : TimerEdit

    /** Termina el tramo en curso ya (saltar el descanso, terminar el "por tiempo"). */
    data class Skip(override val atMs: Long) : TimerEdit
}

/** Foto del reloj en un instante. Se calcula, nunca se guarda segundo a segundo. */
data class TimerState(
    val segmentIndex: Int,
    val kind: SegmentKind,
    val round: Int,
    val set: Int,
    val label: String?,
    val elapsedInSegmentMs: Long,
    /** null en un tramo abierto. */
    val remainingInSegmentMs: Long?,
    /** Duración del tramo en curso ya con los ajustes; null si es abierto. */
    val segmentDurationMs: Long?,
    val totalElapsedMs: Long,
    val isPaused: Boolean,
    val isFinished: Boolean,
) {
    /** Segundos que se muestran en una cuenta atrás: 1:12 mientras queden entre 71,001 y 72 s. */
    val displaySecondsLeft: Long?
        get() = remainingInSegmentMs?.let { (it + 999) / 1000 }
}
