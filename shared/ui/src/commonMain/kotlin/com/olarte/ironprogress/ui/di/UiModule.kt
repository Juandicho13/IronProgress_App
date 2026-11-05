package com.olarte.ironprogress.ui.di

import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.catalog.ExerciseEditorViewModel
import com.olarte.ironprogress.ui.plan.PlanViewModel
import com.olarte.ironprogress.ui.plan.RoutineEditorViewModel
import com.olarte.ironprogress.ui.today.TodayViewModel
import com.olarte.ironprogress.ui.workout.WorkoutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels de la interfaz. Los casos de uso los provee sharedAppModule. */
val uiModule = module {
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TodayViewModel(get(), get(), get(), get(), get()) }
    viewModel { PlanViewModel(get(), get()) }
    viewModel { (id: String?) -> RoutineEditorViewModel(get(), get(), get(), get(), id) }
    viewModel { (id: String?) -> ExerciseEditorViewModel(get(), get(), id) }
    viewModel { (languageCode: String) -> CatalogViewModel(get(), get(), languageCode) }
}
