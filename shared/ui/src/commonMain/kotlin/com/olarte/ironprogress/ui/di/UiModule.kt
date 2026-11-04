package com.olarte.ironprogress.ui.di

import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModels de la interfaz. Los casos de uso los provee sharedAppModule. */
val uiModule = module {
    viewModel { (languageCode: String) -> CatalogViewModel(get(), get(), languageCode) }
}
