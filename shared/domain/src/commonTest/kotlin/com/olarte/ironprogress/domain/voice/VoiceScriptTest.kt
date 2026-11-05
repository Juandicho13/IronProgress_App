package com.olarte.ironprogress.domain.voice

import com.olarte.ironprogress.domain.time.TimerPlans
import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceScriptTest {

    private val start = 1_700_000_000_000L

    // 15 s de trabajo y 45 s de descanso, alternando dos ejercicios (PANTALLAS 5.3).
    private val plan = TimerPlans.intervals(
        workSeconds = 15,
        restSeconds = 45,
        rounds = 3,
        exerciseIds = listOf("burpee", "kettlebell-swing"),
    )

    /** Recorre el plan de 100 en 100 ms, como lo haría la pantalla, y junta lo que dice la voz. */
    private fun spoken(settings: VoiceSettings = VoiceSettings(), toMs: Long = 135_000): List<VoiceLine> {
        val lines = mutableListOf<VoiceLine>()
        var previous = start - 1
        var now = start
        while (now <= start + toMs) {
            lines += plan.voiceLinesBetween(previous, now, start, settings = settings)
            previous = now
            now += 100
        }
        return lines
    }

    @Test
    fun followsTheScriptOfTheIntervalsScreen() {
        val lines = spoken(toMs = 61_000)

        assertEquals(
            listOf(
                VoiceLine.Go("burpee"),
                VoiceLine.Halfway,
                VoiceLine.Count(5),
                VoiceLine.Count(4),
                VoiceLine.Count(3),
                VoiceLine.Count(2),
                VoiceLine.Count(1),
                VoiceLine.Rest,
                VoiceLine.ThirtySecondsLeft,
                VoiceLine.Next("kettlebell-swing"),
                VoiceLine.Count(3),
                VoiceLine.Count(2),
                VoiceLine.Count(1),
                VoiceLine.Go("kettlebell-swing"),
            ),
            lines,
        )
    }

    @Test
    fun endsWithDone() {
        assertEquals(VoiceLine.Done, spoken().last())
    }

    @Test
    fun withoutWhatsNextItOnlyCounts() {
        val lines = spoken(VoiceSettings(saysWhatsNext = false), toMs = 61_000)

        assertEquals(
            List(2) { (5 downTo 1).map { VoiceLine.Count(it) } }.flatten(),
            lines,
        )
    }

    @Test
    fun withoutCountdownItOnlySaysThePhases() {
        val lines = spoken(VoiceSettings(countdown = false), toMs = 61_000)

        assertEquals(
            listOf(
                VoiceLine.Go("burpee"),
                VoiceLine.Halfway,
                VoiceLine.Rest,
                VoiceLine.ThirtySecondsLeft,
                VoiceLine.Next("kettlebell-swing"),
                VoiceLine.Go("kettlebell-swing"),
            ),
            lines,
        )
    }

    @Test
    fun mutedSaysNothing() {
        assertEquals(emptyList(), spoken(VoiceSettings(enabled = false)))
    }

    @Test
    fun aLateQueryOnlySpeaksAboutTheCurrentPhase() {
        // La app estuvo dormida 70 s: no repite lo que se perdió, solo dice dónde va.
        val lines = plan.voiceLinesBetween(start, start + 70_000, start)

        assertEquals(listOf<VoiceLine>(VoiceLine.Go("kettlebell-swing")), lines)
    }
}
