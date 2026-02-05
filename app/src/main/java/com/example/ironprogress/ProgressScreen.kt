package com.example.ironprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Análisis de Progreso y Evolución.
 *
 * Ofrece al usuario dos vistas principales:
 * 1. Selector de Ejercicio: Lista de todos los ejercicios registrados históricamente.
 * 2. Vista de Análisis (Dashboard):
 * - Métricas clave (KPIs): Récord Personal (PR) y Tendencia vs Sesión anterior.
 * - Gráfica de Línea: Evolución visual de la fuerza a lo largo del tiempo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ExerciseViewModel, navController: NavController) {

    //region --- Estado de Datos (Data State) ---
    // Lista reactiva de nombres únicos para el selector.
    val exerciseNames by viewModel.uniqueExerciseNames.collectAsState(initial = emptyList())

    // Estado local: Si es null, muestra el selector; si tiene valor, muestra el análisis de ese ejercicio.
    var selectedExercise by remember { mutableStateOf<String?>(null) }
    //endregion

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MI PROGRESO", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    if (selectedExercise != null) {
                        // Si estamos en detalle, el botón "Atrás" limpia la selección (vuelve a la lista)
                        IconButton(onClick = { selectedExercise = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver a lista")
                        }
                    } else {
                        // Si estamos en la lista, el botón "Atrás" sale de la pantalla (vuelve al Dashboard)
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Salir")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (selectedExercise == null) {
                // --- VISTA 1: SELECTOR DE EJERCICIO ---
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Selecciona un ejercicio para analizar:",
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (exerciseNames.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay historial aún.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(exerciseNames) { name ->
                                ExerciseSelectorCard(name = name) {
                                    selectedExercise = name
                                }
                            }
                        }
                    }
                }
            } else {
                // --- VISTA 2: ANÁLISIS DETALLADO ---
                // Cargamos dinámicamente el historial completo del ejercicio seleccionado.
                // El operador !! es seguro aquí porque selectedExercise no es null en este bloque else.
                val history by viewModel.getExerciseHistory(selectedExercise!!).collectAsState(initial = emptyList())

                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    AnalysisView(exerciseName = selectedExercise!!, history = history)
                }
            }
        }
    }
}

/**
 * Sub-pantalla que renderiza el dashboard de análisis para un ejercicio específico.
 */
@Composable
fun AnalysisView(exerciseName: String, history: List<ExerciseEntity>) {

    //region --- Procesamiento de Datos (Business Logic) ---
    // Agrupamos el historial por fecha (día) y extraemos la "Mejor Serie" (Max Weight) de cada sesión.
    // Esto normaliza los datos para la gráfica, evitando múltiples puntos por día.
    val dataPoints = remember(history) {
        history.groupBy {
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.date))
        }.map { (_, exercises) ->
            exercises.maxByOrNull { it.currentWeight }!!
        }.sortedBy { it.date } // Orden cronológico ascendente para la gráfica
    }

    // Datos para comparación de tendencias
    val currentSession = dataPoints.last()
    val previousSession = if (dataPoints.size > 1) dataPoints[dataPoints.size - 2] else null

    // Cálculo de Delta (Diferencia)
    val currentMax = currentSession.currentWeight
    val previousMax = previousSession?.currentWeight ?: currentMax
    val diff = currentMax - previousMax

    // Récord Histórico Absoluto (PR)
    val pr = dataPoints.maxOf { it.currentWeight }
    //endregion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Encabezado del Reporte
        Text(
            exerciseName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Historial de Fuerza (Mejor Serie)", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de KPIs (Tarjetas de Estadísticas)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // KPI 1: Récord Personal
            StatCard(
                title = "Récord (PR)",
                value = "${pr.toInt()}", // Simplificación visual a enteros
                unit = "kg/lb",
                icon = Icons.Default.EmojiEvents,
                color = Color(0xFFFFD700), // Dorado (IronGold)
                modifier = Modifier.weight(1f)
            )

            // KPI 2: Tendencia Reciente
            TrendCard(
                diff = diff,
                currentWeight = currentMax,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sección de Visualización de Datos (Gráfica)
        Text("Evolución en el tiempo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (dataPoints.size < 2) {
                // Estado vacío para gráficas insuficientes
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Se necesitan al menos 2 sesiones para graficar", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LineChart(dataPoints = dataPoints)
            }
        }
    }
}

/**
 * Componente de Gráfica Lineal personalizado utilizando Canvas.
 * Dibuja la evolución del peso levantado a lo largo del tiempo.
 */
@Composable
fun LineChart(dataPoints: List<ExerciseEntity>) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Determinar escalas (Y Axis)
        val maxWeight = dataPoints.maxOf { it.currentWeight }.toFloat()
        val minWeight = dataPoints.minOf { it.currentWeight }.toFloat()
        // Rango de peso para normalización (evita división por cero)
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

        // Determinar paso horizontal (X Axis)
        val xStep = width / (dataPoints.size - 1)

        // Mapeo de datos a coordenadas de pantalla (Píxeles)
        val points = dataPoints.mapIndexed { index, exercise ->
            val normalizedY = (exercise.currentWeight.toFloat() - minWeight) / weightRange
            val x = index * xStep
            // Inversión del eje Y (Canvas 0,0 es top-left)
            val y = height - (normalizedY * height)
            Offset(x, y)
        }

        // 1. Trazado de la Línea de Tendencia
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        // 2. Renderizado de Puntos de Datos y Etiquetas
        points.forEachIndexed { index, point ->
            // Círculo exterior (Borde)
            drawCircle(
                color = Color.White,
                center = point,
                radius = 10f
            )
            // Círculo interior (Relleno)
            drawCircle(
                color = primaryColor,
                center = point,
                radius = 10f,
                style = Stroke(width = 4f)
            )

            // Etiquetas de Texto (Valores)
            // Se muestran solo para el primer punto, el último, o si hay pocos datos (evita superposición)
            if (index == points.lastIndex || index == 0 || (points.size < 10)) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "${dataPoints[index].currentWeight.toInt()}",
                        point.x,
                        point.y - 25f, // Offset vertical superior
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }
    }
}

//region --- Componentes UI Reutilizables ---

@Composable
fun ExerciseSelectorCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

/**
 * Tarjeta de Tendencia Inteligente.
 * Calcula visualmente si el usuario ha mejorado, empeorado o mantenido su rendimiento.
 */
@Composable
fun TrendCard(diff: Double, currentWeight: Double, modifier: Modifier = Modifier) {
    val isPositive = diff > 0
    val isNegative = diff < 0

    val color = when {
        isPositive -> Color(0xFF4CAF50) // Verde Éxito
        isNegative -> MaterialTheme.colorScheme.error // Rojo Alerta
        else -> Color.Gray // Neutro
    }

    val icon = when {
        isPositive -> Icons.Rounded.ArrowUpward
        isNegative -> Icons.Rounded.ArrowDownward
        else -> Icons.Rounded.Remove
    }

    val text = when {
        isPositive -> "+${diff.toInt()}"
        isNegative -> "${diff.toInt()}" // El signo negativo es implícito en el valor
        else -> "Igual"
    }

    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tendencia", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
                    if(diff != 0.0) {
                        Text("vs anterior", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
//endregion