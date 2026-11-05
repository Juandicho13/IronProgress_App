package com.olarte.ironprogress.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.equivalentPreviousSet
import com.olarte.ironprogress.domain.session.groupOf
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorChoice
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.components.ForjaTextField
import com.olarte.ironprogress.ui.components.forjaGlass
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_loading
import com.olarte.ironprogress.ui.resources.editor_retry
import com.olarte.ironprogress.ui.resources.hidden_value
import com.olarte.ironprogress.ui.resources.workout_add_exercise
import com.olarte.ironprogress.ui.resources.workout_add_set
import com.olarte.ironprogress.ui.resources.workout_all_done
import com.olarte.ironprogress.ui.resources.workout_blank
import com.olarte.ironprogress.ui.resources.workout_block_pending
import com.olarte.ironprogress.ui.resources.workout_circuit
import com.olarte.ironprogress.ui.resources.workout_complete
import com.olarte.ironprogress.ui.resources.workout_delete
import com.olarte.ironprogress.ui.resources.workout_done
import com.olarte.ironprogress.ui.resources.workout_empty
import com.olarte.ironprogress.ui.resources.workout_error
import com.olarte.ironprogress.ui.resources.workout_exercise_options
import com.olarte.ironprogress.ui.resources.workout_free
import com.olarte.ironprogress.ui.resources.workout_group
import com.olarte.ironprogress.ui.resources.workout_invalid
import com.olarte.ironprogress.ui.resources.workout_invalid_value
import com.olarte.ironprogress.ui.resources.workout_minimize
import com.olarte.ironprogress.ui.resources.workout_move_down
import com.olarte.ironprogress.ui.resources.workout_move_up
import com.olarte.ironprogress.ui.resources.workout_next
import com.olarte.ironprogress.ui.resources.workout_no_results
import com.olarte.ironprogress.ui.resources.workout_no_session
import com.olarte.ironprogress.ui.resources.workout_note
import com.olarte.ironprogress.ui.resources.workout_options
import com.olarte.ironprogress.ui.resources.workout_options_short
import com.olarte.ironprogress.ui.resources.workout_plates
import com.olarte.ironprogress.ui.resources.workout_previous
import com.olarte.ironprogress.ui.resources.workout_remove_set
import com.olarte.ironprogress.ui.resources.workout_saved
import com.olarte.ironprogress.ui.resources.workout_saving
import com.olarte.ironprogress.ui.resources.workout_search
import com.olarte.ironprogress.ui.resources.workout_set_label
import com.olarte.ironprogress.ui.resources.workout_skip
import com.olarte.ironprogress.ui.resources.workout_slide_finish
import com.olarte.ironprogress.ui.resources.workout_substitute
import com.olarte.ironprogress.ui.resources.workout_substitute_title
import com.olarte.ironprogress.ui.resources.workout_superset
import com.olarte.ironprogress.ui.resources.workout_type
import com.olarte.ironprogress.ui.resources.workout_undo
import com.olarte.ironprogress.ui.resources.workout_ungroup
import com.olarte.ironprogress.ui.resources.workout_use_kg
import com.olarte.ironprogress.ui.resources.workout_use_lb
import com.olarte.ironprogress.ui.rest.RestBar
import com.olarte.ironprogress.ui.rest.RestScreen
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.settings.forDisplay
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun WorkoutRoute(viewModel: WorkoutViewModel, onMinimize: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsed by produceState(0L, state.session?.session?.id) {
        while (true) withFrameNanos { value = viewModel.elapsedSeconds() }
    }
    WorkoutScreen(state, elapsed, viewModel::onIntent, onMinimize)
}

