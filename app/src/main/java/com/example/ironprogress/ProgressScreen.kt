package com.example.ironprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ExerciseViewModel, navController: NavController) {
    // Lista de nombres de ejercicios únicos para el selector
    val exerciseNames by viewModel.uniqueExerciseNames.collectAsState(initial = emptyList())

    // Estado para el ejercicio seleccionado
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MI PROGRESO", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    if (selectedExercise != null) {
                        IconButton(onClick = { selectedExercise = null }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null)
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
                // Cargamos el historial de ese ejercicio específico
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

@Composable
fun AnalysisView(exerciseName: String, history: List<ExerciseEntity>) {
    // 1. PROCESAMIENTO DE DATOS
    // Agrupamos por fecha y nos quedamos con el PESO MÁXIMO de cada sesión
    val dataPoints = remember(history) {
        history.groupBy {
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.date))
        }.map { (_, exercises) ->
            // De cada día, sacamos el objeto con el peso más alto
            exercises.maxByOrNull { it.currentWeight }!!
        }.sortedBy { it.date } // Ordenamos cronológicamente
    }

    val currentSession = dataPoints.last()
    val previousSession = if (dataPoints.size > 1) dataPoints[dataPoints.size - 2] else null

    // Cálculos de Tendencia
    val currentMax = currentSession.currentWeight
    val previousMax = previousSession?.currentWeight ?: currentMax
    val diff = currentMax - previousMax
    val isImprovement = diff > 0
    val isRegression = diff < 0
    val isSame = diff == 0.0

    // Récord Histórico (PR)
    val pr = dataPoints.maxOf { it.currentWeight }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TÍTULO
        Text(exerciseName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Historial de Fuerza (Mejor Serie)", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // TARJETAS DE ESTADÍSTICAS
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Tarjeta 1: PR
            StatCard(
                title = "Récord (PR)",
                value = "${pr.toInt()}", // Asumimos enteros para limpieza visual
                unit = "kg/lb", // Ojalá dinámica, pero genérica por ahora
                icon = Icons.Default.EmojiEvents,
                color = Color(0xFFFFD700), // Dorado
                modifier = Modifier.weight(1f)
            )

            // Tarjeta 2: TENDENCIA (Lo que pediste)
            TrendCard(
                diff = diff,
                currentWeight = currentMax,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // GRÁFICA "DARK GYM"
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Se necesitan al menos 2 sesiones para graficar", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LineChart(dataPoints = dataPoints)
            }
        }
    }
}

// --- GRÁFICA PERSONALIZADA ---
@Composable
fun LineChart(dataPoints: List<ExerciseEntity>) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val padding = 20f

        // Encontrar rangos (Min y Max peso) para escalar la gráfica
        val maxWeight = dataPoints.maxOf { it.currentWeight }.toFloat()
        val minWeight = dataPoints.minOf { it.currentWeight }.toFloat()
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1f) // Evitar división por cero

        // Coordenadas X e Y
        val xStep = width / (dataPoints.size - 1)

        val points = dataPoints.mapIndexed { index, exercise ->
            val normalizedY = (exercise.currentWeight.toFloat() - minWeight) / weightRange
            val x = index * xStep
            val y = height - (normalizedY * height) // Invertir Y porque en Canvas 0 está arriba
            Offset(x, y)
        }

        // 1. DIBUJAR LÍNEA
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                // Curva suave (Bézier cúbica simple) o Línea recta
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        // 2. DIBUJAR PUNTOS Y ETIQUETAS
        points.forEachIndexed { index, point ->
            // Punto
            drawCircle(
                color = Color.White,
                center = point,
                radius = 10f
            )
            drawCircle(
                color = primaryColor,
                center = point,
                radius = 10f,
                style = Stroke(width = 4f)
            )

            // Etiqueta de peso (Solo si cabe o es el último)
            if (index == points.lastIndex || index == 0 || (points.size < 10)) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "${dataPoints[index].currentWeight.toInt()}",
                        point.x,
                        point.y - 25f, // Arriba del punto
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

// --- COMPONENTES UI ---

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
fun StatCard(title: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
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

// 🔥 TARJETA INTELIGENTE DE TENDENCIA
@Composable
fun TrendCard(diff: Double, currentWeight: Double, modifier: Modifier = Modifier) {
    val isPositive = diff > 0
    val isNegative = diff < 0

    val color = when {
        isPositive -> Color(0xFF4CAF50) // Verde
        isNegative -> MaterialTheme.colorScheme.error // Rojo
        else -> Color.Gray
    }

    val icon = when {
        isPositive -> Icons.Rounded.ArrowUpward
        isNegative -> Icons.Rounded.ArrowDownward
        else -> Icons.Rounded.Remove
    }

    val text = when {
        isPositive -> "+${diff.toInt()}"
        isNegative -> "${diff.toInt()}" // El signo ya viene en negativo
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