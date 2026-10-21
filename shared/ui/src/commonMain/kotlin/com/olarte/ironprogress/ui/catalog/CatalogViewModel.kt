package com.olarte.ironprogress.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseQuery
import com.olarte.ironprogress.domain.catalog.SearchExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** ViewModel MVI del catalogo de ejercicios. */
class CatalogViewModel(
    searchExercises: SearchExercises,
    languageCode: String,
) : ViewModel() {

    private val query = MutableStateFlow(ExerciseQuery())

    val state: StateFlow<CatalogState> = combine(
        query,
        searchExercises(query, languageCode),
    ) { currentQuery, results ->
        CatalogState(
            searchText = currentQuery.text,
            selectedMuscle = currentQuery.muscle,
            exercises = results,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogState(),
    )

    fun onIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.SearchChanged -> {
                query.update { it.copy(text = intent.text) }
            }
            is CatalogIntent.MuscleSelected -> {
                query.update { current ->
                    val nextMuscle = if (current.muscle == intent.muscle) null else intent.muscle
                    current.copy(muscle = nextMuscle)
                }
            }
        }
    }
}
