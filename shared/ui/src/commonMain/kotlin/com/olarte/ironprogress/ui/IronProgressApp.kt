package com.olarte.ironprogress.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.olarte.ironprogress.ui.catalog.CatalogScreen
import com.olarte.ironprogress.ui.catalog.CatalogViewModel
import com.olarte.ironprogress.ui.components.ForjaIsland
import com.olarte.ironprogress.ui.components.ForjaTab
import com.olarte.ironprogress.ui.components.LocalForjaHazeState
import com.olarte.ironprogress.ui.components.clearFocusOnTap
import com.olarte.ironprogress.ui.discipline.BreathingRoute
import com.olarte.ironprogress.ui.discipline.DisciplineTarget
import com.olarte.ironprogress.ui.discipline.DisciplinesScreen
import com.olarte.ironprogress.ui.discipline.FlowRoute
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.navigation.Route
import com.olarte.ironprogress.ui.navigation.navConfiguration
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.nav_plan
import com.olarte.ironprogress.ui.resources.nav_progress
import com.olarte.ironprogress.ui.resources.nav_search_exercises
import com.olarte.ironprogress.ui.resources.nav_today
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.settings.SettingsRoute
import com.olarte.ironprogress.ui.settings.darkTheme
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
    val languageCode = appLanguage()

    // Lo usan Buscar y Progreso, por eso vive a nivel de la app y no en una entrada de la pila.
    val viewModel = koinViewModel<CatalogViewModel>(key = "catalog-$languageCode") {
        parametersOf(languageCode)
    }
    val catalogState by viewModel.state.collectAsStateWithLifecycle()
    val workoutViewModel = koinViewModel<WorkoutViewModel>()
    val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()
    // Las pestañas guardan su ViewModel en la app: al volver a una no hay pantalla de carga.
    val todayViewModel = koinViewModel<com.olarte.ironprogress.ui.today.TodayViewModel>()
    val planViewModel = koinViewModel<com.olarte.ironprogress.ui.plan.PlanViewModel>()
    val progressViewModel = koinViewModel<com.olarte.ironprogress.ui.progress.ProgressViewModel>()

    // Pila de Navigation 3: la raíz es una pestaña y encima pueden ir Catálogo o Entreno.
    // Sobrevive a girar el teléfono y al cierre del sistema (se guarda con navConfiguration).
    val backStack = rememberNavBackStack(navConfiguration, Route.Today)
    // Recién terminada la bienvenida: "Crear mi primera rutina" abre el editor.
    val onboardingExit = com.olarte.ironprogress.ui.settings.LocalOnboardingExit.current
    LaunchedEffect(onboardingExit.value) {
        if (onboardingExit.value == com.olarte.ironprogress.ui.settings.OnboardingExit.CREATE_ROUTINE) {
            backStack.clear()
            backStack.add(Route.Plan)
            backStack.add(Route.RoutineEditor(null))
        }
        onboardingExit.value = null
    }
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

    val appSettings = LocalAppSettings.current
    ForjaTheme(darkTheme = appSettings.darkTheme(), hapticsEnabled = appSettings.vibration) {
        val haptics = ForjaTheme.haptics

        com.olarte.ironprogress.ui.rest.RestNotificationBridge(
            restController = workoutViewModel.restController,
            isWorkoutScreenOpen = isWorkoutScreenOpen,
        )

        LaunchedEffect(workoutViewModel, appSettings.proMode) {
            workoutViewModel.onIntent(WorkoutIntent.SetProMode(appSettings.proMode))
        }
        LaunchedEffect(workoutViewModel, workoutState.rest != null) {
            if (workoutState.rest != null) while (true) withFrameNanos { workoutViewModel.refreshRest() }
        }
        LaunchedEffect(workoutViewModel, workoutState.work != null) {
            if (workoutState.work != null) while (true) withFrameNanos { workoutViewModel.refreshWork() }
        }
        LaunchedEffect(workoutViewModel.restController) {
            workoutViewModel.restController.openWorkoutRequests.collect { sessionId ->
                backStack.removeAll { it is Route.Workout }
                backStack.add(Route.Workout(sessionId))
            }
        }
        LaunchedEffect(workoutViewModel) {
            workoutViewModel.effects.collect { effect ->
                when (effect) {
                    is WorkoutEffect.OpenWorkout -> {
                        backStack.removeAll { it is Route.Workout }
                        backStack.add(Route.Workout(effect.sessionId))
                    }
                    WorkoutEffect.RestTick -> haptics.restCountdownTick()
                    WorkoutEffect.RestFinished -> haptics.restFinished()
                    WorkoutEffect.SetCompleted -> haptics.setCompleted()
                    is WorkoutEffect.Finished -> {
                        backStack.removeAll { it is Route.Workout }
                        backStack.add(Route.Summary(effect.sessionId))
                    }
                    WorkoutEffect.Discarded -> {
                        backStack.removeAll { it is Route.Workout }
                        if (backStack.isEmpty()) backStack.add(Route.Today)
                    }
                }
            }
        }

        val reduceMotion = ForjaTheme.motion.reduceMotion
        val screenBackground = ForjaTheme.colors.background
        // Cada pantalla pinta su fondo: en las transiciones nunca se ve la de abajo a través.
        val opaqueEntryDecorator = remember(screenBackground) {
            NavEntryDecorator<NavKey>(decorate = { entry ->
                Box(Modifier.fillMaxSize().background(screenBackground)) { entry.Content() }
            })
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForjaTheme.colors.background)
                .clearFocusOnTap()
                .nestedScroll(scrollConnection),
        ) {
            NavDisplay(
                backStack = backStack,
                // El contenido que pasa por detrás de la isla se desenfoca en el vidrio (F0-10).
                modifier = Modifier.hazeSource(hazeState),
                onBack = {
                    if (workoutState.restExpanded && isWorkoutScreenOpen) {
                        workoutViewModel.onIntent(WorkoutIntent.MinimizeRest)
                    } else if (editorBack != null) {
                        editorBack?.invoke()
                    } else if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    opaqueEntryDecorator,
                ),
                transitionSpec = { forjaPushTransition(reduceMotion) },
                popTransitionSpec = { forjaPopTransition(reduceMotion) },
                predictivePopTransitionSpec = { forjaPopTransition(reduceMotion) },
                entryProvider = entryProvider {
                    entry<Route.Workout> {
                        CompositionLocalProvider(LocalForjaHazeState provides hazeState) {
                            WorkoutRoute(workoutViewModel, onMinimize = {
                                // Un entreno libre sin ningún ejercicio no se deja abierto: salir lo descarta.
                                if (workoutState.session?.exercises?.isEmpty() == true) workoutViewModel.onIntent(WorkoutIntent.Cancel)
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
                            onOpenSettings = {
                                backStack.add(Route.Settings)
                            },
                            viewModel = todayViewModel,
                        )
                    }

                    entry<Route.Plan> {
                        com.olarte.ironprogress.ui.plan.PlanRoute(
                            onEdit = { backStack.add(Route.RoutineEditor(it)) },
                            intervals = {
                                com.olarte.ironprogress.ui.interval.IntervalsSection(
                                    onNew = { backStack.add(Route.IntervalEditor()) },
                                    onEdit = { backStack.add(Route.IntervalEditor(it)) },
                                    onRun = { backStack.add(Route.IntervalRun(it)) },
                                    onWod = { backStack.add(Route.Wod) },
                                    onDisciplines = { backStack.add(Route.Disciplines) },
                                )
                            },
                            model = planViewModel,
                        )
                    }
                    entry<Route.IntervalEditor> { route ->
                        com.olarte.ironprogress.ui.interval.IntervalEditorRoute(
                            route.id,
                            languageCode = languageCode,
                            onClose = { backStack.removeLastOrNull() },
                            onRun = { id ->
                                backStack.removeLastOrNull()
                                backStack.add(Route.IntervalRun(id))
                            },
                        )
                    }
                    entry<Route.Summary> { route ->
                        com.olarte.ironprogress.ui.summary.SummaryRoute(route.sessionId, languageCode, onDone = {
                            backStack.removeAll { it is Route.Summary }
                            if (backStack.isEmpty()) backStack.add(Route.Today)
                        })
                    }
                    entry<Route.History> {
                        com.olarte.ironprogress.ui.history.HistoryRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onOpen = { backStack.add(Route.HistorySession(it)) },
                        )
                    }
                    entry<Route.HistorySession> { route ->
                        com.olarte.ironprogress.ui.history.HistorySessionRoute(
                            route.sessionId,
                            languageCode,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Route.Disciplines> {
                        DisciplinesScreen(onBack = { backStack.removeLastOrNull() }) { target ->
                            when (target) {
                                DisciplineTarget.Workout -> {
                                    backStack.add(Route.Workout())
                                    workoutViewModel.onIntent(WorkoutIntent.StartFree)
                                }
                                DisciplineTarget.Functional -> backStack.add(Route.Wod)
                                DisciplineTarget.Intervals -> backStack.add(Route.IntervalEditor())
                                DisciplineTarget.Breathing -> backStack.add(Route.Breathing)
                                is DisciplineTarget.Flow -> backStack.add(Route.Flow(target.flowId))
                            }
                        }
                    }
                    entry<Route.Flow> { route ->
                        FlowRoute(route.id, languageCode, onClose = { backStack.removeLastOrNull() })
                    }
                    entry<Route.Breathing> {
                        BreathingRoute(onClose = { backStack.removeLastOrNull() })
                    }
                    entry<Route.Backup> {
                        com.olarte.ironprogress.ui.backup.BackupScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<Route.Settings> {
                        SettingsRoute(onBack = { backStack.removeLastOrNull() }, onOpenBackup = { backStack.add(Route.Backup) })
                    }
                    entry<Route.Wod> {
                        com.olarte.ironprogress.ui.wod.WodRoute(languageCode = languageCode, onClose = { backStack.removeLastOrNull() })
                    }
                    entry<Route.IntervalRun> { route ->
                        com.olarte.ironprogress.ui.interval.IntervalRunRoute(
                            route.id,
                            languageCode = languageCode,
                            onClose = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Route.RoutineEditor> { route ->
                        com.olarte.ironprogress.ui.plan.RoutineEditorRoute(
                            route.id,
                            onClose = { backStack.removeLastOrNull() },
                            setBack = { editorBack = it },
                        )
                    }

                    entry<Route.Progress> {
                        com.olarte.ironprogress.ui.progress.ProgressRoute(
                            listState = progressListState,
                            catalogState = catalogState,
                            languageCode = languageCode,
                            onCatalogIntent = viewModel::onIntent,
                            catalogListState = catalogListState,
                            onHistory = { backStack.add(Route.History) },
                            model = progressViewModel,
                        )
                    }
                },
            )

            val isFullScreen = currentDestination is Route.ExerciseEditor ||
                currentDestination is Route.ExerciseDetail ||
                currentDestination is Route.IntervalEditor ||
                currentDestination is Route.IntervalRun ||
                currentDestination is Route.Wod ||
                currentDestination is Route.Summary ||
                currentDestination is Route.History ||
                currentDestination is Route.HistorySession ||
                currentDestination is Route.Settings ||
                currentDestination is Route.Backup ||
                currentDestination is Route.Disciplines ||
                currentDestination is Route.Flow ||
                currentDestination is Route.Breathing ||
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
                        onSkipRest = { workoutViewModel.onIntent(WorkoutIntent.SkipRest) },
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

private const val TRANSITION_MS = 200

/**
 * Entrar a una pantalla o cambiar de pestaña: la nueva aparece encima con un fundido corto
 * mientras la de abajo se queda quieta. Sin deslizar de lado, así no se ven dos pantallas
 * corriéndose una sobre otra. Con Reducir movimiento el fundido es aún más corto.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.forjaPushTransition(reduceMotion: Boolean): ContentTransform =
    (fadeIn(tween(if (reduceMotion) TRANSITION_MS / 2 else TRANSITION_MS)) togetherWith ExitTransition.KeepUntilTransitionsFinished)
        .apply { targetContentZIndex = 1f }

/** Volver: la pantalla de arriba se apaga encima de la de abajo, que ya está en su sitio. */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.forjaPopTransition(reduceMotion: Boolean): ContentTransform =
    (EnterTransition.None togetherWith fadeOut(tween(if (reduceMotion) TRANSITION_MS / 2 else TRANSITION_MS)))
        .apply { targetContentZIndex = -1f }

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
    is Route.IntervalEditor -> ForjaTab.PLAN
    is Route.IntervalRun -> ForjaTab.PLAN
    is Route.Wod -> ForjaTab.PLAN
    is Route.Summary -> ForjaTab.TODAY
    Route.History -> ForjaTab.PROGRESS
    is Route.HistorySession -> ForjaTab.PROGRESS
    Route.Settings -> ForjaTab.TODAY
    Route.Backup -> ForjaTab.TODAY
    Route.Disciplines -> ForjaTab.PLAN
    is Route.Flow -> ForjaTab.PLAN
    Route.Breathing -> ForjaTab.PLAN
    Route.Catalog -> ForjaTab.TODAY
    is Route.Workout -> ForjaTab.TODAY
}
