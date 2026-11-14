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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.domain.progress.VolumeStatus
import com.olarte.ironprogress.ui.body.BodyActions
import com.olarte.ironprogress.ui.body.BodyContent
import com.olarte.ironprogress.ui.body.BodyState
import com.olarte.ironprogress.ui.body.BodyViewModel
import com.olarte.ironprogress.ui.catalog.CatalogIntent
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaHeaderTop
import com.olarte.ironprogress.ui.components.ForjaLargeTitle
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.muscle_row_a11y
import com.olarte.ironprogress.ui.resources.muscle_sets_effective_hint
import com.olarte.ironprogress.ui.resources.muscle_status_above
import com.olarte.ironprogress.ui.resources.muscle_status_below
import com.olarte.ironprogress.ui.resources.muscle_status_in_range
import com.olarte.ironprogress.ui.resources.nav_progress
import com.olarte.ironprogress.ui.resources.progress_empty_body
import com.olarte.ironprogress.ui.resources.progress_empty_title
import com.olarte.ironprogress.ui.resources.progress_history
import com.olarte.ironprogress.ui.resources.progress_last7
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
import org.koin.core.parameter.parametersOf

@Composable
fun ProgressRoute(
    listState: LazyListState,
    catalogState: CatalogState,
    languageCode: String,
    onCatalogIntent: (CatalogIntent) -> Unit,
    catalogListState: LazyListState,
    onHistory: () -> Unit,
    onOpenExercise: (String) -> Unit = {},
    onOpenSummary: (String) -> Unit = {},
    onComparePhotos: () -> Unit = {},
    model: ProgressViewModel = koinViewModel(),
    body: BodyViewModel = koinViewModel(),
    period: PeriodSummaryViewModel = koinViewModel(key = PERIOD_CARD_KEY) { parametersOf("") },
) {
    val state by model.state.collectAsStateWithLifecycle()
    val bodyState by body.state.collectAsStateWithLifecycle()
    val periodState by period.state.collectAsStateWithLifecycle()
    val bodyActions = remember(body, onComparePhotos) {
        BodyActions(
            logWeight = body::logWeight,
            logMeasure = body::logMeasure,
            deleteWeight = body::deleteWeight,
            deleteMeasure = body::deleteMeasure,
            addPhoto = body::addPhoto,
            deletePhoto = { body.deletePhoto(it.id) },
            comparePhotos = onComparePhotos,
        )
    }
    ProgressScreen(
        state,
        listState,
        catalogState,
        languageCode,
        onCatalogIntent,
        catalogListState,
        onHistory,
        onOpenExercise,
        bodyState = bodyState,
        bodyActions = bodyActions,
        periodState = periodState,
        onOpenSummary = onOpenSummary,
    )
}

/** Clave del resumen del mes de la tarjeta de Progreso: vive a nivel de la app (F2-10). */
const val PERIOD_CARD_KEY = "period-card"

