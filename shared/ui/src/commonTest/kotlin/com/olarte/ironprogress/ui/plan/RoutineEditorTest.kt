package com.olarte.ironprogress.ui.plan

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.EditRoutines
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
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
class RoutineEditorTest {
    @Test fun draftIsNotPersistedAndInvalidRangesCannotSave() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var next = 0
            val ids = IdGenerator { "id-${next++}" }
            val repo = RoutineFake()
            val edit = EditRoutines(repo, ids)
            val model = RoutineEditorViewModel(repo, CatalogFake(), edit, ids, null)
            runCurrent()
            assertTrue(repo.details.isEmpty())
            model.onIntent(RoutineEditorIntent.Name("Pierna A"))
            val block = model.state.value.detail!!.blocks.single().id
            model.onIntent(RoutineEditorIntent.AddExercise(block, ExerciseId("squat")))
            val set = model.state.value.detail!!.setTemplates.first().id
            model.onIntent(RoutineEditorIntent.ChangeSet(set, SetDraft("12", "8", "2")))
            model.onIntent(RoutineEditorIntent.Save)
            runCurrent()
            assertTrue(repo.details.isEmpty())
            model.onIntent(RoutineEditorIntent.ChangeSet(set, SetDraft("8", "12", "2")))
            repo.fail = true
            model.onIntent(RoutineEditorIntent.Save)
            runCurrent()
            assertTrue(model.state.value.failed)
            assertEquals("Pierna A", model.state.value.name)
            repo.fail = false
            model.onIntent(RoutineEditorIntent.Save)
            runCurrent()
            val original = repo.details.values.single()
            val duplicate = edit.duplicate(original.routine.id, "Pierna B")
            assertTrue(original.routine.id != duplicate.routine.id)
            assertTrue(original.blocks.single().id != duplicate.blocks.single().id)
            assertTrue(original.exercises.single().id != duplicate.exercises.single().id)
            assertEquals(3, duplicate.setTemplates.size)
            assertTrue(duplicate.setTemplates.all { it.routineExerciseId == duplicate.exercises.single().id })
            assertEquals(original, repo.details.getValue(original.routine.id))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun removingExerciseRemovesInvalidDraftsAndItsSets() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var next = 0
            val ids = IdGenerator { "id-${next++}" }
            val repo = RoutineFake()
            val model = RoutineEditorViewModel(repo, CatalogFake(), EditRoutines(repo, ids), ids, null)
            runCurrent()
            model.onIntent(RoutineEditorIntent.Name("Rutina"))
            model.onIntent(RoutineEditorIntent.AddExercise(model.state.value.detail!!.blocks.single().id, ExerciseId("squat")))
            val exercise = model.state.value.detail!!.exercises.single().id
            model.onIntent(RoutineEditorIntent.Rest(exercise, "-5"))
            assertFalse(model.state.value.valid)
            model.onIntent(RoutineEditorIntent.RemoveExercise(exercise))
            assertTrue(model.state.value.valid)
            assertTrue(model.state.value.sets.isEmpty())
            model.onIntent(RoutineEditorIntent.Back)
            assertTrue(model.state.value.confirmDiscard)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
private class RoutineFake : RoutineRepository {
    val details = mutableMapOf<RoutineId, RoutineDetail>()
    val routines = MutableStateFlow<List<Routine>>(emptyList())
    val folders = MutableStateFlow<List<Folder>>(emptyList())
    var fail = false
    override fun observeRoutines() = routines
    override fun observeFolders() = folders
    override suspend fun findRoutine(id: RoutineId) = details[id]
    override suspend fun saveRoutine(detail: RoutineDetail) {
        if (fail) error("Error de prueba")
        details[detail.routine.id] = detail
        routines.value = details.values.map { it.routine }.sortedBy { it.sortOrder }
    }
    override suspend fun reorderRoutines(ids: List<RoutineId>) {
        routines.value =
            ids.mapIndexed { index, id -> details.getValue(id).routine.copy(sortOrder = index) }
    }
    override suspend fun reorderFolders(ids: List<FolderId>) {
        folders.value =
            ids.mapIndexed { index, id -> folders.value.first { it.id == id }.copy(sortOrder = index) }
    }
    override suspend fun deleteRoutine(id: RoutineId) {
        details.remove(id)
    }
    override suspend fun saveFolder(folder: Folder) {
        folders.value = folders.value.filterNot { it.id == folder.id } + folder
    }
    override suspend fun deleteFolder(id: FolderId) {
        folders.value = folders.value.filterNot { it.id == id }
    }
}
private class CatalogFake : ExerciseRepository {
    override fun observeExercises() = MutableStateFlow<List<Exercise>>(emptyList())
    override suspend fun findExercise(id: ExerciseId): Exercise? = null
    override suspend fun saveCustomExercise(exercise: Exercise) = Unit
    override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
}
