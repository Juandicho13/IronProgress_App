package com.olarte.ironprogress.domain.voice

import com.olarte.ironprogress.domain.time.SegmentKind
import com.olarte.ironprogress.domain.time.TimerEdit
import com.olarte.ironprogress.domain.time.TimerEvent
import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.eventsBetween
import com.olarte.ironprogress.domain.time.stateAt

/** Lo que la persona eligió para la voz (PANTALLAS 5.2, sección Voz). */
data class VoiceSettings(
    val enabled: Boolean = true,
    /** "Cuenta atrás": los últimos 5 s de cada fase. */
    val countdown: Boolean = true,
    /** "Dice qué sigue": «Descansa», «Burpees en 3, 2, 1». */
    val saysWhatsNext: Boolean = true,
    /** "Baja tu música": solo mientras la voz habla. */
    val duckMusic: Boolean = true,
)

/**
 * Una frase de la voz (PANTALLAS 5.3). La interfaz la convierte en texto del idioma del teléfono
 * y la muestra igual en el subtítulo. [exerciseId] es el id del ejercicio que sigue o empieza.
 */
sealed interface VoiceLine {
    /** «Ya. Todas las que puedas». */
    data class Go(val exerciseId: String?) : VoiceLine

    /** «Mitad. No pares». */
    data object Halfway : VoiceLine

    /** «Cinco», «Cuatro»... */
    data class Count(val seconds: Int) : VoiceLine

    /** «Descansa». */
    data object Rest : VoiceLine

    /** «Quedan treinta segundos». */
    data object ThirtySecondsLeft : VoiceLine

    /** «Siguiente: kettlebell swing». */
    data class Next(val exerciseId: String?) : VoiceLine

    /** «Terminaste. Buen trabajo». */
    data object Done : VoiceLine
}

/**
 * Frases que tocan entre dos instantes. Sale del motor de tiempo (eventsBetween y stateAt), así que
 * con la pantalla apagada o después de una pausa dice lo mismo. Si la consulta llega tarde y se
 * saltó varias fases, solo dice lo de la fase actual: nunca una ráfaga de frases viejas.
 */
fun TimerPlan.voiceLinesBetween(
    fromMs: Long,
    toMs: Long,
    startedAtMs: Long,
    edits: List<TimerEdit> = emptyList(),
    settings: VoiceSettings = VoiceSettings(),
): List<VoiceLine> {
    if (!settings.enabled || toMs <= fromMs || toMs < startedAtMs) return emptyList()
    // El motor no anuncia el primer tramo: ya está en él desde el inicio. Se agrega aquí.
    val planStarts = fromMs < startedAtMs
    val events = eventsBetween(maxOf(fromMs, startedAtMs), toMs, startedAtMs, edits)
    if (events.isEmpty() && !planStarts) return thirtySeconds(fromMs, toMs, startedAtMs, edits, settings)
    if (TimerEvent.Finished in events) return listOf(VoiceLine.Done)

    val lines = mutableListOf<VoiceLine>()
    val first = segments.first()
    val lastStart = events.filterIsInstance<TimerEvent.SegmentStarted>().lastOrNull()
        ?: if (planStarts) TimerEvent.SegmentStarted(0, first.kind, first.round, first.label) else null
    if (lastStart != null && settings.saysWhatsNext) {
        when (lastStart.kind) {
            SegmentKind.WORK -> lines += VoiceLine.Go(lastStart.label)
            SegmentKind.REST -> lines += VoiceLine.Rest
            else -> Unit
        }
    }
    // Consulta tardía (la app estuvo dormida): solo la fase actual, sin mitades ni cuentas viejas.
    if (toMs - fromMs > LATE_MS && !planStarts) return lines
    val current = stateAt(toMs, startedAtMs, edits)
    for (event in events) {
        when (event) {
            is TimerEvent.Halfway -> if (settings.saysWhatsNext && event.index == current.segmentIndex) lines += VoiceLine.Halfway
            is TimerEvent.SecondsLeft -> if (event.index == current.segmentIndex) lines += countdownLine(event, current.kind, settings)
            else -> Unit
        }
    }
    return lines + thirtySeconds(fromMs, toMs, startedAtMs, edits, settings)
}

private fun TimerPlan.countdownLine(event: TimerEvent.SecondsLeft, kind: SegmentKind, settings: VoiceSettings): List<VoiceLine> {
    val seconds = event.seconds.toInt()
    if (kind != SegmentKind.WORK && kind != SegmentKind.REST) return emptyList()
    if (kind == SegmentKind.REST && settings.saysWhatsNext) {
        // En el descanso, a los 5 s se anuncia lo que sigue y se cuentan solo los últimos 3.
        if (seconds == 5) return listOf(VoiceLine.Next(nextWorkLabel(event.index)))
        if (seconds == 4) return emptyList()
    }
    return if (settings.countdown) listOf(VoiceLine.Count(seconds)) else emptyList()
}

private fun TimerPlan.nextWorkLabel(fromIndex: Int): String? =
    segments.drop(fromIndex + 1).firstOrNull { it.kind == SegmentKind.WORK }?.label

private fun TimerPlan.thirtySeconds(
    fromMs: Long,
    toMs: Long,
    startedAtMs: Long,
    edits: List<TimerEdit>,
    settings: VoiceSettings,
): List<VoiceLine> {
    if (!settings.saysWhatsNext) return emptyList()
    val before = stateAt(fromMs, startedAtMs, edits)
    val after = stateAt(toMs, startedAtMs, edits)
    if (after.isFinished || after.kind != SegmentKind.REST || before.segmentIndex != after.segmentIndex) return emptyList()
    val beforeLeft = before.remainingInSegmentMs ?: return emptyList()
    val afterLeft = after.remainingInSegmentMs ?: return emptyList()
    return if (beforeLeft > THIRTY_SECONDS_MS && afterLeft <= THIRTY_SECONDS_MS) listOf(VoiceLine.ThirtySecondsLeft) else emptyList()
}

private const val THIRTY_SECONDS_MS = 30_000L
private const val LATE_MS = 1_500L
