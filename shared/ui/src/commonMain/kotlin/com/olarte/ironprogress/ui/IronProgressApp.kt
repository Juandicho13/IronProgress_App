package com.olarte.ironprogress.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.SearchExercises
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogViewModel

@Composable
fun IronProgressApp(
    exerciseRepository: ExerciseRepository,
) {
    val darkTheme = isSystemInDarkTheme()
    val languageCode = Locale.current.language

    val viewModel = viewModel(key = "catalog-$languageCode") {
        CatalogViewModel(
            searchExercises = SearchExercises(exerciseRepository),
            languageCode = languageCode,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        CatalogScreen(
            state = state,
            languageCode = languageCode,
            onIntent = viewModel::onIntent,
        )
    }
}
