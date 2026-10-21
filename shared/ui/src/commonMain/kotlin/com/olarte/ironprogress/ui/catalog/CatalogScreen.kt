package com.olarte.ironprogress.ui.catalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.ui.components.ForjaIcons
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
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaMotion
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScreen(
    state: CatalogState,
    languageCode: String,
    onIntent: (CatalogIntent) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val searchFieldState = rememberTextFieldState(initialText = state.searchText)
    val searchInteraction = remember { MutableInteractionSource() }
    val isSearchFocused by searchInteraction.collectIsFocusedAsState()

    LaunchedEffect(searchFieldState, onIntent) {
        snapshotFlow { searchFieldState.text.toString() }
            .collect { text ->
                onIntent(CatalogIntent.SearchChanged(text))
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Spacer(modifier = Modifier.height(ForjaSpacing.esp2))

        // Un solo titulo grande por pantalla (titulo-1)
        Text(
            text = stringResource(Res.string.catalog_title),
            style = typography.title1,
            color = colors.text1,
        )

        // Campo de busqueda Forja en superficie-3 con radio-m y foco en brasa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ForjaMeasures.secondaryButtonHeight)
                .clip(ForjaRadius.shapeM)
                .background(colors.surface3)
                .then(
                    if (isSearchFocused) {
                        Modifier.border(2.dp, colors.ember, ForjaRadius.shapeM)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = ForjaSpacing.esp4),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            ) {
                Icon(
                    imageVector = ForjaIcons.Search,
                    contentDescription = null,
                    tint = if (isSearchFocused) colors.emberInk else colors.text2,
                    modifier = Modifier.size(ForjaMeasures.inlineIconSize),
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (searchFieldState.text.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.catalog_search_placeholder),
                            style = typography.body,
                            color = colors.text3,
                        )
                    }
                    BasicTextField(
                        state = searchFieldState,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = typography.body.copy(color = colors.text1),
                        cursorBrush = SolidColor(colors.ember),
                        interactionSource = searchInteraction,
                    )
                }

                if (searchFieldState.text.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.catalog_clear_search),
                        style = typography.label,
                        color = colors.emberInk,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { searchFieldState.clearText() }
                            .padding(horizontal = ForjaSpacing.esp2, vertical = ForjaSpacing.esp1),
                    )
                }
            }
        }

        // Chips de filtro de musculo: 36 dp visuales en superficie-3; elegido en brasa-velo y brasa-tinta
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            contentPadding = PaddingValues(vertical = ForjaSpacing.esp0_5),
        ) {
            item(key = "all") {
                ForjaFilterChip(
                    label = stringResource(Res.string.catalog_filter_all),
                    selected = state.selectedMuscle == null,
                    onClick = { onIntent(CatalogIntent.MuscleSelected(null)) },
                )
            }
            items(
                items = Muscle.entries,
                key = { it.id },
            ) { muscle ->
                ForjaFilterChip(
                    label = muscle.label(),
                    selected = state.selectedMuscle == muscle,
                    onClick = { onIntent(CatalogIntent.MuscleSelected(muscle)) },
                )
            }
        }

        // Conteo de ejercicios con cifras tabulares
        Text(
            text = pluralStringResource(
                Res.plurals.catalog_exercise_count,
                state.exercises.size,
                state.exercises.size,
            ).uppercase(),
            style = typography.caption,
            color = colors.text2,
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
                    verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
                ) {
                    Text(
                        text = stringResource(Res.string.catalog_empty_title),
                        style = typography.title3,
                        color = colors.text1,
                    )
                    Text(
                        text = stringResource(Res.string.catalog_empty_body),
                        style = typography.body2,
                        color = colors.text2,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
                // Deja libres abajo los 64 dp de la isla + 12 dp de margen + aire de lectura
                contentPadding = PaddingValues(bottom = 104.dp),
            ) {
                items(
                    items = state.exercises,
                    key = { it.id.value },
                ) { exercise ->
                    ForjaExerciseCard(
                        exercise = exercise,
                        languageCode = languageCode,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForjaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = ForjaMotion.spatialFast(),
        label = "ChipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(ForjaMeasures.minTouch)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .background(if (selected) colors.emberVeil else colors.surface3)
                .padding(horizontal = ForjaSpacing.esp4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = typography.label,
                color = if (selected) colors.emberInk else colors.text1,
            )
        }
    }
}

@Composable
private fun ForjaExerciseCard(
    exercise: Exercise,
    languageCode: String,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val primaryLabels = exercise.primaryMuscles.map { it.label() }.joinToString(" · ")
    val secondaryLabels = exercise.secondaryMuscles.map { it.label() }.joinToString(" · ")
    val equipmentLabel = exercise.equipment.label()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ForjaRadius.shapeL)
            .background(colors.surface1)
            .padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Text(
            text = exercise.name.resolve(languageCode),
            style = typography.title3,
            color = colors.text1,
        )
        Text(
            text = if (primaryLabels.isNotEmpty()) {
                "$primaryLabels · $equipmentLabel"
            } else {
                equipmentLabel
            },
            style = typography.body2,
            color = colors.text2,
        )
        if (secondaryLabels.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.catalog_secondary_prefix, secondaryLabels),
                style = typography.label,
                color = colors.text3,
            )
        }
    }
}
