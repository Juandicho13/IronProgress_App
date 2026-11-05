package com.olarte.ironprogress.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.olarte.ironprogress.ui.components.ActiveSessionBannerState
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.rest_title
import com.olarte.ironprogress.ui.resources.session_set_of
import com.olarte.ironprogress.ui.resources.workout_all_done
import com.olarte.ironprogress.ui.resources.workout_empty
import com.olarte.ironprogress.ui.resources.workout_error
import com.olarte.ironprogress.ui.resources.workout_free
import com.olarte.ironprogress.ui.resources.workout_saving
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.settings.forDisplay
import org.jetbrains.compose.resources.stringResource

/** El accesorio se reconstruye desde Room, también después de reiniciar la app. */
@Composable
fun workoutBanner(state: WorkoutState): ActiveSessionBannerState? {
    val detail = state.session ?: return null
    val exercise = detail.exercises.firstOrNull { it.id == state.selectedExerciseId } ?: detail.exercises.firstOrNull()
    val sets = exercise?.let { detail.setsOf(it.id) }.orEmpty()
    val next = sets.firstOrNull { !it.isCompleted }
    val formats = rememberFormats()
    val hide = LocalAppSettings.current.hideBodyMetrics
    val summary = next?.forDisplay(hide)?.let { set ->
        WorkoutField.entries.filter {
            it.measure in state.catalog[exercise?.exerciseId]?.trackingType?.fields.orEmpty() &&
                it.measure != null
        }
            .mapNotNull { field -> set.value(field)?.let { formats.number(it, 2) + " " + field.label() } }.joinToString(" · ")
    }.orEmpty()
    return ActiveSessionBannerState(
        exerciseName =
        state.catalog[exercise?.exerciseId]?.name?.resolve(Locale.current.language) ?: state.routineName
            ?: stringResource(Res.string.workout_free),
        setSummary = stringResource(Res.string.session_set_of, next?.sortOrder?.plus(1) ?: sets.size, sets.size),
        statusText = when {
            state.rest != null -> stringResource(Res.string.rest_title) + " " +
                com.olarte.ironprogress.ui.format.Formats.duration(state.rest.at(state.restNowMs).displaySecondsLeft ?: 0)
            state.failed -> stringResource(Res.string.workout_error)
            state.pendingWrites > 0 -> stringResource(Res.string.workout_saving)
            detail.exercises.isEmpty() -> stringResource(Res.string.workout_empty)
            detail.sets.all { it.isCompleted } -> stringResource(Res.string.workout_all_done)
            else -> summary
        },
        progress = if (detail.sets.isEmpty()) 0f else detail.sets.count { it.isCompleted }.toFloat() / detail.sets.size,
        isReady = state.rest == null,
    )
}
