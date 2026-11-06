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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.equivalentPreviousSet
import com.olarte.ironprogress.domain.session.groupOf
import com.olarte.ironprogress.ui.a11y.timerPhase
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorChoice
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDialog
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNote
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaSectionLabel
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
import com.olarte.ironprogress.ui.resources.workout_block_pending
import com.olarte.ironprogress.ui.resources.workout_cancel
import com.olarte.ironprogress.ui.resources.workout_cancel_body
import com.olarte.ironprogress.ui.resources.workout_cancel_confirm
import com.olarte.ironprogress.ui.resources.workout_cancel_keep
import com.olarte.ironprogress.ui.resources.workout_cancel_title
import com.olarte.ironprogress.ui.resources.workout_complete
import com.olarte.ironprogress.ui.resources.workout_current
import com.olarte.ironprogress.ui.resources.workout_delete
import com.olarte.ironprogress.ui.resources.workout_done
import com.olarte.ironprogress.ui.resources.workout_done_editing
import com.olarte.ironprogress.ui.resources.workout_edit_set
import com.olarte.ironprogress.ui.resources.workout_effort_example
import com.olarte.ironprogress.ui.resources.workout_effort_info
import com.olarte.ironprogress.ui.resources.workout_effort_ok
import com.olarte.ironprogress.ui.resources.workout_effort_title
import com.olarte.ironprogress.ui.resources.workout_empty
import com.olarte.ironprogress.ui.resources.workout_error
import com.olarte.ironprogress.ui.resources.workout_exercise_of
import com.olarte.ironprogress.ui.resources.workout_exercise_options
import com.olarte.ironprogress.ui.resources.workout_exercises
import com.olarte.ironprogress.ui.resources.workout_free
import com.olarte.ironprogress.ui.resources.workout_group
import com.olarte.ironprogress.ui.resources.workout_invalid
import com.olarte.ironprogress.ui.resources.workout_invalid_value
import com.olarte.ironprogress.ui.resources.workout_menu
import com.olarte.ironprogress.ui.resources.workout_minimize
import com.olarte.ironprogress.ui.resources.workout_mode_title
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
import com.olarte.ironprogress.ui.resources.workout_previous_value
import com.olarte.ironprogress.ui.resources.workout_remove_set
import com.olarte.ironprogress.ui.resources.workout_rir_explain
import com.olarte.ironprogress.ui.resources.workout_rpe_explain
import com.olarte.ironprogress.ui.resources.workout_saved
import com.olarte.ironprogress.ui.resources.workout_search
import com.olarte.ironprogress.ui.resources.workout_set_label
import com.olarte.ironprogress.ui.resources.workout_set_of
import com.olarte.ironprogress.ui.resources.workout_sets_left
import com.olarte.ironprogress.ui.resources.workout_sets_progress
import com.olarte.ironprogress.ui.resources.workout_skip
import com.olarte.ironprogress.ui.resources.workout_slide_finish
import com.olarte.ironprogress.ui.resources.workout_substitute
import com.olarte.ironprogress.ui.resources.workout_substitute_title
import com.olarte.ironprogress.ui.resources.workout_timed_start
import com.olarte.ironprogress.ui.resources.workout_timed_stop
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
import org.jetbrains.compose.resources.pluralStringResource
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
    val detail = state.session
    var optionsId by remember { mutableStateOf<WorkoutSetId?>(null) }
    var exerciseOptionsId by remember { mutableStateOf<SessionExerciseId?>(null) }
    var modeId by remember { mutableStateOf<SessionExerciseId?>(null) }
    var platesId by remember { mutableStateOf<SessionExerciseId?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var effortInfo by remember { mutableStateOf(false) }
    val selected = detail?.exercises?.firstOrNull { it.id == state.selectedExerciseId }
    val currentSet = selected?.let { detail.setsOf(it.id).firstOrNull { set -> !set.isCompleted } }
    val block = detail?.blocks?.firstOrNull { it.id == selected?.blockId }
    val enabled = !state.loading && !state.failed
    val timed = state.modeOf(selected?.id) == ExerciseMode.TIMED

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp3), verticalAlignment = Alignment.CenterVertically) {
            ForjaIconButton(ForjaIcons.ArrowBack, stringResource(Res.string.workout_minimize), filled = false, onClick = onMinimize)
            Column(Modifier.weight(1f).padding(horizontal = ForjaSpacing.esp2), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.routineName ?: stringResource(Res.string.workout_free),
                    style = typography.title3,
                    color = colors.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                ForjaNumber(Formats.duration(elapsedSeconds), typography.smallFigure, color = colors.text2, textAlign = TextAlign.Center)
            }
            ForjaIconButton(
                ForjaIcons.MoreVert,
                stringResource(Res.string.workout_menu),
                enabled = detail != null && enabled,
                filled = false,
                onClick = { menuOpen = true },
            )
        }
        val ordered = detail?.exercises?.sortedBy { it.sortOrder }.orEmpty()
        if (detail != null && ordered.isNotEmpty()) {
            ExerciseStepper(detail, ordered, selected?.id, enabled) { onIntent(WorkoutIntent.SelectExercise(it)) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
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
            if (detail != null && selected != null) {
                item(key = "current-${selected.id.value}") {
                    CurrentExercise(
                        state = state,
                        exerciseId = selected.id,
                        enabled = enabled,
                        onIntent = onIntent,
                        onExerciseOptions = { exerciseOptionsId = selected.id },
                        onMode = { modeId = selected.id },
                        onSetOptions = { optionsId = it },
                        onEffortInfo = { effortInfo = true },
                    )
                }
            }
            if (detail != null) {
                val ordered = detail.exercises.sortedBy { it.sortOrder }
                if (ordered.size > 1) {
                    item(key = "order") {
                        Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                            ForjaSectionLabel(stringResource(Res.string.workout_exercises))
                            ForjaListGroup {
                                ordered.forEachIndexed { index, exercise ->
                                    if (index > 0) ForjaDivider()
                                    val sets = detail.setsOf(exercise.id)
                                    ExerciseOrderRow(
                                        number = index + 1,
                                        name = state.catalog[exercise.exerciseId]?.name?.resolve(language) ?: exercise.exerciseId.value,
                                        progress = stringResource(
                                            Res.string.workout_sets_progress,
                                            sets.count {
                                                it.isCompleted
                                            },
                                            sets.size,
                                        ),
                                        done = sets.isNotEmpty() && sets.all { it.isCompleted },
                                        current = exercise.id == selected?.id,
                                        enabled = enabled,
                                    ) { onIntent(WorkoutIntent.SelectExercise(exercise.id)) }
                                }
                            }
                        }
                    }
                }
                if (detail.exercises.isNotEmpty()) {
                    item(key = "add") {
                        ForjaButton(
                            stringResource(Res.string.workout_add_exercise),
                            Modifier.fillMaxWidth(),
                            ForjaButtonStyle.TONAL,
                            enabled = enabled && state.pendingWrites == 0,
                            icon = ForjaIcons.Add,
                        ) { onIntent(WorkoutIntent.ChooseExercise) }
                    }
                }
                // Terminar solo aparece cuando ya hay algo que guardar; antes se cancela desde el menú.
                if (detail.sets.any { it.isCompleted }) {
                    item(key = "finish") {
                        SlideToFinish(
                            stringResource(Res.string.workout_slide_finish),
                            enabled && state.pendingWrites == 0,
                        ) { onIntent(WorkoutIntent.Finish) }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp2),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            state.undoSetId?.let { id ->
                val number =
                    detail?.sets?.firstOrNull { it.id == id }?.let { set -> detail.setsOf(set.sessionExerciseId).indexOf(set) + 1 } ?: 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.workout_saved, number),
                        Modifier.weight(1f),
                        color = colors.text1,
                        style = typography.body2,
                    )
                    ForjaButton(stringResource(Res.string.workout_undo), style = ForjaButtonStyle.TEXT, enabled = enabled) {
                        onIntent(WorkoutIntent.Undo)
                    }
                }
            }
            if (block != null && block.format != BlockFormat.SETS) {
                Text(stringResource(Res.string.workout_block_pending), color = colors.text2, style = typography.body2)
            }
            val work = state.work
            when {
                state.rest != null -> RestBar(state, onIntent)
                detail?.sets?.isNotEmpty() == true && detail.sets.all { it.isCompleted } ->
                    Text(
                        stringResource(Res.string.workout_all_done),
                        Modifier.fillMaxWidth(),
                        color = colors.text1,
                        style = typography.body,
                        textAlign = TextAlign.Center,
                    )
                work != null && currentSet?.id == work.setId -> Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                    ForjaButton(stringResource(Res.string.workout_timed_stop), Modifier.weight(1f), ForjaButtonStyle.SECONDARY) {
                        onIntent(WorkoutIntent.StopWork)
                    }
                    WorkoutAction(stringResource(Res.string.workout_complete), enabled, Modifier.weight(2f)) {
                        onIntent(WorkoutIntent.Complete(work.setId))
                    }
                }
                currentSet != null && timed -> {
                    val seconds = currentSet.durationSeconds ?: selected.timedSeconds ?: ExerciseMode.DEFAULT_TIMED_SECONDS
                    WorkoutAction(stringResource(Res.string.workout_timed_start, Formats.duration(seconds.toLong())), enabled) {
                        onIntent(WorkoutIntent.StartWork(currentSet.id))
                    }
                }
                currentSet != null ->
                    WorkoutAction(
                        (block?.format ?: BlockFormat.SETS).actionLabel(),
                        enabled && block?.format != null && block.format == BlockFormat.SETS,
                    ) {
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
            SetOptions(set, detail.setsOf(set.sessionExerciseId).indexOf(set) + 1, onIntent) { optionsId = null }
        }
    }
    exerciseOptionsId?.let { id ->
        detail?.let {
            ExerciseOptions(
                it,
                id,
                state.unitOf(id),
                onIntent,
                onPlates = { platesId = id },
                onMode = { modeId = id },
            ) { exerciseOptionsId = null }
        }
    }
    modeId?.let { id ->
        detail?.exercises?.firstOrNull { it.id == id }?.let { exercise ->
            val last = detail.exercises.maxByOrNull { it.sortOrder }?.id == id
            ExerciseModeSheet(exercise.mode, exercise.restSeconds, exercise.timedSeconds, canGroup = !last, onDismiss = {
                modeId = null
            }) { mode, seconds ->
                onIntent(WorkoutIntent.SetMode(id, mode, seconds))
                modeId = null
            }
        }
    }
    platesId?.let { id -> PlatesDialog(state, id, onIntent) { platesId = null } }
    if (state.choosingExercise) ExercisePicker(state, onIntent)
    if (state.keyboard != null) WorkoutKeyboardSheet(state, onIntent)
    if (menuOpen) {
        ForjaSheet(stringResource(Res.string.workout_menu), onDismiss = { menuOpen = false }) {
            ForjaListRow(
                stringResource(Res.string.workout_cancel),
                leading = ForjaIcons.Close,
                leadingTint = colors.danger,
                titleColor = colors.danger,
                chevron = false,
                onClick = {
                    menuOpen = false
                    confirmCancel = true
                },
            )
        }
    }
    if (confirmCancel) {
        ForjaDialog(
            title = stringResource(Res.string.workout_cancel_title),
            text = stringResource(Res.string.workout_cancel_body),
            onDismiss = { confirmCancel = false },
            dismissText = stringResource(Res.string.workout_cancel_keep),
            confirmText = stringResource(Res.string.workout_cancel_confirm),
            destructive = true,
            onConfirm = {
                confirmCancel = false
                onIntent(WorkoutIntent.Cancel)
            },
        )
    }
    if (effortInfo) EffortInfoSheet { effortInfo = false }
}

