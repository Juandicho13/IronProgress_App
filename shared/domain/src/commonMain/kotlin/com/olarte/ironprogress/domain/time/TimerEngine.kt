package com.olarte.ironprogress.domain.time

/**
 * Motor de tiempo (HANDOFF 11.9). El estado sale de instantes, nunca de contar segundos: con el
 * plan, el instante de inicio, las ediciones y el instante actual, la pantalla, la Live Activity,
 * la Live Update y los relojes calculan siempre lo mismo. Todos los instantes en ms desde época.
 */
fun TimerPlan.stateAt(nowMs: Long, startedAtMs: Long, edits: List<TimerEdit> = emptyList()): TimerState {
    val durations = segments.map { it.durationMs }.toMutableList()
    var pausedTotalMs = 0L
    var pausedSinceMs: Long? = null

    fun activeAt(atMs: Long): Long {
        val pauseNow = pausedSinceMs?.let { atMs - it } ?: 0L
        return (atMs - startedAtMs - pausedTotalMs - pauseNow).coerceAtLeast(0L)
    }

    for (edit in edits.filter { it.atMs <= nowMs }.sortedBy { it.atMs }) {
        when (edit) {
            is TimerEdit.Pause -> if (pausedSinceMs == null) pausedSinceMs = edit.atMs
            is TimerEdit.Resume -> pausedSinceMs?.let { since ->
                pausedTotalMs += edit.atMs - since
                pausedSinceMs = null
            }
            is TimerEdit.Adjust -> {
                val position = locate(durations, activeAt(edit.atMs))
                val duration = durations[position.index]
                if (!position.finished && duration != null) {
                    // Restar nunca deja el tramo por debajo de lo que ya pasó: termina en ese instante.
                    durations[position.index] = (duration + edit.deltaMs).coerceAtLeast(position.elapsedInSegment)
                }
            }
            is TimerEdit.Skip -> {
                val position = locate(durations, activeAt(edit.atMs))
                if (!position.finished) durations[position.index] = position.elapsedInSegment
            }
        }
    }

    val active = activeAt(nowMs)
    val position = locate(durations, active)
    val segment = segments[position.index]
    val duration = durations[position.index]
    return TimerState(
        segmentIndex = position.index,
        kind = segment.kind,
        round = segment.round,
        set = segment.set,
        label = segment.label,
        elapsedInSegmentMs = position.elapsedInSegment,
        remainingInSegmentMs = duration?.let { (it - position.elapsedInSegment).coerceAtLeast(0L) },
        segmentDurationMs = duration,
        totalElapsedMs = if (position.finished) durations.sumOf { it ?: 0L } else active,
        isPaused = pausedSinceMs != null && !position.finished,
        isFinished = position.finished,
    )
}

/**
 * Instante (ms desde época) en que termina el tramo en curso si nadie toca nada. Es lo que se
 * le manda a la Live Activity (`Text(timerInterval:)`) y a la Live Update (`setWhen`). null si
 * el tramo es abierto, está en pausa o el plan ya terminó.
 */
fun TimerPlan.segmentEndsAtMs(nowMs: Long, startedAtMs: Long, edits: List<TimerEdit> = emptyList()): Long? {
    val state = stateAt(nowMs, startedAtMs, edits)
    if (state.isFinished || state.isPaused) return null
    return state.remainingInSegmentMs?.let { nowMs + it }
}

/** Lo que el reloj anuncia entre dos instantes: la voz y la vibración salen de aquí. */
sealed interface TimerEvent {
    data class SegmentStarted(val index: Int, val kind: SegmentKind, val round: Int, val label: String?) : TimerEvent

    /** Mitad de un tramo de trabajo ("Mitad. No pares"). */
    data class Halfway(val index: Int) : TimerEvent

    /** Cuenta atrás de los últimos [COUNTDOWN_SECONDS] segundos de un tramo con duración. */
    data class SecondsLeft(val index: Int, val seconds: Long) : TimerEvent

    data object Finished : TimerEvent

    companion object {
        const val COUNTDOWN_SECONDS = 5L
    }
}

/**
 * Eventos que ocurrieron después de [fromMs] y hasta [toMs], incluido. Quien llama lo consulta
 * seguido (cada cuadro o cada 100 ms) y pasa el instante de la consulta anterior como [fromMs].
 */
fun TimerPlan.eventsBetween(fromMs: Long, toMs: Long, startedAtMs: Long, edits: List<TimerEdit> = emptyList()): List<TimerEvent> {
    if (toMs <= fromMs) return emptyList()
    val before = stateAt(fromMs, startedAtMs, edits)
    val after = stateAt(toMs, startedAtMs, edits)
    if (before.isFinished) return emptyList()

    val events = mutableListOf<TimerEvent>()
    // Tramos que empezaron en el intervalo (puede saltarse más de uno si la consulta tardó).
    val lastStarted = if (after.isFinished) segments.lastIndex else after.segmentIndex
    for (index in before.segmentIndex + 1..lastStarted) {
        val segment = segments[index]
        events += TimerEvent.SegmentStarted(index, segment.kind, segment.round, segment.label)
    }
    if (after.isFinished) {
        events += TimerEvent.Finished
        return events
    }
    val sameSegment = before.segmentIndex == after.segmentIndex
    val duration = after.segmentDurationMs
    if (after.kind == SegmentKind.WORK && duration != null) {
        val half = duration / 2
        val startElapsed = if (sameSegment) before.elapsedInSegmentMs else -1L
        if (startElapsed < half && after.elapsedInSegmentMs >= half) events += TimerEvent.Halfway(after.segmentIndex)
    }
    val secondsAfter = after.displaySecondsLeft
    if (secondsAfter != null) {
        // En un tramo que acaba de empezar se cuenta desde su duración, no desde el anterior.
        val secondsBefore = if (sameSegment) {
            before.displaySecondsLeft ?: Long.MAX_VALUE
        } else {
            (duration ?: 0L).let { (it + 999) / 1000 } + 1
        }
        for (seconds in minOf(secondsBefore - 1, TimerEvent.COUNTDOWN_SECONDS) downTo maxOf(secondsAfter, 1L)) {
            events += TimerEvent.SecondsLeft(after.segmentIndex, seconds)
        }
    }
    return events
}

private class Position(val index: Int, val elapsedInSegment: Long, val finished: Boolean)

private fun locate(durations: List<Long?>, activeMs: Long): Position {
    var start = 0L
    for ((index, duration) in durations.withIndex()) {
        if (duration == null || activeMs < start + duration) {
            return Position(index, activeMs - start, finished = false)
        }
        start += duration
    }
    val last = durations.lastIndex
    return Position(last, durations[last] ?: 0L, finished = true)
}
