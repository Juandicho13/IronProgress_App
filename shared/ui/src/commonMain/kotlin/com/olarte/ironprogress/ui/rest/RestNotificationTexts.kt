package com.olarte.ironprogress.ui.rest

import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.live_completed_line
import com.olarte.ironprogress.ui.resources.live_free
import com.olarte.ironprogress.ui.resources.live_next_only
import com.olarte.ironprogress.ui.resources.live_next_with_target
import com.olarte.ironprogress.ui.resources.live_ready_chip
import com.olarte.ironprogress.ui.resources.live_ready_text
import com.olarte.ironprogress.ui.resources.live_ready_title
import com.olarte.ironprogress.ui.resources.live_reps
import com.olarte.ironprogress.ui.resources.live_resting_title
import org.jetbrains.compose.resources.getString

/**
 * Los textos de la Live Update y la Live Activity desde los recursos de Compose, para iPhone (en
 * Android salen de los recursos de androidApp). Las plantillas se cargan una vez y se llenan sin
 * suspender.
 */
suspend fun loadRestNotificationStrings(): RestNotificationStrings {
    val restingTitle = getString(Res.string.live_resting_title)
    val nextWithTarget = getString(Res.string.live_next_with_target)
    val nextOnly = getString(Res.string.live_next_only)
    val completed = getString(Res.string.live_completed_line)
    val readyTitle = getString(Res.string.live_ready_title)
    val readyText = getString(Res.string.live_ready_text)
    return RestNotificationStrings(
        defaultWorkoutName = getString(Res.string.live_free),
        repsSuffix = getString(Res.string.live_reps),
        readyChip = getString(Res.string.live_ready_chip),
        restingTitle = { fill(restingTitle, it) },
        nextSetWithTarget = { set, total, target -> fill(nextWithTarget, set, total, target) },
        nextSetOnly = { set, total -> fill(nextOnly, set, total) },
        completedSetLine = { set, target -> fill(completed, set, target) },
        readyTitle = { set, total -> fill(readyTitle, set, total) },
        readyTextWithTarget = { exercise, target -> fill(readyText, exercise, target) },
    )
}

/** Llena "%1$s", "%2$d"... como String.format, que no existe en código común. */
internal fun fill(template: String, vararg args: Any): String {
    var result = template
    args.forEachIndexed { index, value ->
        val n = index + 1
        result = result.replace("%$n\$s", value.toString()).replace("%$n\$d", value.toString())
    }
    return result
}
