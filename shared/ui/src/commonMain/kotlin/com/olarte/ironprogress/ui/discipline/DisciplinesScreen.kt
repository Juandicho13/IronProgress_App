package com.olarte.ironprogress.ui.discipline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.ui.catalog.editorLabel
import com.olarte.ironprogress.ui.components.ForjaIcons
import com.olarte.ironprogress.ui.components.ForjaMoreIcons
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.disc_bar
import com.olarte.ironprogress.ui.resources.disc_breathing_detail
import com.olarte.ironprogress.ui.resources.disc_calisthenics_detail
import com.olarte.ironprogress.ui.resources.disc_cardio_detail
import com.olarte.ironprogress.ui.resources.disc_combat_detail
import com.olarte.ironprogress.ui.resources.disc_functional_detail
import com.olarte.ironprogress.ui.resources.disc_hiit
import com.olarte.ironprogress.ui.resources.disc_hiit_detail
import com.olarte.ironprogress.ui.resources.disc_mobility_detail
import com.olarte.ironprogress.ui.resources.disc_pilates_detail
import com.olarte.ironprogress.ui.resources.disc_strength_detail
import com.olarte.ironprogress.ui.resources.disc_subtitle
import com.olarte.ironprogress.ui.resources.disc_title
import com.olarte.ironprogress.ui.resources.disc_yoga_detail
import com.olarte.ironprogress.ui.resources.settings_back
import com.olarte.ironprogress.ui.settings.TopBar
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Adónde lleva cada baldosa de Disciplinas. */
sealed interface DisciplineTarget {
    /** Entreno libre de series (Fuerza y Calistenia). */
    data object Workout : DisciplineTarget

    /** Bloque funcional (7.4). */
    data object Functional : DisciplineTarget

    /** Intervalos a tu medida (5.2): HIIT, Cardio y Combate. */
    data object Intervals : DisciplineTarget

    data class Flow(val flowId: String) : DisciplineTarget

    data object Breathing : DisciplineTarget
}

private class Tile(
    val discipline: Discipline,
    val detail: StringResource,
    val calm: Boolean,
    val target: DisciplineTarget,
    val title: StringResource? = null,
)

private val tiles = listOf(
    Tile(Discipline.STRENGTH, Res.string.disc_strength_detail, false, DisciplineTarget.Workout),
    Tile(Discipline.FUNCTIONAL, Res.string.disc_functional_detail, false, DisciplineTarget.Functional),
    Tile(Discipline.HIIT, Res.string.disc_hiit_detail, false, DisciplineTarget.Intervals, Res.string.disc_hiit),
    Tile(Discipline.CALISTHENICS, Res.string.disc_calisthenics_detail, false, DisciplineTarget.Workout),
    Tile(Discipline.YOGA, Res.string.disc_yoga_detail, true, DisciplineTarget.Flow("sun-salutation-a")),
    Tile(Discipline.PILATES, Res.string.disc_pilates_detail, true, DisciplineTarget.Flow("pilates-mat")),
    Tile(Discipline.MOBILITY, Res.string.disc_mobility_detail, true, DisciplineTarget.Flow("mobility-warmup")),
    Tile(Discipline.BREATHING, Res.string.disc_breathing_detail, true, DisciplineTarget.Breathing),
    Tile(Discipline.CARDIO, Res.string.disc_cardio_detail, false, DisciplineTarget.Intervals),
    Tile(Discipline.COMBAT, Res.string.disc_combat_detail, false, DisciplineTarget.Intervals),
)

/**
 * Disciplinas (PANTALLAS 7.1): empezar un entreno de cualquier disciplina desde Plan. La fila
 * "Ver todas" espera al catálogo de F5 y no se muestra todavía.
 */
@Composable
fun DisciplinesScreen(onBack: () -> Unit, onOpen: (DisciplineTarget) -> Unit) {
    val colors = ForjaTheme.colors
    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp3),
        verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        TopBar(stringResource(Res.string.disc_bar), ForjaMoreIcons.ChevronLeft, stringResource(Res.string.settings_back), onBack)
        Text(
            stringResource(Res.string.disc_title),
            modifier = Modifier.semantics { heading() },
            style = ForjaTheme.typography.title1.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            color = colors.text1,
        )
        Text(stringResource(Res.string.disc_subtitle), style = ForjaTheme.typography.body, color = colors.text2)
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3)) {
                row.forEach { tile -> DisciplineTile(tile, Modifier.weight(1f)) { onOpen(tile.target) } }
            }
        }
    }
}

@Composable
private fun DisciplineTile(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    val colors = ForjaTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, ForjaTheme.motion.spatialFast())
    Column(
        modifier.height(96.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
            .clip(ForjaRadius.shapeM).background(colors.surface1)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (tile.calm) colors.iceVeil else colors.emberVeil),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tile.icon(),
                contentDescription = null,
                tint = if (tile.calm) colors.ice else colors.ember,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.fillMaxWidth()) {
            Text(
                tile.title?.let {
                    stringResource(it)
                } ?: tile.discipline.editorLabel(),
                style = ForjaTheme.typography.label,
                color = colors.text1,
            )
            Text(stringResource(tile.detail), style = ForjaTheme.typography.caption, color = colors.text2, maxLines = 1)
        }
    }
}

@Composable
private fun Tile.icon(): Painter = when (discipline) {
    Discipline.STRENGTH -> ForjaMoreIcons.FitnessCenter
    Discipline.FUNCTIONAL -> ForjaIcons.FireOutline
    Discipline.HIIT -> ForjaIcons.Timer
    Discipline.CALISTHENICS -> ForjaMoreIcons.SportsGymnastics
    Discipline.YOGA -> ForjaMoreIcons.SelfImprovement
    Discipline.PILATES -> ForjaMoreIcons.AccessibilityNew
    Discipline.MOBILITY -> ForjaMoreIcons.Spa
    Discipline.BREATHING -> ForjaMoreIcons.Air
    Discipline.CARDIO -> ForjaMoreIcons.DirectionsRun
    else -> ForjaMoreIcons.SportsMma
}
