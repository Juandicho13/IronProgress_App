package com.olarte.ironprogress.ui.catalog

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.SaveCustomExercise
import com.olarte.ironprogress.domain.common.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditorViewModelTest {
    @Test fun validatesSavesOnceAndPreservesDraftAfterFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = EditorRepository()
            val model = ExerciseEditorViewModel(repository, SaveCustomExercise(repository, IdGenerator { "new" }), null)
            model.onIntent(ExerciseEditorIntent.Save)
            runCurrent()
            assertEquals(0, repository.writes)
            val draft =
                ExerciseDraft(
                    spanish = "Remo propio",
                    english = "Custom row",
                    muscles = listOf(ExerciseMuscle(Muscle.LATS, MuscleRole.PRIMARY)),
                )
            model.onIntent(ExerciseEditorIntent.Change(draft))
            repository.fail = true
            model.onIntent(ExerciseEditorIntent.Save)
            runCurrent()
            assertTrue(model.state.value.failed)
            assertEquals(draft, model.state.value.draft)
            repository.fail = false
            model.onIntent(ExerciseEditorIntent.Save)
            model.onIntent(ExerciseEditorIntent.Save)
            runCurrent()
            assertEquals(1, repository.writes)
            assertTrue(repository.items.value.single().isCustom)
            assertFalse(model.state.value.dirty)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun backRequiresDiscardOnlyWhenDirty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = EditorRepository()
            val model = ExerciseEditorViewModel(repository, SaveCustomExercise(repository, IdGenerator { "new" }), null)
            model.onIntent(ExerciseEditorIntent.Change(ExerciseDraft(spanish = "Borrador")))
            model.onIntent(ExerciseEditorIntent.Back)
            assertTrue(model.state.value.confirmDiscard)
            model.onIntent(ExerciseEditorIntent.KeepEditing)
            assertFalse(model.state.value.confirmDiscard)
            assertEquals("Borrador", model.state.value.draft.spanish)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
private class EditorRepository : ExerciseRepository {
    val items = MutableStateFlow<List<Exercise>>(emptyList())
    var fail = false
    var writes = 0
    override fun observeExercises() = items
    override suspend fun findExercise(id: ExerciseId) = items.value.find { it.id == id }
    override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    override suspend fun saveCustomExercise(exercise: Exercise) {
        if (fail) error("Fallo de prueba")
        writes++
        items.value = listOf(exercise)
    }
}
