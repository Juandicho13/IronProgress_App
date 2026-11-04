package com.olarte.ironprogress.di

import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.local.CatalogDao
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.RoutineDao
import com.olarte.ironprogress.data.local.SessionDao
import com.olarte.ironprogress.data.local.StageOneDao
import com.olarte.ironprogress.data.workout.RoomRoutineRepository
import com.olarte.ironprogress.data.workout.RoomSessionRepository
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.SaveCustomExercise
import com.olarte.ironprogress.domain.catalog.SearchExercises
import com.olarte.ironprogress.domain.catalog.SetExerciseArchived
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.common.UuidV7IdGenerator
import com.olarte.ironprogress.domain.health.HealthRepository
import com.olarte.ironprogress.domain.routine.CreateRoutine
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.CompleteSet
import com.olarte.ironprogress.domain.session.EndSession
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.StartSession
import com.olarte.ironprogress.health.UnavailableHealthRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Modulo principal de Koin compartido entre Android e iOS.
 * Provee los DAO, los repositorios respaldados por Room 3, el reloj, los ids y los casos de uso.
 */
val sharedAppModule: Module = module {
    single<Clock> { Clock.System }
    single<IdGenerator> { UuidV7IdGenerator }

    single<CatalogDao> { get<IronProgressDatabase>().catalogDao() }
    single<RoutineDao> { get<IronProgressDatabase>().routineDao() }
    single<SessionDao> { get<IronProgressDatabase>().sessionDao() }
    single<StageOneDao> { get<IronProgressDatabase>().stageOneDao() }

    single<ExerciseRepository> { RoomExerciseRepository(get()) }
    single<RoutineRepository> { RoomRoutineRepository(get(), get()) }
    single<SessionRepository> { RoomSessionRepository(get(), get()) }
    single<HealthRepository> { UnavailableHealthRepository() }

    factory { SaveCustomExercise(get(), get()) }
    factory { SearchExercises(get()) }
    factory { SetExerciseArchived(get()) }
    factory { CreateRoutine(get(), get()) }
    factory { StartSession(get(), get(), get()) }
    factory { CompleteSet(get(), get()) }
    factory { EndSession(get(), get()) }
}
