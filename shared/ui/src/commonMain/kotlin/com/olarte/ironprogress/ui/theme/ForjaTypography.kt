package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.archivo_bold
import com.olarte.ironprogress.ui.resources.archivo_medium
import com.olarte.ironprogress.ui.resources.archivo_regular
import com.olarte.ironprogress.ui.resources.archivo_semibold
import com.olarte.ironprogress.ui.resources.archivo_semiexpanded_bold
import com.olarte.ironprogress.ui.resources.archivo_semiexpanded_semibold
import org.jetbrains.compose.resources.Font

/**
 * Escala tipografica de Forja: grupo de texto y grupo de cifras con soporte tabular (tnum).
 */
@Immutable
data class ForjaTypography(
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val body: TextStyle,
    val body2: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val tab: TextStyle,
    val tabSelected: TextStyle,
    val heroFigure: TextStyle,
    val largeFigure: TextStyle,
    val figure: TextStyle,
    val smallFigure: TextStyle,
    val primaryButton: TextStyle,
    val secondaryButton: TextStyle,
)

fun createForjaTypography(
    textFamily: FontFamily = FontFamily.SansSerif,
    figureFamily: FontFamily = FontFamily.SansSerif,
): ForjaTypography = ForjaTypography(
    title1 = TextStyle(
        fontFamily = textFamily,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
    ),
    title2 = TextStyle(
        fontFamily = textFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    title3 = TextStyle(
        fontFamily = textFamily,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    body = TextStyle(
        fontFamily = textFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    body2 = TextStyle(
        fontFamily = textFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    label = TextStyle(
        fontFamily = textFamily,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.01.em,
    ),
    caption = TextStyle(
        fontFamily = textFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.04.em,
    ),
    tab = TextStyle(
        fontFamily = textFamily,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
    tabSelected = TextStyle(
        fontFamily = textFamily,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    heroFigure = TextStyle(
        fontFamily = figureFamily,
        fontSize = 72.sp,
        lineHeight = 76.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    ),
    largeFigure = TextStyle(
        fontFamily = figureFamily,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    ),
    figure = TextStyle(
        fontFamily = figureFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    smallFigure = TextStyle(
        fontFamily = figureFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    primaryButton = TextStyle(
        fontFamily = textFamily,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    secondaryButton = TextStyle(
        fontFamily = textFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

internal val LocalForjaTypography = staticCompositionLocalOf { createForjaTypography() }

/**
 * Tipografía con Archivo (OFL 1.1, licencia en licenses/Archivo-OFL.txt). Textos con Archivo
 * de ancho normal; cifras con "Archivo Cifras", la instancia SemiExpanded (ancho 112,5).
 * Los TTF estáticos salen de la fuente variable de Google Fonts.
 */
@Composable
fun rememberForjaTypography(): ForjaTypography {
    val textFamily = FontFamily(
        Font(Res.font.archivo_regular, FontWeight.Normal),
        Font(Res.font.archivo_medium, FontWeight.Medium),
        Font(Res.font.archivo_semibold, FontWeight.SemiBold),
        Font(Res.font.archivo_bold, FontWeight.Bold),
    )
    val figureFamily = FontFamily(
        Font(Res.font.archivo_semiexpanded_semibold, FontWeight.SemiBold),
        Font(Res.font.archivo_semiexpanded_bold, FontWeight.Bold),
    )
    return remember(textFamily, figureFamily) { createForjaTypography(textFamily, figureFamily) }
}
