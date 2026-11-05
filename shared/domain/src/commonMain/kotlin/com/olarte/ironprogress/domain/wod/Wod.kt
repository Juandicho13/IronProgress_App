package com.olarte.ironprogress.domain.wod

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.TimerPlans

/** Un movimiento de la ronda: "10 · Wall balls · Balón de 9 kg". */
data class WodMovement(val exerciseId: ExerciseId, val reps: Int, val load: String? = null) {
    init {
        require(reps > 0) { "Cada movimiento necesita reps" }
    }
}

/**
 * Bloque funcional (PANTALLAS 7.4): AMRAP, EMOM, por tiempo con límite o rondas. [minutes] es el
 * total del AMRAP, los minutos del EMOM o el límite del por tiempo (null sin límite).
 */
data class Wod(val name: String, val format: BlockFormat, val minutes: Int?, val movements: List<WodMovement>) {
    init {
        require(format in FORMATS) { "Un bloque funcional es AMRAP, EMOM, por tiempo o rondas" }
        require(name.isNotBlank()) { "El bloque necesita un nombre" }
        require(movements.isNotEmpty()) { "El bloque necesita al menos un movimiento" }
        require(minutes == null || minutes in 1..MAX_MINUTES) { "Los minutos van de 1 a 120" }
        require(format !in setOf(BlockFormat.AMRAP, BlockFormat.EMOM) || minutes != null) { "AMRAP y EMOM necesitan minutos" }
    }

    val repsPerRound: Int get() = movements.sumOf { it.reps }

    /** Cuenta atrás en AMRAP y EMOM; hacia arriba en por tiempo y rondas. */
    val countsDown: Boolean get() = format == BlockFormat.AMRAP || format == BlockFormat.EMOM

    fun toPlan(): TimerPlan = when (format) {
        BlockFormat.AMRAP -> TimerPlans.amrap(minutes!! * 60)
        BlockFormat.EMOM -> TimerPlans.emom(minutes!!)
        BlockFormat.FOR_TIME -> TimerPlans.forTime(minutes?.let { it * 60 })
        else -> TimerPlans.rounds()
    }

    companion object {
        val FORMATS = setOf(BlockFormat.AMRAP, BlockFormat.EMOM, BlockFormat.FOR_TIME, BlockFormat.ROUNDS)
        const val MAX_MINUTES = 120
    }
}

/** El marcador: rondas completas más los movimientos hechos de la ronda en curso. */
data class WodScore(val rounds: Int = 0, val movementsDone: Int = 0) {

    /** Reps de la ronda en curso según los movimientos marcados. */
    fun reps(wod: Wod): Int = wod.movements.take(movementsDone).sumOf { it.reps }

    /** Toca el siguiente movimiento; al marcar el último, la ronda se suma sola. */
    fun tick(wod: Wod): WodScore =
        if (movementsDone + 1 >= wod.movements.size) WodScore(rounds + 1, 0) else copy(movementsDone = movementsDone + 1)

    /** "+ 1 ronda": suma una ronda completa sin tocar lo de la ronda en curso. */
    fun addRound(): WodScore = copy(rounds = rounds + 1)

    /**
     * Ritmo del AMRAP: (rondas + reps hechas / reps por ronda) × total / segundos transcurridos.
     * Null en los primeros 10 s, cuando el ritmo todavía no dice nada.
     */
    fun paceRounds(wod: Wod, elapsedSeconds: Long): Int? {
        if (wod.format != BlockFormat.AMRAP || elapsedSeconds < MIN_PACE_SECONDS) return null
        val done = rounds + reps(wod).toDouble() / wod.repsPerRound
        return (done * wod.minutes!! * 60 / elapsedSeconds).toInt()
    }

    /** El resultado que se guarda según el formato (HANDOFF 10.3). */
    fun result(wod: Wod, elapsedSeconds: Int): BlockResult = when (wod.format) {
        BlockFormat.AMRAP -> BlockResult.RoundsAndReps(rounds, reps(wod))
        BlockFormat.EMOM -> BlockResult.Rounds(rounds)
        else -> BlockResult.Time(elapsedSeconds)
    }

    private companion object {
        const val MIN_PACE_SECONDS = 10L
    }
}

/**
 * ¿Es [candidate] mejor marca que [best]? AMRAP y EMOM: más rondas (y más reps). Por tiempo y
 * rondas: menos segundos.
 */
fun isBetter(candidate: BlockResult, best: BlockResult?): Boolean = when {
    best == null -> true
    candidate is BlockResult.RoundsAndReps && best is BlockResult.RoundsAndReps ->
        candidate.rounds > best.rounds || (candidate.rounds == best.rounds && candidate.reps > best.reps)
    candidate is BlockResult.Rounds && best is BlockResult.Rounds -> candidate.rounds > best.rounds
    candidate is BlockResult.Time && best is BlockResult.Time -> candidate.seconds < best.seconds
    else -> false
}
