package com.olarte.ironprogress.ui.workout

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.AddSessionExercise
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/** Dobles de prueba de Entreno, compartidos por las pruebas de ViewModel y las de interfaz (F1-27). */
internal class TestClock(var now: Instant = Instant.parse("2026-11-05T12:00:00Z")) : Clock {
    override fun now() = now
}

internal class TestSessions : SessionRepository {
    val data = MutableStateFlow<List<SessionDetail>>(emptyList())
    var failSave = false
    var completedWrites = 0
    override fun observeActiveSession() = data.map { all -> all.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS } }
    override fun observeFinishedSessions() = data.map { all -> all.map { it.session }.filter { it.status == SessionStatus.FINISHED } }
    override suspend fun findSession(id: SessionId) = data.value.firstOrNull { it.session.id == id }
    override suspend fun createSession(detail: SessionDetail): SessionDetail {
        data.value.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS }?.let { return it }
        data.value += detail
        return detail
    }
    override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) {
        data.value =
            data.value.map {
                if (it.session.id ==
                    exercise.sessionId
                ) {
                    it.copy(
                        blocks = it.blocks.filterNot { b -> b.id == block.id } + block,
                        exercises =
                        it.exercises + exercise,
                        sets = it.sets + sets,
                    )
                } else {
                    it
                }
            }
    }
    override suspend fun saveSession(session: WorkoutSession) {
        data.value =
            data.value.map { if (it.session.id == session.id) it.copy(session = session) else it }
    }
    override suspend fun saveBlock(block: SessionBlock) = Unit
    override suspend fun saveExercise(exercise: SessionExercise) {
        data.value = data.value.map { detail ->
            if (detail.exercises.any { it.id == exercise.id }) {
                detail.copy(exercises = detail.exercises.map { if (it.id == exercise.id) exercise else it })
            } else {
                detail
            }
        }
    }
    override suspend fun saveSet(set: WorkoutSet) {
        check(!failSave) { "Fallo de escritura de prueba" }
        if (set.isCompleted) completedWrites++
        data.value =
            data.value.map { detail ->
                if (detail.exercises.any { it.id == set.sessionExerciseId }) {
                    detail.copy(
                        sets =
                        detail.sets.filterNot { it.id == set.id } + set,
                    )
                } else {
                    detail
                }
            }
    }
    override suspend fun deleteSet(id: WorkoutSetId) = Unit
}

internal val testBench =
    Exercise(
        ExerciseId("bench"),
        LocalizedText("Press", "Press"),
        TrackingType.WEIGHT_REPS,
        Equipment.BARBELL,
        MovementPattern.HORIZONTAL_PUSH,
        listOf(
            com.olarte.ironprogress.domain.catalog.ExerciseMuscle(
                com.olarte.ironprogress.domain.catalog.Muscle.CHEST,
                com.olarte.ironprogress.domain.catalog.MuscleRole.PRIMARY,
            ),
        ),
    )
internal class TestCatalog : ExerciseRepository {
    private val catalogFlow = MutableStateFlow(listOf(testBench))
    override fun observeExercises() = catalogFlow
    override suspend fun setPreferredUnit(id: ExerciseId, unit: com.olarte.ironprogress.domain.catalog.WeightUnit?) {
        catalogFlow.value = catalogFlow.value.map { if (it.id == id) it.copy(preferredUnit = unit) else it }
    }
    override suspend fun findExercise(id: ExerciseId) = testBench.takeIf { it.id == id }
    override suspend fun saveCustomExercise(exercise: Exercise) = Unit
    override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit
}
internal class EmptyRoutines : RoutineRepository {
    override fun observeFolders() = MutableStateFlow(emptyList<Folder>())
    override fun observeRoutines() = MutableStateFlow(emptyList<Routine>())
    override suspend fun findRoutine(id: RoutineId): RoutineDetail? = null
    override suspend fun saveRoutine(detail: RoutineDetail) = Unit
    override suspend fun reorderRoutines(ids: List<RoutineId>) = Unit
    override suspend fun reorderFolders(ids: List<FolderId>) = Unit
    override suspend fun deleteRoutine(id: RoutineId) = Unit
    override suspend fun saveFolder(folder: Folder) = Unit
    override suspend fun deleteFolder(id: FolderId) = Unit
}

/** Un ViewModel de Entreno real sobre los dobles, con ids predecibles. */
internal fun newWorkoutViewModel(
    sessions: TestSessions,
    clock: TestClock = TestClock(),
    catalog: ExerciseRepository = TestCatalog(),
): WorkoutViewModel {
    var index = 0
    val ids = IdGenerator { "id-${index++}" }
    return WorkoutViewModel(
        sessions,
        catalog,
        EmptyRoutines(),
        StartSession(sessions, ids, clock),
        CompleteSet(sessions, clock),
        AddSessionExercise(sessions, ids),
        ids,
        clock,
        com.olarte.ironprogress.ui.rest.RestNotifications { },
    )
}
