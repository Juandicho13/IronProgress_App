package com.olarte.ironprogress.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.SearchExercises
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.components.ActiveSessionBannerState
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaIsland
import com.olarte.ironprogress.ui.components.ForjaTab
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaMotion
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IronProgressApp(
    exerciseRepository: ExerciseRepository,
) {
    val languageCode = "es"

    val viewModel = viewModel(key = "catalog-$languageCode") {
        CatalogViewModel(
            searchExercises = SearchExercises(exerciseRepository),
            languageCode = languageCode,
        )
    }
    val catalogState by viewModel.state.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(ForjaTab.TODAY) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var isIslandMinimized by rememberSaveable { mutableStateOf(false) }

    // Estado de la sesion de entreno interactiva
    var isSessionStarted by rememberSaveable { mutableStateOf(false) }
    var isWorkoutScreenOpen by rememberSaveable { mutableStateOf(false) }
    var currentSetNumber by rememberSaveable { mutableIntStateOf(2) }
    var isResting by rememberSaveable { mutableStateOf(false) }
    var restSecondsRemaining by rememberSaveable { mutableIntStateOf(85) }
    var showUndoNotice by rememberSaveable { mutableStateOf(false) }

    // Cuenta regresiva en vivo cuando el descanso esta activo
    LaunchedEffect(isResting, restSecondsRemaining) {
        if (isResting && restSecondsRemaining > 0) {
            delay(1_000L)
            restSecondsRemaining -= 1
        } else if (isResting && restSecondsRemaining <= 0) {
            isResting = false
        }
    }

    val catalogListState = rememberLazyListState()
    val todayListState = rememberLazyListState()
    val planListState = rememberLazyListState()
    val progressListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val scrollConnection = remember(density) {
        val collapseThresholdPx = with(density) { 24.dp.toPx() }
        val expandThresholdPx = with(density) { 8.dp.toPx() }
        var accumulatedScroll = 0f

        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val deltaY = consumed.y
                if (deltaY < 0f) {
                    accumulatedScroll = if (accumulatedScroll < 0f) accumulatedScroll + deltaY else deltaY
                    if (-accumulatedScroll >= collapseThresholdPx) {
                        isIslandMinimized = true
                    }
                } else if (deltaY > 0f) {
                    accumulatedScroll = if (accumulatedScroll > 0f) accumulatedScroll + deltaY else deltaY
                    if (accumulatedScroll >= expandThresholdPx) {
                        isIslandMinimized = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    ForjaTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForjaTheme.colors.background)
                .nestedScroll(scrollConnection),
        ) {
            when {
                isWorkoutScreenOpen -> {
                    ForjaWorkoutScreen(
                        currentSetNumber = currentSetNumber,
                        isResting = isResting,
                        restSecondsRemaining = restSecondsRemaining,
                        showUndoNotice = showUndoNotice,
                        onMinimizeToIsland = { isWorkoutScreenOpen = false },
                        onCompleteSet = {
                            if (currentSetNumber <= 4) {
                                currentSetNumber += 1
                                isResting = true
                                restSecondsRemaining = 85
                                showUndoNotice = true
                            }
                        },
                        onUndoSet = {
                            if (currentSetNumber > 2) {
                                currentSetNumber -= 1
                            }
                            isResting = false
                            showUndoNotice = false
                        },
                        onAdjustRest = { delta ->
                            restSecondsRemaining = (restSecondsRemaining + delta).coerceAtLeast(0)
                            if (restSecondsRemaining == 0) {
                                isResting = false
                            }
                        },
                        onSkipRest = {
                            isResting = false
                            showUndoNotice = false
                        },
                    )
                }

                isSearchActive -> {
                    CatalogScreen(
                        state = catalogState,
                        languageCode = languageCode,
                        onIntent = viewModel::onIntent,
                        listState = catalogListState,
                    )
                }

                else -> {
                    when (selectedTab) {
                        ForjaTab.TODAY -> ForjaTodayScreen(
                            listState = todayListState,
                            onStartWorkout = {
                                isSessionStarted = true
                                isWorkoutScreenOpen = true
                            },
                        )
                        ForjaTab.PLAN -> ForjaPlanScreen(
                            listState = planListState,
                            isSessionStarted = isSessionStarted,
                            onOpenWorkout = {
                                isSessionStarted = true
                                isWorkoutScreenOpen = true
                            },
                        )
                        ForjaTab.PROGRESS -> ForjaProgressScreen(
                            listState = progressListState,
                            catalogState = catalogState,
                            languageCode = languageCode,
                            onCatalogIntent = viewModel::onIntent,
                            catalogListState = catalogListState,
                        )
                    }
                }
            }

            if (!isWorkoutScreenOpen) {
                val activeBanner = if (isSessionStarted) {
                    val minutes = restSecondsRemaining / 60
                    val seconds = (restSecondsRemaining % 60).toString().padStart(2, '0')
                    ActiveSessionBannerState(
                        exerciseName = "Press banca",
                        setSummary = "Serie ${currentSetNumber.coerceAtMost(4)} de 4",
                        statusText = if (isResting) {
                            "Descanso $minutes:$seconds · ♥ 128"
                        } else {
                            "Listo · 62,5 kg × 10"
                        },
                        progress = if (isResting) restSecondsRemaining / 90f else 1f,
                        isReady = !isResting,
                    )
                } else {
                    null
                }

                ForjaIsland(
                    selectedTab = selectedTab,
                    isSearchActive = isSearchActive,
                    isMinimized = isIslandMinimized,
                    todayLabel = "Hoy",
                    planLabel = "Plan",
                    progressLabel = "Progreso",
                    searchContentDescription = "Buscar ejercicios",
                    activeSession = activeBanner,
                    onOpenActiveSession = { isWorkoutScreenOpen = true },
                    onSkipRest = { isResting = false },
                    onTabSelected = { tab ->
                        if (isIslandMinimized) {
                            isIslandMinimized = false
                        } else if (selectedTab == tab && !isSearchActive) {
                            coroutineScope.launch {
                                todayListState.animateScrollToItem(0)
                                planListState.animateScrollToItem(0)
                                progressListState.animateScrollToItem(0)
                            }
                        } else {
                            isSearchActive = false
                            selectedTab = tab
                        }
                    },
                    onSearchClicked = {
                        isIslandMinimized = false
                        isSearchActive = !isSearchActive
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * Pantalla "Hoy" del sistema Forja (seccion 15 del diseno).
 */
@Composable
private fun ForjaTodayScreen(
    listState: LazyListState,
    onStartWorkout: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        contentPadding = PaddingValues(top = ForjaSpacing.esp3, bottom = 148.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                Text(
                    text = "MIÉRCOLES 21 DE OCTUBRE",
                    style = typography.caption,
                    color = colors.text2,
                )
                Text(
                    text = "Hoy",
                    style = typography.title1,
                    color = colors.text1,
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.iceVeil)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            ) {
                Text(
                    text = "RECUPERACIÓN",
                    style = typography.caption,
                    color = colors.iceInk,
                )
                Text(
                    text = "Lista para entrenar completo",
                    style = typography.title3,
                    color = colors.text1,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "7 h 42 min",
                            style = typography.smallFigure,
                            color = colors.text1,
                        )
                        Text(
                            text = " de sueño",
                            style = typography.body2,
                            color = colors.text2,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "52",
                            style = typography.smallFigure,
                            color = colors.text1,
                        )
                        Text(
                            text = " lpm en reposo",
                            style = typography.body2,
                            color = colors.text2,
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Esta semana",
                        style = typography.title3,
                        color = colors.text1,
                    )
                    Text(
                        text = "2 de 5 · racha de 6 semanas",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                val days = listOf("L", "M", "X", "J", "V", "S", "D")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    days.forEachIndexed { index, day ->
                        val completed = index < 2
                        val isToday = index == 2
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completed -> colors.ember
                                        index in 4..5 -> colors.surface3
                                        else -> colors.surface1
                                    },
                                )
                                .then(
                                    if (isToday) {
                                        Modifier.border(2.dp, colors.ember, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (completed) {
                                Icon(
                                    imageVector = ForjaIcons.Check,
                                    contentDescription = "Completado",
                                    tint = colors.onEmber,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Text(
                                    text = day,
                                    style = typography.label,
                                    color = if (isToday) colors.text1 else colors.text2,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val previewExercises = listOf(
                Triple("Press banca con barra", "62,5 kg", true),
                Triple("Press militar con barra", "40 kg", false),
                Triple("Cruce de poleas", "+1 rep", true),
                Triple("Fondos en paralelas", "+10 kg", false),
                Triple("Extensión de tríceps en polea", "25 kg", false),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                    Text(
                        text = "SIGUIENTE · SEMANA 3 DE 6",
                        style = typography.caption,
                        color = colors.text2,
                    )
                    Text(
                        text = "Empuje A",
                        style = typography.title2,
                        color = colors.text1,
                    )
                    Text(
                        text = "5 ejercicios · 55 min · Gimnasio",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                Column {
                    previewExercises.forEachIndexed { index, (name, target, isProgression) ->
                        if (index > 0) {
                            HorizontalDivider(thickness = 1.dp, color = colors.line)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                style = typography.body,
                                color = colors.text1,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isProgression) {
                                    Icon(
                                        imageVector = ForjaIcons.NorthEast,
                                        contentDescription = "Progresión",
                                        tint = colors.emberInk,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = target,
                                    style = typography.smallFigure,
                                    color = if (isProgression) colors.emberInk else colors.text2,
                                )
                            }
                        }
                    }
                }

                val buttonInteraction = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed) 0.97f else 1f,
                    animationSpec = ForjaMotion.spatialFast(),
                    label = "PrimaryButtonScale",
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .fillMaxWidth()
                        .height(ForjaMeasures.primaryButtonHeight)
                        .clip(CircleShape)
                        .background(if (isButtonPressed) colors.emberPressed else colors.ember)
                        .clickable(
                            interactionSource = buttonInteraction,
                            indication = null,
                            onClick = onStartWorkout,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = ForjaIcons.PlayFilled,
                            contentDescription = null,
                            tint = colors.onEmber,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(ForjaSpacing.esp2))
                        Text(
                            text = "Empezar",
                            style = typography.primaryButton,
                            color = colors.onEmber,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pantalla "Plan" del sistema Forja (pagina 40 del diseno):
 * Programa Hipertrofia 6 semanas y lista de rutinas de la carpeta Gimnasio.
 */
@Composable
private fun ForjaPlanScreen(
    listState: LazyListState,
    isSessionStarted: Boolean,
    onOpenWorkout: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    val routines = listOf(
        Triple("Pierna A", "6 ejercicios · 60 min", "Lunes"),
        Triple("Tirón A", "5 ejercicios · 50 min", "Martes"),
        Triple("Empuje A", "5 ejercicios · 55 min", if (isSessionStarted) "En curso" else "Hoy"),
        Triple("Pierna B", "5 ejercicios · 55 min", "Viernes"),
        Triple("Tirón B", "5 ejercicios · 50 min", "Sábado"),
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        contentPadding = PaddingValues(top = ForjaSpacing.esp3, bottom = 156.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Plan",
                    style = typography.title1,
                    color = colors.text1,
                )
                Box(
                    modifier = Modifier
                        .size(ForjaMeasures.secondaryButtonHeight)
                        .clip(CircleShape)
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        style = typography.title2,
                        color = colors.text1,
                    )
                }
            }
        }

        // Tarjeta del programa activo por semanas
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                    Text(
                        text = "PROGRAMA",
                        style = typography.caption,
                        color = colors.text2,
                    )
                    Text(
                        text = "Hipertrofia 6 semanas",
                        style = typography.title2,
                        color = colors.text1,
                    )
                    Text(
                        text = "Semana 3 de 6 · descarga en la semana 6",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                // Barra de 6 semanas (3 en brasa, 2 pendientes, semana 6 de descarga en hielo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (week in 1..6) {
                        val barColor = when {
                            week <= 3 -> colors.ember
                            week == 6 -> colors.ice
                            else -> colors.surface3
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(barColor),
                        )
                    }
                }
            }
        }

        // Tarjeta de Rutinas en carpeta Gimnasio
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = ForjaSpacing.esp1),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Rutinas",
                        style = typography.title3,
                        color = colors.text1,
                    )
                    Text(
                        text = "Gimnasio",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                routines.forEachIndexed { index, (name, details, status) ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 1.dp, color = colors.line)
                    }
                    val isCurrent = name == "Empuje A"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenWorkout)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = name,
                                style = typography.title3,
                                color = colors.text1,
                            )
                            Text(
                                text = details,
                                style = typography.body2,
                                color = colors.text2,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.emberVeil)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = status,
                                        style = typography.label,
                                        color = colors.emberInk,
                                    )
                                }
                            } else {
                                Text(
                                    text = if (index < 2) "✓ $status" else status,
                                    style = typography.body2,
                                    color = if (index < 2) colors.emberInk else colors.text2,
                                )
                            }
                            Text(
                                text = "›",
                                style = typography.title2,
                                color = colors.text2,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pantalla "Progreso" del sistema Forja (pagina 39 del diseno):
 * Control segmentado (Ejercicios, Musculos, Cuerpo), Mapa de recuperacion y Series por musculo.
 */
@Composable
private fun ForjaProgressScreen(
    listState: LazyListState,
    catalogState: com.olarte.ironprogress.ui.catalog.CatalogState,
    languageCode: String,
    onCatalogIntent: (com.olarte.ironprogress.ui.catalog.CatalogIntent) -> Unit,
    catalogListState: LazyListState,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    var selectedSegment by rememberSaveable { mutableIntStateOf(1) }
    val segments = listOf("Ejercicios", "Músculos", "Cuerpo")

    if (selectedSegment == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp2),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                Text(
                    text = "Progreso",
                    style = typography.title1,
                    color = colors.text1,
                )
                ForjaSegmentedControl(
                    segments = segments,
                    selectedIndex = selectedSegment,
                    onSegmentSelected = { selectedSegment = it },
                )
            }
            CatalogScreen(
                state = catalogState,
                languageCode = languageCode,
                onIntent = onCatalogIntent,
                listState = catalogListState,
            )
        }
        return
    }

    val volumeRows = listOf(
        Triple("Pecho", 12, null),
        Triple("Espalda", 14, null),
        Triple("Hombros", 9, "bajo"),
        Triple("Bíceps", 8, "bajo"),
        Triple("Tríceps", 11, null),
        Triple("Cuádriceps", 16, null),
        Triple("Isquios", 10, null),
        Triple("Glúteos", 12, null),
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        contentPadding = PaddingValues(top = ForjaSpacing.esp3, bottom = 156.dp),
    ) {
        item {
            Text(
                text = "Progreso",
                style = typography.title1,
                color = colors.text1,
            )
        }

        item {
            ForjaSegmentedControl(
                segments = segments,
                selectedIndex = selectedSegment,
                onSegmentSelected = { selectedSegment = it },
            )
        }

        // Tarjeta del Mapa corporal de recuperacion con escala calor-0 a calor-4
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                    Text(
                        text = "Recuperación muscular",
                        style = typography.title3,
                        color = colors.text1,
                    )
                    Text(
                        text = "Pecho y tríceps siguen cargados desde ayer",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ForjaSpacing.esp2),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BodyCapsuleSilhouettes(
                        label = "Frente",
                        shoulderColor = colors.heat3,
                        torsoColor = colors.heat4,
                        coreColor = colors.heat2,
                        armColor = colors.heat1,
                        legColor = colors.heat2,
                        calfColor = colors.heat0,
                    )
                    BodyCapsuleSilhouettes(
                        label = "Espalda",
                        shoulderColor = colors.heat3,
                        torsoColor = colors.heat1,
                        coreColor = colors.heat2,
                        armColor = colors.heat4,
                        legColor = colors.heat2,
                        calfColor = colors.heat0,
                    )
                }

                // Leyenda de la escala de calor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Fresco",
                        style = typography.label,
                        color = colors.text2,
                    )
                    Spacer(modifier = Modifier.width(ForjaSpacing.esp2))
                    val heatSteps = listOf(
                        colors.heat0,
                        colors.heat1,
                        colors.heat2,
                        colors.heat3,
                        colors.heat4,
                    )
                    heatSteps.forEach { stepColor ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(width = 22.dp, height = 10.dp)
                                .clip(CircleShape)
                                .background(stepColor),
                        )
                    }
                    Spacer(modifier = Modifier.width(ForjaSpacing.esp2))
                    Text(
                        text = "Cargado",
                        style = typography.label,
                        color = colors.text2,
                    )
                }
            }
        }

        // Tarjeta de Series por musculo (ultimos 7 dias) sobre banda de rango objetivo
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ForjaRadius.shapeL)
                    .background(colors.surface1)
                    .padding(ForjaSpacing.esp4),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                    Text(
                        text = "Series por músculo",
                        style = typography.title3,
                        color = colors.text1,
                    )
                    Text(
                        text = "Últimos 7 días · 15 al 21 oct",
                        style = typography.body2,
                        color = colors.text2,
                    )
                }

                volumeRows.forEach { (muscleName, sets, tag) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                    ) {
                        Text(
                            text = muscleName,
                            style = typography.body2,
                            color = colors.text1,
                            modifier = Modifier.width(86.dp),
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Banda del rango objetivo (10 a 20 series = 50% a 100% del ancho)
                            Row(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.surface3),
                                )
                            }

                            // Barra en brasa de 12 dp de grosor
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (sets / 20f).coerceIn(0.05f, 1f))
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                    .background(colors.ember),
                            )
                        }

                        Row(
                            modifier = Modifier.width(56.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = sets.toString(),
                                style = typography.smallFigure,
                                color = colors.text1,
                            )
                            if (tag != null) {
                                Text(
                                    text = " $tag",
                                    style = typography.label,
                                    color = colors.text3,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Rango objetivo: 10 a 20 series",
                    style = typography.label,
                    color = colors.text2,
                )
            }
        }
    }
}

