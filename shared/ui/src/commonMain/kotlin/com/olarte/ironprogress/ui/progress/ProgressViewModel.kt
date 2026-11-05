package com.olarte.ironprogress.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.domain.session.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** Músculos de Progreso con datos reales: nada se inventa si todavía no hay entrenos. */
data class ProgressState(
    val loading: Boolean = true,
    val hasSessions: Boolean = false,
    val sets: List<Pair<Muscle, Double>> = emptyList(),
    val fromEpochMs: Long = 0L,
    val toEpochMs: Long = 0L,
)

class ProgressViewModel(private val sessions: SessionRepository, exercises: ExerciseRepository, private val clock: Clock) : ViewModel() {
    val state: StateFlow<ProgressState> = combine(sessions.observeFinishedSessions(), exercises.observeExercises()) { finished, catalog ->
        val now = clock.now()
        val window = now - 8.days
        val details = finished.filter { (it.endedAt ?: it.startedAt) >= window }.mapNotNull { sessions.findSession(it.id) }
        val totals = MuscleSets.lastSevenDays(details, catalog.associateBy { it.id }, now)
        ProgressState(
            loading = false,
            hasSessions = finished.isNotEmpty(),
            sets = totals.toList().sortedWith(compareByDescending<Pair<Muscle, Double>> { it.second }.thenBy { it.first.ordinal }),
            fromEpochMs = (now - 6.days).toEpochMilliseconds(),
            toEpochMs = now.toEpochMilliseconds(),
        )
    }.catch { emit(ProgressState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())
}
