package com.olarte.ironprogress.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.CivilDate
import com.olarte.ironprogress.domain.session.monthGrid
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.EditorField
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.interval.TextAction
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.history_back
import com.olarte.ironprogress.ui.resources.history_calendar
import com.olarte.ironprogress.ui.resources.history_cancel
import com.olarte.ironprogress.ui.resources.history_delete_block
import com.olarte.ironprogress.ui.resources.history_delete_set
import com.olarte.ironprogress.ui.resources.history_edit_set
import com.olarte.ironprogress.ui.resources.history_empty
import com.olarte.ironprogress.ui.resources.history_empty_day
import com.olarte.ironprogress.ui.resources.history_failed
import com.olarte.ironprogress.ui.resources.history_invalid
import com.olarte.ironprogress.ui.resources.history_list
import com.olarte.ironprogress.ui.resources.history_loading
import com.olarte.ironprogress.ui.resources.history_next_month
import com.olarte.ironprogress.ui.resources.history_pending
import com.olarte.ironprogress.ui.resources.history_previous_month
import com.olarte.ironprogress.ui.resources.history_reps
import com.olarte.ironprogress.ui.resources.history_retry
import com.olarte.ironprogress.ui.resources.history_row
import com.olarte.ironprogress.ui.resources.history_save
import com.olarte.ironprogress.ui.resources.history_set
import com.olarte.ironprogress.ui.resources.history_title
import com.olarte.ironprogress.ui.resources.history_trained
import com.olarte.ironprogress.ui.resources.history_weekdays
import com.olarte.ironprogress.ui.resources.history_weight
import com.olarte.ironprogress.ui.resources.month_1
import com.olarte.ironprogress.ui.resources.month_10
import com.olarte.ironprogress.ui.resources.month_11
import com.olarte.ironprogress.ui.resources.month_12
import com.olarte.ironprogress.ui.resources.month_2
import com.olarte.ironprogress.ui.resources.month_3
import com.olarte.ironprogress.ui.resources.month_4
import com.olarte.ironprogress.ui.resources.month_5
import com.olarte.ironprogress.ui.resources.month_6
import com.olarte.ironprogress.ui.resources.month_7
import com.olarte.ironprogress.ui.resources.month_8
import com.olarte.ironprogress.ui.resources.month_9
import com.olarte.ironprogress.ui.resources.summary_free
import com.olarte.ironprogress.ui.summary.resultText
import com.olarte.ironprogress.ui.summary.summaryName
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val months: List<StringResource> = listOf(
    Res.string.month_1, Res.string.month_2, Res.string.month_3, Res.string.month_4, Res.string.month_5, Res.string.month_6,
    Res.string.month_7, Res.string.month_8, Res.string.month_9, Res.string.month_10, Res.string.month_11, Res.string.month_12,
)

@Composable
fun HistoryRoute(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val model = koinViewModel<HistoryViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    HistoryScreen(state, model::onIntent, onBack, onOpen)
}

/** Historial por calendario y por lista (F1-23, ANA-01). */
@Composable
fun HistoryScreen(state: HistoryState, onIntent: (HistoryIntent) -> Unit, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        TopBar(stringResource(Res.string.history_title), onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            Chip(stringResource(Res.string.history_calendar), state.calendar) { onIntent(HistoryIntent.ShowCalendar(true)) }
            Chip(stringResource(Res.string.history_list), !state.calendar) { onIntent(HistoryIntent.ShowCalendar(false)) }
        }
        when {
            state.loading -> Text(stringResource(Res.string.history_loading), color = colors.text2, style = typography.body)
            state.failed -> {
                Text(stringResource(Res.string.history_failed), color = colors.text1, style = typography.body)
                EditorAction(stringResource(Res.string.history_retry)) { onIntent(HistoryIntent.Retry) }
            }
            else -> {
                if (state.calendar) Calendar(state, onIntent)
                val rows = state.visibleRows
                if (rows.isEmpty()) {
                    val empty = if (state.calendar) Res.string.history_empty_day else Res.string.history_empty
                    Text(stringResource(empty), color = colors.text2, style = typography.body)
                }
                rows.forEach { row ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface1)
                            .clickable { onOpen(row.session.id.value) }.padding(ForjaSpacing.esp4),
                        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
                    ) {
                        Text(
                            formats.date(row.session.startedAt.toEpochMilliseconds()),
                            style = typography.caption,
                            color = colors.text2,
                        )
                        Text(row.routineName ?: stringResource(Res.string.summary_free), style = typography.body, color = colors.text1)
                        Text(
                            stringResource(
                                Res.string.history_row,
                                Formats.duration(row.durationSeconds),
                                formats.volume(row.volumeKg),
                                row.workingSets,
                            ),
                            style = typography.body2,
                            color = colors.text2,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(ForjaSpacing.esp8))
    }
}

@Composable
private fun Calendar(state: HistoryState, onIntent: (HistoryIntent) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val trained = state.trainedDays
    val trainedLabel = stringResource(Res.string.history_trained)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surface1).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthButton("‹", stringResource(Res.string.history_previous_month)) { onIntent(HistoryIntent.MoveMonth(-1)) }
            Text(
                stringResource(months[state.month - 1]) + " " + state.year,
                Modifier.weight(1f).semantics { heading() },
                style = typography.title3,
                color = colors.text1,
            )
            MonthButton("›", stringResource(Res.string.history_next_month)) { onIntent(HistoryIntent.MoveMonth(1)) }
        }
        Row(Modifier.fillMaxWidth()) {
            stringResource(Res.string.history_weekdays).split(",").forEach { day ->
                Text(day.trim(), Modifier.weight(1f), style = typography.caption, color = colors.text2)
            }
        }
        monthGrid(state.year, state.month).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(day, day in trained, day == state.selectedDay, trainedLabel) {
                                onIntent(HistoryIntent.SelectDay(day))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
    }
}

