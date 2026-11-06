package com.olarte.ironprogress.ui.discipline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.components.ForjaButton
import com.olarte.ironprogress.ui.components.ForjaButtonStyle
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaNumber
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.flow_failed
import com.olarte.ironprogress.ui.resources.flow_saved
import com.olarte.ironprogress.ui.resources.guided_summary_done
import com.olarte.ironprogress.ui.resources.guided_summary_title
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource

/** Un dato del resumen: etiqueta arriba y valor grande. */
data class SummaryStat(val label: String, val value: String)

/**
 * Resumen al terminar Respiración o un Flujo: el check, el nombre, los datos en tarjetas iguales
 * de dos en dos (el último solo ocupa todo el ancho) y Listo a todo el ancho abajo. Dice si se guardó o si falló.
 */
@Composable
fun GuidedSummary(name: String, stats: List<SummaryStat>, state: GuidedRunState, onDone: () -> Unit) {
    val colors = ForjaTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp4),
            ) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(colors.iceVeil), contentAlignment = Alignment.Center) {
                    Icon(ForjaIcons.Check, contentDescription = null, tint = colors.ice, modifier = Modifier.size(48.dp))
                }
                Text(
                    stringResource(Res.string.guided_summary_title),
                    Modifier.semantics { heading() },
                    style = ForjaTheme.typography.title1,
                    color = colors.text1,
                    textAlign = TextAlign.Center,
                )
                Text(name, style = ForjaTheme.typography.body, color = colors.ice, textAlign = TextAlign.Center)
                stats.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                        row.forEach { stat ->
                            Column(
                                Modifier.weight(1f).clip(ForjaRadius.shapeM).background(colors.surface1).padding(ForjaSpacing.esp4),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(stat.label, style = ForjaTheme.typography.caption, color = colors.text2, maxLines = 1)
                                ForjaNumber(stat.value, ForjaTheme.typography.largeFigure, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Text(
                    stringResource(if (state.failed) Res.string.flow_failed else Res.string.flow_saved),
                    style = ForjaTheme.typography.caption,
                    color = if (state.failed) colors.danger else colors.text2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        ForjaButton(
            stringResource(Res.string.guided_summary_done),
            Modifier.fillMaxWidth().padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
            ForjaButtonStyle.PRIMARY,
            onClick = onDone,
        )
    }
}

/** Minutos y segundos de una sesión: "5:04". */
internal fun summaryClock(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
