package com.olarte.ironprogress.domain.catalog

import kotlinx.coroutines.flow.Flow

/** Fuente de ejercicios. Domain la define y la capa data la implementa. */
interface ExerciseRepository {

    /** El catálogo base y los ejercicios propios; emite de nuevo cada vez que algo cambia. */
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun findExercise(id: ExerciseId): Exercise?
}