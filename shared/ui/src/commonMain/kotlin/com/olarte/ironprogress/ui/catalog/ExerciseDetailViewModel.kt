package com.olarte.ironprogress.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseFacts
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Una alternativa de la escalera con su exigencia, para la fila "Menos exigente" o "Más exigente". */
data class ExerciseAlternative(val exercise: Exercise, val overall: Int?)

data class ExerciseDetailState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    val facts: ExerciseFacts? = null,
    val easier: ExerciseAlternative? = null,
    val harder: ExerciseAlternative? = null,
) {
    val notFound: Boolean get() = !loading && exercise == null
}

/** Ficha de ejercicio (PANTALLAS 7.5): solo lee, no cambia nada. */
class ExerciseDetailViewModel(private val repository: ExerciseRepository, private val id: String) : ViewModel() {
    private val mutableState = MutableStateFlow(ExerciseDetailState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val exerciseId = ExerciseId(id)
            val exercise = repository.findExercise(exerciseId)
            val facts = exercise?.let { repository.findFacts(exerciseId) }
            mutableState.value = ExerciseDetailState(
                loading = false,
                exercise = exercise,
                facts = facts,
                easier = facts?.progression?.easier?.let { alternative(it) },
                harder = facts?.progression?.harder?.let { alternative(it) },
            )
        }
    }

    private suspend fun alternative(id: ExerciseId): ExerciseAlternative? {
        val exercise = repository.findExercise(id) ?: return null
        return ExerciseAlternative(exercise, repository.findFacts(id)?.demand?.overall)
    }
}
