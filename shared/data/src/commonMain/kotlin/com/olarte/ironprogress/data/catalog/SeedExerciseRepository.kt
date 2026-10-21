package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SeedExerciseRepository(
    initialExercises: List<Exercise> = SeedCatalog.exercises,
) : ExerciseRepository {

    private val exercises = MutableStateFlow(initialExercises)

    override fun observeExercises(): Flow<List<Exercise>> = exercises.asStateFlow()

    override suspend fun findExercise(id: ExerciseId): Exercise? =
        exercises.value.firstOrNull { it.id == id }
}
