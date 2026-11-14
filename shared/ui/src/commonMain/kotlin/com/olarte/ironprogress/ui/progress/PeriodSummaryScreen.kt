package com.olarte.ironprogress.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.progress.Period
import com.olarte.ironprogress.domain.progress.PeriodKind
import com.olarte.ironprogress.domain.progress.PeriodStats
import com.olarte.ironprogress.domain.progress.PeriodSummary
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaCard
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaEmptyState
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaSegmented
import com.olarte.ironprogress.ui.components.ForjaTopBar
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.format.appLanguage
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.editor_back
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
import com.olarte.ironprogress.ui.resources.period_card_sessions
import com.olarte.ironprogress.ui.resources.period_card_title
import com.olarte.ironprogress.ui.resources.period_days
import com.olarte.ironprogress.ui.resources.period_empty_body
import com.olarte.ironprogress.ui.resources.period_empty_title
import com.olarte.ironprogress.ui.resources.period_favourite
import com.olarte.ironprogress.ui.resources.period_favourite_sets
import com.olarte.ironprogress.ui.resources.period_hours_minutes
import com.olarte.ironprogress.ui.resources.period_minutes
import com.olarte.ironprogress.ui.resources.period_month
import com.olarte.ironprogress.ui.resources.period_month_year
import com.olarte.ironprogress.ui.resources.period_muscles
import com.olarte.ironprogress.ui.resources.period_next
import com.olarte.ironprogress.ui.resources.period_previous
import com.olarte.ironprogress.ui.resources.period_records
import com.olarte.ironprogress.ui.resources.period_records_more
import com.olarte.ironprogress.ui.resources.period_records_none
import com.olarte.ironprogress.ui.resources.period_same
import com.olarte.ironprogress.ui.resources.period_see_year
import com.olarte.ironprogress.ui.resources.period_sessions
import com.olarte.ironprogress.ui.resources.period_sets_value
import com.olarte.ironprogress.ui.resources.period_streak
import com.olarte.ironprogress.ui.resources.period_time
import com.olarte.ironprogress.ui.resources.period_title
import com.olarte.ironprogress.ui.resources.period_volume
import com.olarte.ironprogress.ui.resources.period_vs
import com.olarte.ironprogress.ui.resources.period_weeks
import com.olarte.ironprogress.ui.resources.period_year
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PeriodSummaryRoute(initialKey: String, onBack: () -> Unit) {
    val model = koinViewModel<PeriodSummaryViewModel>(key = "period-$initialKey") { parametersOf(initialKey) }
    val state by model.state.collectAsStateWithLifecycle()
    PeriodSummaryScreen(state, onBack, onKind = model::show, onPrevious = model::previous, onNext = model::next)
}

/**
 * Resumen del mes o del año (F2-10): entrenos, tiempo, volumen y días con su comparación, mejor
 * racha, récords, músculos más trabajados y ejercicio favorito. Se cambia de periodo con las
 * flechas o deslizando. Solo datos reales; sin entrenos, un estado vacío.
 */
