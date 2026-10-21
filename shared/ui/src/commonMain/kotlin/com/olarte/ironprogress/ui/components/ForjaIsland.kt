package com.olarte.ironprogress.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaMotion
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme

/** Pestanas principales de la Isla de navegacion. */
enum class ForjaTab {
    TODAY,
    PLAN,
    PROGRESS,
}

/** Estado resumido de la sesion en curso para mostrar en el Accesorio sobre la Isla. */
data class ActiveSessionBannerState(
    val exerciseName: String,
    val setSummary: String,
    val statusText: String,
    val progress: Float,
    val isReady: Boolean = false,
)

/**
 * La Isla de navegacion flotante de Forja con soporte para el Accesorio de sesion encima.
 */
@Composable
fun ForjaIsland(
    selectedTab: ForjaTab,
    isSearchActive: Boolean,
    isMinimized: Boolean,
    todayLabel: String,
    planLabel: String,
    progressLabel: String,
    searchContentDescription: String,
    activeSession: ActiveSessionBannerState? = null,
    onOpenActiveSession: () -> Unit = {},
    onSkipRest: () -> Unit = {},
    onTabSelected: (ForjaTab) -> Unit,
    onSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ForjaTheme.colors

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Borde de desplazamiento: fundido progresivo sobre la isla (96 dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (activeSession != null) 156.dp else 96.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.background.copy(alpha = 0.85f),
                            colors.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = ForjaSpacing.esp5,
                    end = ForjaSpacing.esp5,
                    bottom = ForjaMeasures.islandMargin,
                ),
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp2),
        ) {
            // Accesorio de sesion: aparece 8 dp encima de la isla cuando hay un entreno en curso
            AnimatedVisibility(
                visible = activeSession != null,
                enter = fadeIn(ForjaMotion.effectFast()) + slideInVertically(ForjaMotion.spatial()) { it / 2 },
                exit = fadeOut(ForjaMotion.effectFast()) + slideOutVertically(ForjaMotion.spatial()) { it / 2 },
            ) {
                if (activeSession != null) {
                    ForjaSessionAccessory(
                        state = activeSession,
                        onClick = onOpenActiveSession,
                        onSkipRest = onSkipRest,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pildora de pestanas (se encoge a un circulo al minimizarse)
                AnimatedContent(
                    targetState = isMinimized,
                    transitionSpec = {
                        fadeIn(animationSpec = ForjaMotion.effectFast()) togetherWith
                                fadeOut(animationSpec = ForjaMotion.effectFast()) using
                                SizeTransform(
                                    clip = false,
                                    sizeAnimationSpec = { _, _ -> ForjaMotion.spatial() },
                                )
                    },
                    modifier = Modifier.weight(1f, fill = !isMinimized),
                    label = "ForjaIslandMorph",
                ) { minimized ->
                    if (minimized) {
                        Box(
                            modifier = Modifier
                                .size(ForjaMeasures.islandHeight)
                                .shadow(16.dp, CircleShape)
                                .clip(CircleShape)
                                .background(colors.glass)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .clickable { onTabSelected(selectedTab) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = selectedTab.icon(filled = true),
                                contentDescription = null,
                                tint = colors.emberInk,
                                modifier = Modifier.size(ForjaMeasures.iconSize),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ForjaMeasures.islandHeight)
                                .shadow(16.dp, CircleShape)
                                .clip(CircleShape)
                                .background(colors.glass)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val tabs = listOf(
                                ForjaTab.TODAY to todayLabel,
                                ForjaTab.PLAN to planLabel,
                                ForjaTab.PROGRESS to progressLabel,
                            )
                            tabs.forEach { (tab, label) ->
                                val isSelected = selectedTab == tab && !isSearchActive
                                IslandTabItem(
                                    tab = tab,
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = { onTabSelected(tab) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(ForjaSpacing.esp2))

                // Circulo aparte de 64 dp para Buscar
                val searchInteraction = remember { MutableInteractionSource() }
                val searchPressed by searchInteraction.collectIsPressedAsState()
                val searchScale by animateFloatAsState(
                    targetValue = if (searchPressed) 0.95f else 1f,
                    animationSpec = ForjaMotion.spatialFast(),
                    label = "SearchButtonScale",
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = searchScale
                            scaleY = searchScale
                        }
                        .size(ForjaMeasures.islandHeight)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isSearchActive) colors.emberVeil else colors.glass)
                        .border(
                            width = 1.dp,
                            color = if (isSearchActive) colors.emberInk else colors.glassBorder,
                            shape = CircleShape,
                        )
                        .clickable(
                            interactionSource = searchInteraction,
                            indication = null,
                            onClick = onSearchClicked,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ForjaIcons.Search,
                        contentDescription = searchContentDescription,
                        tint = if (isSearchActive) colors.emberInk else colors.text1,
                        modifier = Modifier.size(ForjaMeasures.iconSize),
                    )
                }
            }
        }
    }
}

/**
 * Capsula de vidrio de 56 dp que mantiene visible el entreno en curso cuando sales de el.
 */
@Composable
private fun ForjaSessionAccessory(
    state: ActiveSessionBannerState,
    onClick: () -> Unit,
    onSkipRest: () -> Unit,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val ringColor = if (state.isReady) colors.ember else colors.ice
    val trackColor = colors.surface3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ForjaMeasures.accessoryHeight)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.glass)
            .border(1.dp, colors.glassBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForjaSpacing.esp3),
    ) {
        // Anillo del descanso de 40 dp con trazo de 4 dp
        Box(
            modifier = Modifier.size(ForjaMeasures.restRingSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(ForjaMeasures.restRingSize)) {
                val strokeWidthPx = 4.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx),
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * state.progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${state.exerciseName} · ${state.setSummary}",
                style = typography.body2,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.statusText,
                style = typography.label,
                color = if (state.isReady) colors.emberInk else colors.iceInk,
                maxLines = 1,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface3)
                .clickable(onClick = onSkipRest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "›|",
                style = typography.label,
                color = colors.text1,
            )
        }
    }
}

@Composable
private fun IslandTabItem(
    tab: ForjaTab,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = ForjaMotion.spatialFast(),
        label = "TabScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (isSelected) colors.glassSelection else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ForjaSpacing.esp0_5),
        ) {
            Icon(
                imageVector = tab.icon(filled = isSelected),
                contentDescription = label,
                tint = if (isSelected) colors.emberInk else colors.text2,
                modifier = Modifier.size(ForjaMeasures.iconSize),
            )
            Text(
                text = label,
                style = if (isSelected) typography.tabSelected else typography.tab,
                color = if (isSelected) colors.text1 else colors.text2,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private fun ForjaTab.icon(filled: Boolean): ImageVector = when (this) {
    ForjaTab.TODAY -> if (filled) ForjaIcons.FireFilled else ForjaIcons.FireOutline
    ForjaTab.PLAN -> if (filled) ForjaIcons.CalendarFilled else ForjaIcons.CalendarOutline
    ForjaTab.PROGRESS -> if (filled) ForjaIcons.MonitoringFilled else ForjaIcons.MonitoringOutline
}
