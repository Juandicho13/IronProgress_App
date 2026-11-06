package com.olarte.ironprogress.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.ic_accessibility_new
import com.olarte.ironprogress.ui.resources.ic_account_circle
import com.olarte.ironprogress.ui.resources.ic_air
import com.olarte.ironprogress.ui.resources.ic_chevron_left
import com.olarte.ironprogress.ui.resources.ic_directions_run
import com.olarte.ironprogress.ui.resources.ic_fitness_center
import com.olarte.ironprogress.ui.resources.ic_info
import com.olarte.ironprogress.ui.resources.ic_ios_share
import com.olarte.ironprogress.ui.resources.ic_pause
import com.olarte.ironprogress.ui.resources.ic_self_improvement
import com.olarte.ironprogress.ui.resources.ic_skip_next
import com.olarte.ironprogress.ui.resources.ic_spa
import com.olarte.ironprogress.ui.resources.ic_sports_gymnastics
import com.olarte.ironprogress.ui.resources.ic_sports_mma
import com.olarte.ironprogress.ui.resources.ic_volume_off
import com.olarte.ironprogress.ui.resources.ic_volume_up
import org.jetbrains.compose.resources.painterResource

/** Íconos de Disciplinas, Respiración y Ajustes (F1-20 y F1-25), con las mismas reglas que [ForjaIcons]. */
object ForjaMoreIcons {
    val AccountCircle: Painter
        @Composable get() = painterResource(Res.drawable.ic_account_circle)

    val AccessibilityNew: Painter
        @Composable get() = painterResource(Res.drawable.ic_accessibility_new)

    val Air: Painter
        @Composable get() = painterResource(Res.drawable.ic_air)

    val ChevronLeft: Painter
        @Composable get() = painterResource(Res.drawable.ic_chevron_left)

    val DirectionsRun: Painter
        @Composable get() = painterResource(Res.drawable.ic_directions_run)

    val FitnessCenter: Painter
        @Composable get() = painterResource(Res.drawable.ic_fitness_center)

    val Info: Painter
        @Composable get() = painterResource(Res.drawable.ic_info)

    val IosShare: Painter
        @Composable get() = painterResource(Res.drawable.ic_ios_share)

    val Pause: Painter
        @Composable get() = painterResource(Res.drawable.ic_pause)

    val SelfImprovement: Painter
        @Composable get() = painterResource(Res.drawable.ic_self_improvement)

    val SkipNext: Painter
        @Composable get() = painterResource(Res.drawable.ic_skip_next)

    val Spa: Painter
        @Composable get() = painterResource(Res.drawable.ic_spa)

    val SportsGymnastics: Painter
        @Composable get() = painterResource(Res.drawable.ic_sports_gymnastics)

    val SportsMma: Painter
        @Composable get() = painterResource(Res.drawable.ic_sports_mma)

    val VolumeOff: Painter
        @Composable get() = painterResource(Res.drawable.ic_volume_off)

    val VolumeUp: Painter
        @Composable get() = painterResource(Res.drawable.ic_volume_up)
}
