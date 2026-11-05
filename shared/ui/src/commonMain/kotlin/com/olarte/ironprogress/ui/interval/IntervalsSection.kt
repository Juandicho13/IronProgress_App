package com.olarte.ironprogress.ui.interval

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaListGroup
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.components.ForjaSectionLabel
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.plan_disciplines
import com.olarte.ironprogress.ui.resources.plan_disciplines_hint
import com.olarte.ironprogress.ui.resources.plan_interval_line
import com.olarte.ironprogress.ui.resources.plan_interval_start
import com.olarte.ironprogress.ui.resources.plan_intervals
import com.olarte.ironprogress.ui.resources.plan_intervals_hint
import com.olarte.ironprogress.ui.resources.plan_new_interval
import com.olarte.ironprogress.ui.resources.plan_train
import com.olarte.ironprogress.ui.resources.plan_wod_hint
import com.olarte.ironprogress.ui.resources.plan_wod_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Parte de Plan con los otros formatos: disciplinas, bloque funcional e intervalos guardados.
 * Cada uno es una fila que abre su pantalla; los intervalos se editan al tocarlos y empiezan con
 * su botón.
 */
@Composable
fun IntervalsSection(
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onRun: (String) -> Unit,
    onWod: () -> Unit = {},
    onDisciplines: () -> Unit = {},
) {
    val model = koinViewModel<IntervalListViewModel>()
    val templates by model.templates.collectAsStateWithLifecycle()

    ForjaSectionLabel(stringResource(Res.string.plan_train))
    ForjaListGroup {
        ForjaListRow(
            stringResource(Res.string.plan_disciplines),
            subtitle = stringResource(Res.string.plan_disciplines_hint),
            leading = ForjaMoreIcons.SelfImprovement,
            onClick = onDisciplines,
        )
        ForjaDivider()
        ForjaListRow(
            stringResource(Res.string.plan_wod_title),
            subtitle = stringResource(Res.string.plan_wod_hint),
            leading = ForjaMoreIcons.SportsMma,
            onClick = onWod,
        )
    }

    ForjaSectionLabel(stringResource(Res.string.plan_intervals))
    ForjaListGroup {
        templates.forEach { template ->
            key(template.id) {
                ForjaListRow(
                    title = template.name,
                    subtitle = stringResource(
                        Res.string.plan_interval_line,
                        "${template.workSeconds}/${template.restSeconds}",
                        template.rounds,
                        clock(template.totalSeconds),
                    ),
                    chevron = false,
                    onClick = { onEdit(template.id.value) },
                    trailing = {
                        ForjaButton(stringResource(Res.string.plan_interval_start), style = ForjaButtonStyle.TONAL) {
                            onRun(template.id.value)
                        }
                    },
                )
                ForjaDivider()
            }
        }
        ForjaListRow(
            stringResource(Res.string.plan_new_interval),
            subtitle = if (templates.isEmpty()) stringResource(Res.string.plan_intervals_hint) else null,
            leading = ForjaIcons.Timer,
            onClick = onNew,
        )
    }
}
