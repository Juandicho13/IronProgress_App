package com.olarte.ironprogress.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.progress.Period
import com.olarte.ironprogress.domain.progress.PeriodKind
import com.olarte.ironprogress.domain.progress.PeriodSummaries
import com.olarte.ironprogress.domain.progress.PeriodSummary
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.ui.today.localEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Instant

/** Lo que pinta el resumen del mes o del año (F2-10). */
data class PeriodUiState(
    val loading: Boolean = true,
    val period: Period? = null,
    val summary: PeriodSummary? = null,
    val exercises: Map<ExerciseId, Exercise> = emptyMap(),
    val hasAnySession: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)

/**
 * Resumen mensual y anual (F2-10). [initialKey] elige el periodo de entrada ("MONTH:2026:11",
 * "YEAR:2026"); vacío es el mes actual. [epochDay] da el día civil local de cada fecha.
 */
class PeriodSummaryViewModel(
    private val sessions: SessionRepository,
    exercises: ExerciseRepository,
    private val clock: Clock,
    initialKey: String = "",
    private val epochDay: (Instant) -> Long = { localEpochDay(it.toEpochMilliseconds()) },
) : ViewModel() {

    private val today: Long get() = epochDay(clock.now())

    private val period = MutableStateFlow(parseKey(initialKey) ?: Period.monthOf(today))

    private val history = combine(sessions.observeFinishedSessions(), exercises.observeExercises()) { finished, catalog ->
        History(finished.mapNotNull { sessions.findSession(it.id) }, catalog.associateBy { it.id })
    }

    val state: StateFlow<PeriodUiState> = combine(history, period) { data, current ->
        val first = PeriodSummaries.firstMonth(data.details, epochDay)
        val todayPeriod = if (current.kind == PeriodKind.MONTH) Period.monthOf(today) else Period.yearOf(today)
        PeriodUiState(
            loading = false,
            period = current,
            summary = PeriodSummaries.summarize(data.details, data.catalog, current, epochDay),
            exercises = data.catalog,
            hasAnySession = data.details.isNotEmpty(),
            canGoBack = first != null && current.previous().lastDay.toEpochDay() >= first.firstDay.toEpochDay(),
            canGoForward = current.next().firstDay.toEpochDay() <= todayPeriod.lastDay.toEpochDay(),
        )
    }.catch { emit(PeriodUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodUiState())

    /** Cambia entre mes y año sin perder el año elegido. */
    fun show(kind: PeriodKind) {
        period.update { current ->
            when {
                current.kind == kind -> current
                kind == PeriodKind.YEAR -> Period(PeriodKind.YEAR, current.year)
                else -> {
                    val now = Period.monthOf(today)
                    Period(PeriodKind.MONTH, current.year, if (current.year == now.year) now.month else 12)
                }
            }
        }
    }

    fun previous() {
        if (state.value.canGoBack) period.update { it.previous() }
    }

    fun next() {
        if (state.value.canGoForward) period.update { it.next() }
    }

    private data class History(val details: List<SessionDetail>, val catalog: Map<ExerciseId, Exercise>)

    companion object {
        fun keyOf(period: Period): String = when (period.kind) {
            PeriodKind.MONTH -> "MONTH:${period.year}:${period.month}"
            PeriodKind.YEAR -> "YEAR:${period.year}"
        }

        fun parseKey(key: String): Period? {
            val parts = key.split(':')
            return runCatching {
                when (parts.firstOrNull()) {
                    "MONTH" -> Period(PeriodKind.MONTH, parts[1].toInt(), parts[2].toInt())
                    "YEAR" -> Period(PeriodKind.YEAR, parts[1].toInt())
                    else -> null
                }
            }.getOrNull()
        }
    }
}
