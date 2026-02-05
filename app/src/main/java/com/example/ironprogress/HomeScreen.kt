package com.example.ironprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(viewModel: ExerciseViewModel, navController: NavController) {
    val routines by viewModel.allRoutines.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 👇 1. ESPACIADOR SUPERIOR
        Spacer(modifier = Modifier.weight(1f))

        // --- CONTENIDO ---
        Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        Text("IronProgress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Tu entrenador personal", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(48.dp))

        // BOTÓN 1: INICIAR
        BigButton(
            text = "INICIAR ENTRENAMIENTO",
            subText = if (routines.isNotEmpty()) "Toca para cargar la siguiente rutina" else "Crea una rutina primero",
            icon = Icons.Default.PlayArrow,
            color = MaterialTheme.colorScheme.primary,
            onClick = {
                if (routines.isNotEmpty()) {
                    val nextId = viewModel.getNextRoutineId(routines)
                    if (nextId != -1) {
                        viewModel.startLiveWorkout(nextId)
                        navController.navigate("live_workout")
                    }
                } else {
                    navController.navigate("routines")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BOTÓN 2: RUTINAS
        BigButton(
            text = "MIS RUTINAS",
            subText = "Crea o edita tus planes",
            icon = Icons.Default.List,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("routines") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BOTÓN 3: PROGRESO
        BigButton(
            text = "MI PROGRESO",
            subText = "Gráficas y estadísticas",
            icon = Icons.Default.ShowChart,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = {
                navController.navigate("progress")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BOTÓN 4: PERFIL (🔥 ACTIVADO AQUÍ)
        BigButton(
            text = "MI PERFIL",
            subText = "Tus datos y ajustes", // Texto actualizado
            icon = Icons.Default.Person,
            color = Color.Gray,
            onClick = { navController.navigate("profile") } // 🔥 Navegación activada
        )

        // 👇 2. ESPACIADOR INFERIOR
        Spacer(modifier = Modifier.weight(1f))

        // --- PUBLICIDAD ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AdmobBanner()
        }
    }
}

@Composable
fun BigButton(text: String, subText: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}