package com.olarte.ironprogress.domain.catalog

import kotlinx.coroutines.flow.Flow

/** Fuente de ejercicios. Domain la define y la capa data la implementa. */
interface ExerciseRepository {

    /** El catálogo base y los ejercicios propios; emite de nuevo cada vez que algo cambia. */
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun saveCustomExercise(exercise: Exercise)

    suspend fun findExercise(id: ExerciseId): Exercise?

    /** Archiva o restaura un ejercicio. Archivar lo oculta de la búsqueda sin borrar su historial. */
    suspend fun setArchived(id: ExerciseId, archived: Boolean)

    /**
     * Exigencia, escalera y fuente; null si el ejercicio no existe o no los tiene. Trae un valor
     * por defecto para que los repositorios de prueba no tengan que implementarlo.
     */
    suspend fun findFacts(id: ExerciseId): ExerciseFacts? = null

    /**
     * Unidad con la que se ve y se escribe el peso de un ejercicio (F1-22); null vuelve a kg. Solo
     * cambia cómo se ve: los pesos siguen en kg. Por defecto no hace nada, para las pruebas.
     */
    suspend fun setPreferredUnit(id: ExerciseId, unit: WeightUnit?) = Unit
}
