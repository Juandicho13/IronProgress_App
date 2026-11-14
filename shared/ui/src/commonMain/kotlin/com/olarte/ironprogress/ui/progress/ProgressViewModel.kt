package com.olarte.ironprogress.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.FeedbackRepository
import com.olarte.ironprogress.domain.progress.MuscleRecovery
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.domain.progress.Recovery
import com.olarte.ironprogress.domain.session.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    /** Mapa de recuperación (F2-08): cada músculo del catálogo con su fatiga de las últimas dos semanas. */
    val recovery: Map<Muscle, MuscleRecovery> = emptyMap(),
)

class ProgressViewModel(
    private val sessions: SessionRepository,
    exercises: ExerciseRepository,
    private val clock: Clock,
    feedback: FeedbackRepository? = null,
) : ViewModel() {
    val state: StateFlow<ProgressState> = combine(
        sessions.observeFinishedSessions(),
        exercises.observeExercises(),
        feedback?.observeAll() ?: flowOf(emptyList()),
    ) { finished, catalog, ratings ->
        val now = clock.now()
        // Dos semanas para el mapa de recuperación; las series por músculo miran solo los últimos 7 días.
        val window = now - Recovery.WINDOW - 1.days
        val details = finished.filter { (it.endedAt ?: it.startedAt) >= window }.mapNotNull { sessions.findSession(it.id) }
        val byId = catalog.associateBy { it.id }
        val totals = MuscleSets.lastSevenDays(details, byId, now)
        ProgressState(
            loading = false,
            hasSessions = finished.isNotEmpty(),
            sets = totals.toList().sortedWith(compareByDescending<Pair<Muscle, Double>> { it.second }.thenBy { it.first.ordinal }),
            fromEpochMs = (now - 6.days).toEpochMilliseconds(),
            toEpochMs = now.toEpochMilliseconds(),
            recovery = Recovery.of(details, byId, now, ratings),
        )
    }.catch { emit(ProgressState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())
}