@Composable
fun WorkoutScreen(state: WorkoutState, elapsedSeconds: Long, onIntent: (WorkoutIntent) -> Unit, onMinimize: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val language = appLanguage()
    val formats = rememberFormats()
    val detail = state.session
    var optionsId by remember { mutableStateOf<WorkoutSetId?>(null) }
    var exerciseOptionsId by remember { mutableStateOf<com.olarte.ironprogress.domain.session.SessionExerciseId?>(null) }
    var platesId by remember { mutableStateOf<com.olarte.ironprogress.domain.session.SessionExerciseId?>(null) }
    val selected = detail?.exercises?.firstOrNull { it.id == state.selectedExerciseId }
    val currentSet = selected?.let { detail.setsOf(it.id).firstOrNull { set -> !set.isCompleted } }
    val block = detail?.blocks?.firstOrNull { it.id == selected?.blockId }
    val enabled = !state.loading && !state.failed

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp4), verticalAlignment = Alignment.CenterVertically) {
            ForjaIconButton(ForjaIcons.ArrowBack, stringResource(Res.string.workout_minimize), filled = false, onClick = onMinimize)
            Column(Modifier.weight(1f).padding(ForjaSpacing.esp2)) {
                Text(state.routineName ?: stringResource(Res.string.workout_free), style = typography.title2, color = colors.text1)
                Text(Formats.duration(elapsedSeconds), style = typography.smallFigure, color = colors.text2)
            }
        }
        if (detail != null && detail.exercises.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
                horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
            ) {
                detail.exercises.forEach { exercise ->
                    val sets = detail.setsOf(exercise.id)
                    LinearProgressIndicator(
                        progress = { if (sets.isEmpty()) 0f else sets.count { it.isCompleted }.toFloat() / sets.size },
                        modifier = Modifier.weight(1f).height(ForjaSpacing.esp1),
                        color = colors.ember,
                        trackColor = colors.surface3,
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(ForjaSpacing.esp4),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            if (state.loading) item { Text(stringResource(Res.string.editor_loading), color = colors.text2) }
            if (state.failed) {
                item {
                    ForjaNote(stringResource(Res.string.workout_error), error = true)
                    ForjaButton(stringResource(Res.string.editor_retry), style = ForjaButtonStyle.SECONDARY) {
                        onIntent(WorkoutIntent.Retry)
                    }
                }
            }
            if (detail == null && !state.loading) {
                item {
                    Text(stringResource(Res.string.workout_no_session), color = colors.text2, style = typography.body)
                }
            }
            if (detail?.exercises?.isEmpty() == true) {
                item {
                    ForjaEmptyState(stringResource(Res.string.workout_empty)) {
                        ForjaButton(
                            stringResource(Res.string.workout_add_exercise),
                            Modifier.fillMaxWidth(),
                            enabled = enabled && state.pendingWrites == 0,
                            icon = ForjaIcons.Add,
                        ) { onIntent(WorkoutIntent.ChooseExercise) }
                    }
                }
            }
            if (detail != null) {
                items(detail.exercises.sortedBy { it.sortOrder }, key = { it.id.value }) { exercise ->
                    val catalog = state.catalog[exercise.exerciseId]
                    val expanded = exercise.id == selected?.id
                    Column(
                        Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp3),
                        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                catalog?.name?.resolve(language) ?: exercise.exerciseId.value,
                                Modifier.weight(1f).heightIn(min = ForjaMeasures.minTouch).clip(ForjaRadius.shapeS)
                                    .clickable(role = Role.Button) { onIntent(WorkoutIntent.SelectExercise(exercise.id)) }
                                    .padding(horizontal = ForjaSpacing.esp1, vertical = ForjaSpacing.esp3),
                                style = typography.title3,
                                color = colors.text1,
                            )
                            if (expanded) {
                                ForjaIconButton(
                                    ForjaIcons.MoreVert,
                                    stringResource(Res.string.workout_options_short),
                                    enabled = enabled && state.pendingWrites == 0,
                                    filled = false,
                                    tint = colors.text2,
                                ) { exerciseOptionsId = exercise.id }
                            }
                        }
                        val group = detail.groupOf(exercise.id)
                        if (group.size > 1) {
                            Text(
                                stringResource(if (group.size == 2) Res.string.workout_superset else Res.string.workout_circuit),
                                color = colors.emberInk,
                                style = typography.label,
                            )
                        }
                        if (expanded) {
                            catalog?.muscles?.map { it.muscle.label() }?.takeIf { it.isNotEmpty() }?.let { names ->
                                Text(names.joinToString(" · "), color = colors.text2, style = typography.body2)
                            }
                            if (exercise.notes.isNotBlank()) Text(exercise.notes, color = colors.text2, style = typography.body2)
                            detail.setsOf(exercise.id).forEach { set ->
                                val previous =
                                    equivalentPreviousSet(set, detail.setsOf(exercise.id), state.previous[exercise.exerciseId].orEmpty())
                                WorkoutSetRow(set, previous, catalog, state, enabled, onIntent) { optionsId = set.id }
                            }
                            if (state.invalidSetId in detail.setsOf(exercise.id).map { it.id }) {
                                Text(stringResource(Res.string.workout_invalid), color = colors.danger, style = typography.body2)
                            }
                            ForjaButton(
                                stringResource(Res.string.workout_add_set),
                                style = ForjaButtonStyle.TEXT,
                                enabled = enabled && state.pendingWrites == 0,
                                icon = ForjaIcons.Add,
                            ) { onIntent(WorkoutIntent.AddSet(exercise.id)) }
                        } else {
                            val sets = detail.setsOf(exercise.id)
                            Text(
                                "${formats.number(
                                    sets.count {
                                        it.isCompleted
                                    }.toDouble(),
                                    0,
                                )} / ${formats.number(sets.size.toDouble(), 0)}",
                                color = colors.text2,
                                style = typography.body2,
                            )
                        }
                    }
                }
                if (detail.exercises.isNotEmpty()) {
                    item {
                        ForjaButton(
                            stringResource(Res.string.workout_add_exercise),
                            Modifier.fillMaxWidth(),
                            ForjaButtonStyle.TONAL,
                            enabled = enabled && state.pendingWrites == 0,
                            icon = ForjaIcons.Add,
                        ) { onIntent(WorkoutIntent.ChooseExercise) }
                    }
                }
                item {
                    SlideToFinish(
                        stringResource(Res.string.workout_slide_finish),
                        enabled && state.pendingWrites == 0 && detail.sets.any { it.isCompleted },
                    ) { onIntent(WorkoutIntent.Finish) }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp4),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            state.undoSetId?.let { id ->
                val number = detail?.sets?.firstOrNull { it.id == id }?.sortOrder?.plus(1) ?: 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.workout_saved, number),
                        Modifier.weight(1f),
                        color = colors.text1,
                        style = typography.body2,
                    )
                    EditorAction(stringResource(Res.string.workout_undo), enabled) { onIntent(WorkoutIntent.Undo) }
                }
            }
            if (state.pendingWrites > 0) Text(stringResource(Res.string.workout_saving), style = typography.caption, color = colors.text2)
            if (block != null && block.format != BlockFormat.SETS) {
                Text(stringResource(Res.string.workout_block_pending), color = colors.text2, style = typography.body2)
            }
            if (state.rest != null) {
                RestBar(state, onIntent)
            } else if (detail?.sets?.isNotEmpty() == true && detail.sets.all { it.isCompleted }) {
                Text(stringResource(Res.string.workout_all_done), color = colors.text1, style = typography.body)
            } else if (currentSet != null) {
                WorkoutAction((block?.format ?: BlockFormat.SETS).actionLabel(), enabled && block?.format == BlockFormat.SETS) {
                    onIntent(WorkoutIntent.Complete(currentSet.id))
                }
            }
        }
    }
    AnimatedVisibility(
        visible = state.rest != null && state.restExpanded,
        enter = slideInVertically(ForjaTheme.motion.spatial()) { it } + fadeIn(ForjaTheme.motion.effect()),
        exit = slideOutVertically(ForjaTheme.motion.spatial()) { it } + fadeOut(ForjaTheme.motion.effect()),
    ) { RestScreen(state, onIntent) }
    optionsId?.let { id ->
        detail?.sets?.firstOrNull { it.id == id }?.let { set ->
            SetOptions(set, onIntent) { optionsId = null }
        }
    }
    exerciseOptionsId?.let { id ->
        detail?.let {
            ExerciseOptions(it, id, state.unitOf(id), onIntent, onPlates = { platesId = id }) { exerciseOptionsId = null }
        }
    }
    platesId?.let { id -> PlatesDialog(state, id, onIntent) { platesId = null } }
    if (state.choosingExercise) ExercisePicker(state, onIntent)
    if (state.keyboard != null) WorkoutKeyboardSheet(state, onIntent)
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    previous: WorkoutSet?,
    exercise: Exercise?,
    state: WorkoutState,
    enabled: Boolean,
    onIntent: (WorkoutIntent) -> Unit,
    onOptions: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val background by animateColorAsState(
        if (state.highlightedSetId == set.id) colors.emberVeil else colors.surface2,
        animationSpec = ForjaTheme.motion.effectFast(),
    )
    val optionsDescription = stringResource(Res.string.workout_options, set.sortOrder + 1)
    val fields = WorkoutField.entries.filter { it.measure != null && it.measure in exercise?.trackingType?.fields.orEmpty() }
    val unit = exercise?.preferredUnit ?: WeightUnit.KG
    Column(Modifier.fillMaxWidth().clip(ForjaRadius.shapeM).background(background).padding(ForjaSpacing.esp2)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            TextButton(
                onClick = onOptions,
                enabled = enabled,
                modifier = Modifier.width(ForjaMeasures.minTouch).semantics {
                    contentDescription =
                        optionsDescription
                },
            ) {
                Text(
                    formats.number((set.sortOrder + 1).toDouble(), 0),
                    style = typography.label,
                    color = if (set.setType ==
                        SetType.WARMUP
                    ) {
                        colors.ice
                    } else {
                        colors.text2
                    },
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.workout_previous), color = colors.text3, style = typography.caption)
                TextButton(onClick = { onIntent(WorkoutIntent.CopyPrevious(set.id)) }, enabled = enabled && previous != null) {
                    val summary = previous?.forDisplay(LocalAppSettings.current.hideBodyMetrics)?.let { item ->
                        fields.mapNotNull { f -> item.value(f)?.let { formats.shown(f, it, unit) } }.joinToString(" × ")
                    }
                    Text(
                        summary?.takeIf {
                            it.isNotEmpty()
                        } ?: stringResource(Res.string.workout_blank),
                        style = typography.body2,
                        color = colors.text2,
                    )
                }
            }
            TextButton(onClick = { onIntent(WorkoutIntent.Complete(set.id)) }, enabled = enabled && !set.isCompleted) {
                Icon(
                    ForjaIcons.Check,
                    stringResource(if (set.isCompleted) Res.string.workout_done else Res.string.workout_complete),
                    tint = if (set.isCompleted) colors.ember else colors.text2,
                    modifier = Modifier.size(ForjaMeasures.iconSize),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            fields.forEach { field ->
                val description = stringResource(Res.string.workout_set_label, set.sortOrder + 1, field.label())
                Column(
                    Modifier.weight(1f).clip(ForjaRadius.shapeS).background(colors.surface3)
                        .then(
                            if (state.keyboard?.setId == set.id &&
                                state.keyboard.field == field
                            ) {
                                Modifier.border(ForjaSpacing.esp0_5, colors.ember, ForjaRadius.shapeS)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = enabled, role = Role.Button) { onIntent(WorkoutIntent.OpenKeyboard(set.id, field)) }
                        .semantics { contentDescription = description }
                        .heightIn(min = ForjaMeasures.minTouch).padding(ForjaSpacing.esp2),
                ) {
                    Text(field.labelIn(unit), style = typography.caption, color = colors.text2)
                    val hidden = field == WorkoutField.CALORIES && LocalAppSettings.current.hideBodyMetrics && set.calories != null
                    Text(
                        if (hidden) {
                            stringResource(Res.string.hidden_value)
                        } else {
                            set.value(field)?.let { formats.shown(field, it, unit) } ?: stringResource(Res.string.workout_blank)
                        },
                        style = typography.figure,
                        color = colors.text1,
                    )
                }
            }
        }
        Text(set.setType.workoutLabel(), color = colors.text2, style = typography.caption)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf(WorkoutField.RIR, WorkoutField.RPE).forEach { field ->
                TextButton(enabled = enabled, onClick = { onIntent(WorkoutIntent.OpenKeyboard(set.id, field)) }) {
                    Text(
                        field.label() + ": " + (
                            set.value(field)?.let {
                                formats.number(it, 1)
                            } ?: stringResource(Res.string.workout_blank)
                            ),
                        color = colors.text2,
                        style = typography.label,
                    )
                }
            }
        }
        if (set.notes.isNotBlank()) Text(set.notes, color = colors.text2, style = typography.body2)
    }
}

