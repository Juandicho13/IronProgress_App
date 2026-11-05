package com.olarte.ironprogress.ui.today

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.WeekProgress
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.catalog_exercise_count
import com.olarte.ironprogress.ui.resources.editor_error
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.nav_today
import com.olarte.ironprogress.ui.resources.plan_create
import com.olarte.ironprogress.ui.resources.settings_open
import com.olarte.ironprogress.ui.resources.today_continue
import com.olarte.ironprogress.ui.resources.today_day_friday_name
import com.olarte.ironprogress.ui.resources.today_day_friday_short
import com.olarte.ironprogress.ui.resources.today_day_monday_name
import com.olarte.ironprogress.ui.resources.today_day_monday_short
import com.olarte.ironprogress.ui.resources.today_day_saturday_name
import com.olarte.ironprogress.ui.resources.today_day_saturday_short
import com.olarte.ironprogress.ui.resources.today_day_status_done
import com.olarte.ironprogress.ui.resources.today_day_status_rest
import com.olarte.ironprogress.ui.resources.today_day_status_today
import com.olarte.ironprogress.ui.resources.today_day_sunday_name
import com.olarte.ironprogress.ui.resources.today_day_sunday_short
import com.olarte.ironprogress.ui.resources.today_day_thursday_name
import com.olarte.ironprogress.ui.resources.today_day_thursday_short
import com.olarte.ironprogress.ui.resources.today_day_tuesday_name
import com.olarte.ironprogress.ui.resources.today_day_tuesday_short
import com.olarte.ironprogress.ui.resources.today_day_wednesday_name
import com.olarte.ironprogress.ui.resources.today_day_wednesday_short
import com.olarte.ironprogress.ui.resources.today_empty_body
import com.olarte.ironprogress.ui.resources.today_empty_title
import com.olarte.ironprogress.ui.resources.today_minutes_short
import com.olarte.ironprogress.ui.resources.today_next_in_progress
import com.olarte.ironprogress.ui.resources.today_next_label
import com.olarte.ironprogress.ui.resources.today_next_rotation
import com.olarte.ironprogress.ui.resources.today_set_count
import com.olarte.ironprogress.ui.resources.today_start
import com.olarte.ironprogress.ui.resources.today_week_count
import com.olarte.ironprogress.ui.resources.today_week_streak
import com.olarte.ironprogress.ui.resources.today_week_title
import com.olarte.ironprogress.ui.resources.today_workout_count
import com.olarte.ironprogress.ui.resources.workout_start_free
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Punto de entrada de la pantalla Hoy conectado a Koin y Navigation 3. */
@Composable
fun TodayRoute(
    listState: LazyListState = rememberLazyListState(),
    onOpenWorkout: (String) -> Unit,
    onCreateRoutine: () -> Unit,
    onStartFree: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val viewModel = koinViewModel<TodayViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TodayEffect.OpenWorkout -> onOpenWorkout(effect.sessionId)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(TodayIntent.Refresh) }

    TodayScreen(
        state = state,
        listState = listState,
        onIntent = viewModel::onIntent,
        onCreateRoutine = onCreateRoutine,
        onStartFree = onStartFree,
        onOpenSettings = onOpenSettings,
    )
}

