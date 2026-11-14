package com.olarte.ironprogress.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.progress.ExerciseProgress
import com.olarte.ironprogress.domain.progress.ProgressMetric
import com.olarte.ironprogress.domain.progress.ProgressPoint
import com.olarte.ironprogress.domain.progress.ProgressRange
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.domain.progress.RecordMark
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaScreen
import com.olarte.ironprogress.ui.components.ForjaSectionLabel
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.day_month
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.resources.exercise_progress_chart_a11y
import com.olarte.ironprogress.ui.resources.exercise_progress_empty_body
import com.olarte.ironprogress.ui.resources.exercise_progress_empty_title
import com.olarte.ironprogress.ui.resources.exercise_progress_info
import com.olarte.ironprogress.ui.resources.exercise_progress_metric_best_set
import com.olarte.ironprogress.ui.resources.exercise_progress_metric_reps
import com.olarte.ironprogress.ui.resources.exercise_progress_metric_total_reps
import com.olarte.ironprogress.ui.resources.exercise_progress_metric_volume
import com.olarte.ironprogress.ui.resources.exercise_progress_one_point
import com.olarte.ironprogress.ui.resources.exercise_progress_range_1y
import com.olarte.ironprogress.ui.resources.exercise_progress_range_3m
import com.olarte.ironprogress.ui.resources.exercise_progress_range_6m
import com.olarte.ironprogress.ui.resources.exercise_progress_range_all
import com.olarte.ironprogress.ui.resources.exercise_progress_range_empty
import com.olarte.ironprogress.ui.resources.exercise_progress_record_point
import com.olarte.ironprogress.ui.resources.exercise_progress_records
import com.olarte.ironprogress.ui.resources.exercise_progress_same_4w
import com.olarte.ironprogress.ui.resources.exercise_progress_sessions
import com.olarte.ironprogress.ui.resources.exercise_progress_title
import com.olarte.ironprogress.ui.resources.exercise_progress_vs_4w
import com.olarte.ironprogress.ui.resources.history_failed
import com.olarte.ironprogress.ui.resources.history_retry
import com.olarte.ironprogress.ui.resources.month_short_1
import com.olarte.ironprogress.ui.resources.month_short_10
import com.olarte.ironprogress.ui.resources.month_short_11
import com.olarte.ironprogress.ui.resources.month_short_12
import com.olarte.ironprogress.ui.resources.month_short_2
import com.olarte.ironprogress.ui.resources.month_short_3
import com.olarte.ironprogress.ui.resources.month_short_4
import com.olarte.ironprogress.ui.resources.month_short_5
import com.olarte.ironprogress.ui.resources.month_short_6
import com.olarte.ironprogress.ui.resources.month_short_7
import com.olarte.ironprogress.ui.resources.month_short_8
import com.olarte.ironprogress.ui.resources.month_short_9
import com.olarte.ironprogress.ui.resources.record_reps_value
import com.olarte.ironprogress.ui.summary.setsLine
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.today.localEpochDay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseProgressRoute(id: String, languageCode: String, onBack: () -> Unit, onOpenSession: (String) -> Unit) {
    val model = koinViewModel<ExerciseProgressViewModel>(key = "progress-$id") { parametersOf(id) }
    val state by model.state.collectAsStateWithLifecycle()
    ExerciseProgressScreen(state, languageCode, model::onIntent, onBack, onOpenSession)
}

/**
 * Progreso de un ejercicio (PANTALLAS 2.2): la marca de hoy y su cambio, la gráfica con su
 * métrica y su rango, los récords y las últimas sesiones. Todo centrado y con datos reales.
 */
