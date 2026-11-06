package com.olarte.ironprogress.di

import androidx.room3.Room
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.CreateRoutine
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.ui.rest.RestBackgroundActions
import com.olarte.ironprogress.ui.rest.RestController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Koin arranca y da el catálogo, las rutinas y el coordinador del descanso sin Compose (F1-13 y F1-14). */
class IronProgressKoinTest {

    private val testPlatformModule = module {
        single<IronProgressDatabase> {
            Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
                factory = { IronProgressDatabaseConstructor.initialize() },
            ).buildIronProgressDatabase()
        }
    }

    @AfterTest
    fun stop() {
        stopKoin()
    }

    @Test
    fun startsWithoutComposeAndReadsTheCatalog() = runTest {
        val koin = IronProgressKoin.start(testPlatformModule)

        val exercises = koin.get<ExerciseRepository>().observeExercises().first()

        assertEquals(77, exercises.size)
        val routine = koin.get<CreateRoutine>()("Torso")
        assertEquals(listOf(routine.routine.id), koin.get<RoutineRepository>().observeRoutines().first().map { it.id })
        assertSame(koin.get<RestController>(), koin.get<RestController>())
        assertNotNull(koin.get<RestBackgroundActions>())
    }

    @Test
    fun startingTwiceKeepsTheSameKoin() {
        val first = IronProgressKoin.start(testPlatformModule)
        val second = IronProgressKoin.start(testPlatformModule)

        assertSame(first, second)
        assertTrue(first.get<IronProgressDatabase>() === second.get<IronProgressDatabase>())
    }
}