@Composable
fun PeriodSummaryScreen(
    state: PeriodUiState,
    onBack: () -> Unit,
    onKind: (PeriodKind) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    val colors = ForjaTheme.colors
    val motion = ForjaTheme.motion
    val period = state.period
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        ForjaTopBar(stringResource(Res.string.period_title), onBack, stringResource(Res.string.editor_back))
        Column(Modifier.padding(horizontal = ForjaSpacing.esp5), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            ForjaSegmented(
                options = PeriodKind.entries,
                selected = period?.kind ?: PeriodKind.MONTH,
                label = { stringResource(if (it == PeriodKind.MONTH) Res.string.period_month else Res.string.period_year) },
                onSelect = onKind,
            )
            if (period != null) PeriodNavigator(period, state.canGoBack, state.canGoForward, onPrevious, onNext)
        }
        AnimatedContent(
            targetState = state.summary,
            transitionSpec = { fadeIn(motion.effect()) togetherWith fadeOut(motion.effect()) },
            modifier = Modifier.fillMaxSize().pointerInput(state.canGoBack, state.canGoForward) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { _, amount -> total += amount },
                    onDragEnd = {
                        val threshold = 64.dp.toPx()
                        if (total > threshold) {
                            onPrevious()
                        } else if (total < -threshold) {
                            onNext()
                        }
                    },
                )
            },
            label = "periodo",
        ) { summary ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ForjaSpacing.esp5,
                    end = ForjaSpacing.esp5,
                    top = ForjaSpacing.esp3,
                    bottom = ForjaSpacing.esp8,
                ),
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
            ) {
                when {
                    summary == null -> Unit
                    summary.isEmpty -> item {
                        ForjaEmptyState(
                            stringResource(Res.string.period_empty_title, periodLabel(summary.period)),
                            stringResource(Res.string.period_empty_body),
                        )
                    }
                    else -> {
                        item { StatsGrid(summary) }
                        item { StreakCard(summary.bestStreakWeeks) }
                        item { RecordsCard(summary, state) }
                        if (summary.topMuscles.isNotEmpty()) item { MusclesCard(summary) }
                        summary.favourite?.let { favourite ->
                            item {
                                val name = state.exercises[favourite.exerciseId]?.name?.resolve(appLanguage()) ?: favourite.exerciseId.value
                                FavouriteCard(name, favourite.sets)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodNavigator(period: Period, canGoBack: Boolean, canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ForjaIconButton(ForjaMoreIcons.ChevronLeft, stringResource(Res.string.period_previous), enabled = canGoBack, onClick = onPrevious)
        Text(
            periodLabel(period).replaceFirstChar { it.uppercase() },
            Modifier.weight(1f).semantics { heading() },
            style = ForjaTheme.typography.title2,
            color = ForjaTheme.colors.text1,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        ForjaIconButton(ForjaIcons.ChevronRight, stringResource(Res.string.period_next), enabled = canGoForward, onClick = onNext)
    }
}

/** Cuatro cifras en una cuadrícula de 2 × 2, cada una con su comparación en palabras. */
@Composable
private fun StatsGrid(summary: PeriodSummary) {
    val formats = rememberFormats()
    val now = summary.stats
    val before = summary.previous
    // "frente a octubre" o "frente a 2025": corto para que quepa en la tarjeta.
    val previousLabel = summary.period.previous().let { if (it.kind == PeriodKind.MONTH) monthName(it.month) else it.year.toString() }
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            StatTile(
                stringResource(Res.string.period_sessions),
                formats.number(now.sessions.toDouble(), 0),
                compare(signed(now.sessions - before.sessions, formats), now.sessions == before.sessions, previousLabel),
                Modifier.weight(1f),
            )
            StatTile(
                stringResource(Res.string.period_time),
                durationText(now.totalSeconds),
                compare(signedDuration(now.totalSeconds - before.totalSeconds), now.totalSeconds == before.totalSeconds, previousLabel),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
            StatTile(
                stringResource(Res.string.period_volume),
                formats.volume(now.volumeKg, WeightUnit.KG),
                compare(percentChange(now, before), before.volumeKg <= 0.0 || percentChange(now, before) == "0 %", previousLabel)
                    .takeIf { before.volumeKg > 0.0 },
                Modifier.weight(1f),
            )
            StatTile(
                stringResource(Res.string.period_days),
                formats.number(now.trainedDays.toDouble(), 0),
                compare(signed(now.trainedDays - before.trainedDays, formats), now.trainedDays == before.trainedDays, previousLabel),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun compare(delta: String, same: Boolean, previousLabel: String): String =
    if (same) stringResource(Res.string.period_same, previousLabel) else stringResource(Res.string.period_vs, delta, previousLabel)

/** Tarjeta de una cifra: nombre pequeño, número grande centrado y la comparación debajo. */
@Composable
private fun StatTile(label: String, value: String, comparison: String?, modifier: Modifier = Modifier) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaCard(modifier.clearAndSetSemantics { contentDescription = listOfNotNull(label, value, comparison).joinToString(", ") }) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
        ) {
            Text(label, style = typography.caption, color = colors.text2, maxLines = 1)
            ForjaNumber(value, typography.figure, textAlign = TextAlign.Center)
            Text(comparison ?: " ", style = typography.caption, color = colors.text3, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun StreakCard(weeks: Int) {
    ForjaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(ForjaIcons.FireFilled, contentDescription = null, tint = ForjaTheme.colors.emberInk, modifier = Modifier.size(24.dp))
            Text(
                stringResource(Res.string.period_streak),
                Modifier.weight(1f).padding(horizontal = ForjaSpacing.esp3),
                style = ForjaTheme.typography.body,
                color = ForjaTheme.colors.text1,
            )
            ForjaNumber(pluralStringResource(Res.plurals.period_weeks, weeks, weeks), ForjaTheme.typography.smallFigure)
        }
    }
}

@Composable
private fun RecordsCard(summary: PeriodSummary, state: PeriodUiState) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val language = appLanguage()
    ForjaCard {
        Text(stringResource(Res.string.period_records), style = typography.title3, color = colors.text1)
        if (summary.records.isEmpty()) {
            Text(stringResource(Res.string.period_records_none), style = typography.body2, color = colors.text2)
        } else {
            Column {
                summary.records.take(MAX_RECORDS).forEachIndexed { index, hit ->
                    if (index > 0) ForjaDivider()
                    val exercise = state.exercises[hit.exerciseId]
                    val unit = exercise?.preferredUnit ?: WeightUnit.KG
                    Row(Modifier.fillMaxWidth().padding(vertical = ForjaSpacing.esp2), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(colors.goldVeil), contentAlignment = Alignment.Center) {
                            Icon(ForjaRecordIcons.Trophy, contentDescription = null, tint = colors.goldInk, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = ForjaSpacing.esp3)) {
                            Text(
                                exercise?.name?.resolve(language) ?: hit.exerciseId.value,
                                style = typography.body,
                                color = colors.text1,
                                maxLines = 1,
                            )
                            Text(
                                "${recordKindLabel(hit.kind, hit.weightKg, unit, formats)} · ${formats.date(hit.at.toEpochMilliseconds())}",
                                style = typography.caption,
                                color = colors.text2,
                                maxLines = 1,
                            )
                        }
                        ForjaNumber(recordValue(hit.kind, hit.value, unit, formats), typography.smallFigure, textAlign = TextAlign.End)
                    }
                }
            }
            val more = summary.records.size - MAX_RECORDS
            if (more > 0) Text(stringResource(Res.string.period_records_more, more), style = typography.caption, color = colors.text2)
        }
    }
}

@Composable
private fun MusclesCard(summary: PeriodSummary) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val formats = rememberFormats()
    val top = summary.topMuscles.maxOf { it.second }
    ForjaCard {
        Text(stringResource(Res.string.period_muscles), style = typography.title3, color = colors.text1)
        summary.topMuscles.forEach { (muscle, sets) ->
            val shown = stringResource(Res.string.period_sets_value, formats.number(sets, 1))
            val name = muscle.label()
            Column(
                Modifier.clearAndSetSemantics {
                    contentDescription = "$name, $shown"
                },
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(name, Modifier.weight(1f), style = typography.body2, color = colors.text1, maxLines = 1)
                    Text(shown, style = typography.caption, color = colors.text2, maxLines = 1)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.surface3)) {
                    Box(
                        Modifier.fillMaxWidth(
                            (sets / top).toFloat().coerceIn(0.04f, 1f),
                        ).height(8.dp).clip(CircleShape).background(colors.ember),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavouriteCard(name: String, sets: Int) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    ForjaCard {
        Text(stringResource(Res.string.period_favourite), style = typography.label, color = colors.text2)
        Text(name, style = typography.title3, color = colors.text1)
        Text(pluralStringResource(Res.plurals.period_favourite_sets, sets, sets), style = typography.body2, color = colors.text2)
    }
}

/**
 * Tarjeta del mes arriba de Progreso: entrenos y tiempo del mes con acceso al resumen del mes y del
 * año. Solo sale si ya hay algún entreno.
 */
@Composable
fun MonthSummaryCard(state: PeriodUiState, onOpenMonth: (Period) -> Unit, onOpenYear: (Period) -> Unit) {
    val period = state.period ?: return
    val summary = state.summary ?: return
    if (!state.hasAnySession) return
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val title = stringResource(Res.string.period_card_title, monthName(period.month))
    val body =
        pluralStringResource(
            Res.plurals.period_card_sessions,
            summary.stats.sessions,
            summary.stats.sessions,
            durationText(summary.stats.totalSeconds),
        )
    ForjaCard {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = title) { onOpenMonth(period) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
                Text(title, style = typography.title3, color = colors.text1)
                Text(body, style = typography.body2, color = colors.text2)
            }
            Icon(ForjaIcons.ChevronRight, contentDescription = null, tint = colors.text3, modifier = Modifier.size(24.dp))
        }
        ForjaButton(stringResource(Res.string.period_see_year), Modifier.fillMaxWidth(), ForjaButtonStyle.TONAL) {
            onOpenYear(Period(PeriodKind.YEAR, period.year))
        }
    }
}

/** "noviembre de 2026" o "2026". */
@Composable
fun periodLabel(period: Period): String = when (period.kind) {
    PeriodKind.MONTH -> stringResource(Res.string.period_month_year, monthName(period.month), period.year)
    PeriodKind.YEAR -> period.year.toString()
}

@Composable
fun monthName(month: Int): String = stringResource(MONTHS[month - 1])

/** "2 h 10 min" o "45 min". */
@Composable
private fun durationText(seconds: Long): String {
    val minutes = (seconds / 60).toInt()
    return if (minutes >= 60) {
        stringResource(Res.string.period_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(Res.string.period_minutes, minutes)
    }
}

@Composable
private fun signedDuration(seconds: Long): String = (
    if (seconds > 0) {
        "+"
    } else if (seconds < 0) {
        "−"
    } else {
        ""
    }
    ) + durationText(abs(seconds))

private fun signed(value: Int, formats: Formats): String = when {
    value > 0 -> "+" + formats.number(value.toDouble(), 0)
    value < 0 -> "−" + formats.number(abs(value).toDouble(), 0)
    else -> "0"
}

private fun percentChange(now: PeriodStats, before: PeriodStats): String {
    if (before.volumeKg <= 0.0) return ""
    val percent = ((now.volumeKg - before.volumeKg) / before.volumeKg * 100).roundToInt()
    return when {
        percent > 0 -> "+$percent %"
        percent < 0 -> "−${abs(percent)} %"
        else -> "0 %"
    }
}

private val MONTHS: List<StringResource> = listOf(
    Res.string.month_1,
    Res.string.month_2,
    Res.string.month_3,
    Res.string.month_4,
    Res.string.month_5,
    Res.string.month_6,
    Res.string.month_7,
    Res.string.month_8,
    Res.string.month_9,
    Res.string.month_10,
    Res.string.month_11,
    Res.string.month_12,
)

private const val MAX_RECORDS = 5
