package com.olarte.ironprogress.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionSummary
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.summary_blocks
import com.olarte.ironprogress.ui.resources.summary_done
import com.olarte.ironprogress.ui.resources.summary_duration
import com.olarte.ironprogress.ui.resources.summary_exercises
import com.olarte.ironprogress.ui.resources.summary_failed
import com.olarte.ironprogress.ui.resources.summary_first_time
import com.olarte.ironprogress.ui.resources.summary_format_amrap
import com.olarte.ironprogress.ui.resources.summary_format_breathing
import com.olarte.ironprogress.ui.resources.summary_format_emom
import com.olarte.ironprogress.ui.resources.summary_format_flow
import com.olarte.ironprogress.ui.resources.summary_format_for_time
import com.olarte.ironprogress.ui.resources.summary_format_intervals
import com.olarte.ironprogress.ui.resources.summary_format_rounds
import com.olarte.ironprogress.ui.resources.summary_free
import com.olarte.ironprogress.ui.resources.summary_less_time
import com.olarte.ironprogress.ui.resources.summary_loading
import com.olarte.ironprogress.ui.resources.summary_more_time
import com.olarte.ironprogress.ui.resources.summary_records
import com.olarte.ironprogress.ui.resources.summary_result_completed
import com.olarte.ironprogress.ui.resources.summary_result_reps
import com.olarte.ironprogress.ui.resources.summary_result_rounds
import com.olarte.ironprogress.ui.resources.summary_result_rounds_reps
import com.olarte.ironprogress.ui.resources.summary_retry
import com.olarte.ironprogress.ui.resources.summary_same_time
import com.olarte.ironprogress.ui.resources.summary_sets
import com.olarte.ironprogress.ui.resources.summary_title
import com.olarte.ironprogress.ui.resources.summary_volume
import com.olarte.ironprogress.ui.resources.summary_vs_last
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs

@Composable
fun SummaryRoute(sessionId: String, languageCode: String, onDone: () -> Unit) {
    val model = koinViewModel<SummaryViewModel>(key = "summary-$sessionId") { parametersOf(sessionId) }
    val state by model.state.collectAsStateWithLifecycle()
    SummaryScreen(state, languageCode, onRetry = model::retry, onDone = onDone)
}

/** Resumen al terminar (PANTALLAS 1.6). El feedback por músculo y las molestias llegan en F2-06. */
@Composable
fun SummaryScreen(state: SummaryState, languageCode: String, onRetry: () -> Unit, onDone: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
    ) {
        val summary = state.summary
        when {
            state.loading -> Text(stringResource(Res.string.summary_loading), color = colors.text2, style = typography.body)
            summary == null -> {
                Text(stringResource(Res.string.summary_failed), color = colors.text1, style = typography.body)
                EditorAction(stringResource(Res.string.summary_retry), onClick = onRetry)
                EditorAction(stringResource(Res.string.summary_done), onClick = onDone)
            }
            else -> {
                Spacer(Modifier.height(ForjaSpacing.esp8))
                val caption = (state.routineName ?: stringResource(Res.string.summary_free)) + " · " +
                    formats.date(summary.session.startedAt.toEpochMilliseconds())
                Text(caption, style = typography.caption, color = colors.text2)
                Text(
                    stringResource(Res.string.summary_title),
                    Modifier.semantics { heading() },
                    style = typography.title1,
                    color = colors.text1,
                )
                if (summary.records.isNotEmpty()) Records(state, summary, languageCode, formats)
                Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                    Metric(
                        Modifier.weight(1f),
                        stringResource(Res.string.summary_duration),
                        Formats.duration(summary.durationSeconds),
                        durationNote(summary),
                    )
                    Metric(
                        Modifier.weight(1f),
                        stringResource(Res.string.summary_volume),
                        formats.volume(summary.volumeKg),
                        summary.volumeChangePercent?.let { pct ->
                            val sign = when {
                                pct > 0 -> "+"
                                pct < 0 -> "−"
                                else -> ""
                            }
                            stringResource(Res.string.summary_vs_last, "$sign${abs(pct)} %")
                        } ?: stringResource(Res.string.summary_first_time),
                    )
                }
                if (summary.exercises.isNotEmpty()) {
                    Card {
                        Label(stringResource(Res.string.summary_exercises))
                        Text(stringResource(Res.string.summary_sets, summary.workingSets), style = typography.caption, color = colors.text2)
                        summary.exercises.forEach { exercise ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    ForjaIcons.Check,
                                    contentDescription = null,
                                    tint = colors.ember,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.size(ForjaSpacing.esp2))
                                Column(Modifier.weight(1f)) {
                                    val name = state.exerciseName(exercise.exerciseId, languageCode)
                                    Text(name, style = typography.body, color = colors.text1)
                                    Text(setsLine(exercise.sets, formats), style = typography.body2, color = colors.text2)
                                }
                            }
                        }
                    }
                }
                if (summary.blockResults.isNotEmpty()) {
                    Card {
                        Label(stringResource(Res.string.summary_blocks))
                        summary.blockResults.forEach { block ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(block.format.summaryName(), Modifier.weight(1f), style = typography.body, color = colors.text1)
                                Text(block.result?.let { resultText(it, formats) }.orEmpty(), style = typography.body, color = colors.text1)
                            }
                        }
                    }
                }
                EditorAction(stringResource(Res.string.summary_done), onClick = onDone)
                Spacer(Modifier.height(ForjaSpacing.esp8))
            }
        }
    }
}

