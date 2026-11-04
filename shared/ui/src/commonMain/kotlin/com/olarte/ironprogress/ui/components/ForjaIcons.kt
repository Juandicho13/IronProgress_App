package com.olarte.ironprogress.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.ic_add
import com.olarte.ironprogress.ui.resources.ic_arrow_back
import com.olarte.ironprogress.ui.resources.ic_calendar_month
import com.olarte.ironprogress.ui.resources.ic_calendar_month_filled
import com.olarte.ironprogress.ui.resources.ic_check
import com.olarte.ironprogress.ui.resources.ic_chevron_right
import com.olarte.ironprogress.ui.resources.ic_close
import com.olarte.ironprogress.ui.resources.ic_local_fire_department
import com.olarte.ironprogress.ui.resources.ic_local_fire_department_filled
import com.olarte.ironprogress.ui.resources.ic_monitoring
import com.olarte.ironprogress.ui.resources.ic_monitoring_filled
import com.olarte.ironprogress.ui.resources.ic_more_vert
import com.olarte.ironprogress.ui.resources.ic_north_east
import com.olarte.ironprogress.ui.resources.ic_play_arrow_filled
import com.olarte.ironprogress.ui.resources.ic_remove
import com.olarte.ironprogress.ui.resources.ic_search
import com.olarte.ironprogress.ui.resources.ic_timer
import org.jetbrains.compose.resources.painterResource

/**
 * Íconos de Forja: Material Symbols Rounded, peso 400, en composeResources/drawable.
 * El relleno (Filled) va solo en el elemento elegido. Se pintan con el tint de Icon.
 */
object ForjaIcons {
    val FireOutline: Painter
        @Composable get() = painterResource(Res.drawable.ic_local_fire_department)

    val FireFilled: Painter
        @Composable get() = painterResource(Res.drawable.ic_local_fire_department_filled)

    val CalendarOutline: Painter
        @Composable get() = painterResource(Res.drawable.ic_calendar_month)

    val CalendarFilled: Painter
        @Composable get() = painterResource(Res.drawable.ic_calendar_month_filled)

    val MonitoringOutline: Painter
        @Composable get() = painterResource(Res.drawable.ic_monitoring)

    val MonitoringFilled: Painter
        @Composable get() = painterResource(Res.drawable.ic_monitoring_filled)

    val Search: Painter
        @Composable get() = painterResource(Res.drawable.ic_search)

    val NorthEast: Painter
        @Composable get() = painterResource(Res.drawable.ic_north_east)

    val PlayFilled: Painter
        @Composable get() = painterResource(Res.drawable.ic_play_arrow_filled)

    val Check: Painter
        @Composable get() = painterResource(Res.drawable.ic_check)

    val Close: Painter
        @Composable get() = painterResource(Res.drawable.ic_close)

    val ArrowBack: Painter
        @Composable get() = painterResource(Res.drawable.ic_arrow_back)

    val ChevronRight: Painter
        @Composable get() = painterResource(Res.drawable.ic_chevron_right)

    val Timer: Painter
        @Composable get() = painterResource(Res.drawable.ic_timer)

    val Add: Painter
        @Composable get() = painterResource(Res.drawable.ic_add)

    val Remove: Painter
        @Composable get() = painterResource(Res.drawable.ic_remove)

    val MoreVert: Painter
        @Composable get() = painterResource(Res.drawable.ic_more_vert)
}
