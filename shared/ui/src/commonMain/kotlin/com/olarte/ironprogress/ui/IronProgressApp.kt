package com.olarte.ironprogress.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.components.ForjaIsland
import com.olarte.ironprogress.ui.components.ForjaTab
import com.olarte.ironprogress.ui.components.LocalForjaHazeState
import com.olarte.ironprogress.ui.navigation.Route
import com.olarte.ironprogress.ui.navigation.navConfiguration
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.nav_plan
import com.olarte.ironprogress.ui.resources.nav_progress
import com.olarte.ironprogress.ui.resources.nav_search_exercises
import com.olarte.ironprogress.ui.resources.nav_today
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.WorkoutEffect
import com.olarte.ironprogress.ui.workout.WorkoutIntent
import com.olarte.ironprogress.ui.workout.WorkoutRoute
import com.olarte.ironprogress.ui.workout.WorkoutViewModel
import com.olarte.ironprogress.ui.workout.workoutBanner
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun IronProgressApp() {
    // Idioma del sistema; LocalizedText.resolve da español a "es" y a "es-CO", e inglés a cualquier otro.
    val languageCode = Locale.current.language

    // Lo usan Buscar y Progreso, por eso vive a nivel de la app y no en una entrada de la pila.
    val viewModel = koinViewModel<CatalogViewModel>(key = "catalog-$languageCode") {
        parametersOf(languageCode)
    }
    val catalogState by viewModel.state.collectAsStateWithLifecycle()
    val workoutViewModel = koinViewModel<WorkoutViewModel>()
    val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()

    // Pila de Navigation 3: la raíz es una pestaña y encima pueden ir Catálogo o Entreno.
    // Sobrevive a girar el teléfono y al cierre del sistema (se guarda con navConfiguration).
    val backStack = rememberNavBackStack(navConfiguration, Route.Today)
    val hazeState = rememberHazeState()
    val currentDestination = backStack.lastOrNull() ?: Route.Today

    val selectedTab = remember(backStack.toList()) {
        (backStack.lastOrNull { it == Route.Today || it == Route.Plan || it == Route.Progress } as? Route)
            ?.toForjaTab() ?: ForjaTab.TODAY
    }
    val isSearchActive = currentDestination == Route.Catalog
    var editorBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    val isWorkoutScreenOpen = currentDestination is Route.Workout

    var isIslandMinimized by rememberSaveable { mutableStateOf(false) }

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
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
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

    ForjaTheme {
        val haptics = ForjaTheme.haptics

        LaunchedEffect(workoutViewModel) {
            workoutViewModel.effects.collect { effect ->
                when (effect) {
                    is WorkoutEffect.OpenWorkout -> {
                        backStack.removeAll { it is Route.Workout }
                        backStack.add(Route.Workout(effect.sessionId))
                    }
                    WorkoutEffect.SetCompleted -> haptics.setCompleted()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForjaTheme.colors.background)
                .nestedScroll(scrollConnection),
        ) {
            NavDisplay(
                backStack = backStack,
                // El contenido que pasa por detrás de la isla se desenfoca en el vidrio (F0-10).
                modifier = Modifier.hazeSource(hazeState),
                onBack = {
                    if (editorBack != null) {
                        editorBack?.invoke()
                    } else if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Route.Workout> {
                        CompositionLocalProvider(LocalForjaHazeState provides hazeState) {
                            WorkoutRoute(workoutViewModel, onMinimize = {
                                backStack.removeAll { it is Route.Workout }
                                if (backStack.isEmpty()) backStack.add(Route.Today)
                            })
                        }
                    }

                    entry<Route.ExerciseEditor> { route ->
                        com.olarte.ironprogress.ui.catalog.ExerciseEditorRoute(
                            route.id,
                            onClose = { backStack.removeLastOrNull() },
                            setBack = { editorBack = it },
                        )
                    }
                    entry<Route.Catalog> {
                        CatalogScreen(
                            state = catalogState,
                            languageCode = languageCode,
                            onIntent = viewModel::onIntent,
                            listState = catalogListState,
                            onEdit = { backStack.add(Route.ExerciseEditor(it)) },
                            onOpen = { backStack.add(Route.ExerciseDetail(it)) },
                        )
                    }
                    entry<Route.ExerciseDetail> { route ->
                        com.olarte.ironprogress.ui.catalog.ExerciseDetailRoute(
                            route.id,
                            languageCode = languageCode,
                            onBack = { backStack.removeLastOrNull() },
                            onOpen = { backStack.add(Route.ExerciseDetail(it)) },
                        )
                    }

                    entry<Route.Today> {
                        com.olarte.ironprogress.ui.today.TodayRoute(
                            listState = todayListState,
                            onStartFree = {
                                backStack.add(Route.Workout())
                                workoutViewModel.onIntent(WorkoutIntent.StartFree)
                            },
                            onOpenWorkout = { sessionId ->
                                backStack.removeAll { it is Route.Workout }
                                backStack.add(Route.Workout(sessionId))
                            },
                            onCreateRoutine = {
                                backStack.add(Route.RoutineEditor(null))
                            },
                        )
                    }

                    entry<Route.Plan> {
                        com.olarte.ironprogress.ui.plan.PlanRoute { backStack.add(Route.RoutineEditor(it)) }
                    }
                    entry<Route.RoutineEditor> { route ->
                        com.olarte.ironprogress.ui.plan.RoutineEditorRoute(
                            route.id,
                            onClose = { backStack.removeLastOrNull() },
                            setBack = { editorBack = it },
                        )
                    }

                    entry<Route.Progress> {
                        ForjaProgressScreen(
                            listState = progressListState,
                            catalogState = catalogState,
                            languageCode = languageCode,
                            onCatalogIntent = viewModel::onIntent,
                            catalogListState = catalogListState,
                        )
                    }
                },
            )

            val isFullScreen = currentDestination is Route.ExerciseEditor ||
                currentDestination is Route.ExerciseDetail ||
                currentDestination is Route.RoutineEditor
            if (!isWorkoutScreenOpen && !isFullScreen) {
                val activeBanner = workoutBanner(workoutState)

                CompositionLocalProvider(LocalForjaHazeState provides hazeState) {
                    ForjaIsland(
                        selectedTab = selectedTab,
                        isSearchActive = isSearchActive,
                        isMinimized = isIslandMinimized,
                        todayLabel = stringResource(Res.string.nav_today),
                        planLabel = stringResource(Res.string.nav_plan),
                        progressLabel = stringResource(Res.string.nav_progress),
                        searchContentDescription = stringResource(Res.string.nav_search_exercises),
                        activeSession = activeBanner,
                        onOpenActiveSession = {
                            backStack.removeAll { it is Route.Workout }
                            backStack.add(Route.Workout(workoutState.session?.session?.id?.value))
                        },
                        onSkipRest = {},
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
                                backStack.clear()
                                backStack.add(tab.toNavDestination())
                            }
                        },
                        onSearchClicked = {
                            isIslandMinimized = false
                            if (isSearchActive) {
                                backStack.removeAll { it == Route.Catalog }
                                if (backStack.isEmpty()) {
                                    backStack.add(Route.Today)
                                }
                            } else {
                                backStack.removeAll { it == Route.Catalog }
                                backStack.add(Route.Catalog)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

private fun ForjaTab.toNavDestination(): Route = when (this) {
    ForjaTab.TODAY -> Route.Today
    ForjaTab.PLAN -> Route.Plan
    ForjaTab.PROGRESS -> Route.Progress
}

private fun Route.toForjaTab(): ForjaTab = when (this) {
    Route.Today -> ForjaTab.TODAY
    Route.Plan -> ForjaTab.PLAN
    Route.Progress -> ForjaTab.PROGRESS
    is Route.RoutineEditor -> ForjaTab.PLAN
    is Route.ExerciseEditor -> ForjaTab.TODAY
    is Route.ExerciseDetail -> ForjaTab.TODAY
    Route.Catalog -> ForjaTab.TODAY
    is Route.Workout -> ForjaTab.TODAY
}

/**
 * Pantalla "Plan" del sistema Forja (pagina 40 del diseno):
 * Programa Hipertrofia 6 semanas y lista de rutinas de la carpeta Gimnasio.
 */
@Composable
private fun ForjaPlanScreen(listState: LazyListState, isSessionStarted: Boolean, onOpenWorkout: () -> Unit) {
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
    val haptics = ForjaTheme.haptics
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
                    onSegmentSelected = { index ->
                        if (selectedSegment != index) {
                            haptics.segmentTick()
                            selectedSegment = index
                        }
                    },
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
                onSegmentSelected = { index ->
                    if (selectedSegment != index) {
                        haptics.segmentTick()
                        selectedSegment = index
                    }
                },
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
                        text = "Últimos 7 días · 17 al 23 oct",
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
private fun ForjaSegmentedControl(segments: List<String>, selectedIndex: Int, onSegmentSelected: (Int) -> Unit) {
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
private fun CapsulePart(width: Dp, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .background(color),
    )
}
