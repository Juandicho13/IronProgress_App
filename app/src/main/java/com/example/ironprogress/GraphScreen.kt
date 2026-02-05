package com.example.ironprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents // Trofeo
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(exerciseName: String, viewModel: ExerciseViewModel, navController: NavController) {
    // Datos del historial
    val history by viewModel.getExerciseHistory(exerciseName).collectAsState(initial = emptyList())

    // 1. Preparar datos Gráfica (Máximo peso por día)
    val graphData = remember(history) {
        history.groupBy {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(it.date))
        }.map { (date, exercises) ->
            val maxWeight = exercises.maxOf { it.currentWeight }
            date to maxWeight
        }.takeLast(10) // Últimas 10 sesiones
    }

    // 2. Récord Personal (PR)
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
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- CONTENIDO SCROLLABLE (Peso 1f para empujar el anuncio abajo) ---
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
                    // MEJORA 1: TARJETA DE PR CON TROFEO
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
                            // Icono Trofeo Dorado
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = Color(0xFFFFD700), // Dorado
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // TÍTULO GRÁFICA
                    Text("Tu Evolución", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    // MEJORA 2: GRÁFICA CON DEGRADADO (Gradient Fill)
                    if (graphData.size > 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val maxVal = graphData.maxOf { it.second }.toFloat()
                                val minVal = graphData.minOf { it.second }.toFloat()
                                val width = size.width
                                val height = size.height
                                val xStep = width / (graphData.size - 1)
                                val yRange = (maxVal - minVal).coerceAtLeast(1f)

                                val points = graphData.mapIndexed { index, data ->
                                    val x = index * xStep
                                    val y = height - ((data.second.toFloat() - minVal) / yRange * (height * 0.7f)) - (height * 0.15f) // Margen visual
                                    Offset(x, y)
                                }

                                // 1. Relleno Degradado (Gradient)
                                val fillPath = Path().apply {
                                    moveTo(points.first().x, height) // Empezar abajo a la izq
                                    lineTo(points.first().x, points.first().y) // Subir al primer punto
                                    points.forEach { lineTo(it.x, it.y) } // Seguir la línea
                                    lineTo(points.last().x, height) // Bajar al final
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFF3D3D).copy(alpha = 0.3f), // Rojo transparente arriba
                                            Color.Transparent // Transparente abajo
                                        ),
                                        startY = 0f,
                                        endY = height
                                    )
                                )

                                // 2. Línea Roja Sólida
                                val strokePath = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    points.forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path = strokePath, color = Color(0xFFFF3D3D), style = Stroke(width = 6f))

                                // 3. Puntos y Texto
                                points.forEachIndexed { index, point ->
                                    drawCircle(Color(0xFF1E1E1E), radius = 8f, center = point)
                                    drawCircle(Color(0xFFFF3D3D), radius = 5f, center = point)

                                    // Texto Native (Peso)
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(
                                            graphData[index].second.toInt().toString(), // Sin decimales en gráfica para limpieza
                                            point.x,
                                            point.y - 25f,
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
                        Text("Necesitas al menos 2 días de datos para ver la línea.", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // MEJORA 3: LISTA ESTILIZADA
                    Text("Historial Detallado", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

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
                                        // Fecha con estilo
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
                                    // Peso destacado en Rojo
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

            // --- PUBLICIDAD AL FONDO (SOLICITADO) ---
            // Usamos un Box contenedor para el Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212)) // Un fondo sutil para separar el ad
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AdmobBanner()
            }
        }
    }
}