package com.olarte.ironprogress.ui.di

import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.catalog.ExerciseEditorViewModel
import com.olarte.ironprogress.ui.plan.PlanViewModel
import com.olarte.ironprogress.ui.plan.RoutineEditorViewModel
import com.olarte.ironprogress.ui.rest.RestBackgroundActions
import com.olarte.ironprogress.ui.rest.RestController
import com.olarte.ironprogress.ui.today.TodayViewModel
import com.olarte.ironprogress.ui.workout.WorkoutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels de la interfaz y coordinador del descanso. Los casos de uso los provee sharedAppModule. */
val uiModule = module {
    single { RestController() }
    single { RestBackgroundActions(get(), get(), get(), get(), get(), get()) }
    single { com.olarte.ironprogress.ui.rest.platformRestNotifications() }
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TodayViewModel(get(), get(), get(), get(), get()) }
    viewModel { PlanViewModel(get(), get()) }
    viewModel { (id: String?) -> RoutineEditorViewModel(get(), get(), get(), get(), id) }
    viewModel { (id: String?) -> ExerciseEditorViewModel(get(), get(), id) }
    viewModel { (id: String) -> com.olarte.ironprogress.ui.catalog.ExerciseDetailViewModel(get(), id) }
    viewModel { (languageCode: String) -> CatalogViewModel(get(), get(), languageCode) }
    viewModel { com.olarte.ironprogress.ui.interval.IntervalListViewModel(get()) }
    viewModel { (id: String?, defaultName: String) ->
        com.olarte.ironprogress.ui.interval.IntervalEditorViewModel(get(), get(), get(), id, defaultName)
    }
    viewModel { (id: String, languageCode: String) ->
        com.olarte.ironprogress.ui.interval.IntervalRunViewModel(get(), get(), get(), get(), get(), id, languageCode)
    }
}
