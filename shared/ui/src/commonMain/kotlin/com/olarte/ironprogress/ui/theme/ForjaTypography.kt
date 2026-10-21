package com.olarte.ironprogress.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

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
