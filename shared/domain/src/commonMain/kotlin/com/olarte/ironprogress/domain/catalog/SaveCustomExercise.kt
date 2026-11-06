package com.olarte.ironprogress.domain.catalog

import com.olarte.ironprogress.domain.common.IdGenerator

/** Crea o edita solo ejercicios propios, conservando identidad y archivado al editar. */
class SaveCustomExercise(private val repository: ExerciseRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(draft: Exercise, editingId: ExerciseId? = null): Exercise {
        require(draft.name.es.isNotBlank() && draft.name.en.isNotBlank()) { "Escribe los dos nombres" }
        val previous = editingId?.let { requireNotNull(repository.findExercise(it)) }
        require(previous == null || previous.isCustom) { "El catálogo base no se edita" }
        val saved = draft.copy(
            id = previous?.id ?: ExerciseId(ids.newId()),
            name = LocalizedText(draft.name.es.trim(), draft.name.en.trim()),
            isCustom = true,
            isArchived = previous?.isArchived ?: false,
        )
        repository.saveCustomExercise(saved)
        return saved
    }
}
