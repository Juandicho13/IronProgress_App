package com.olarte.ironprogress.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.ExerciseQuery
import com.olarte.ironprogress.domain.catalog.SearchExercises
import com.olarte.ironprogress.domain.catalog.SetExerciseArchived
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel MVI del catalogo de ejercicios. */
class CatalogViewModel(searchExercises: SearchExercises, private val setExerciseArchived: SetExerciseArchived, languageCode: String) :
    ViewModel() {

    private val query = MutableStateFlow(ExerciseQuery())

    val state: StateFlow<CatalogState> = combine(
        query,
        searchExercises(query, languageCode),
    ) { currentQuery, results ->
        CatalogState(
            equipment = currentQuery.equipment,
            discipline = currentQuery.discipline,
            searchText = currentQuery.text,
            selectedMuscle = currentQuery.muscle,
            showArchived = currentQuery.includeArchived,
            // Con el filtro de archivados se ven solo esos; sin el, la búsqueda ya los oculta.
            exercises = if (currentQuery.includeArchived) results.filter { it.isArchived } else results,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogState(),
    )

    fun onIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.EquipmentSelected -> query.update { it.copy(equipment = intent.value) }
            is CatalogIntent.DisciplineSelected -> query.update { it.copy(discipline = intent.value) }
            is CatalogIntent.SearchChanged -> {
                query.update { it.copy(text = intent.text) }
            }
            is CatalogIntent.MuscleSelected -> {
                query.update { current ->
                    val nextMuscle = if (current.muscle == intent.muscle) null else intent.muscle
                    current.copy(muscle = nextMuscle)
                }
            }
            CatalogIntent.ArchivedFilterToggled -> {
                query.update { it.copy(includeArchived = !it.includeArchived) }
            }
            is CatalogIntent.ArchiveToggled -> {
                viewModelScope.launch { setExerciseArchived(intent.id, intent.archived) }
            }
        }
    }
}
