package com.olarte.ironprogress.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.Joint
import com.olarte.ironprogress.domain.catalog.JointImpact
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.label
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.detail_alternatives
import com.olarte.ironprogress.ui.resources.detail_demand
import com.olarte.ironprogress.ui.resources.detail_easier
import com.olarte.ironprogress.ui.resources.detail_harder
import com.olarte.ironprogress.ui.resources.detail_joints
import com.olarte.ironprogress.ui.resources.detail_muscles
import com.olarte.ironprogress.ui.resources.detail_muscles_hint
import com.olarte.ironprogress.ui.resources.detail_no_joints
import com.olarte.ironprogress.ui.resources.detail_not_found
import com.olarte.ironprogress.ui.resources.detail_out_of
import com.olarte.ironprogress.ui.resources.detail_overall
import com.olarte.ironprogress.ui.resources.detail_source
import com.olarte.ironprogress.ui.resources.detail_spine
import com.olarte.ironprogress.ui.resources.detail_technique
import com.olarte.ironprogress.ui.resources.detail_title
import com.olarte.ironprogress.ui.resources.editor_back
import com.olarte.ironprogress.ui.resources.impact_high
import com.olarte.ironprogress.ui.resources.impact_low
import com.olarte.ironprogress.ui.resources.impact_medium
import com.olarte.ironprogress.ui.resources.joint_elbow
import com.olarte.ironprogress.ui.resources.joint_hip
import com.olarte.ironprogress.ui.resources.joint_knee
import com.olarte.ironprogress.ui.resources.joint_lower_back
import com.olarte.ironprogress.ui.resources.joint_shoulder
import com.olarte.ironprogress.ui.resources.joint_wrist
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseDetailRoute(id: String, languageCode: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val model = koinViewModel<ExerciseDetailViewModel>(key = "detail-$id") { parametersOf(id) }
    val state by model.state.collectAsStateWithLifecycle()
    ExerciseDetailScreen(state, languageCode, onBack, onOpen)
}

/** Ficha de ejercicio (PANTALLAS 7.5): músculos, exigencia, impacto, alternativas y fuente. */
@Composable
fun ExerciseDetailScreen(state: ExerciseDetailState, languageCode: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val colors = ForjaTheme.colors
    val type = ForjaTheme.typography
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp5),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(ForjaMeasures.minTouch).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ForjaIcons.ArrowBack,
                    contentDescription = stringResource(Res.string.editor_back),
                    colorFilter = ColorFilter.tint(colors.text1),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(stringResource(Res.string.detail_title), style = type.title3, color = colors.text1)
        }
        val exercise = state.exercise
        when {
            state.loading -> Unit
            exercise == null -> Text(stringResource(Res.string.detail_not_found), style = type.body, color = colors.text2)
            else -> DetailContent(state, exercise, languageCode, onOpen)
        }
    }
}

@Composable
private fun DetailContent(state: ExerciseDetailState, exercise: Exercise, languageCode: String, onOpen: (String) -> Unit) {
    val colors = ForjaTheme.colors
    val type = ForjaTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
        Text(exercise.name.resolve(languageCode), Modifier.semantics { heading() }, style = type.title1, color = colors.text1)
        Text(
            "${exercise.discipline.editorLabel()} · ${exercise.equipment.label()}",
            style = type.body2,
            color = colors.text2,
        )
    }

    if (exercise.muscles.isNotEmpty()) {
        Section(stringResource(Res.string.detail_muscles), stringResource(Res.string.detail_muscles_hint)) {
            val total = exercise.muscles.sumOf { it.workShare ?: 0 }
            exercise.muscles.sortedByDescending { it.workShare ?: 0 }.forEach { item ->
                // Sin porcentaje (ejercicio propio): el principal cuenta 1 y el secundario 0,5, como en la v2.
                val share = item.workShare ?: 0
                val fraction = if (total > 0) share / 100f else item.role.setWeight.toFloat()
                BarRow(item.muscle.label(), fraction, if (total > 0) "$share %" else "")
            }
        }
    }

    val demand = state.facts?.demand
    if (demand != null) {
        Section(stringResource(Res.string.detail_demand)) {
            MeterRow(stringResource(Res.string.detail_overall), demand.overall, 5)
            MeterRow(stringResource(Res.string.detail_technique), demand.technique, 5)
            MeterRow(stringResource(Res.string.detail_spine), demand.spine, 3)
        }
        Section(stringResource(Res.string.detail_joints)) {
            val marked = Joint.entries.filter { demand.impactOn(it) != JointImpact.LOW }
            if (marked.isEmpty()) {
                Text(stringResource(Res.string.detail_no_joints), style = type.body2, color = colors.text2)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                    verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
                ) {
                    marked.sortedByDescending { demand.impactOn(it) }.forEach { JointChip(it, demand.impactOn(it)) }
                }
            }
        }
    }

    if (state.easier != null || state.harder != null) {
        Section(stringResource(Res.string.detail_alternatives)) {
            state.easier?.let { AlternativeRow(stringResource(Res.string.detail_easier), it, languageCode, colors.ice, onOpen) }
            state.harder?.let { AlternativeRow(stringResource(Res.string.detail_harder), it, languageCode, colors.ember, onOpen) }
        }
    }

    state.facts?.source?.let { source ->
        Text(stringResource(Res.string.detail_source, source.source, source.license), style = type.caption, color = colors.text3)
    }
    Spacer(Modifier.height(ForjaSpacing.esp8))
}

