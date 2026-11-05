package com.olarte.ironprogress.ui.interval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.ui.components.EditorAction
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.disc_bar
import com.olarte.ironprogress.ui.resources.plan_interval_edit
import com.olarte.ironprogress.ui.resources.plan_interval_line
import com.olarte.ironprogress.ui.resources.plan_interval_start
import com.olarte.ironprogress.ui.resources.plan_intervals
import com.olarte.ironprogress.ui.resources.plan_new_interval
import com.olarte.ironprogress.ui.resources.plan_wod
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sección de Plan con los intervalos guardados: empezar, editar o crear uno nuevo. */
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
    val colors = ForjaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Text(stringResource(Res.string.plan_intervals), style = ForjaTheme.typography.title3, color = colors.text1)
        templates.forEach { template ->
            Row(
                Modifier.fillMaxWidth().clip(ForjaRadius.shapeL).background(colors.surface1).padding(ForjaSpacing.esp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = ForjaTheme.typography.body, color = colors.text1)
                    Text(
                        stringResource(
                            Res.string.plan_interval_line,
                            "${template.workSeconds}/${template.restSeconds}",
                            template.rounds,
                            clock(template.totalSeconds),
                        ),
                        style = ForjaTheme.typography.caption,
                        color = colors.text2,
                    )
                }
                TextAction(stringResource(Res.string.plan_interval_edit), colors.text2) { onEdit(template.id.value) }
                TextAction(stringResource(Res.string.plan_interval_start)) { onRun(template.id.value) }
            }
        }
        EditorAction(stringResource(Res.string.plan_new_interval), onClick = onNew)
        EditorAction(stringResource(Res.string.plan_wod), onClick = onWod)
        // Disciplinas (F1-20): yoga, pilates, movilidad, respiración y el resto desde una sola pantalla.
        EditorAction(stringResource(Res.string.disc_bar), onClick = onDisciplines)
    }
}
