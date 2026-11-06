package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoRoutinesTest {

    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }

    @Test
    fun seedsTheDemoFolderOnceWhenThereAreNoRoutines() = runTest {
        val routines = Routines()
        val settings = Settings()
        val seed = SeedDemoRoutines(routines, Catalog(), settings, ids)

        assertTrue(seed())

        val folders = routines.observeFolders().first()
        assertEquals(listOf(DemoRoutines.FOLDER_NAME), folders.map { it.name })
        val saved = routines.observeRoutines().first()
        assertEquals(DemoRoutines.all.size, saved.size)
        assertTrue(saved.all { it.name.startsWith("Demo") && it.folderId == folders.single().id })
        assertTrue(settings.settings.first().demoRoutinesSeeded)
    }

    @Test
    fun deletingTheDemosDoesNotBringThemBack() = runTest {
        val routines = Routines()
        val seed = SeedDemoRoutines(routines, Catalog(), Settings(), ids)
        seed()
        routines.observeRoutines().first().forEach { routines.deleteRoutine(it.id) }

        assertFalse(seed())
        assertTrue(routines.observeRoutines().first().isEmpty())
    }

    @Test
    fun skipsWhenTheUserAlreadyHasRoutinesButMarksItDone() = runTest {
        val routines = Routines()
        CreateRoutine(routines, ids)("Mi rutina")
        val settings = Settings()

        assertFalse(SeedDemoRoutines(routines, Catalog(), settings, ids)())
        assertEquals(1, routines.observeRoutines().first().size)
        assertTrue(settings.settings.first().demoRoutinesSeeded)
    }

    @Test
    fun keepsEachExerciseModeAndSkipsExercisesMissingFromTheCatalog() = runTest {
        val routines = Routines()
        val catalog = Catalog(missing = setOf("barbell-bench-press"))
        SeedDemoRoutines(routines, catalog, Settings(), ids)()

        val details = routines.observeRoutines().first().map { routines.findRoutine(it.id)!! }
        val push = details.first { it.routine.name == "Demo · Empuje" }
        assertTrue(push.exercises.none { it.exerciseId.value == "barbell-bench-press" })
        val legs = details.first { it.routine.name == "Demo · Pierna" }
        assertEquals(ExerciseMode.NO_REST, legs.exercises.first { it.exerciseId.value == "standing-calf-raise" }.mode)
        val full = details.first { it.routine.name == "Demo · Cuerpo completo" }
        assertEquals(ExerciseMode.TIMED, full.exercises.first { it.exerciseId.value == "plank" }.mode)
        assertTrue(details.all { detail -> detail.exercises.all { detail.setsOf(it.id).isNotEmpty() } })
    }

    private class Settings : SettingsRepository {
        private val state = MutableStateFlow(AppSettings(onboardingDone = true))
        override val settings: Flow<AppSettings> = state

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            state.value = transform(state.value)
        }
    }

    private class Catalog(private val missing: Set<String> = emptySet()) : ExerciseRepository {
        override fun observeExercises(): Flow<List<Exercise>> = flowOf(emptyList())

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun findExercise(id: ExerciseId): Exercise? = if (id.value in missing) {
            null
        } else {
            Exercise(
                id = id,
                name = LocalizedText(es = id.value, en = id.value),
                trackingType = TrackingType.WEIGHT_REPS,
                equipment = Equipment.BARBELL,
                pattern = MovementPattern.HORIZONTAL_PUSH,
                muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY)),
            )
        }

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
    }

    private class Routines : RoutineRepository {
        private val details = linkedMapOf<RoutineId, RoutineDetail>()
        private val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
        private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())

        override fun observeFolders(): Flow<List<Folder>> = foldersFlow

        override fun observeRoutines(): Flow<List<Routine>> = routinesFlow

        override suspend fun findRoutine(id: RoutineId): RoutineDetail? = details[id]

        override suspend fun saveRoutine(detail: RoutineDetail) {
            details[detail.routine.id] = detail
            routinesFlow.value = details.values.map { it.routine }
        }

        override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit

        override suspend fun reorderFolders(ids: List<FolderId>) = Unit

        override suspend fun deleteRoutine(id: RoutineId) {
            details.remove(id)
            routinesFlow.value = details.values.map { it.routine }
        }

        override suspend fun saveFolder(folder: Folder) {
            foldersFlow.value = foldersFlow.value.filterNot { it.id == folder.id } + folder
        }

        override suspend fun deleteFolder(id: FolderId) {
            foldersFlow.value = foldersFlow.value.filterNot { it.id == id }
        }
    }
}
