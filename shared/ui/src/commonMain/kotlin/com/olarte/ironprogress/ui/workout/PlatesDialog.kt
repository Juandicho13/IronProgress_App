package com.olarte.ironprogress.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.WeightUnits
import com.olarte.ironprogress.domain.session.platesPerSide
import com.olarte.ironprogress.domain.session.warmupSets
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.format.rememberFormats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.plates_add_warmup
import com.olarte.ironprogress.ui.resources.plates_bar
import com.olarte.ironprogress.ui.resources.plates_empty
import com.olarte.ironprogress.ui.resources.plates_only_bar
import com.olarte.ironprogress.ui.resources.plates_per_side
import com.olarte.ironprogress.ui.resources.plates_remainder
import com.olarte.ironprogress.ui.resources.plates_title
import com.olarte.ironprogress.ui.resources.plates_warmup
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Calculadora de discos por lado y calentamiento sugerido (F1-22, SES-09). Toma el peso de la
 * primera serie de trabajo pendiente, o de la última si ya están todas hechas.
 */
@Composable
fun PlatesDialog(state: WorkoutState, id: SessionExerciseId, onIntent: (WorkoutIntent) -> Unit, onClose: () -> Unit) {
    val formats = rememberFormats()
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val unit = state.unitOf(id)
    val sets = state.session?.setsOf(id).orEmpty()
    val working = sets.filter { it.setType.countsAsWorking && it.weightKg != null }
    val target = (working.firstOrNull { !it.isCompleted } ?: working.lastOrNull())?.weightKg
    val bar = WeightUnits.defaultBar(unit)
    ForjaSheet(stringResource(Res.string.plates_title), onDismiss = onClose) {
        Column(Modifier.padding(horizontal = ForjaSpacing.esp5), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            if (target == null) {
                Text(stringResource(Res.string.plates_empty), style = typography.body, color = colors.text2)
                return@Column
            }
            val total = WeightUnits.fromKg(target, unit)
            val load = platesPerSide(total, bar, WeightUnits.defaultPlates(unit))
            Text(formats.weight(target, unit), style = typography.largeFigure, color = colors.text1)
            val barText = formats.number(bar) + " " + unit.symbol()
            Text(stringResource(Res.string.plates_bar, barText), style = typography.caption, color = colors.text2)
            Text(
                if (load.perSide.isEmpty()) {
                    stringResource(Res.string.plates_only_bar)
                } else {
                    stringResource(Res.string.plates_per_side, load.perSide.joinToString(" + ") { formats.number(it, 2) })
                },
                style = typography.body,
                color = colors.text1,
            )
            if (!load.isExact) {
                Text(
                    stringResource(Res.string.plates_remainder, formats.number(load.remainder, 2) + " " + unit.symbol()),
                    style = typography.caption,
                    color = colors.goldInk,
                )
            }
            val steps = warmupSets(
                target,
                barKg = WeightUnits.toKg(bar, unit),
                stepKg = WeightUnits.toKg(if (unit == WeightUnit.LB) 5.0 else 2.5, unit),
            )
            if (steps.isNotEmpty()) {
                Text(stringResource(Res.string.plates_warmup), style = typography.caption, color = colors.text2)
                steps.forEach { step ->
                    Text(formats.weight(step.weightKg, unit) + " × " + step.reps, style = typography.body2, color = colors.text1)
                }
                if (sets.none { it.setType == SetType.WARMUP }) {
                    ForjaButton(
                        stringResource(Res.string.plates_add_warmup),
                        Modifier.fillMaxWidth(),
                        ForjaButtonStyle.TONAL,
                        enabled = state.pendingWrites == 0,
                    ) {
                        onIntent(WorkoutIntent.AddWarmup(id))
                        onClose()
                    }
                }
            }
        }
    }
}

internal fun WeightUnit.symbol(): String = if (this == WeightUnit.LB) "lb" else "kg"
