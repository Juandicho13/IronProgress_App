package com.olarte.ironprogress.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos vectoriales del sistema Forja (trazos redondeados de 24 dp, contorno y relleno).
 */
object ForjaIcons {

    val FireOutline: ImageVector = icon("FireOutline") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            curveTo(14.5f, 6.2f, 18f, 8.8f, 18f, 13.5f)
            curveTo(18f, 17.1f, 15.3f, 20f, 12f, 20f)
            curveTo(8.7f, 20f, 6f, 17.1f, 6f, 13.5f)
            curveTo(6f, 10.5f, 7.8f, 8.4f, 9.5f, 6.8f)
            curveTo(9.8f, 8.3f, 10.7f, 9.2f, 11.5f, 9.5f)
            curveTo(11f, 7.2f, 11.2f, 4.8f, 12f, 3f)
            close()
        }
    }

    val FireFilled: ImageVector = icon("FireFilled") {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            curveTo(14.5f, 6.2f, 18f, 8.8f, 18f, 13.5f)
            curveTo(18f, 17.1f, 15.3f, 20f, 12f, 20f)
            curveTo(8.7f, 20f, 6f, 17.1f, 6f, 13.5f)
            curveTo(6f, 10.5f, 7.8f, 8.4f, 9.5f, 6.8f)
            curveTo(9.8f, 8.3f, 10.7f, 9.2f, 11.5f, 9.5f)
            curveTo(11f, 7.2f, 11.2f, 4.8f, 12f, 3f)
            close()
        }
    }

    val CalendarOutline: ImageVector = icon("CalendarOutline") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
            lineTo(20f, 18f)
            curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
            lineTo(6f, 20f)
            curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
            lineTo(4f, 7f)
            curveTo(4f, 5.9f, 4.9f, 5f, 6f, 5f)
            close()
            moveTo(8f, 3f)
            lineTo(8f, 6.5f)
            moveTo(16f, 3f)
            lineTo(16f, 6.5f)
            moveTo(4f, 9.5f)
            lineTo(20f, 9.5f)
        }
    }

    val CalendarFilled: ImageVector = icon("CalendarFilled") {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
            lineTo(20f, 18f)
            curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
            lineTo(6f, 20f)
            curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
            lineTo(4f, 7f)
            curveTo(4f, 5.9f, 4.9f, 5f, 6f, 5f)
            close()
        }
    }

    val MonitoringOutline: ImageVector = icon("MonitoringOutline") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 19f)
            lineTo(5f, 15f)
            moveTo(9.5f, 19f)
            lineTo(9.5f, 12f)
            moveTo(14f, 19f)
            lineTo(14f, 14f)
            moveTo(18.5f, 19f)
            lineTo(18.5f, 9f)
            moveTo(5f, 11f)
            lineTo(9.5f, 7.5f)
            lineTo(14f, 9.5f)
            lineTo(19f, 5f)
        }
    }

    val MonitoringFilled: ImageVector = icon("MonitoringFilled") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 19f)
            lineTo(5f, 14.5f)
            moveTo(9.5f, 19f)
            lineTo(9.5f, 11.5f)
            moveTo(14f, 19f)
            lineTo(14f, 13.5f)
            moveTo(18.5f, 19f)
            lineTo(18.5f, 8.5f)
            moveTo(5f, 10.5f)
            lineTo(9.5f, 7f)
            lineTo(14f, 9f)
            lineTo(19f, 4.5f)
        }
    }

    val Search: ImageVector = icon("Search") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(11f, 5f)
            curveTo(14.3f, 5f, 17f, 7.7f, 17f, 11f)
            curveTo(17f, 14.3f, 14.3f, 17f, 11f, 17f)
            curveTo(7.7f, 17f, 5f, 14.3f, 5f, 11f)
            curveTo(5f, 7.7f, 7.7f, 5f, 11f, 5f)
            close()
            moveTo(15.5f, 15.5f)
            lineTo(19.5f, 19.5f)
        }
    }

    val NorthEast: ImageVector = icon("NorthEast") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 17f)
            lineTo(17f, 7f)
            moveTo(9f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 15f)
        }
    }

    val PlayFilled: ImageVector = icon("PlayFilled") {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(8f, 6f)
            lineTo(18f, 12f)
            lineTo(8f, 18f)
            close()
        }
    }

    val Check: ImageVector = icon("Check") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5.5f, 12.5f)
            lineTo(10f, 17f)
            lineTo(18.5f, 7.5f)
        }
    }

    val Close: ImageVector = icon("Close") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(7f, 7f)
            lineTo(17f, 17f)
            moveTo(17f, 7f)
            lineTo(7f, 17f)
        }
    }

    private fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()
}
