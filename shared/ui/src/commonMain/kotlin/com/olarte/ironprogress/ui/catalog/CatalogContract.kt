package com.olarte.ironprogress.ui.catalog

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle

/** Estado inmutable de la pantalla de catalogo. */
data class CatalogState(
    val equipment: Equipment? = null,
    val discipline: Discipline? = null,
    val searchText: String = "",
    val selectedMuscle: Muscle? = null,
    /** true: la lista muestra solo los archivados, para restaurarlos. */
    val showArchived: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
)

/** Intenciones del usuario en la pantalla de catalogo. */
sealed interface CatalogIntent {
    data class EquipmentSelected(val value: Equipment?) : CatalogIntent
    data class DisciplineSelected(val value: Discipline?) : CatalogIntent
    data class SearchChanged(val text: String) : CatalogIntent
    data class MuscleSelected(val muscle: Muscle?) : CatalogIntent
    data object ArchivedFilterToggled : CatalogIntent
    data class ArchiveToggled(val id: ExerciseId, val archived: Boolean) : CatalogIntent
}
