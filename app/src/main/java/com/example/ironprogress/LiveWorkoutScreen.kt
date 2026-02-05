package com.example.ironprogress

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController

/**
 * Pantalla Principal de Ejecución de Rutina (Live Workout).
 *
 * Esta es la vista central durante una sesión de entrenamiento activa.
 * Gestiona:
 * 1. La navegación secuencial entre ejercicios y series.
 * 2. El registro de datos en tiempo real (Input de peso/reps).
 * 3. La visualización del historial inmediato (Series completadas hoy).
 * 4. La transición hacia el temporizador de descanso o el resumen final.
 *
 * @param viewModel ViewModel compartido que mantiene el estado de la sesión.
 * @param navController Controlador para navegar al Timer o al Dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWorkoutScreen(viewModel: ExerciseViewModel, navController: NavController) {

    //region --- Recolección de Estado (State Collection) ---
    // Se observa la lista de ejercicios activos. Si es null, indica carga; si es vacía, error.
    val activeExercisesState = viewModel.activeRoutineExercises.collectAsState()
    val activeExercises = activeExercisesState.value

    // Observamos índices y contadores para la UI reactiva
    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val currentSet by viewModel.currentSetNumber.collectAsState()
    val routineBaseRestTime by viewModel.currentRestTime.collectAsState()
    val allRoutines by viewModel.allRoutines.collectAsState(initial = emptyList())
    //endregion

    //region --- Estados Locales de UI ---
    // Controla la visibilidad del diálogo para cambiar de rutina "al vuelo"
    var showChangeDialog by remember { mutableStateOf(false) }

    // Controla la visibilidad del resumen final (activado por fin de rutina o botón de bandera)
    var showSummaryDialog by remember { mutableStateOf(false) }
    //endregion

    // 1. Manejo de Estado: Cargando datos desde BD
    if (activeExercises == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // 2. Manejo de Estado: Rutina sin ejercicios
    if (activeExercises.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Esta rutina está vacía", color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("Agrega ejercicios para entrenar.", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("VOLVER")
                }
            }
        }
        return
    }

    // 3. Manejo de Estado: Rutina Finalizada (Navegación Automática)
    // Si el índice supera la lista, mostramos el resumen automáticamente.
    if (currentIndex >= activeExercises.size) {
        WorkoutSummaryContent(
            exercises = activeExercises,
            viewModel = viewModel,
            onFinish = {
                navController.navigate("dashboard") {
                    // Limpiamos la pila de navegación para evitar volver al resumen con "Atrás"
                    popUpTo("dashboard") { inclusive = true }
                }
            }
        )
        return
    }

    // --- INTERFAZ DE ENTRENAMIENTO ACTIVO ---

    val exercise = activeExercises[currentIndex]

    // Gestión de Inputs: Usamos 'remember(exercise)' para resetear los campos
    // solo cuando cambiamos al siguiente ejercicio, pero mantenerlos entre series.
    var actualReps by remember(exercise) { mutableStateOf(exercise.reps) }
    var actualWeight by remember(exercise) { mutableStateOf(exercise.weight.toString()) }

    // Determinamos el tiempo de descanso: Prioridad al específico del ejercicio, sino el global.
    val effectiveRestTime = remember(exercise) {
        exercise.targetRestTime ?: routineBaseRestTime
    }

    // Obtenemos historial en tiempo real para mostrar "Completado hoy"
    val todaysSets by viewModel.getTodaysHistory(exercise.name).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // --- Header Superior (Progreso y Acciones) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra de progreso lineal
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / activeExercises.size.toFloat() },
                modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                trackColor = Color.Gray.copy(alpha = 0.25f),
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Botonera de acciones rápidas
            Row {
                // Acción: Finalizar prematuramente (Bandera)
                IconButton(onClick = { showSummaryDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = "Terminar Entrenamiento", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Acción: Cambiar Rutina (Refresh)
                IconButton(onClick = { showChangeDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Cambiar Rutina", tint = Color.Gray)
                }
            }
        }

        // --- Cuerpo Principal (Scrollable) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ejercicio ${currentIndex + 1} de ${activeExercises.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta de Ejercicio Actual (Input Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Indicador de Serie Actual
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(
                            "SERIE $currentSet / ${exercise.sets}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campos de Entrada (Inputs)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Input: Peso
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val unitLabel = if (exercise.isKg) "KG" else "LB"
                            Text("PESO ($unitLabel)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = actualWeight,
                                onValueChange = { actualWeight = it },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                        // Input: Repeticiones
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = actualReps,
                                onValueChange = { actualReps = it },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }

            // Lista de sets completados hoy (Feedback visual)
            if (todaysSets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Completado hoy:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    todaysSets.forEach { set ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Serie ${set.targetSets}: ${set.currentWeight}kg x ${set.targetReps}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // --- Footer (Botón Principal y Ads) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // Lógica de navegación y texto para el temporizador
                    var nextExerciseName = ""
                    if (currentSet < exercise.sets) {
                        nextExerciseName = "Serie ${currentSet + 1} de ${exercise.name}"
                    } else {
                        // Si terminamos series, miramos si hay siguiente ejercicio
                        if (currentIndex + 1 < activeExercises.size) {
                            nextExerciseName = "Sigue: ${activeExercises[currentIndex + 1].name}"
                        } else {
                            nextExerciseName = "¡Último Esfuerzo!"
                        }
                    }
                    val encodedNextName = Uri.encode(nextExerciseName)

                    // 1. Guardar datos en BD
                    viewModel.logSetData(
                        currentEx = exercise,
                        actualReps = actualReps,
                        actualWeight = actualWeight.toDoubleOrNull() ?: 0.0
                    )

                    // 2. Navegar al servicio de Temporizador
                    navController.navigate("timer/$effectiveRestTime/$encodedNextName")
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Timer, null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text("TERMINAR SERIE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Descanso de ${effectiveRestTime}s", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AdmobBanner()
            }
        }
    }

    // --- DIÁLOGOS (Modales) ---

    // Modal: Selección de cambio de rutina
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeDialog = false },
            title = { Text("Cambiar Rutina") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(allRoutines) { routine ->
                        ListItem(
                            headlineContent = { Text(routine.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${routine.restTimeSeconds}s descanso base") },
                            modifier = Modifier
                                .clickable {
                                    viewModel.startLiveWorkout(routine.id); showChangeDialog = false
                                }
                                .padding(vertical = 4.dp),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    // Modal: Resumen de Entrenamiento (Trigger manual)
    if (showSummaryDialog) {
        Dialog(
            onDismissRequest = { showSummaryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            WorkoutSummaryContent(
                exercises = activeExercises,
                viewModel = viewModel,
                onFinish = {
                    showSummaryDialog = false
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Pantalla de Resumen de Entrenamiento (Post-Workout Summary).
 *
 * Muestra un listado de los ejercicios realizados y calcula el 1RM estimado
 * (Repetición Máxima) basado en la última serie efectiva registrada.
 * Sirve como refuerzo positivo al usuario ("Gamification").
 */
@Composable
fun WorkoutSummaryContent(
    exercises: List<RoutineExerciseEntity>,
    viewModel: ExerciseViewModel,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Header de Celebración
            Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "¡ENTRENAMIENTO COMPLETADO!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Text("Resumen de fuerza hoy (1RM Est.)", color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            // Lista de Logros y Estimaciones
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(exercises) { ex ->
                    // Cálculo de 1RM usando la fórmula de Epley (implementada en ViewModel)
                    val repsInt = ex.reps.split("-")[0].toIntOrNull() ?: 1
                    val estimated1RM = viewModel.calculate1RM(ex.weight, repsInt)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${ex.weight} ${if(ex.isKg)"kg" else "lb"} x ${ex.reps}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            // Badge de 1RM (Visualización de métrica)
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("1RM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(
                                        "$estimated1RM",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Botón de Salida
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("GUARDAR Y SALIR", fontWeight = FontWeight.Bold)
            }
        }
    }
}