/** Qué son RIR y RPE, en palabras simples y con un ejemplo (solo en modo Pro). */
@Composable
private fun EffortInfoSheet(onClose: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaSheet(stringResource(Res.string.workout_effort_title), onDismiss = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5).padding(bottom = ForjaSpacing.esp4),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            Text(stringResource(Res.string.workout_rir_explain), style = typography.body, color = colors.text1)
            Text(stringResource(Res.string.workout_rpe_explain), style = typography.body, color = colors.text1)
            Text(stringResource(Res.string.workout_effort_example), style = typography.body2, color = colors.text2)
            ForjaButton(
                stringResource(Res.string.workout_effort_ok),
                Modifier.fillMaxWidth(),
                ForjaButtonStyle.SECONDARY,
                onClick = onClose,
            )
        }
    }
}

/**
 * "Ejercicio 2 de 3" y una barra por ejercicio en el orden de la rutina. Tocar una lleva a ese
 * ejercicio; la del que va ahora se marca. El avance de cada barra se anima, no salta.
 */
@Composable
private fun ExerciseStepper(
    detail: com.olarte.ironprogress.domain.session.SessionDetail,
    ordered: List<com.olarte.ironprogress.domain.session.SessionExercise>,
    selected: SessionExerciseId?,
    enabled: Boolean,
    onSelect: (SessionExerciseId) -> Unit,
) {
    val colors = ForjaTheme.colors
    val position = ordered.indexOfFirst { it.id == selected } + 1
    Column(
        Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        if (position > 0) {
            Text(
                stringResource(Res.string.workout_exercise_of, position, ordered.size),
                style = ForjaTheme.typography.caption,
                color = colors.text2,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            ordered.forEachIndexed { index, exercise ->
                val sets = detail.setsOf(exercise.id)
                val target = if (sets.isEmpty()) 0f else sets.count { it.isCompleted }.toFloat() / sets.size
                val done by androidx.compose.animation.core.animateFloatAsState(target, ForjaTheme.motion.effect())
                val isCurrent = exercise.id == selected
                Box(
                    Modifier.weight(1f).heightIn(min = 24.dp).clip(CircleShape)
                        .clickable(enabled = enabled, role = Role.Tab, onClickLabel = (index + 1).toString()) { onSelect(exercise.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(if (isCurrent) 6.dp else ForjaSpacing.esp1).clip(CircleShape)
                            .background(if (isCurrent) colors.surface3 else colors.surface2),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(
                                done,
                            ).height(if (isCurrent) 6.dp else ForjaSpacing.esp1).clip(CircleShape).background(colors.ember),
                        )
                    }
                }
            }
        }
    }
}

/** Número del ejercicio dentro de la rutina, en un círculo. */
@Composable
private fun NumberBadge(number: Int, current: Boolean, done: Boolean = false) {
    val colors = ForjaTheme.colors
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(
            when {
                current -> colors.ember
                done -> colors.emberVeil
                else -> colors.surface3
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (done && !current) {
            Icon(ForjaIcons.Check, null, tint = colors.emberInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        } else {
            ForjaNumber(number.toString(), ForjaTheme.typography.label, color = if (current) colors.onEmber else colors.text1)
        }
    }
}

/** Fila numerada del orden de la rutina; la de ahora va resaltada. */
@Composable
private fun ExerciseOrderRow(
    number: Int,
    name: String,
    progress: String,
    done: Boolean,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = ForjaTheme.colors
    Row(
        Modifier.fillMaxWidth().background(if (current) colors.emberVeil else colors.surface1)
            .heightIn(min = ForjaMeasures.setRowHeight)
            .clickable(enabled = enabled && !current, role = Role.Button, onClick = onClick)
            .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        NumberBadge(number, current = current, done = done)
        Column(Modifier.weight(1f)) {
            Text(name, style = ForjaTheme.typography.body, color = if (done && !current) colors.text2 else colors.text1, maxLines = 2)
            Text(progress, style = ForjaTheme.typography.body2, color = colors.text2)
        }
        if (current) {
            Text(stringResource(Res.string.workout_current), style = ForjaTheme.typography.label, color = colors.emberInk)
        }
    }
}

/**
 * El ejercicio en curso, una serie a la vez: arriba las series hechas en filas cortas (tocar una la
 * corrige), en el centro la serie actual con números grandes y abajo cuántas faltan.
 */
@Composable
private fun CurrentExercise(
    state: WorkoutState,
    exerciseId: SessionExerciseId,
    enabled: Boolean,
    onIntent: (WorkoutIntent) -> Unit,
    onExerciseOptions: () -> Unit,
    onMode: () -> Unit,
    onSetOptions: (WorkoutSetId) -> Unit,
    onEffortInfo: () -> Unit,
) {
    val detail = state.session ?: return
    val exercise = detail.exercises.firstOrNull { it.id == exerciseId } ?: return
    val catalog = state.catalog[exercise.exerciseId]
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val language = appLanguage()
    val sets = detail.setsOf(exercise.id)
    val current = sets.firstOrNull { !it.isCompleted }
    val unit = state.unitOf(exercise.id)
    val measures = state.measuresOf(exercise.id)
    val hide = LocalAppSettings.current.hideBodyMetrics
    val group = detail.groupOf(exercise.id)
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
        val position = detail.exercises.sortedBy { it.sortOrder }.indexOfFirst { it.id == exercise.id } + 1
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            NumberBadge(position, current = true)
            Column(Modifier.weight(1f)) {
                Text(
                    catalog?.name?.resolve(language) ?: exercise.exerciseId.value,
                    style = typography.title2,
                    color = colors.text1,
                )
                catalog?.muscles?.map { it.muscle.label() }?.takeIf { it.isNotEmpty() }?.let { names ->
                    Text(names.joinToString(" · "), color = colors.text2, style = typography.body2)
                }
            }
            ForjaIconButton(
                ForjaIcons.MoreVert,
                stringResource(Res.string.workout_options_short),
                enabled = enabled && state.pendingWrites == 0,
                filled = false,
                tint = colors.text2,
                onClick = onExerciseOptions,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            ModeChip(exercise.mode.summary(exercise.restSeconds, exercise.timedSeconds, group.size), enabled, onMode)
        }
        if (exercise.notes.isNotBlank()) Text(exercise.notes, color = colors.text2, style = typography.body2)
        val done = sets.filter { it.isCompleted }
        if (done.isNotEmpty()) {
            ForjaListGroup {
                done.forEachIndexed { index, set ->
                    if (index > 0) ForjaDivider()
                    val number = sets.indexOf(set) + 1
                    DoneSetRow(
                        number = number,
                        summary = set.forDisplay(hide).summary(measures, formats, unit),
                        highlighted = state.highlightedSetId == set.id,
                        description = stringResource(Res.string.workout_edit_set, number),
                        enabled = enabled,
                    ) { onIntent(WorkoutIntent.OpenKeyboard(set.id, measures.firstOrNull() ?: WorkoutField.REPS)) }
                }
            }
        }
        if (current != null) {
            val previous = equivalentPreviousSet(current, sets, state.previous[exercise.exerciseId].orEmpty())
            CurrentSetCard(
                set = current,
                number = sets.indexOf(current) + 1,
                total = sets.size,
                previous = previous?.forDisplay(hide)?.summary(measures, formats, unit)?.takeIf { it.isNotEmpty() },
                measures = measures,
                unit = unit,
                state = state,
                enabled = enabled,
                onIntent = onIntent,
                onOptions = { onSetOptions(current.id) },
                onEffortInfo = onEffortInfo,
            )
            if (state.invalidSetId == current.id) {
                Text(stringResource(Res.string.workout_invalid), color = colors.danger, style = typography.body2)
            }
        }
        val left = sets.count { !it.isCompleted } - if (current != null) 1 else 0
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (left > 0) pluralStringResource(Res.plurals.workout_sets_left, left, left) else "",
                Modifier.weight(1f),
                color = colors.text2,
                style = typography.body2,
            )
            ForjaButton(
                stringResource(Res.string.workout_add_set),
                style = ForjaButtonStyle.TEXT,
                enabled = enabled && state.pendingWrites == 0,
                icon = ForjaIcons.Add,
            ) { onIntent(WorkoutIntent.AddSet(exercise.id)) }
        }
    }
}

