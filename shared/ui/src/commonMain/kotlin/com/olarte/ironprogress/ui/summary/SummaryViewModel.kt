package com.olarte.ironprogress.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionSummary
import com.olarte.ironprogress.domain.session.SummarizeSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SummaryState(
    val loading: Boolean = true,
    val summary: SessionSummary? = null,
    val routineName: String? = null,
    val exercises: Map<ExerciseId, Exercise> = emptyMap(),
    val failed: Boolean = false,
) {
    fun exerciseName(id: ExerciseId, languageCode: String): String = exercises[id]?.name?.resolve(languageCode) ?: id.value
}

/** Resumen de una sesión terminada (F1-21). Solo lee: la sesión ya quedó guardada al terminar. */
class SummaryViewModel(
    private val summarize: SummarizeSession,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val sessionId: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SummaryState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        mutableState.value = SummaryState()
        viewModelScope.launch {
            try {
                val summary = summarize(SessionId(sessionId))
                val name = summary?.session?.routineId?.let { routines.findRoutine(it)?.routine?.name }
                mutableState.value = SummaryState(
                    loading = false,
                    summary = summary,
                    routineName = name,
                    exercises = exercises.observeExercises().first().associateBy { it.id },
                    failed = summary == null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = SummaryState(loading = false, failed = true)
            }
        }
    }
}
