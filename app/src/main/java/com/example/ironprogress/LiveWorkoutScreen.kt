package com.example.ironprogress

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // 🔥 IMPORTACIÓN AGREGADA
import androidx.compose.foundation.lazy.items      // 🔥 IMPORTACIÓN AGREGADA
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning // 🔥 IMPORTACIÓN AGREGADA
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
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWorkoutScreen(viewModel: ExerciseViewModel, navController: NavController) {

    // --- ESTADOS ---
    // TRUCO PARA EVITAR ERRORES DE "SMART CAST":
    // 1. Obtenemos el Estado
    val activeExercisesState = viewModel.activeRoutineExercises.collectAsState()
    // 2. Lo guardamos en una variable local FIJA. Ahora Kotlin sabe que no cambiará.
    val activeExercises = activeExercisesState.value

    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val currentSet by viewModel.currentSetNumber.collectAsState()
    val routineBaseRestTime by viewModel.currentRestTime.collectAsState()
    val allRoutines by viewModel.allRoutines.collectAsState(initial = emptyList())

    // Estado para el diálogo de cambio de rutina
    var showChangeDialog by remember { mutableStateOf(false) }

    // 1. PANTALLAS DE ESTADO (Cargando / Vacía)
    if (activeExercises == null) {
        // Caso A: Aún estamos buscando en la BD...
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (activeExercises.isEmpty()) {
        // Caso B: Rutina vacía (Kotlin ya sabe que activeExercises NO es null aquí)
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

    // 2. PANTALLA DE FINALIZACIÓN
    if (currentIndex >= activeExercises.size) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "¡ENTRENAMIENTO COMPLETADO!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("VOLVER AL INICIO")
            }
        }
        return
    }

    // 3. PANTALLA DE ENTRENAMIENTO EN VIVO
    val exercise = activeExercises[currentIndex]

    // GESTIÓN DE MEMORIA (Inputs)
    var actualReps by remember(exercise) { mutableStateOf(exercise.reps) }
    var actualWeight by remember(exercise) { mutableStateOf(exercise.weight.toString()) }

    // Tiempo de descanso inteligente
    val effectiveRestTime = remember(exercise) {
        exercise.targetRestTime ?: routineBaseRestTime
    }

    // Historial de lo que llevamos hecho hoy
    val todaysSets by viewModel.getTodaysHistory(exercise.name).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // --- ENCABEZADO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / activeExercises.size.toFloat() },
                modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                trackColor = Color.Gray.copy(alpha = 0.25f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { showChangeDialog = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Cambiar Rutina", tint = Color.Gray)
            }
        }

        // --- CUERPO SCROLLABLE ---
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

            // TARJETA PRINCIPAL
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

                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(
                            "SERIE $currentSet / ${exercise.sets}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // INPUTS
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Peso
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
                        // Reps
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

            // HISTORIAL DE HOY
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

        // PIE DE PÁGINA (BOTÓN)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // 1. CALCULAMOS QUE SIGUE
                    var nextExerciseName = ""
                    if (currentSet < exercise.sets) {
                        nextExerciseName = "Serie ${currentSet + 1} de ${exercise.name}"
                    } else {
                        if (currentIndex + 1 < activeExercises.size) {
                            nextExerciseName = "Sigue: ${activeExercises[currentIndex + 1].name}"
                        } else {
                            nextExerciseName = "¡Último Esfuerzo!"
                        }
                    }
                    val encodedNextName = Uri.encode(nextExerciseName)

                    // 2. SOLO GUARDAR DATOS
                    viewModel.logSetData(
                        currentEx = exercise,
                        actualReps = actualReps,
                        actualWeight = actualWeight.toDoubleOrNull() ?: 0.0
                    )

                    // 3. NAVEGAR AL TIMER
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

    // DIÁLOGO CAMBIAR RUTINA (Con imports corregidos)
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeDialog = false },
            title = { Text("Cambiar Rutina") },
            text = {
                // Ahora LazyColumn funcionará porque importamos androidx.compose.foundation.lazy.LazyColumn
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
}