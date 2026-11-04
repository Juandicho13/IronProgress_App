package com.olarte.ironprogress.domain.catalog

/** Caso de uso: archivar o restaurar un ejercicio del catálogo. */
class SetExerciseArchived(private val repository: ExerciseRepository) {

    suspend operator fun invoke(id: ExerciseId, archived: Boolean) {
        repository.setArchived(id, archived)
    }
}
