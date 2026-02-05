package com.example.ironprogress.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Configuración del Sistema Tipográfico (Typography).
 *
 * Define la jerarquía de estilos de texto para la aplicación, alineada con
 * los tokens de Material Design 3. Se prioriza la legibilidad y el peso visual
 * (FontWeight.Black/Bold) para reforzar la identidad agresiva del "Dark Gym".
 */
val Typography = Typography(

    /**
     * Encabezado Grande (Headline Large).
     * Reservado para la identidad de marca (ej. pantalla de inicio) y estados
     * de alto impacto visual. Utiliza tracking extendido para un look moderno.
     */
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp
    ),

    /**
     * Encabezado Mediano (Headline Medium).
     * Utilizado para títulos de secciones principales, nombres de rutinas
     * y encabezados de tarjetas de alto nivel.
     */
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp
    ),

    /**
     * Título Mediano (Title Medium).
     * Estilo secundario para subtítulos, botones de acción prominentes
     * y elementos de navegación.
     */
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    /**
     * Cuerpo Mediano (Body Medium).
     * Estilo estándar para lectura de contenido, instrucciones y párrafos.
     * Balanceado para una lectura cómoda en listas.
     */
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    /**
     * Cuerpo Pequeño (Body Small).
     * Utilizado para metadatos, contadores de series/reps, etiquetas
     * y elementos de interfaz densos.
     */
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
)