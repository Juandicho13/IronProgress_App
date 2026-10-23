package com.olarte.ironprogress.di

import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.local.CatalogDao
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.SearchExercises
import com.olarte.ironprogress.domain.health.HealthRepository
import com.olarte.ironprogress.health.UnavailableHealthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Modulo principal de Koin compartido entre Android e iOS.
 * Provee el DAO del catalogo, los repositorios respaldados por Room 3 y los casos de uso.
 */
val sharedAppModule: Module = module {
    single<CatalogDao> { get<IronProgressDatabase>().catalogDao() }
    single<ExerciseRepository> { RoomExerciseRepository(get()) }
    single<HealthRepository> { UnavailableHealthRepository() }
    factory { SearchExercises(get()) }
}