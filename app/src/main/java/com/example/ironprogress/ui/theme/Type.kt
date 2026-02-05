package com.example.ironprogress.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Configuración de tipografía Material 3
val Typography = Typography(

    // TÍTULOS GRANDES (Ej: "IronProgress" en el home)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black, // Letra muy gruesa
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp // Espaciado para toque moderno
    ),

    // TÍTULOS MEDIANOS (Ej: Encabezados de Rutina)
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp
    ),

    // SUBTÍTULOS Y BOTONES GRANDES
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // TEXTO NORMAL (Cuerpo)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // TEXTO PEQUEÑO (Detalles, series, reps)
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
)