@Composable
private fun Records(state: SummaryState, summary: SessionSummary, languageCode: String, formats: Formats) {
    val colors = ForjaTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.goldVeil).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Text(stringResource(Res.string.summary_records), style = ForjaTheme.typography.caption, color = colors.goldInk)
        Text(summary.records.size.toString(), style = ForjaTheme.typography.largeFigure, color = colors.text1)
        Text(
            summary.records.joinToString(" · ") {
                state.exerciseName(it.exerciseId, languageCode) + " " + formats.number(it.weightKg, 2) + " × " + it.reps
            },
            style = ForjaTheme.typography.body2,
            color = colors.text1,
        )
    }
}

@Composable
private fun Metric(modifier: Modifier, label: String, value: String, note: String) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(ForjaTheme.colors.surface1).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
    ) {
        Text(label, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
        Text(value, style = ForjaTheme.typography.figure, color = ForjaTheme.colors.text1)
        Text(note, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(ForjaTheme.colors.surface1).padding(ForjaSpacing.esp4),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text2)
}

@Composable
private fun durationNote(summary: SessionSummary): String {
    val change = summary.durationChangeSeconds ?: return stringResource(Res.string.summary_first_time)
    val minutes = abs(change) / 60
    return when {
        minutes == 0L -> stringResource(Res.string.summary_same_time)
        change < 0 -> stringResource(Res.string.summary_less_time, minutes.toInt())
        else -> stringResource(Res.string.summary_more_time, minutes.toInt())
    }
}

/** "62,5 kg × 11, 10, 10, 9" si el peso no cambia; si cambia, cada serie con su peso. */
internal fun setsLine(sets: List<WorkoutSet>, formats: Formats): String {
    val weights = sets.map { it.weightKg }.distinct()
    val reps = sets.map { it.reps?.toString() ?: it.durationSeconds?.let { s -> Formats.duration(s.toLong()) } ?: "" }
    return if (weights.size == 1) {
        (weights.single()?.let { formats.weight(it) + " × " } ?: "") + reps.joinToString(", ")
    } else {
        sets.zip(reps).joinToString(", ") { (set, r) -> (set.weightKg?.let { formats.number(it, 2) + " × " } ?: "") + r }
    }
}

@Composable
internal fun BlockFormat.summaryName(): String = stringResource(
    when (this) {
        BlockFormat.EMOM -> Res.string.summary_format_emom
        BlockFormat.AMRAP -> Res.string.summary_format_amrap
        BlockFormat.FOR_TIME -> Res.string.summary_format_for_time
        BlockFormat.INTERVALS -> Res.string.summary_format_intervals
        BlockFormat.ROUNDS -> Res.string.summary_format_rounds
        BlockFormat.FLOW -> Res.string.summary_format_flow
        BlockFormat.BREATHING -> Res.string.summary_format_breathing
        BlockFormat.SETS -> Res.string.summary_exercises
    },
)

@Composable
internal fun resultText(result: BlockResult, formats: Formats): String = when (result) {
    is BlockResult.Time -> Formats.duration(result.seconds.toLong())
    is BlockResult.RoundsAndReps -> stringResource(Res.string.summary_result_rounds_reps, result.rounds, result.reps)
    is BlockResult.Rounds -> stringResource(Res.string.summary_result_rounds, result.rounds)
    is BlockResult.RepsPerRound -> stringResource(
        Res.string.summary_result_reps,
        result.reps.joinToString(", ") { formats.number(it.toDouble(), 0) },
    )
    is BlockResult.Completed -> stringResource(Res.string.summary_result_completed)
}
