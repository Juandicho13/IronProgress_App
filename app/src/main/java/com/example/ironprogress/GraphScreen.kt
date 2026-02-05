package com.example.ironprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Análisis y Evolución (Analytics).
 *
 * Visualiza el progreso del usuario mediante:
 * 1. KPI de Récord Personal (PR) con refuerzo positivo (Trofeo).
 * 2. Gráfica de Tendencia Lineal (Canvas) con relleno degradado.
 * 3. Historial detallado de sesiones anteriores.
 *
 * @param exerciseName Nombre del ejercicio a analizar (pasado como argumento de navegación).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(exerciseName: String, viewModel: ExerciseViewModel, navController: NavController) {

    // --- Data Collection ---
    // Recolectamos todo el historial de este ejercicio específico
    val history by viewModel.getExerciseHistory(exerciseName).collectAsState(initial = emptyList())

    // 1. Procesamiento de Datos para la Gráfica (Business Logic in UI Layer for simplicity)
    // Agrupamos por día y extraemos el "Mejor Peso" de cada sesión (1RM teórico simple)
    val graphData = remember(history) {
        history.groupBy {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(it.date))
        }.map { (date, exercises) ->
            val maxWeight = exercises.maxOf { it.currentWeight }
            date to maxWeight
        }.sortedBy { it.first } // Asegurar orden cronológico si la fecha lo permite
            .takeLast(10) // Limitamos a las últimas 10 sesiones para legibilidad móvil
    }

    // 2. Cálculo del Personal Record (PR) Histórico
    val personalRecord = history.maxOfOrNull { it.currentWeight } ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color.Black // Fondo OLED puro
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- CONTENIDO PRINCIPAL (Scrollable) ---
            // Usamos weight(1f) para que ocupe todo el espacio disponible, empujando el banner al fondo.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin datos. ¡A entrenar!", color = Color.Gray)
                    }
                } else {
                    // KPI: Tarjeta de Récord Personal (PR)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RÉCORD PERSONAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${personalRecord} kg", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            }
                            // Icono de Trofeo (Feedback visual de logro)
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = Color(0xFFFFD700), // IronGold
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Título de Sección
                    Text("Tu Evolución", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    // Componente de Gráfica Custom (Canvas)
                    if (graphData.size > 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Cálculos de escala
                                val maxVal = graphData.maxOf { it.second }.toFloat()
                                val minVal = graphData.minOf { it.second }.toFloat()
                                val width = size.width
                                val height = size.height
                                val xStep = width / (graphData.size - 1)
                                // Evitar división por cero si max == min
                                val yRange = (maxVal - minVal).coerceAtLeast(1f)

                                // Mapeo de datos a coordenadas (X, Y)
                                val points = graphData.mapIndexed { index, data ->
                                    val x = index * xStep
                                    // Invertimos Y porque en Canvas (0,0) es arriba-izquierda
                                    val y = height - ((data.second.toFloat() - minVal) / yRange * (height * 0.7f)) - (height * 0.15f)
                                    Offset(x, y)
                                }

                                // 1. Dibujar Relleno Degradado (Area Chart effect)
                                val fillPath = Path().apply {
                                    moveTo(points.first().x, height) // Inicio abajo-izq
                                    lineTo(points.first().x, points.first().y) // Subir al primer punto
                                    points.forEach { lineTo(it.x, it.y) } // Trazar línea
                                    lineTo(points.last().x, height) // Bajar al final-der
                                    close() // Cerrar loop
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFF3D3D).copy(alpha = 0.3f), // Rojo IronProgress semitransparente
                                            Color.Transparent // Desvanecer a negro
                                        ),
                                        startY = 0f,
                                        endY = height
                                    )
                                )

                                // 2. Dibujar Línea de Tendencia (Stroke)
                                val strokePath = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    points.forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path = strokePath, color = Color(0xFFFF3D3D), style = Stroke(width = 6f))

                                // 3. Dibujar Puntos de Datos (Data Points)
                                points.forEachIndexed { index, point ->
                                    drawCircle(Color(0xFF1E1E1E), radius = 8f, center = point) // Borde negro para contraste
                                    drawCircle(Color(0xFFFF3D3D), radius = 5f, center = point) // Punto rojo

                                    // Etiquetas de Valor (Text) usando Native Canvas
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(
                                            graphData[index].second.toInt().toString(), // Valor entero
                                            point.x,
                                            point.y - 25f, // Offset vertical para no tapar el punto
                                            android.graphics.Paint().apply {
                                                color = android.graphics.Color.LTGRAY
                                                textSize = 32f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                isFakeBoldText = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Estado vacío para gráfica
                        Text("Registra al menos 2 sesiones para visualizar tu tendencia.", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Título Historial
                    Text("Historial Detallado", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    // Lista de Sesiones Anteriores
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(history.reversed()) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        // Fecha con icono
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FitnessCenter, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.date)),
                                                color = Color.Gray, fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${item.targetSets} series x ${item.targetReps} reps", color = Color.White)
                                    }
                                    // Peso destacado
                                    Text(
                                        "${item.currentWeight} kg",
                                        color = Color(0xFFFF3D3D),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- ZONA DE MONETIZACIÓN (Sticky Bottom) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AdmobBanner()
            }
        }
    }
}