@Composable
private fun SetOptions(set: WorkoutSet, onIntent: (WorkoutIntent) -> Unit, onClose: () -> Unit) {
    var note by remember(set.id) { mutableStateOf(set.notes) }
    ForjaSheet(stringResource(Res.string.workout_options, set.sortOrder + 1), onDismiss = onClose) {
        Column(
            Modifier.padding(horizontal = ForjaSpacing.esp5).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            EditorChoice(stringResource(Res.string.workout_type), set.setType, SetType.entries, { it.workoutLabel() }) {
                onIntent(WorkoutIntent.ChangeType(set.id, it))
            }
            ForjaTextField(stringResource(Res.string.workout_note), note, {
                note = it
                onIntent(WorkoutIntent.ChangeNote(set.id, it))
            })
            ForjaButton(stringResource(Res.string.workout_remove_set), Modifier.fillMaxWidth(), ForjaButtonStyle.SECONDARY) {
                onIntent(WorkoutIntent.RemoveSet(set.id))
                onClose()
            }
        }
    }
}

/** Opciones de un ejercicio de la sesión: orden, saltar, cambiar y agrupar (F1-12). */
@Composable
private fun ExerciseOptions(
    detail: com.olarte.ironprogress.domain.session.SessionDetail,
    id: com.olarte.ironprogress.domain.session.SessionExerciseId,
    unit: WeightUnit,
    onIntent: (WorkoutIntent) -> Unit,
    onPlates: () -> Unit,
    onClose: () -> Unit,
) {
    val ordered = detail.exercises.sortedBy { it.sortOrder }
    val index = ordered.indexOfFirst { it.id == id }
    val inGroup = ordered.getOrNull(index)?.supersetGroup != null
    fun act(intent: WorkoutIntent) {
        onIntent(intent)
        onClose()
    }
    val rows = buildList<Pair<String, () -> Unit>> {
        if (index > 0) add(stringResource(Res.string.workout_move_up) to { act(WorkoutIntent.MoveExercise(id, -1)) })
        if (index in 0 until ordered.lastIndex) {
            add(stringResource(Res.string.workout_move_down) to { act(WorkoutIntent.MoveExercise(id, 1)) })
            add(stringResource(Res.string.workout_skip) to { act(WorkoutIntent.SkipExercise(id)) })
        }
        add(stringResource(Res.string.workout_substitute) to { act(WorkoutIntent.Substitute(id)) })
        add(
            stringResource(if (unit == WeightUnit.LB) Res.string.workout_use_kg else Res.string.workout_use_lb) to
                { act(WorkoutIntent.ToggleUnit(id)) },
        )
        add(
            stringResource(Res.string.workout_plates) to {
                onPlates()
                onClose()
            },
        )
        if (inGroup) {
            add(stringResource(Res.string.workout_ungroup) to { act(WorkoutIntent.ToggleGroup(id)) })
        } else if (index in 0 until ordered.lastIndex) {
            add(stringResource(Res.string.workout_group) to { act(WorkoutIntent.ToggleGroup(id)) })
        }
    }
    ForjaSheet(stringResource(Res.string.workout_exercise_options), onDismiss = onClose) {
        rows.forEachIndexed { position, (label, action) ->
            if (position > 0) ForjaDivider()
            ForjaListRow(label, chevron = false, onClick = action)
        }
    }
}

