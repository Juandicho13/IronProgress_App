package com.olarte.ironprogress.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.catalog_clear_search
import com.olarte.ironprogress.ui.resources.catalog_empty_body
import com.olarte.ironprogress.ui.resources.catalog_empty_title
import com.olarte.ironprogress.ui.resources.catalog_exercise_count
import com.olarte.ironprogress.ui.resources.catalog_filter_all
import com.olarte.ironprogress.ui.resources.catalog_search_placeholder
import com.olarte.ironprogress.ui.resources.catalog_secondary_prefix
import com.olarte.ironprogress.ui.resources.catalog_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScreen(
    state: CatalogState,
    languageCode: String,
    onIntent: (CatalogIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFieldState = rememberTextFieldState(initialText = state.searchText)

    LaunchedEffect(searchFieldState, onIntent) {
        snapshotFlow { searchFieldState.text.toString() }
            .collect { text ->
                onIntent(CatalogIntent.SearchChanged(text))
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.catalog_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 12.dp),
            )

            OutlinedTextField(
                state = searchFieldState,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.catalog_search_placeholder)) },
                trailingIcon = if (searchFieldState.text.isNotEmpty()) {
                    {
                        TextButton(onClick = { searchFieldState.clearText() }) {
                            Text(stringResource(Res.string.catalog_clear_search))
                        }
                    }
                } else {
                    null
                },
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = state.selectedMuscle == null,
                        onClick = { onIntent(CatalogIntent.MuscleSelected(null)) },
                        label = { Text(stringResource(Res.string.catalog_filter_all)) },
                    )
                }
                items(
                    items = Muscle.entries,
                    key = { it.id },
                ) { muscle ->
                    FilterChip(
                        selected = state.selectedMuscle == muscle,
                        onClick = { onIntent(CatalogIntent.MuscleSelected(muscle)) },
                        label = { Text(muscle.label()) },
                    )
                }
            }

            Text(
                text = pluralStringResource(
                    Res.plurals.catalog_exercise_count,
                    state.exercises.size,
                    state.exercises.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.catalog_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(Res.string.catalog_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(
                        items = state.exercises,
                        key = { it.id.value },
                    ) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            languageCode = languageCode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    languageCode: String,
) {
    val primaryLabels = exercise.primaryMuscles.map { it.label() }.joinToString(" · ")
    val secondaryLabels = exercise.secondaryMuscles.map { it.label() }.joinToString(" · ")
    val equipmentLabel = exercise.equipment.label()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = exercise.name.resolve(languageCode),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (primaryLabels.isNotEmpty()) {
                    "$primaryLabels · $equipmentLabel"
                } else {
                    equipmentLabel
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (secondaryLabels.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.catalog_secondary_prefix, secondaryLabels),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