@Composable
private fun ModeChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 36.dp).clip(CircleShape).background(ForjaTheme.colors.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ForjaSpacing.esp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Icon(ForjaIcons.Timer, null, tint = ForjaTheme.colors.emberInk, modifier = Modifier.size(ForjaMeasures.inlineIconSize))
        Text(text, style = ForjaTheme.typography.label, color = ForjaTheme.colors.text1, maxLines = 1)
    }
}

@Composable
private fun DoneSetRow(number: Int, summary: String, highlighted: Boolean, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    val background by animateColorAsState(
        if (highlighted) colors.emberVeil else colors.surface1,
        animationSpec = ForjaTheme.motion.effectFast(),
    )
    Row(
        Modifier.fillMaxWidth().background(background).heightIn(min = ForjaMeasures.minTouch)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .padding(horizontal = ForjaSpacing.esp4, vertical = ForjaSpacing.esp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        ForjaNumber(number.toString(), ForjaTheme.typography.label, Modifier.width(24.dp), colors.text2, TextAlign.Center)
        ForjaNumber(summary, ForjaTheme.typography.body, Modifier.weight(1f), colors.text1)
        Icon(
            ForjaIcons.Check,
            stringResource(Res.string.workout_done),
            tint = colors.ember,
            modifier = Modifier.size(ForjaMeasures.iconSize),
        )
    }
}

/** La serie actual: número grande de cada medida, el anterior para copiar y RIR y RPE opcionales. */
@Composable
private fun CurrentSetCard(
    set: WorkoutSet,
    number: Int,
    total: Int,
    previous: String?,
    measures: List<WorkoutField>,
    unit: WeightUnit,
    state: WorkoutState,
    enabled: Boolean,
    onIntent: (WorkoutIntent) -> Unit,
    onOptions: () -> Unit,
    onEffortInfo: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val hide = LocalAppSettings.current.hideBodyMetrics
    val work = state.work?.takeIf { it.setId == set.id }
    ForjaCard(spacing = ForjaSpacing.esp3) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.workout_set_of, number, total),
                    style = typography.title3,
                    color = colors.text1,
                    modifier = Modifier.semantics { heading() },
                )
                if (set.setType != SetType.WORKING) {
                    Text(
                        set.setType.workoutLabel(),
                        style = typography.label,
                        color = if (set.setType ==
                            SetType.WARMUP
                        ) {
                            colors.ice
                        } else {
                            colors.emberInk
                        },
                    )
                }
            }
            ForjaIconButton(
                ForjaIcons.MoreVert,
                stringResource(Res.string.workout_options, number),
                enabled = enabled,
                filled = false,
                tint = colors.text2,
                onClick = onOptions,
            )
        }
        if (previous != null) {
            Row(
                Modifier.fillMaxWidth().clip(ForjaRadius.shapeS)
                    .clickable(enabled = enabled, role = Role.Button) { onIntent(WorkoutIntent.CopyPrevious(set.id)) }
                    .padding(vertical = ForjaSpacing.esp1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.workout_previous_value, previous),
                    style = typography.body2,
                    color = colors.text2,
                    maxLines = 1,
                )
            }
        }
        if (work != null) {
            val left = (work.remainingMs(state.workNowMs) + 999) / 1000
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ForjaNumber(
                    Formats.duration(left),
                    typography.heroFigure,
                    Modifier.timerPhase(),
                    colors.ember,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            measures.forEach { field ->
                val description = stringResource(Res.string.workout_set_label, number, field.labelIn(unit))
                val selectedField = state.keyboard?.setId == set.id && state.keyboard.field == field
                Column(
                    Modifier.weight(1f).clip(ForjaRadius.shapeM).background(colors.surface3)
                        .then(if (selectedField) Modifier.border(2.dp, colors.ember, ForjaRadius.shapeM) else Modifier)
                        .clickable(enabled = enabled && work == null, role = Role.Button) {
                            onIntent(WorkoutIntent.OpenKeyboard(set.id, field))
                        }
                        .semantics { contentDescription = description }
                        .heightIn(min = 88.dp).padding(horizontal = ForjaSpacing.esp3, vertical = ForjaSpacing.esp2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val hidden = field == WorkoutField.CALORIES && hide && set.calories != null
                    ForjaNumber(
                        if (hidden) {
                            stringResource(Res.string.hidden_value)
                        } else {
                            set.value(field)?.let { formats.shown(field, it, unit) }
                                ?: "-"
                        },
                        typography.largeFigure,
                        color = if (set.value(field) == null) colors.text3 else colors.text1,
                        textAlign = TextAlign.Center,
                    )
                    Text(field.labelIn(unit), style = typography.caption, color = colors.text2, maxLines = 1)
                }
            }
        }
        if (state.proMode) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(WorkoutField.RIR, WorkoutField.RPE).forEach { field ->
                    val value = set.value(field)?.let { formats.number(it, 1) }
                    Box(
                        Modifier.heightIn(min = 36.dp).clip(CircleShape)
                            .background(if (value != null) colors.emberVeil else colors.surface3)
                            .clickable(enabled = enabled, role = Role.Button) { onIntent(WorkoutIntent.OpenKeyboard(set.id, field)) }
                            .padding(horizontal = ForjaSpacing.esp4),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            field.label() + " " + (value ?: "-"),
                            style = typography.label,
                            color = if (value != null) colors.emberInk else colors.text2,
                            maxLines = 1,
                        )
                    }
                }
                ForjaIconButton(
                    ForjaMoreIcons.Info,
                    stringResource(Res.string.workout_effort_info),
                    filled = false,
                    tint = colors.text2,
                    onClick = onEffortInfo,
                )
            }
        }
        if (set.notes.isNotBlank()) Text(set.notes, color = colors.text2, style = typography.body2)
    }
}

