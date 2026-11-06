package com.olarte.ironprogress.ui.voice

import com.olarte.ironprogress.domain.voice.VoiceLine
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.voice_count_1
import com.olarte.ironprogress.ui.resources.voice_count_2
import com.olarte.ironprogress.ui.resources.voice_count_3
import com.olarte.ironprogress.ui.resources.voice_count_4
import com.olarte.ironprogress.ui.resources.voice_count_5
import com.olarte.ironprogress.ui.resources.voice_done
import com.olarte.ironprogress.ui.resources.voice_go
import com.olarte.ironprogress.ui.resources.voice_halfway
import com.olarte.ironprogress.ui.resources.voice_next
import com.olarte.ironprogress.ui.resources.voice_next_unknown
import com.olarte.ironprogress.ui.resources.voice_rest
import com.olarte.ironprogress.ui.resources.voice_thirty
import org.jetbrains.compose.resources.getString

/**
 * El texto de cada frase en el idioma del teléfono (PANTALLAS 5.3). Es el mismo que se muestra en
 * el subtítulo. [exerciseName] da el nombre de un ejercicio a partir de su id.
 */
suspend fun VoiceLine.text(exerciseName: (String) -> String?): String = when (this) {
    is VoiceLine.Go -> getString(Res.string.voice_go)
    VoiceLine.Halfway -> getString(Res.string.voice_halfway)
    is VoiceLine.Count -> getString(
        when (seconds) {
            1 -> Res.string.voice_count_1
            2 -> Res.string.voice_count_2
            3 -> Res.string.voice_count_3
            4 -> Res.string.voice_count_4
            else -> Res.string.voice_count_5
        },
    )
    VoiceLine.Rest -> getString(Res.string.voice_rest)
    VoiceLine.ThirtySecondsLeft -> getString(Res.string.voice_thirty)
    is VoiceLine.Next -> exerciseId?.let(exerciseName)?.let { getString(Res.string.voice_next, it) }
        ?: getString(Res.string.voice_next_unknown)
    VoiceLine.Done -> getString(Res.string.voice_done)
}
