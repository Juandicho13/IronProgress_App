package com.olarte.ironprogress.ui.catalog

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.Muscle

/** Estado inmutable de la pantalla de catalogo. */
data class CatalogState(
    val searchText: String = "",
    val selectedMuscle: Muscle? = null,
    val exercises: List<Exercise> = emptyList(),
)

/** Intenciones del usuario en la pantalla de catalogo. */
sealed interface CatalogIntent {
    data class SearchChanged(val text: String) : CatalogIntent
    data class MuscleSelected(val muscle: Muscle?) : CatalogIntent
}