@Composable
private fun ForjaSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(colors.surface3)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (selected) colors.activeControl else Color.Transparent)
                    .clickable { onSegmentSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = typography.label,
                    color = if (selected) colors.text1 else colors.text2,
                )
            }
        }
    }
}

@Composable
private fun BodyCapsuleSilhouettes(
    label: String,
    shoulderColor: Color,
    torsoColor: Color,
    coreColor: Color,
    armColor: Color,
    legColor: Color,
    calfColor: Color,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Cabeza neutra en superficie-3
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(colors.surface3),
        )
        // Hombros y pecho/espalda
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CapsulePart(width = 14.dp, height = 18.dp, color = shoulderColor)
            CapsulePart(width = 18.dp, height = 22.dp, color = torsoColor)
            CapsulePart(width = 18.dp, height = 22.dp, color = torsoColor)
            CapsulePart(width = 14.dp, height = 18.dp, color = shoulderColor)
        }
        // Brazos y zona media
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsulePart(width = 12.dp, height = 34.dp, color = armColor)
            CapsulePart(width = 34.dp, height = 34.dp, color = coreColor)
            CapsulePart(width = 12.dp, height = 34.dp, color = armColor)
        }
        // Cuadriceps / isquios
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CapsulePart(width = 16.dp, height = 42.dp, color = legColor)
            CapsulePart(width = 16.dp, height = 42.dp, color = legColor)
        }
        // Gemelos
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CapsulePart(width = 12.dp, height = 32.dp, color = calfColor)
            CapsulePart(width = 12.dp, height = 32.dp, color = calfColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = typography.label,
            color = colors.text2,
        )
    }
}