/** "20 kg × 8" o "0:45"; vacío si la serie no tiene datos. */
@Composable
private fun WorkoutSet.summary(measures: List<WorkoutField>, formats: Formats, unit: WeightUnit): String = measures.mapNotNull { field ->
    value(field)?.let { v ->
        when (field) {
            WorkoutField.WEIGHT -> formats.shown(field, v, unit) + " " + unit.symbol()
            WorkoutField.DURATION -> Formats.duration(v.toLong())
            else -> formats.shown(field, v, unit)
        }
    }
}.joinToString(" × ")

@Composable
private fun SetOptions(set: WorkoutSet, number: Int, onIntent: (WorkoutIntent) -> Unit, onClose: () -> Unit) {
    var note by remember(set.id) { mutableStateOf(set.notes) }
    ForjaSheet(stringResource(Res.string.workout_options, number), onDismiss = onClose) {
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
    onMode: () -> Unit,
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
        add(
            stringResource(Res.string.workout_mode_title) to {
                onMode()
                onClose()
            },
        )
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
    val fields = state.measuresOf(exercise.id) + if (state.proMode) listOf(WorkoutField.RIR, WorkoutField.RPE) else emptyList()
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val haptics = ForjaTheme.haptics
    val formats = rememberFormats()
    val separator = if (formats.number(1.5).contains(',')) "," else "."
    val previous = equivalentPreviousSet(set, detail.setsOf(exercise.id), state.previous[exercise.exerciseId].orEmpty())
    val unit = state.unitOf(exercise.id)
    // En el descanso o al corregir una serie hecha solo se cambian números: el botón es Listo.
    val editingOnly = state.rest != null || set.isCompleted
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
            Modifier.fillMaxWidth().forjaGlass().verticalScroll(rememberScrollState())
                .padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            if (fields.size > 1) {
                com.olarte.ironprogress.ui.components.ForjaSegmented(
                    options = fields,
                    selected = keyboard.field,
                    label = { it.labelIn(unit) },
                    enabled = !state.failed,
                ) { field ->
                    haptics.segmentTick()
                    onIntent(WorkoutIntent.OpenKeyboard(set.id, field))
                }
            }
            val step = when (keyboard.field) {
                WorkoutField.WEIGHT -> if (unit == WeightUnit.LB) 5.0 else 2.5
                WorkoutField.RPE -> 0.5
                else -> 1.0
            }
            // El valor en el centro y los dos ajustes a los lados, simétricos.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                StepChip("-" + formats.number(step), Modifier.weight(1f)) {
                    haptics.keyboardTap()
                    onIntent(WorkoutIntent.Adjust(-step))
                }
                Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ForjaNumber(
                        keyboard.text.replace(".", separator).ifEmpty { "-" },
                        typography.largeFigure,
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Text(keyboard.field.labelIn(unit), style = typography.caption, color = colors.text2, maxLines = 1)
                }
                StepChip("+" + formats.number(step), Modifier.weight(1f)) {
                    haptics.keyboardTap()
                    onIntent(WorkoutIntent.Adjust(step))
                }
            }
            previous?.forDisplay(LocalAppSettings.current.hideBodyMetrics)?.value(keyboard.field)?.let {
                Text(
                    stringResource(Res.string.workout_previous) + ": " + formats.shown(keyboard.field, it, unit),
                    color = colors.text2,
                    style = typography.body2,
                    textAlign = TextAlign.Center,
                )
            }
            if (keyboard.field == WorkoutField.RIR) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    (0..4).forEach { value ->
                        StepChip(formats.number(value.toDouble(), 0), Modifier.weight(1f)) {
                            haptics.keyboardTap()
                            onIntent(WorkoutIntent.SetValue(value.toDouble()))
                        }
                    }
                }
            }
            if (keyboard.invalid) {
                Text(
                    stringResource(Res.string.workout_invalid_value),
                    color = colors.danger,
                    style = typography.body2,
                    textAlign = TextAlign.Center,
                )
            }
            if (state.failed) {
                Text(
                    stringResource(Res.string.workout_error),
                    color = colors.danger,
                    style = typography.body2,
                    textAlign = TextAlign.Center,
                )
                EditorAction(stringResource(Res.string.editor_retry)) { onIntent(WorkoutIntent.Retry) }
            }
            val decimalAllowed = keyboard.field != WorkoutField.RIR && keyboard.field != WorkoutField.REPS
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "backspace")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                        row.forEach { key ->
                            val label = when (key) {
                                "." -> separator
                                "backspace" -> stringResource(Res.string.workout_delete)
                                else -> key
                            }
                            val keyEnabled = key != "." || decimalAllowed
                            Box(
                                Modifier.weight(1f).heightIn(min = ForjaMeasures.keyHeight).clip(ForjaRadius.shapeM)
                                    .background(if (keyEnabled) colors.surface3 else colors.surface2)
                                    .clickable(enabled = keyEnabled, role = Role.Button) {
                                        haptics.keyboardTap()
                                        onIntent(WorkoutIntent.Key(key))
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (keyEnabled) label else "",
                                    style = if (key == "backspace") typography.label else typography.figure,
                                    color = colors.text1,
                                )
                            }
                        }
                    }
                }
            }
            if (editingOnly) {
                ForjaButton(stringResource(Res.string.workout_done_editing), Modifier.fillMaxWidth(), ForjaButtonStyle.PRIMARY) {
                    onIntent(WorkoutIntent.CloseKeyboard)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                    ForjaButton(
                        stringResource(Res.string.workout_next),
                        Modifier.weight(1f),
                        ForjaButtonStyle.SECONDARY,
                        enabled = !keyboard.invalid && !state.failed && fields.size > 1,
                    ) { onIntent(WorkoutIntent.NextField) }
                    ForjaButton(
                        stringResource(Res.string.workout_complete),
                        Modifier.weight(1f),
                        ForjaButtonStyle.PRIMARY,
                        enabled = !keyboard.invalid && !state.failed,
                    ) { onIntent(WorkoutIntent.Complete(set.id)) }
                }
            }
        }
    }
}

