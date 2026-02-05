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

/**
 * Pantalla Principal (Dashboard).
 *
 * Actúa como el centro de navegación de la aplicación.
 * Implementa un diseño centrado verticalmente para adaptarse a diferentes
 * tamaños de pantalla y prioriza la acción principal "Iniciar Entrenamiento".
 *
 * Funcionalidades clave:
 * 1. Smart Start: Calcula automáticamente cuál es la siguiente rutina lógica.
 * 2. Navegación a módulos CRUD (Rutinas, Perfil).
 * 3. Acceso a Analytics (Progreso).
 */
@Composable
fun HomeScreen(viewModel: ExerciseViewModel, navController: NavController) {
    // Observamos las rutinas para determinar el estado del botón "Iniciar"
    val routines by viewModel.allRoutines.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espaciador flexible para centrado vertical
        Spacer(modifier = Modifier.weight(1f))

        // --- Identidad de Marca (Header) ---
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = "Logo IronProgress",
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "IronProgress",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Tu entrenador personal",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // --- Menú de Acciones Principales ---

        // 1. INICIAR ENTRENAMIENTO (Smart Action)
        BigButton(
            text = "INICIAR ENTRENAMIENTO",
            subText = if (routines.isNotEmpty()) "Toca para cargar la siguiente rutina" else "Crea una rutina primero",
            icon = Icons.Default.PlayArrow,
            color = MaterialTheme.colorScheme.primary,
            onClick = {
                if (routines.isNotEmpty()) {
                    // Lógica inteligente: Buscar la rutina que sigue en la rotación
                    val nextId = viewModel.getNextRoutineId(routines)
                    if (nextId != -1) {
                        viewModel.startLiveWorkout(nextId)
                        navController.navigate("live_workout")
                    }
                } else {
                    // Si no hay rutinas, redirigir a la creación
                    navController.navigate("routines")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. GESTIÓN DE RUTINAS
        BigButton(
            text = "MIS RUTINAS",
            subText = "Crea o edita tus planes",
            icon = Icons.Default.List,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("routines") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. ANALYTICS
        BigButton(
            text = "MI PROGRESO",
            subText = "Gráficas y estadísticas",
            icon = Icons.Default.ShowChart,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("progress") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. PERFIL DE USUARIO
        BigButton(
            text = "MI PERFIL",
            subText = "Tus datos y ajustes",
            icon = Icons.Default.Person,
            color = Color.Gray,
            onClick = { navController.navigate("profile") }
        )

        // Espaciador flexible inferior
        Spacer(modifier = Modifier.weight(1f))

        // --- Espacio de Monetización (Bottom) ---
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

/**
 * Componente de UI: Botón de Acción Grande (Dashboard Card).
 *
 * @param text Título principal de la acción.
 * @param subText Descripción secundaria o estado.
 * @param icon Icono vectorial representativo.
 * @param color Color de acento para el icono (tint).
 * @param onClick Lambda ejecutada al presionar la tarjeta.
 */
@Composable
fun BigButton(
    text: String,
    subText: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor del Icono con fondo semitransparente
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}