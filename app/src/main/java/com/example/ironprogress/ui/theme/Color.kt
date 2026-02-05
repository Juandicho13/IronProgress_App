package com.example.ironprogress.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sistema de Diseño IronProgress - Paleta de Colores.
 *
 * Este archivo define los valores hexadecimales (hex) utilizados para establecer
 * la estética "Dark Gym". Estos colores son referenciados en [Theme.kt]
 * para mapear los roles de Material Design.
 */

//Region: Identidad de Marca (Colores Primarios)
/**
 * Color principal de la marca. Se utiliza para botones de llamada a la acción (CTA),
 * estados activos y elementos visuales clave.
 */
val IronRed = Color(0xFFE53935)

/**
 * Variante más oscura del color principal, adecuada para degradados
 * o estados de "presionado" para proporcionar profundidad visual.
 */
val IronRedDark = Color(0xFFB71C1C)

/**
 * Contenedor de fondo rojo tenue. Se utiliza para resaltar sutilmente
 * elementos activos sin comprometer el contraste del texto.
 */
val IronRedContainer = Color(0xFF3E0000)

//Region: Fondos y Superficies (Base Tema Oscuro)
/**
 * Fondo negro puro. Optimizado para pantallas AMOLED/OLED
 * para minimizar el consumo de batería durante entrenamientos largos.
 */
val IronBlack = Color(0xFF000000)

/**
 * Color de fondo secundario, típicamente usado para fondos de Scaffold
 * o detrás de elementos de lista para diferenciar del negro puro.
 */
val IronDarkGrey = Color(0xFF121212)

/**
 * Color de superficie estándar para Tarjetas (Cards), Diálogos y Menús.
 * Proporciona separación de elevación respecto al fondo.
 */
val IronSurface = Color(0xFF1E1E1E)

/**
 * Variante de superficie más clara. Se utiliza para elementos seleccionados,
 * tarjetas destacadas o elementos que requieren mayor jerarquía visual.
 */
val IronSurfaceVariant = Color(0xFF2C2C2E)

//Region: Tipografía y Contenido
/**
 * Color de alto énfasis para Títulos, Encabezados y valores de datos primarios.
 */
val IronWhite = Color(0xFFFFFFFF)

/**
 * Color de énfasis medio (Subtítulo 1/2). Se utiliza para texto de apoyo,
 * etiquetas e íconos inactivos para reducir el ruido visual.
 */
val IronGreyText = Color(0xFFB0B3B8)

//Region: Estado y Utilidad
/**
 * Color de acento reservado para Logros, Récords Personales (1RM)
 * e indicadores de funciones Premium.
 */
val IronGold = Color(0xFFFFD700)

/**
 * Color semántico para estados de éxito, tendencias positivas en gráficos
 * e indicadores de temporizador activo.
 */
val IronGreen = Color(0xFF4CAF50)