@Composable
private fun CapsulePart(
    width: Dp,
    height: Dp,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Pantalla "Entreno" del sistema Forja (paginas 35 y 36 del diseno):
 * Filas de serie con calentamiento, record en oro-velo, serie actual y Barra del entreno que se transforma en el descanso.
 */
@Composable
private fun ForjaWorkoutScreen(
    currentSetNumber: Int,
    isResting: Boolean,
    restSecondsRemaining: Int,
    showUndoNotice: Boolean,
    onMinimizeToIsland: () -> Unit,
    onCompleteSet: () -> Unit,
    onUndoSet: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = ForjaSpacing.esp5),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            contentPadding = PaddingValues(top = ForjaSpacing.esp2, bottom = 164.dp),
        ) {
            // Encabezado del entreno con minimizar a la isla
            item {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ForjaMeasures.minTouch)
                                .clip(CircleShape)
                                .clickable(onClick = onMinimizeToIsland),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⌄",
                                style = typography.title2,
                                color = colors.text1,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Empuje A",
                                style = typography.title3,
                                color = colors.text1,
                            )
                            Text(
                                text = "18:42",
                                style = typography.caption,
                                color = colors.text2,
                            )
                        }
                        Box(
                            modifier = Modifier.size(ForjaMeasures.minTouch),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "•••",
                                style = typography.label,
                                color = colors.text2,
                            )
                        }
                    }

                    // Progreso de los 5 ejercicios de la sesion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (index in 0 until 5) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) colors.ember else colors.surface3),
                            )
                        }
                    }
                }
            }

            // Tarjeta del ejercicio activo: Press banca con barra
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ForjaRadius.shapeL)
                        .background(colors.surface1)
                        .padding(ForjaSpacing.esp4),
                    verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Press banca con barra",
                            style = typography.title2,
                            color = colors.text1,
                        )
                        Text(
                            text = "Pecho · Tríceps · Hombros",
                            style = typography.body2,
                            color = colors.text2,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = ForjaIcons.NorthEast,
                            contentDescription = null,
                            tint = colors.emberInk,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "+2,5 kg porque hiciste 3 × 12 con RIR 2",
                            style = typography.body2,
                            color = colors.text1,
                        )
                    }

                    // Encabezado de columnas de la Fila de serie
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "#",
                            style = typography.caption,
                            color = colors.text2,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(
                            text = "ANTERIOR",
                            style = typography.caption,
                            color = colors.text2,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "KG",
                            style = typography.caption,
                            color = colors.text2,
                            modifier = Modifier.width(76.dp),
                        )
                        Text(
                            text = "REPS",
                            style = typography.caption,
                            color = colors.text2,
                            modifier = Modifier.width(60.dp),
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // Serie C de calentamiento
                    WorkoutSetRow(
                        badge = "C",
                        previous = "40 × 10",
                        weight = "40",
                        reps = "10",
                        isWarmup = true,
                        isRecord = false,
                        isCompleted = true,
                        isCurrent = false,
                        onComplete = {},
                    )

                    // Serie 1: Record en oro-velo
                    WorkoutSetRow(
                        badge = "★",
                        previous = "60 × 12",
                        weight = "62,5",
                        reps = "11",
                        isWarmup = false,
                        isRecord = true,
                        isCompleted = true,
                        isCurrent = false,
                        onComplete = {},
                    )

                    // Series 2, 3 y 4
                    val targetReps = listOf(2 to "60 × 12", 3 to "60 × 12", 4 to "60 × 11")
                    targetReps.forEach { (setNum, prev) ->
                        WorkoutSetRow(
                            badge = setNum.toString(),
                            previous = prev,
                            weight = "62,5",
                            reps = "10",
                            isWarmup = false,
                            isRecord = false,
                            isCompleted = setNum < currentSetNumber,
                            isCurrent = setNum == currentSetNumber,
                            onComplete = onCompleteSet,
                        )
                    }

                    Text(
                        text = "+ Añadir serie",
                        style = typography.label,
                        color = colors.emberInk,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = ForjaSpacing.esp1),
                    )
                }
            }

            // Siguientes ejercicios plegados
            item {
                FoldedExerciseCard(order = "2", name = "Press militar con barra", summary = "4 series · 40 kg × 6-10")
            }
            item {
                FoldedExerciseCard(order = "3", name = "Cruce de poleas", summary = "4 series · 15 kg × 12-15")
            }
        }

        // Aviso de Deshacer y Barra del entreno en la zona del pulgar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = ForjaSpacing.esp5,
                    end = ForjaSpacing.esp5,
                    bottom = ForjaMeasures.islandMargin,
                ),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            if (showUndoNotice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ForjaMeasures.secondaryButtonHeight)
                        .clip(CircleShape)
                        .background(colors.glass)
                        .border(1.dp, colors.glassBorder, CircleShape)
                        .padding(horizontal = ForjaSpacing.esp4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Serie ${(currentSetNumber - 1).coerceAtLeast(2)} guardada",
                        style = typography.body2,
                        color = colors.text1,
                    )
                    Text(
                        text = "Deshacer",
                        style = typography.label,
                        color = colors.emberInk,
                        modifier = Modifier.clickable(onClick = onUndoSet),
                    )
                }
            }

            // La Barra del entreno: se transforma con resorte entre Completar serie y el Descanso
            AnimatedContent(
                targetState = isResting,
                transitionSpec = {
                    fadeIn(ForjaMotion.effectFast()) togetherWith
                            fadeOut(ForjaMotion.effectFast()) using
                            SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ -> ForjaMotion.spatial() },
                            )
                },
                label = "WorkoutBarMorph",
            ) { resting ->
                if (resting) {
                    val minutes = restSecondsRemaining / 60
                    val seconds = (restSecondsRemaining % 60).toString().padStart(2, '0')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ForjaMeasures.islandHeight)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.glass)
                            .border(1.dp, colors.glassBorder, CircleShape)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ForjaMeasures.secondaryButtonHeight)
                                .clip(CircleShape)
                                .background(colors.surface3)
                                .clickable { onAdjustRest(-15) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "−15",
                                style = typography.smallFigure,
                                color = colors.text1,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
                        ) {
                            Canvas(modifier = Modifier.size(ForjaMeasures.restRingSize)) {
                                val strokeWidthPx = 4.dp.toPx()
                                drawArc(
                                    color = colors.surface3,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthPx),
                                )
                                drawArc(
                                    color = colors.ice,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (restSecondsRemaining / 90f).coerceIn(0f, 1f),
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                                )
                            }
                            Column {
                                Text(
                                    text = "$minutes:$seconds",
                                    style = typography.figure,
                                    color = colors.text1,
                                )
                                Text(
                                    text = "♥ 139",
                                    style = typography.caption,
                                    color = colors.iceInk,
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                            Box(
                                modifier = Modifier
                                    .size(ForjaMeasures.secondaryButtonHeight)
                                    .clip(CircleShape)
                                    .background(colors.surface3)
                                    .clickable { onAdjustRest(15) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+15",
                                    style = typography.smallFigure,
                                    color = colors.text1,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(ForjaMeasures.secondaryButtonHeight)
                                    .clip(CircleShape)
                                    .background(colors.surface3)
                                    .clickable(onClick = onSkipRest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "›|",
                                    style = typography.smallFigure,
                                    color = colors.text1,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ForjaMeasures.islandHeight)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.ember)
                            .clickable(onClick = onCompleteSet),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = ForjaIcons.Check,
                            contentDescription = null,
                            tint = colors.onEmber,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(ForjaSpacing.esp3))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Completar serie ${currentSetNumber.coerceAtMost(4)}",
                                style = typography.primaryButton,
                                color = colors.onEmber,
                            )
                            Text(
                                text = "62,5 kg × 10",
                                style = typography.smallFigure,
                                color = colors.onEmber,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    badge: String,
    previous: String,
    weight: String,
    reps: String,
    isWarmup: Boolean,
    isRecord: Boolean,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onComplete: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    val rowBackground = when {
        isRecord -> colors.goldVeil
        isCurrent -> colors.surface2
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ForjaMeasures.setRowHeight)
            .clip(ForjaRadius.shapeM)
            .background(rowBackground)
            .then(
                if (isCurrent) {
                    Modifier.border(1.dp, colors.line, ForjaRadius.shapeM)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = badge,
            style = typography.smallFigure,
            color = when {
                isWarmup -> colors.iceInk
                isRecord -> colors.goldInk
                else -> colors.text2
            },
            modifier = Modifier.width(28.dp),
        )

        Text(
            text = previous,
            style = typography.smallFigure,
            color = colors.text3,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .width(76.dp)
                .height(44.dp)
                .clip(ForjaRadius.shapeS)
                .background(if (isCompleted) Color.Transparent else colors.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = weight,
                style = typography.figure,
                color = if (isCompleted && !isRecord) colors.text2 else colors.text1,
            )
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(44.dp)
                .clip(ForjaRadius.shapeS)
                .background(if (isCompleted) Color.Transparent else colors.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = reps,
                style = typography.figure,
                color = if (isCompleted && !isRecord) colors.text2 else colors.text1,
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isCompleted) colors.ember else colors.surface3)
                .clickable(enabled = !isCompleted, onClick = onComplete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ForjaIcons.Check,
                contentDescription = "Completar serie",
                tint = if (isCompleted) colors.onEmber else colors.text2,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FoldedExerciseCard(
    order: String,
    name: String,
    summary: String,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ForjaRadius.shapeL)
            .background(colors.surface1)
            .padding(ForjaSpacing.esp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = order,
                style = typography.label,
                color = colors.text2,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = typography.title3,
                color = colors.text1,
            )
            Text(
                text = summary,
                style = typography.body2,
                color = colors.text2,
            )
        }
        Text(
            text = "⌄",
            style = typography.title3,
            color = colors.text2,
        )
    }
}