@Composable
fun ExerciseProgressScreen(
    state: ExerciseProgressState,
    languageCode: String,
    onIntent: (ExerciseProgressIntent) -> Unit,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    var info by remember { mutableStateOf(false) }
    ForjaScreen(
        title = state.exercise?.name?.resolve(languageCode).orEmpty(),
        subtitle = stringResource(Res.string.exercise_progress_title),
        onBack = onBack,
        backDescription = stringResource(Res.string.editor_back),
    ) {
        when {
            state.loading -> Unit
            state.failed -> item {
                ForjaEmptyState(stringResource(Res.string.history_failed)) {
                    EditorAction(stringResource(Res.string.history_retry)) { onIntent(ExerciseProgressIntent.Retry) }
                }
            }
            state.progress.isEmpty -> item {
                ForjaEmptyState(
                    stringResource(Res.string.exercise_progress_empty_title),
                    stringResource(Res.string.exercise_progress_empty_body),
                )
            }
            else -> {
                item(key = "hero") { Hero(state, formats) { info = true } }
                item(key = "chart") { ChartCard(state, formats, onIntent) }
                item(key = "records") { RecordsCard(state, formats) }
                item(key = "sessions") { SessionsCard(state, formats, onOpenSession) }
            }
        }
    }
    if (info) StrengthInfoSheet { info = false }
}

/** La marca más reciente de la métrica elegida, grande y centrada, con el cambio de 4 semanas. */
@Composable
private fun Hero(state: ExerciseProgressState, formats: Formats, onInfo: () -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val progress = state.progress
    val latest = progress.points.lastOrNull()?.let { progress.valueOf(it, state.metric) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metricLabel(progress, state.metric), style = typography.caption, color = colors.text2)
            if (state.metric == ProgressMetric.ESTIMATED_ONE_REP_MAX) {
                ForjaIconButton(
                    ForjaMoreIcons.Info,
                    stringResource(Res.string.exercise_progress_info),
                    filled = false,
                    tint = colors.text2,
                    onClick = onInfo,
                )
            }
        }
        if (latest != null) {
            ForjaNumber(
                metricValue(progress, state.metric, latest, state.unit, formats),
                typography.heroFigure,
                textAlign = TextAlign.Center,
            )
        }
        val change = progress.changeOverFourWeeks(state.metric)
        if (change != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                val rounded = kotlin.math.abs(change) < 0.05
                if (!rounded) {
                    Icon(
                        ForjaIcons.NorthEast,
                        contentDescription = null,
                        tint = if (change > 0) colors.emberInk else colors.text2,
                        modifier = Modifier.size(18.dp).rotate(if (change > 0) 0f else 90f),
                    )
                }
                Text(
                    if (rounded) {
                        stringResource(Res.string.exercise_progress_same_4w)
                    } else {
                        stringResource(
                            Res.string.exercise_progress_vs_4w,
                            signedMetric(progress, state.metric, change, state.unit, formats),
                        )
                    },
                    style = typography.body2,
                    color = if (change > 0 && !rounded) colors.emberInk else colors.text2,
                )
            }
        }
    }
}