/** Pantalla Hoy real (docs/PANTALLAS.md 1.1), sin conocer Koin ni ViewModels. */
@Composable
fun TodayScreen(
    state: TodayState,
    listState: LazyListState = rememberLazyListState(),
    onIntent: (TodayIntent) -> Unit,
    onCreateRoutine: () -> Unit,
    onStartFree: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val languageCode = Locale.current.language

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = ForjaSpacing.esp5),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        contentPadding = PaddingValues(
            top = ForjaSpacing.esp3,
            bottom = ForjaMeasures.islandHeight + ForjaMeasures.accessoryHeight + ForjaSpacing.esp7,
        ),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                TodayTitle(state, formats, Modifier.weight(1f))
                // Botón redondo de 48 en surface3 a la derecha del título (PANTALLAS 0): abre Ajustes.
                val settingsLabel = stringResource(Res.string.settings_open)
                Box(
                    Modifier.size(ForjaMeasures.minTouch).clip(CircleShape).background(colors.surface3)
                        .semantics { contentDescription = settingsLabel }
                        .clickable(role = Role.Button, onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        ForjaMoreIcons.AccountCircle,
                        contentDescription = null,
                        tint = colors.text1,
                        modifier = Modifier.size(ForjaMeasures.iconSize),
                    )
                }
            }
        }
        if (state.loading) {
            item {
                Text(
                    text = stringResource(Res.string.editor_loading),
                    style = typography.body,
                    color = colors.text2,
                )
            }
        }

        if (state.failed) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ForjaRadius.shapeL)
                        .background(colors.surface1)
                        .padding(ForjaSpacing.esp4),
                    verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                ) {
                    Text(
                        text = stringResource(Res.string.editor_error),
                        style = typography.body,
                        color = colors.danger,
                    )
                    EditorAction(
                        text = stringResource(Res.string.editor_retry),
                        enabled = !state.starting,
                    ) {
                        onIntent(TodayIntent.Retry)
                    }
                }
            }
        }

        if (!state.loading && state.weekProgress.days.isNotEmpty()) {
            item {
                TodayWeekCard(weekProgress = state.weekProgress)
            }
        }

        if (!state.loading) {
            if (state.activeSessionId == null) {
                item { EditorAction(stringResource(Res.string.workout_start_free), !state.starting, onStartFree) }
            }
            if (state.activeSessionId != null && state.nextRoutine?.isActiveSession != true) {
                item {
                    TodayPrimaryButton(stringResource(Res.string.today_continue), !state.starting, true) {
                        onIntent(TodayIntent.StartWorkout)
                    }
                }
            }
            item {
                val next = state.nextRoutine
                if (next != null) {
                    TodayNextRoutineCard(
                        card = next,
                        languageCode = languageCode,
                        starting = state.starting || (state.activeSessionId != null && !next.isActiveSession),
                        onStartWorkout = { onIntent(TodayIntent.StartWorkout) },
                    )
                } else {
                    TodayEmptyRoutineCard(onCreateRoutine = onCreateRoutine)
                }
            }
        }
    }
}

@Composable
private fun TodayWeekCard(weekProgress: WeekProgress) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    val workoutSummary = pluralStringResource(
        Res.plurals.today_workout_count,
        weekProgress.trainedDaysCount,
        weekProgress.trainedDaysCount,
    )
    val subtitle = if (weekProgress.streakWeeks > 0) {
        val streakText = pluralStringResource(
            Res.plurals.today_week_count,
            weekProgress.streakWeeks,
            weekProgress.streakWeeks,
        )
        stringResource(Res.string.today_week_streak, workoutSummary, streakText)
    } else {
        workoutSummary
    }

    val dayLabels = listOf(
        stringResource(Res.string.today_day_monday_short) to stringResource(Res.string.today_day_monday_name),
        stringResource(Res.string.today_day_tuesday_short) to stringResource(Res.string.today_day_tuesday_name),
        stringResource(Res.string.today_day_wednesday_short) to stringResource(Res.string.today_day_wednesday_name),
        stringResource(Res.string.today_day_thursday_short) to stringResource(Res.string.today_day_thursday_name),
        stringResource(Res.string.today_day_friday_short) to stringResource(Res.string.today_day_friday_name),
        stringResource(Res.string.today_day_saturday_short) to stringResource(Res.string.today_day_saturday_name),
        stringResource(Res.string.today_day_sunday_short) to stringResource(Res.string.today_day_sunday_name),
    )

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
                text = stringResource(Res.string.today_week_title),
                style = typography.title3,
                color = colors.text1,
                modifier = Modifier.weight(1f).padding(end = ForjaSpacing.esp2),
            )
            Text(
                text = subtitle,
                style = typography.body2,
                color = colors.text2,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weekProgress.days.forEachIndexed { index, day ->
                val (shortLabel, fullName) = dayLabels.getOrElse(index) { "" to "" }
                val a11yStatus = when {
                    day.completed -> stringResource(Res.string.today_day_status_done, fullName)
                    day.isToday -> stringResource(Res.string.today_day_status_today, fullName)
                    else -> stringResource(Res.string.today_day_status_rest, fullName)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = a11yStatus }
                        .clip(CircleShape)
                        .background(
                            when {
                                day.completed -> colors.ember
                                day.isToday -> colors.surface3
                                else -> colors.surface1
                            },
                        )
                        .then(
                            if (day.isToday && !day.completed) {
                                Modifier.border(2.dp, colors.ember, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (day.completed) {
                        Icon(
                            painter = ForjaIcons.Check,
                            contentDescription = null,
                            tint = colors.onEmber,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = shortLabel,
                            style = typography.label,
                            color = if (day.isToday) colors.text1 else colors.text3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayNextRoutineCard(card: TodayRoutineCard, languageCode: String, starting: Boolean, onStartWorkout: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    val headerCaption = when {
        card.isActiveSession -> stringResource(Res.string.today_next_in_progress)
        card.rotationSize > 1 -> stringResource(Res.string.today_next_rotation, card.positionInRotation, card.rotationSize)
        else -> stringResource(Res.string.today_next_label)
    }

    val exerciseCountText = pluralStringResource(
        Res.plurals.catalog_exercise_count,
        card.exercises.size,
        card.exercises.size,
    )
    val durationText = if (card.estimatedMinutes > 0) {
        stringResource(Res.string.today_minutes_short, card.estimatedMinutes)
    } else {
        null
    }
    val metaLine = listOfNotNull(
        exerciseCountText,
        durationText,
        card.folderName?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

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
                text = headerCaption.uppercase(),
                style = typography.caption,
                color = colors.text2,
            )
            Text(
                text = card.detail.routine.name,
                style = typography.title2,
                color = colors.text1,
            )
            Text(
                text = metaLine,
                style = typography.body2,
                color = colors.text2,
            )
        }

        if (card.exercises.isNotEmpty()) {
            Column {
                card.exercises.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 1.dp, color = colors.line)
                    }
                    val resolvedName = row.name?.resolve(languageCode) ?: row.fallbackName
                    val targetText = formatExerciseTarget(row)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = resolvedName,
                            style = typography.body,
                            color = colors.text1,
                            modifier = Modifier.weight(1f).padding(end = ForjaSpacing.esp2),
                        )
                        Text(
                            text = targetText,
                            style = typography.smallFigure,
                            color = colors.text2,
                        )
                    }
                }
            }
        }

        TodayPrimaryButton(
            text = stringResource(if (card.isActiveSession) Res.string.today_continue else Res.string.today_start),
            enabled = !starting && (card.isActiveSession || card.exercises.any { it.workingSetsCount > 0 }),
            showPlayIcon = true,
            onClick = onStartWorkout,
        )
    }
}