/**
 * Progreso (PANTALLAS 2.1): título con Historial y segmentado Ejercicios, Músculos y Cuerpo.
 * Músculos muestra las series reales de los últimos 7 días; sin entrenos, un estado vacío y ningún
 * número inventado. Arriba, el mapa de recuperación de las últimas dos semanas (F2-08).
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
    onOpenExercise: (String) -> Unit = {},
    initialSegment: Int = 1,
    bodyState: BodyState = BodyState(loading = false),
    bodyActions: BodyActions = BodyActions(),
    periodState: PeriodUiState = PeriodUiState(loading = false),
    onOpenSummary: (String) -> Unit = {},
    withPhotoPicker: Boolean = true,
) {
    val colors = ForjaTheme.colors
    val haptics = ForjaTheme.haptics
    var selected by rememberSaveable { mutableIntStateOf(initialSegment) }
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
            // En Progreso, tocar un ejercicio abre su progreso (F2-04); la ficha sale del buscador.
            CatalogScreen(
                state = catalogState,
                languageCode = languageCode,
                onIntent = onCatalogIntent,
                listState = catalogListState,
                onOpen = onOpenExercise,
            )
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
        // Resumen del mes (F2-10): arriba de Músculos en cuanto hay algún entreno.
        if (selected == 1 && periodState.hasAnySession) {
            item {
                MonthSummaryCard(
                    periodState,
                    onOpenMonth = { onOpenSummary(PeriodSummaryViewModel.keyOf(it)) },
                    onOpenYear = { onOpenSummary(PeriodSummaryViewModel.keyOf(it)) },
                )
            }
        }
        when {
            selected == 2 -> item {
                if (bodyState.loading) {
                    com.olarte.ironprogress.ui.components.DelayedLoading(true) {
                        Text(stringResource(Res.string.editor_loading), style = ForjaTheme.typography.body2, color = colors.text2)
                    }
                } else {
                    BodyContent(bodyState, bodyActions, withPhotoPicker = withPhotoPicker)
                }
            }
            state.loading -> item {
                com.olarte.ironprogress.ui.components.DelayedLoading(true) {
                    Text(stringResource(Res.string.editor_loading), style = ForjaTheme.typography.body2, color = colors.text2)
                }
            }
            !state.hasSessions -> item {
                ForjaEmptyState(stringResource(Res.string.progress_empty_title), stringResource(Res.string.progress_empty_body))
            }
            else -> {
                // F2-08: el mapa de recuperación va arriba; las series por músculo, si hay de los últimos 7 días.
                item(key = "recovery") { RecoveryCard(state.recovery, state.toEpochMs) }
                if (state.sets.isNotEmpty()) item(key = "sets") { MuscleSetsCard(state) }
            }
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
        Text(stringResource(Res.string.muscle_sets_effective_hint), style = typography.caption, color = colors.text3)
    }
}

/** Color de la barra y del punto según el rango: bajo en hielo, en rango en ember, alto en peligro. */
@Composable
private fun VolumeStatus.color(): Color = when (this) {
    VolumeStatus.BELOW -> ForjaTheme.colors.ice
    VolumeStatus.IN_RANGE -> ForjaTheme.colors.ember
    VolumeStatus.ABOVE -> ForjaTheme.colors.danger
}

@Composable
private fun VolumeStatus.label(): String = stringResource(
    when (this) {
        VolumeStatus.BELOW -> Res.string.muscle_status_below
        VolumeStatus.IN_RANGE -> Res.string.muscle_status_in_range
        VolumeStatus.ABOVE -> Res.string.muscle_status_above
    },
)

/**
 * Una fila de series efectivas (F2-07): el músculo, la barra sobre la banda del rango (10 a 20) y el
 * número con su semáforo. El estado va con palabra y punto, nunca solo con color.
 */
@Composable
private fun MuscleRow(muscle: Muscle, sets: Double, shown: String) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val status = MuscleSets.status(sets)
    // La escala llega a 24 para que "alto" se vea por encima de la banda.
    val max = (MuscleSets.TARGET_MAX + 4).toFloat()
    val name = muscle.label()
    val statusLabel = status.label()
    val description = stringResource(Res.string.muscle_row_a11y, name, shown, statusLabel)
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Text(name, Modifier.width(92.dp), style = typography.body2, color = colors.text1, maxLines = 1)
        Box(Modifier.weight(1f).height(20.dp), contentAlignment = Alignment.CenterStart) {
            // Banda del rango objetivo: de 10 a 20 series.
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(MuscleSets.TARGET_MIN.toFloat()))
                Box(
                    Modifier.weight((MuscleSets.TARGET_MAX - MuscleSets.TARGET_MIN).toFloat()).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp)).background(colors.surface3),
                )
                Spacer(Modifier.weight(max - MuscleSets.TARGET_MAX))
            }
            Box(
                Modifier.fillMaxWidth((sets.toFloat() / max).coerceIn(0.04f, 1f)).height(12.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(status.color()),
            )
        }
        Column(Modifier.width(76.dp), horizontalAlignment = Alignment.End) {
            ForjaNumber(shown, typography.smallFigure, color = colors.text1, textAlign = TextAlign.End)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(status.color()))
                Text(statusLabel, style = typography.caption, color = colors.text2, maxLines = 1)
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
