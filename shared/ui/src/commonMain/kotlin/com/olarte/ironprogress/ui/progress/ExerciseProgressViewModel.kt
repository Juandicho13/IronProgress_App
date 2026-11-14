package com.olarte.ironprogress.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.progress.ExerciseProgress
import com.olarte.ironprogress.domain.progress.ExerciseRecords
import com.olarte.ironprogress.domain.progress.ProgressMetric
import com.olarte.ironprogress.domain.progress.ProgressPoint
import com.olarte.ironprogress.domain.progress.ProgressRange
import com.olarte.ironprogress.domain.session.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class ExerciseProgressState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val exercise: Exercise? = null,
    val progress: ExerciseProgress = ExerciseProgress(emptyList(), ExerciseRecords()),
    val metric: ProgressMetric = ProgressMetric.ESTIMATED_ONE_REP_MAX,
    val range: ProgressRange = ProgressRange.SIX_MONTHS,
    val nowEpochMs: Long = 0,
) {
    val unit: WeightUnit get() = exercise?.preferredUnit ?: WeightUnit.KG

    /** Los puntos del rango elegido, del más viejo al más nuevo. */
    val visible: List<ProgressPoint>
        get() = progress.inRange(range, Instant.fromEpochMilliseconds(nowEpochMs))
}

sealed interface ExerciseProgressIntent {
    data class Metric(val value: ProgressMetric) : ExerciseProgressIntent
    data class Range(val value: ProgressRange) : ExerciseProgressIntent
    data object Retry : ExerciseProgressIntent
}

/**
 * Progreso de un ejercicio (F2-04, PANTALLAS 2.2): gráficas de fuerza estimada, mejor serie y
 * volumen, sus récords y las últimas sesiones. Solo lee; se recarga si cambia el historial.
 */
class ExerciseProgressViewModel(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val clock: Clock,
    private val id: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExerciseProgressState())
    val state = mutableState.asStateFlow()
    private var loading: Job? = null

    init {
        viewModelScope.launch {
            // Corregir o borrar una serie en el historial cambia las gráficas: se vuelve a leer.
            sessions.observeFinishedSessions().collect { load() }
        }
    }

    fun onIntent(intent: ExerciseProgressIntent) {
        when (intent) {
            is ExerciseProgressIntent.Metric -> mutableState.update { it.copy(metric = intent.value) }
            is ExerciseProgressIntent.Range -> mutableState.update { it.copy(range = intent.value) }
            ExerciseProgressIntent.Retry -> load()
        }
    }

    private fun load() {
        loading?.cancel()
        loading = viewModelScope.launch {
            try {
                val exerciseId = ExerciseId(id)
                val exercise = exercises.findExercise(exerciseId)
                val progress = ExerciseProgress.of(sessions.exerciseHistory(exerciseId))
                mutableState.update { current ->
                    val metric = current.metric.takeIf { it in progress.metrics } ?: progress.metrics.first()
                    current.copy(
                        loading = false,
                        failed = false,
                        exercise = exercise,
                        progress = progress,
                        metric = metric,
                        nowEpochMs = clock.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}
