package com.olarte.ironprogress.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaDivider
import com.olarte.ironprogress.ui.components.ForjaListRow
import com.olarte.ironprogress.ui.components.ForjaSheet
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.ladder_body
import com.olarte.ironprogress.ui.resources.ladder_routine
import com.olarte.ironprogress.ui.resources.ladder_routine_hint
import com.olarte.ironprogress.ui.resources.ladder_title
import com.olarte.ironprogress.ui.resources.ladder_today
import com.olarte.ironprogress.ui.resources.ladder_today_hint
import com.olarte.ironprogress.ui.resources.stagnation_later
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Cambiar de variante por la escalera (F2-12): solo hoy, o hoy y en la rutina. */
@Composable
internal fun LadderSheet(name: String, reps: Int, inRoutine: Boolean, onChoose: (Boolean) -> Unit, onDismiss: () -> Unit) {
    ForjaSheet(stringResource(Res.string.ladder_title, name), onDismiss = onDismiss) {
        LadderOptions(reps, inRoutine, onChoose, onDismiss)
    }
}

/** El contenido de la hoja, aparte para poder verlo en las capturas. */
@Composable
internal fun LadderOptions(reps: Int, inRoutine: Boolean, onChoose: (Boolean) -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = ForjaSpacing.esp4), verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Text(
            stringResource(Res.string.ladder_body, reps),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            style = ForjaTheme.typography.body2,
            color = ForjaTheme.colors.text2,
            textAlign = TextAlign.Center,
        )
        Column {
            ForjaListRow(
                stringResource(Res.string.ladder_today),
                subtitle = stringResource(Res.string.ladder_today_hint),
                onClick = { onChoose(false) },
            )
            if (inRoutine) {
                ForjaDivider()
                ForjaListRow(
                    stringResource(Res.string.ladder_routine),
                    subtitle = stringResource(Res.string.ladder_routine_hint),
                    onClick = { onChoose(true) },
                )
            }
        }
        ForjaButton(
            stringResource(Res.string.stagnation_later),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5),
            ForjaButtonStyle.TEXT,
            onClick = onLater,
        )
    }
}
