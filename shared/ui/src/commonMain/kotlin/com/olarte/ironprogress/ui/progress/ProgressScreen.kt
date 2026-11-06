package com.olarte.ironprogress.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.ui.catalog.CatalogIntent
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaHeaderTop
import com.olarte.ironprogress.ui.components.ForjaLargeTitle
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.nav_progress
import com.olarte.ironprogress.ui.resources.progress_body_body
import com.olarte.ironprogress.ui.resources.progress_body_title
import com.olarte.ironprogress.ui.resources.progress_empty_body
import com.olarte.ironprogress.ui.resources.progress_empty_title
import com.olarte.ironprogress.ui.resources.progress_history
import com.olarte.ironprogress.ui.resources.progress_last7
import com.olarte.ironprogress.ui.resources.progress_low
import com.olarte.ironprogress.ui.resources.progress_seg_body
import com.olarte.ironprogress.ui.resources.progress_seg_exercises
import com.olarte.ironprogress.ui.resources.progress_seg_muscles
import com.olarte.ironprogress.ui.resources.progress_sets_title
import com.olarte.ironprogress.ui.resources.progress_target
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressRoute(
    listState: LazyListState,
    catalogState: CatalogState,
    languageCode: String,
    onCatalogIntent: (CatalogIntent) -> Unit,
    catalogListState: LazyListState,
    onHistory: () -> Unit,
    model: ProgressViewModel = koinViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    ProgressScreen(state, listState, catalogState, languageCode, onCatalogIntent, catalogListState, onHistory)
}

/**
 * Progreso (PANTALLAS 2.1): título con Historial y segmentado Ejercicios, Músculos y Cuerpo.
 * Músculos muestra las series reales de los últimos 7 días; sin entrenos, un estado vacío y ningún
 * número inventado. El mapa de recuperación llega con F2-08.
 */
@Composable
fun ProgressScreen(
    state: ProgressState,
    listState: LazyListState,
    catalogState: CatalogState,
    languageCode: String,
    onCatalogIntent: (CatalogIntent) -> Unit,
    catalogListState: LazyListState,
    onHistory: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val haptics = ForjaTheme.haptics
    var selected by rememberSaveable { mutableIntStateOf(1) }
    val segments = listOf(
        stringResource(Res.string.progress_seg_exercises),
        stringResource(Res.string.progress_seg_muscles),
        stringResource(Res.string.progress_seg_body),
    )
    val header: @Composable () -> Unit = {
        ForjaLargeTitle(stringResource(Res.string.nav_progress)) {
            ForjaButton(stringResource(Res.string.progress_history), style = ForjaButtonStyle.TONAL, onClick = onHistory)
        }
        Spacer(Modifier.height(ForjaSpacing.esp3))
        SegmentedControl(segments, selected) { index ->
            if (selected != index) {
                haptics.segmentTick()
                selected = index
            }
        }
    }

    if (selected == 0) {
        Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
            Column(Modifier.padding(horizontal = ForjaSpacing.esp5).padding(top = ForjaHeaderTop, bottom = ForjaSpacing.esp2)) {
                header()
            }
            CatalogScreen(state = catalogState, languageCode = languageCode, onIntent = onCatalogIntent, listState = catalogListState)
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        contentPadding = PaddingValues(
            start = ForjaSpacing.esp5,
            end = ForjaSpacing.esp5,
            top = ForjaHeaderTop,
            bottom = ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight + ForjaSpacing.esp8,
        ),
    ) {
        item { Column { header() } }
        when {
            selected == 2 -> item {
                ForjaEmptyState(stringResource(Res.string.progress_body_title), stringResource(Res.string.progress_body_body))
            }
            state.loading -> item {
                com.olarte.ironprogress.ui.components.DelayedLoading(true) {
                    Text(stringResource(Res.string.editor_loading), style = ForjaTheme.typography.body2, color = colors.text2)
                }
            }
            state.sets.isEmpty() -> item {
                ForjaEmptyState(stringResource(Res.string.progress_empty_title), stringResource(Res.string.progress_empty_body))
            }
            else -> item { MuscleSetsCard(state) }
        }
    }
}

@Composable
private fun MuscleSetsCard(state: ProgressState) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    ForjaCard {
        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            Text(stringResource(Res.string.progress_sets_title), style = typography.title3, color = colors.text1)
            Text(
                stringResource(Res.string.progress_last7, formats.date(state.fromEpochMs), formats.date(state.toEpochMs)),
                style = typography.body2,
                color = colors.text2,
            )
        }
        state.sets.forEach { (muscle, sets) -> MuscleRow(muscle, sets, formats.number(sets, 1)) }
        Text(stringResource(Res.string.progress_target), style = typography.label, color = colors.text2)
    }
}

@Composable
private fun MuscleRow(muscle: Muscle, sets: Double, shown: String) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val max = MuscleSets.TARGET_MAX.toFloat()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Text(muscle.label(), Modifier.width(96.dp), style = typography.body2, color = colors.text1)
        Box(Modifier.weight(1f).height(20.dp), contentAlignment = Alignment.CenterStart) {
            // Banda del rango objetivo: de 10 a 20 series, la mitad derecha.
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(MuscleSets.TARGET_MIN.toFloat()))
                Box(
                    Modifier.weight(max - MuscleSets.TARGET_MIN).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(colors.surface3),
                )
            }
            Box(
                Modifier.fillMaxWidth((sets.toFloat() / max).coerceIn(0.04f, 1f)).height(12.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(colors.ember),
            )
        }
        Row(Modifier.width(64.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(shown, style = typography.smallFigure, color = colors.text1, textAlign = TextAlign.End)
            if (sets < MuscleSets.TARGET_MIN) {
                Text(" " + stringResource(Res.string.progress_low), style = typography.label, color = colors.text3)
            }
        }
    }
}

/** Segmentado de PANTALLAS 0: píldora surface3 con la opción elegida en activeControl. */
@Composable
private fun SegmentedControl(segments: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = ForjaTheme.colors
    Row(
        Modifier.fillMaxWidth().height(ForjaMeasures.minTouch).clip(CircleShape).background(colors.surface3).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .background(if (isSelected) colors.activeControl else Color.Transparent)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(title, style = ForjaTheme.typography.label, color = if (isSelected) colors.text1 else colors.text2)
            }
        }
    }
}
