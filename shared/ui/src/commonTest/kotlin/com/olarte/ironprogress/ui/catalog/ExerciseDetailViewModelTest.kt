package com.olarte.ironprogress.ui.catalog

import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseDemand
import com.olarte.ironprogress.domain.catalog.ExerciseFacts
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.ExerciseProgression
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private fun exercise(id: String) = Exercise(
        id = ExerciseId(id),
        name = LocalizedText(es = id, en = id),
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        muscles = listOf(ExerciseMuscle(Muscle.CHEST, MuscleRole.PRIMARY, 100)),
    )

    private val repository = object : ExerciseRepository {
        private val all = listOf("bench", "dumbbell-bench").associateWith { exercise(it) }

        override fun observeExercises(): Flow<List<Exercise>> = flowOf(all.values.toList())

        override suspend fun saveCustomExercise(exercise: Exercise) = Unit

        override suspend fun findExercise(id: ExerciseId) = all[id.value]

        override suspend fun setArchived(id: ExerciseId, archived: Boolean) = Unit

        override suspend fun findFacts(id: ExerciseId) = when (id.value) {
            "bench" -> ExerciseFacts(ExerciseDemand(4, 3, 0), ExerciseProgression(ExerciseId("dumbbell-bench"), null), null)
            "dumbbell-bench" -> ExerciseFacts(ExerciseDemand(3, 2, 0), null, null)
            else -> null
        }
    }

    @Test
    fun loadsTheExerciseItsDemandAndTheEasierAlternative() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = ExerciseDetailViewModel(repository, "bench")
            runCurrent()

            val state = model.state.value
            assertEquals("bench", state.exercise?.id?.value)
            assertEquals(4, state.facts?.demand?.overall)
            assertEquals("dumbbell-bench", state.easier?.exercise?.id?.value)
            assertEquals(3, state.easier?.overall)
            assertEquals(null, state.harder)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun anUnknownIdEndsInNotFound() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = ExerciseDetailViewModel(repository, "nope")
            runCurrent()

            assertTrue(model.state.value.notFound)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