@Composable
private fun ExercisePicker(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val language = appLanguage()
    val matches = state.catalog.values.filter { !it.isArchived && it.name.resolve(language).contains(state.query, ignoreCase = true) }
        .sortedBy { it.name.resolve(language) }
    ForjaSheet(
        stringResource(if (state.substitutingId != null) Res.string.workout_substitute_title else Res.string.workout_add_exercise),
        onDismiss = { onIntent(WorkoutIntent.ClosePicker) },
    ) {
        Box(Modifier.padding(horizontal = ForjaSpacing.esp5).padding(bottom = ForjaSpacing.esp2)) {
            ForjaTextField(stringResource(Res.string.workout_search), state.query, { onIntent(WorkoutIntent.Search(it)) })
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            if (matches.isEmpty()) {
                item { ForjaNote(stringResource(Res.string.workout_no_results), modifier = Modifier.padding(ForjaSpacing.esp5)) }
            }
            items(matches, key = { it.id.value }) { exercise ->
                ForjaListRow(
                    exercise.name.resolve(language),
                    subtitle = exercise.primaryMuscles.map { it.label() }.joinToString(" · ").ifBlank { null },
                    chevron = false,
                    onClick = { onIntent(WorkoutIntent.AddExercise(exercise.id)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutKeyboardSheet(state: WorkoutState, onIntent: (WorkoutIntent) -> Unit) {
    val keyboard = state.keyboard ?: return
    val detail = state.session ?: return
    val set = detail.sets.firstOrNull { it.id == keyboard.setId } ?: return
    val exercise = detail.exercises.first { it.id == set.sessionExerciseId }
    val fields = WorkoutField.entries.filter {
        it.measure == null ||
            it.measure in state.catalog[exercise.exerciseId]?.trackingType?.fields.orEmpty()
    }
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val haptics = ForjaTheme.haptics
    val formats = rememberFormats()
    val separator = if (formats.number(1.5).contains(',')) "," else "."
    val previous = equivalentPreviousSet(set, detail.setsOf(exercise.id), state.previous[exercise.exerciseId].orEmpty())
    val unit = state.unitOf(exercise.id)
    ModalBottomSheet(
        onDismissRequest = { onIntent(WorkoutIntent.CloseKeyboard) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        shape = ForjaRadius.shapeXlTop,
        containerColor = colors.glass,
    ) {
        Column(
            Modifier.fillMaxWidth().forjaGlass().verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp4),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                fields.forEach { field ->
                    TextButton(enabled = state.pendingWrites == 0 && !state.failed, onClick = {
                        haptics.segmentTick()
                        onIntent(WorkoutIntent.OpenKeyboard(set.id, field))
                    }) {
                        val color = if (field == keyboard.field) colors.ember else colors.text2
                        Text(field.labelIn(unit), color = color, style = typography.label)
                    }
                }
            }
            Text(
                keyboard.text.replace(".", separator).ifEmpty {
                    stringResource(Res.string.workout_blank)
                },
                style = typography.largeFigure,
                color = colors.text1,
            )
            previous?.forDisplay(LocalAppSettings.current.hideBodyMetrics)?.value(keyboard.field)?.let {
                Text(
                    stringResource(Res.string.workout_previous) + ": " + formats.shown(keyboard.field, it, unit),
                    color = colors.text2,
                    style = typography.body2,
                )
            }
            if (keyboard.invalid) Text(stringResource(Res.string.workout_invalid_value), color = colors.danger, style = typography.body2)
            if (state.failed) {
                Text(stringResource(Res.string.workout_error), color = colors.danger, style = typography.body2)
                EditorAction(stringResource(Res.string.editor_retry)) { onIntent(WorkoutIntent.Retry) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                if (keyboard.field == WorkoutField.RIR) {
                    (0..4).forEach { value ->
                        EditorAction(formats.number(value.toDouble(), 0)) {
                            haptics.keyboardTap()
                            onIntent(WorkoutIntent.SetValue(value.toDouble()))
                        }
                    }
                } else {
                    val step = when (keyboard.field) {
                        WorkoutField.WEIGHT -> if (unit == WeightUnit.LB) 5.0 else 2.5
                        WorkoutField.RPE -> 0.5
                        else -> 1.0
                    }
                    EditorAction("-" + formats.number(step)) {
                        haptics.keyboardTap()
                        onIntent(WorkoutIntent.Adjust(-step))
                    }
                    EditorAction("+" + formats.number(step)) {
                        haptics.keyboardTap()
                        onIntent(WorkoutIntent.Adjust(step))
                    }
                }
            }
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "backspace")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                    row.forEach { key ->
                        val label = when (key) {
                            "." -> separator
                            "backspace" -> stringResource(Res.string.workout_delete)
                            else -> key
                        }
                        Box(
                            Modifier.weight(1f).heightIn(min = ForjaMeasures.keyHeight).clip(ForjaRadius.shapeM).background(colors.surface3)
                                .clickable(role = Role.Button) {
                                    haptics.keyboardTap()
                                    onIntent(WorkoutIntent.Key(key))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = if (key == "backspace") typography.label else typography.figure, color = colors.text1)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                EditorAction(stringResource(Res.string.workout_next), !keyboard.invalid && !state.failed && state.pendingWrites == 0) {
                    onIntent(WorkoutIntent.NextField)
                }
                EditorAction(
                    stringResource(Res.string.workout_complete),
                    !keyboard.invalid && !state.failed && !set.isCompleted,
                    style = ForjaButtonStyle.PRIMARY,
                ) {
                    onIntent(WorkoutIntent.Complete(set.id))
                }
            }
        }
    }
}

@Composable
private fun WorkoutAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        Modifier.fillMaxWidth().heightIn(min = ForjaMeasures.primaryButtonHeight).clip(ForjaRadius.shapeFull)
            .background(if (enabled) colors.ember else colors.surface3).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(ForjaSpacing.esp3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.primaryButton, color = if (enabled) colors.onEmber else colors.text3)
    }
}

/** Valor de un campo para mostrar: el peso en la unidad del ejercicio (lb con un decimal). */
private fun com.olarte.ironprogress.ui.format.Formats.shown(field: WorkoutField, value: Double, unit: WeightUnit): String =
    if (field == WorkoutField.WEIGHT && unit == WeightUnit.LB) {
        number(WeightUnits.fromKg(value, unit), 1)
    } else {
        number(value, 2)
    }

/** Etiqueta del campo; el peso lleva su unidad, "Peso (lb)" o "Peso (kg)". */
@Composable
private fun WorkoutField.labelIn(unit: WeightUnit): String =
    if (this == WorkoutField.WEIGHT) label() + " (" + unit.symbol() + ")" else label()