@Composable
private fun DayCell(day: CivilDate, trained: Boolean, selected: Boolean, trainedLabel: String, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(
                when {
                    selected -> colors.ember
                    trained -> colors.emberVeil
                    else -> colors.surface1
                },
            )
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                if (trained) contentDescription = "${day.day} · $trainedLabel"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.day.toString(),
            style = ForjaTheme.typography.label,
            color = when {
                selected -> colors.onEmber
                trained -> colors.ember
                else -> colors.text1
            },
        )
    }
}

@Composable
internal fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextAction(stringResource(Res.string.history_back), ForjaTheme.colors.text2, onClick = onBack)
        Text(
            title,
            Modifier.weight(1f).semantics { heading() },
            style = ForjaTheme.typography.title3,
            color = ForjaTheme.colors.text1,
        )
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    Box(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(if (selected) colors.emberVeil else colors.surface2)
            .clickable(onClick = onClick).semantics { this.selected = selected }.padding(horizontal = ForjaSpacing.esp4),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ForjaTheme.typography.label, color = if (selected) colors.ember else colors.text1)
    }
}

@Composable
fun HistorySessionRoute(sessionId: String, languageCode: String, onBack: () -> Unit) {
    val model = koinViewModel<HistorySessionViewModel>(key = "history-$sessionId") { parametersOf(sessionId) }
    val state by model.state.collectAsStateWithLifecycle()
    HistorySessionScreen(state, languageCode, model::onIntent, onBack)
}

/** Una sesión pasada: tocar una serie la corrige o la borra; cada bloque con tiempo se puede borrar. */
@Composable
fun HistorySessionScreen(state: HistorySessionState, languageCode: String, onIntent: (HistorySessionIntent) -> Unit, onBack: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val detail = state.detail
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        TopBar(state.routineName ?: stringResource(Res.string.summary_free), onBack)
        when {
            state.loading -> Text(stringResource(Res.string.history_loading), color = colors.text2, style = typography.body)
            detail == null || state.failed -> {
                Text(stringResource(Res.string.history_failed), color = colors.text1, style = typography.body)
                EditorAction(stringResource(Res.string.history_retry)) { onIntent(HistorySessionIntent.Retry) }
            }
            else -> {
                Text(formats.date(detail.session.startedAt.toEpochMilliseconds()), style = typography.caption, color = colors.text2)
                detail.blocks.sortedBy { it.sortOrder }.forEach { block ->
                    val exercises = detail.exercises.filter { it.blockId == block.id }.sortedBy { it.sortOrder }
                    if (block.format == BlockFormat.SETS && exercises.isEmpty()) return@forEach
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surface1).padding(ForjaSpacing.esp4),
                        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                    ) {
                        if (block.format != BlockFormat.SETS) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(block.format.summaryName(), Modifier.weight(1f), style = typography.body, color = colors.text1)
                                Text(block.result?.let { resultText(it, formats) }.orEmpty(), style = typography.body, color = colors.text1)
                            }
                        }
                        exercises.forEach { exercise ->
                            Text(state.exerciseName(exercise.exerciseId, languageCode), style = typography.body, color = colors.text1)
                            detail.setsOf(exercise.id).forEach { set ->
                                val values = listOfNotNull(
                                    set.weightKg?.let { formats.weight(it) },
                                    set.reps?.let { stringResource(Res.string.history_reps) + " " + it },
                                    set.durationSeconds?.let { Formats.duration(it.toLong()) },
                                ).joinToString(" · ")
                                val pending = if (set.isCompleted) "" else " · " + stringResource(Res.string.history_pending)
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !state.saving) { onIntent(HistorySessionIntent.Open(set.id)) }
                                        .padding(horizontal = ForjaSpacing.esp2),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(Res.string.history_set, set.sortOrder),
                                        Modifier.weight(0.3f),
                                        style = typography.body2,
                                        color = colors.text2,
                                    )
                                    Text(values + pending, Modifier.weight(0.7f), style = typography.body2, color = colors.text1)
                                }
                            }
                        }
                        if (block.format != BlockFormat.SETS) {
                            TextAction(stringResource(Res.string.history_delete_block), colors.danger, enabled = !state.saving) {
                                onIntent(HistorySessionIntent.DeleteBlock(block.id))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(ForjaSpacing.esp8))
    }
    state.draft?.let { draft ->
        AlertDialog(
            onDismissRequest = { onIntent(HistorySessionIntent.Cancel) },
            title = { Text(stringResource(Res.string.history_edit_set), style = typography.title3) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
                    EditorField(stringResource(Res.string.history_weight), draft.weight) { onIntent(HistorySessionIntent.Weight(it)) }
                    EditorField(stringResource(Res.string.history_reps), draft.reps) { onIntent(HistorySessionIntent.Reps(it)) }
                    if (draft.invalid) Text(stringResource(Res.string.history_invalid), color = colors.danger, style = typography.caption)
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(HistorySessionIntent.Save) }, enabled = !state.saving) {
                    Text(stringResource(Res.string.history_save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onIntent(HistorySessionIntent.DeleteSet) }, enabled = !state.saving) {
                        Text(stringResource(Res.string.history_delete_set), color = colors.danger)
                    }
                    TextButton(onClick = { onIntent(HistorySessionIntent.Cancel) }) { Text(stringResource(Res.string.history_cancel)) }
                }
            },
        )
    }
}