@Composable
private fun Section(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2)) {
            Text(title, style = ForjaTheme.typography.title3, color = ForjaTheme.colors.text1)
            if (hint != null) Text(hint, style = ForjaTheme.typography.caption, color = ForjaTheme.colors.text3)
        }
        content()
    }
}

/** Fila de músculo: nombre, barra de 10 dp y porcentaje (columnas 140 / 1fr / 44). */
@Composable
private fun BarRow(label: String, fraction: Float, value: String) {
    val colors = ForjaTheme.colors
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ForjaTheme.typography.body2, color = colors.text1, modifier = Modifier.width(140.dp))
        Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(colors.surface3)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).clip(CircleShape).background(colors.ember))
        }
        Text(
            value,
            style = ForjaTheme.typography.label,
            color = colors.text2,
            modifier = Modifier.width(44.dp).padding(start = ForjaSpacing.esp2),
        )
    }
}

/** Medidor de segmentos de 8 dp (columnas 150 / 1fr / 56). */
@Composable
private fun MeterRow(label: String, value: Int, max: Int) {
    val colors = ForjaTheme.colors
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ForjaTheme.typography.body2, color = colors.text1, modifier = Modifier.width(150.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp1)) {
            repeat(max) { index ->
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(if (index < value) colors.ember else colors.surface3))
            }
        }
        Text(
            stringResource(Res.string.detail_out_of, value, max),
            style = ForjaTheme.typography.label,
            color = colors.text2,
            modifier = Modifier.width(56.dp).padding(start = ForjaSpacing.esp2),
        )
    }
}

@Composable
private fun JointChip(joint: Joint, impact: JointImpact) {
    val colors = ForjaTheme.colors
    val dot: Color = when (impact) {
        JointImpact.HIGH -> colors.gold
        JointImpact.MEDIUM -> colors.text2
        JointImpact.LOW -> colors.activeControl
    }
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(colors.surface2).padding(horizontal = ForjaSpacing.esp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text("${joint.label()} ${impact.label()}", style = ForjaTheme.typography.label, color = colors.text1)
    }
}

@Composable
private fun AlternativeRow(title: String, alternative: ExerciseAlternative, languageCode: String, accent: Color, onOpen: (String) -> Unit) {
    val colors = ForjaTheme.colors
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clip(ForjaRadius.shapeL).background(colors.surface1)
            .clickable { onOpen(alternative.exercise.id.value) }.padding(horizontal = ForjaSpacing.esp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        Column(Modifier.weight(1f)) {
            Text(title, style = ForjaTheme.typography.caption, color = colors.text2)
            Text(alternative.exercise.name.resolve(languageCode), style = ForjaTheme.typography.body, color = colors.text1)
        }
        alternative.overall?.let {
            Text(stringResource(Res.string.detail_out_of, it, 5), style = ForjaTheme.typography.label, color = colors.text2)
        }
    }
}

@Composable
private fun Joint.label(): String = stringResource(
    when (this) {
        Joint.KNEE -> Res.string.joint_knee
        Joint.HIP -> Res.string.joint_hip
        Joint.SHOULDER -> Res.string.joint_shoulder
        Joint.ELBOW -> Res.string.joint_elbow
        Joint.WRIST -> Res.string.joint_wrist
        Joint.LOWER_BACK -> Res.string.joint_lower_back
    },
)

@Composable
private fun JointImpact.label(): String = stringResource(
    when (this) {
        JointImpact.LOW -> Res.string.impact_low
        JointImpact.MEDIUM -> Res.string.impact_medium
        JointImpact.HIGH -> Res.string.impact_high
    },
)