/** Botón redondo de ajuste (-2,5, +2,5, 0 a 4) del teclado de la serie. */
@Composable
private fun StepChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = ForjaMeasures.minTouch).clip(CircleShape).background(ForjaTheme.colors.surface3)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ForjaNumber(text, ForjaTheme.typography.label, color = ForjaTheme.colors.text1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WorkoutAction(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        modifier.fillMaxWidth().heightIn(min = ForjaMeasures.primaryButtonHeight).clip(ForjaRadius.shapeFull)
            .background(if (enabled) colors.ember else colors.surface3).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(ForjaSpacing.esp3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.primaryButton, color = if (enabled) colors.onEmber else colors.text3, maxLines = 1)
    }
}

/** Valor de un campo para mostrar: el peso en la unidad del ejercicio (lb con un decimal). */
private fun com.olarte.ironprogress.ui.format.Formats.shown(field: WorkoutField, value: Double, unit: WeightUnit): String =
    if (field == WorkoutField.WEIGHT && unit == WeightUnit.LB) {
        number(WeightUnits.fromKg(value, unit), 1)
    } else {
        number(value, 2)
    }

/** Etiqueta del campo; el peso es solo su unidad, "kg" o "lb". */
@Composable
private fun WorkoutField.labelIn(unit: WeightUnit): String = if (this == WorkoutField.WEIGHT) unit.symbol() else label()