@Composable
private fun ChartCard(state: ExerciseProgressState, formats: Formats, onIntent: (ExerciseProgressIntent) -> Unit) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val progress = state.progress
    val haptics = ForjaTheme.haptics
    val metricNames = progress.metrics.associateWith { metricLabel(progress, it) }
    val rangeNames = mapOf(
        ProgressRange.THREE_MONTHS to stringResource(Res.string.exercise_progress_range_3m),
        ProgressRange.SIX_MONTHS to stringResource(Res.string.exercise_progress_range_6m),
        ProgressRange.ONE_YEAR to stringResource(Res.string.exercise_progress_range_1y),
        ProgressRange.ALL to stringResource(Res.string.exercise_progress_range_all),
    )
    ForjaCard {
        if (progress.metrics.size > 1) {
            ForjaSegmented(progress.metrics, state.metric, { metricNames.getValue(it) }) {
                if (it != state.metric) haptics.segmentTick()
                onIntent(ExerciseProgressIntent.Metric(it))
            }
        }
        val visible = state.visible.mapNotNull { point -> progress.valueOf(point, state.metric)?.let { point to it } }
        if (visible.isEmpty()) {
            Text(
                stringResource(Res.string.exercise_progress_range_empty),
                Modifier.fillMaxWidth(),
                style = typography.body2,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        } else {
            val shown = visible.map { (point, value) -> ChartPoint(point.at.toEpochMilliseconds(), displayed(progress, value, state.unit)) }
            val allTime = progress.points.mapNotNull { progress.valueOf(it, state.metric) }.maxOrNull()
            val recordIndex = allTime?.let { best -> visible.indexOfLast { (_, value) -> value >= best - 1e-6 } }?.takeIf { it >= 0 }
            val monthNames = monthNames()
            val dayMonth = stringResource(Res.string.day_month, 0, "%s")
            val label = metricLabel(progress, state.metric)
            val last = visible.last()
            val description = stringResource(
                Res.string.exercise_progress_chart_a11y,
                label,
                visible.size,
                metricValue(progress, state.metric, last.second, state.unit, formats),
                formats.date(last.first.at.toEpochMilliseconds()),
                metricValue(progress, state.metric, visible.maxOf { it.second }, state.unit, formats),
            )
            val recordFormat = stringResource(Res.string.exercise_progress_record_point, "%s")
            ProgressChart(
                points = shown,
                recordIndex = recordIndex,
                formatValue = { valueInDisplayUnit(progress, state.metric, it, state.unit, formats) },
                formatTick = { formats.number(it, if (it % 1.0 == 0.0) 0 else 1) },
                formatDate = { epochMs -> shortDate(epochMs, monthNames, dayMonth) },
                recordLabel = { recordFormat.replace("%s", it) },
                tooltipDetail = { index -> bestSetDetail(visible[index].first, state.unit, formats) },
                description = description,
            )
            if (visible.size == 1) {
                Text(
                    stringResource(Res.string.exercise_progress_one_point),
                    Modifier.fillMaxWidth(),
                    style = typography.caption,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        ForjaSegmented(ProgressRange.entries, state.range, { rangeNames.getValue(it) }) {
            if (it != state.range) haptics.segmentTick()
            onIntent(ExerciseProgressIntent.Range(it))
        }
    }
}

/** Récords con el trofeo en oro: fuerza estimada, mayor peso, más reps con el peso de siempre y volumen. */
@Composable
private fun RecordsCard(state: ExerciseProgressState, formats: Formats) {
    val colors = ForjaTheme.colors
    val records = state.progress.records
    val repsKey = state.progress.mostUsedWeightKey ?: 0.0
    val rows = buildList {
        records.estimatedOneRepMax?.let { add(RecordKind.ESTIMATED_ONE_REP_MAX to it) }
        records.heaviestWeight?.let { add(RecordKind.HEAVIEST_WEIGHT to it) }
        records.mostRepsByWeight[repsKey]?.let { add(RecordKind.MOST_REPS to it) }
        records.bestSessionVolume?.let { add(RecordKind.BEST_SESSION_VOLUME to it) }
    }
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        ForjaSectionLabel(stringResource(Res.string.exercise_progress_records))
        ForjaListGroup {
            rows.forEachIndexed { index, (kind, mark) ->
                if (index > 0) ForjaDivider()
                ForjaListRow(
                    title = recordKindLabel(kind, mark.weightKg.takeIf { kind == RecordKind.MOST_REPS }, state.unit, formats),
                    subtitle = formats.date(mark.at.toEpochMilliseconds()),
                    leading = ForjaRecordIcons.Trophy,
                    leadingTint = colors.goldInk,
                    chevron = false,
                    trailing = { ForjaNumber(markValue(kind, mark, state.unit, formats), ForjaTheme.typography.smallFigure) },
                )
            }
        }
    }
}

@Composable
private fun SessionsCard(state: ExerciseProgressState, formats: Formats, onOpenSession: (String) -> Unit) {
    val points = state.progress.points.takeLast(LAST_SESSIONS).reversed()
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        ForjaSectionLabel(stringResource(Res.string.exercise_progress_sessions))
        ForjaListGroup {
            points.forEachIndexed { index, point ->
                if (index > 0) ForjaDivider()
                ForjaListRow(
                    title = formats.date(point.at.toEpochMilliseconds()),
                    value = setsLine(point.sets, formats),
                    onClick = { onOpenSession(point.sessionId.value) },
                )
            }
        }
    }
}

private const val LAST_SESSIONS = 6

/** Nombre de la métrica: con peso, fuerza estimada, mejor serie y volumen; sin peso, reps. */
@Composable
internal fun metricLabel(progress: ExerciseProgress, metric: ProgressMetric): String = when (metric) {
    ProgressMetric.ESTIMATED_ONE_REP_MAX -> strengthLabel()
    ProgressMetric.BEST_SET -> stringResource(
        if (progress.usesWeight) Res.string.exercise_progress_metric_best_set else Res.string.exercise_progress_metric_reps,
    )
    ProgressMetric.VOLUME -> stringResource(
        if (progress.usesWeight) Res.string.exercise_progress_metric_volume else Res.string.exercise_progress_metric_total_reps,
    )
}

/** El valor de una métrica con su unidad: "85 kg", "2.500 kg" o "12 reps". [value] va en kg o reps. */
@Composable
internal fun metricValue(progress: ExerciseProgress, metric: ProgressMetric, value: Double, unit: WeightUnit, formats: Formats): String =
    when {
        !progress.usesWeight -> pluralStringResource(Res.plurals.record_reps_value, value.toInt(), value.toInt())
        metric == ProgressMetric.VOLUME -> formats.volume(value, unit)
        else -> formats.weight(roundForDisplay(value, unit), unit)
    }

/** El cambio con signo: "+2 kg", "−500 kg" o "+3 reps". */
@Composable
private fun signedMetric(progress: ExerciseProgress, metric: ProgressMetric, change: Double, unit: WeightUnit, formats: Formats): String {
    val sign = if (change > 0) "+" else "−"
    return sign + metricValue(progress, metric, kotlin.math.abs(change), unit, formats)
}

/** Lo que se dibuja: los kilos pasan a libras si el ejercicio se ve en libras; las reps no cambian. */
private fun displayed(progress: ExerciseProgress, value: Double, unit: WeightUnit): Double =
    if (progress.usesWeight && unit == WeightUnit.LB) value * Formats.LB_PER_KG else value

/** Texto de un valor ya en la unidad que se ve (el de la gráfica). */
private fun valueInDisplayUnit(
    progress: ExerciseProgress,
    metric: ProgressMetric,
    shown: Double,
    unit: WeightUnit,
    formats: Formats,
): String {
    if (!progress.usesWeight) return formats.number(shown, 0)
    val suffix = if (unit == WeightUnit.LB) " lb" else " kg"
    val digits = if (metric == ProgressMetric.VOLUME) 0 else 1
    return formats.number(if (metric == ProgressMetric.VOLUME) shown else kotlin.math.round(shown * 2) / 2, digits) + suffix
}

/** El detalle del punto tocado: la mejor serie de ese día, "62,5 kg × 10". */
private fun bestSetDetail(point: ProgressPoint, unit: WeightUnit, formats: Formats): String? {
    val best = point.bestSet
    val reps = best.reps ?: return null
    val weight = best.weightKg?.takeIf { it > 0 } ?: return null
    return formats.weight(weight, unit) + " × " + reps
}

@Composable
private fun markValue(kind: RecordKind, mark: RecordMark, unit: WeightUnit, formats: Formats): String = when (kind) {
    RecordKind.HEAVIEST_WEIGHT -> formats.weight(mark.value, unit) + (mark.reps?.let { " × $it" } ?: "")
    else -> recordValue(kind, mark.value, unit, formats)
}

@Composable
private fun monthNames(): List<String> = listOf(
    stringResource(Res.string.month_short_1),
    stringResource(Res.string.month_short_2),
    stringResource(Res.string.month_short_3),
    stringResource(Res.string.month_short_4),
    stringResource(Res.string.month_short_5),
    stringResource(Res.string.month_short_6),
    stringResource(Res.string.month_short_7),
    stringResource(Res.string.month_short_8),
    stringResource(Res.string.month_short_9),
    stringResource(Res.string.month_short_10),
    stringResource(Res.string.month_short_11),
    stringResource(Res.string.month_short_12),
)

/** "9 sep" o "Sep 9" para el eje de la gráfica, con el día local del teléfono. */
private fun shortDate(epochMs: Long, months: List<String>, pattern: String): String {
    val date = com.olarte.ironprogress.domain.session.CivilDate.fromEpochDay(localEpochDay(epochMs))
    return pattern.replace("0", date.day.toString()).replace("%s", months[date.month - 1])
}
