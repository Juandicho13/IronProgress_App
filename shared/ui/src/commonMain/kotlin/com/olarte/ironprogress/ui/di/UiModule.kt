package com.olarte.ironprogress.ui.di

import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.catalog.ExerciseEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels de la interfaz. Los casos de uso los provee sharedAppModule. */
val uiModule = module {
    viewModel { (id: String?) -> ExerciseEditorViewModel(get(), get(), id) }
    viewModel { (languageCode: String) -> CatalogViewModel(get(), get(), languageCode) }
}
