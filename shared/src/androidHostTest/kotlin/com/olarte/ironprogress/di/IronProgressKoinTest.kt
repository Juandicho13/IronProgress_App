package com.olarte.ironprogress.di

import androidx.room3.Room
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.routine.CreateRoutine
import com.olarte.ironprogress.domain.routine.RoutineRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Koin arranca y da el catálogo y las rutinas sin Compose, como lo usará el servicio del descanso. */
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
    }

    @Test
    fun startingTwiceKeepsTheSameKoin() {
        val first = IronProgressKoin.start(testPlatformModule)
        val second = IronProgressKoin.start(testPlatformModule)

        assertSame(first, second)
        assertTrue(first.get<IronProgressDatabase>() === second.get<IronProgressDatabase>())
    }
}
