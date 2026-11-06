package com.olarte.ironprogress.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.domain.routine.ExerciseMode
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaIconButton
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.components.ForjaRadioRow
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.format.Formats
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.workout_mode_apply
import com.olarte.ironprogress.ui.resources.workout_mode_less
import com.olarte.ironprogress.ui.resources.workout_mode_more
import com.olarte.ironprogress.ui.resources.workout_mode_no_rest
import com.olarte.ironprogress.ui.resources.workout_mode_no_rest_hint
import com.olarte.ironprogress.ui.resources.workout_mode_rest
import com.olarte.ironprogress.ui.resources.workout_mode_rest_hint
import com.olarte.ironprogress.ui.resources.workout_mode_rest_seconds
import com.olarte.ironprogress.ui.resources.workout_mode_summary_circuit
import com.olarte.ironprogress.ui.resources.workout_mode_summary_no_rest
import com.olarte.ironprogress.ui.resources.workout_mode_summary_rest
import com.olarte.ironprogress.ui.resources.workout_mode_summary_superset
import com.olarte.ironprogress.ui.resources.workout_mode_summary_timed
import com.olarte.ironprogress.ui.resources.workout_mode_superset
import com.olarte.ironprogress.ui.resources.workout_mode_superset_hint
import com.olarte.ironprogress.ui.resources.workout_mode_timed
import com.olarte.ironprogress.ui.resources.workout_mode_timed_hint
import com.olarte.ironprogress.ui.resources.workout_mode_timed_seconds
import com.olarte.ironprogress.ui.resources.workout_mode_title
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Texto corto del modo para el chip bajo el nombre del ejercicio. */
@Composable
fun ExerciseMode.summary(restSeconds: Int?, timedSeconds: Int?, groupSize: Int = 2): String = when (this) {
    ExerciseMode.REST -> stringResource(
        Res.string.workout_mode_summary_rest,
        Formats.duration((restSeconds ?: Routine.DEFAULT_REST_SECONDS).toLong()),
    )
    ExerciseMode.NO_REST -> stringResource(Res.string.workout_mode_summary_no_rest)
    ExerciseMode.TIMED -> stringResource(
        Res.string.workout_mode_summary_timed,
        Formats.duration((timedSeconds ?: ExerciseMode.DEFAULT_TIMED_SECONDS).toLong()),
    )
    ExerciseMode.SUPERSET -> stringResource(
        if (groupSize > 2) Res.string.workout_mode_summary_circuit else Res.string.workout_mode_summary_superset,
    )
}

/**
 * Hoja para elegir cómo se hace un ejercicio. Con descanso y por tiempo piden sus segundos con un
 * paso de 15 s; superserie solo se ofrece si hay un ejercicio después.
 */
@Composable
fun ExerciseModeSheet(
    mode: ExerciseMode,
    restSeconds: Int?,
    timedSeconds: Int?,
    canGroup: Boolean,
    onDismiss: () -> Unit,
    onApply: (ExerciseMode, Int?) -> Unit,
) {
    var chosen by remember { mutableStateOf(mode) }
    var rest by remember { mutableIntStateOf(restSeconds?.takeIf { it > 0 } ?: Routine.DEFAULT_REST_SECONDS) }
    var work by remember { mutableIntStateOf(timedSeconds ?: ExerciseMode.DEFAULT_TIMED_SECONDS) }
    ForjaSheet(stringResource(Res.string.workout_mode_title), onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
        ) {
            ForjaListGroup {
                val options = buildList {
                    add(Triple(ExerciseMode.REST, Res.string.workout_mode_rest, Res.string.workout_mode_rest_hint))
                    add(Triple(ExerciseMode.NO_REST, Res.string.workout_mode_no_rest, Res.string.workout_mode_no_rest_hint))
                    add(Triple(ExerciseMode.TIMED, Res.string.workout_mode_timed, Res.string.workout_mode_timed_hint))
                    if (canGroup || mode == ExerciseMode.SUPERSET) {
                        add(Triple(ExerciseMode.SUPERSET, Res.string.workout_mode_superset, Res.string.workout_mode_superset_hint))
                    }
                }
                options.forEachIndexed { index, (option, title, hint) ->
                    if (index > 0) ForjaDivider()
                    ForjaRadioRow(stringResource(title), chosen == option, stringResource(hint)) { chosen = option }
                }
            }
            when (chosen) {
                ExerciseMode.REST -> SecondsStepper(stringResource(Res.string.workout_mode_rest_seconds), rest, min = 15) { rest = it }
                ExerciseMode.TIMED -> SecondsStepper(stringResource(Res.string.workout_mode_timed_seconds), work, min = 5) { work = it }
                else -> Unit
            }
            ForjaButton(stringResource(Res.string.workout_mode_apply), Modifier.fillMaxWidth()) {
                onApply(
                    chosen,
                    when (chosen) {
                        ExerciseMode.REST -> rest
                        ExerciseMode.TIMED -> work
                        else -> null
                    },
                )
            }
        }
    }
}

/** Fila centrada: etiqueta, menos, segundos en m:ss y más. Pasos de 15 s (5 s por debajo de 30). */
@Composable
fun SecondsStepper(label: String, seconds: Int, min: Int, max: Int = 3_600, onChange: (Int) -> Unit) {
    val colors = ForjaTheme.colors
    val step = if (seconds <= 30) 5 else 15
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(label, Modifier.weight(1f), style = ForjaTheme.typography.body, color = colors.text1)
        ForjaIconButton(ForjaIcons.Remove, stringResource(Res.string.workout_mode_less), enabled = seconds > min) {
            onChange((seconds - step).coerceAtLeast(min))
        }
        ForjaNumber(
            Formats.duration(seconds.toLong()),
            ForjaTheme.typography.figure,
            Modifier.width(88.dp),
            textAlign = TextAlign.Center,
        )
        ForjaIconButton(ForjaIcons.Add, stringResource(Res.string.workout_mode_more), enabled = seconds < max) {
            onChange((seconds + if (seconds < 30) 5 else 15).coerceAtMost(max))
        }
    }
}