@Composable
private fun formatExerciseTarget(row: TodayExerciseRow): String {
    val setsCount = row.workingSetsCount
    val min = row.repsMin
    val max = row.repsMax
    return when {
        min != null && max != null && min == max -> "$setsCount × $min"
        min != null && max != null -> "$setsCount × $min-$max"
        min != null -> "$setsCount × $min"
        max != null -> "$setsCount × $max"
        else -> pluralStringResource(Res.plurals.today_set_count, setsCount, setsCount)
    }
}

@Composable
private fun TodayEmptyRoutineCard(onCreateRoutine: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

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
                text = stringResource(Res.string.today_next_label).uppercase(),
                style = typography.caption,
                color = colors.text2,
            )
            Text(
                text = stringResource(Res.string.today_empty_title),
                style = typography.title2,
                color = colors.text1,
            )
            Text(
                text = stringResource(Res.string.today_empty_body),
                style = typography.body2,
                color = colors.text2,
            )
        }

        TodayPrimaryButton(
            text = stringResource(Res.string.plan_create),
            enabled = true,
            showPlayIcon = false,
            onClick = onCreateRoutine,
        )
    }
}

@Composable
private fun TodayPrimaryButton(text: String, enabled: Boolean, showPlayIcon: Boolean, onClick: () -> Unit) {
    val motion = ForjaTheme.motion
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography

    val buttonInteraction = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed && enabled) 0.97f else 1f,
        animationSpec = motion.spatialFast(),
        label = "TodayPrimaryButtonScale",
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
            .background(
                if (!enabled) {
                    colors.surface3
                } else if (isButtonPressed) {
                    colors.emberPressed
                } else {
                    colors.ember
                },
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = buttonInteraction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (showPlayIcon) {
                Icon(
                    painter = ForjaIcons.PlayFilled,
                    contentDescription = null,
                    tint = if (enabled) colors.onEmber else colors.text3,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(ForjaSpacing.esp2))
            }
            Text(
                text = text,
                style = typography.primaryButton,
                color = if (enabled) colors.onEmber else colors.text3,
            )
        }
    }
}

@Composable
private fun TodayTitle(state: TodayState, formats: Formats, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
        if (state.todayEpochMs > 0L) {
            Text(
                text = formats.date(state.todayEpochMs).uppercase(),
                style = ForjaTheme.typography.caption,
                color = ForjaTheme.colors.text2,
            )
        }
        Text(
            text = stringResource(Res.string.nav_today),
            style = ForjaTheme.typography.title1,
            color = ForjaTheme.colors.text1,
        )
    }
}
