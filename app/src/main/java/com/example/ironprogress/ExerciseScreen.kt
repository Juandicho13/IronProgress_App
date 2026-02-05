package com.example.ironprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(viewModel: ExerciseViewModel, navController: NavController) {

    // 🚀 OPTIMIZACIÓN DE FLUIDEZ:
    // Leemos los datos YA AGRUPADOS desde el ViewModel.
    // La pantalla ya no pierde tiempo calculando fechas, solo dibuja.
    val groupedExercises by viewModel.groupedExercises.collectAsState()

    val currentRestTime by viewModel.currentRestTime.collectAsState()

    // Variables del formulario
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Estado para edición
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }

    val muscleGroups = listOf("Pecho", "Espalda", "Pierna", "Hombro", "Bíceps", "Tríceps", "Abdomen", "Cardio")

    // Ventana de Edición
    if (exerciseToEdit != null) {
        EditExerciseDialog(
            exercise = exerciseToEdit!!,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { updatedExercise ->
                viewModel.updateExercise(updatedExercise)
                exerciseToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Entrenamiento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. BOTÓN DE CAMBIO RÁPIDO DE RUTINA
            OutlinedButton(
                onClick = { navController.navigate("routines") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("¿No es hoy? Cambiar Rutina")
            }

            // 2. FORMULARIO DE REGISTRO
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nuevo Registro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nombre del Ejercicio") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = muscle, onValueChange = {}, readOnly = true,
                            label = { Text("Grupo Muscular") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            muscleGroups.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = { muscle = selectionOption; expanded = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sets, onValueChange = { if (it.all { char -> char.isDigit() }) sets = it },
                            label = { Text("Series") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                        )
                        OutlinedTextField(
                            value = reps, onValueChange = { reps = it },
                            label = { Text("Reps") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true
                        )
                        OutlinedTextField(
                            value = weight, onValueChange = { weight = it },
                            label = { Text("Kg") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && muscle.isNotEmpty()) {
                                viewModel.addExercise(
                                    name, muscle,
                                    sets.toIntOrNull() ?: 0, reps, weight.toDoubleOrNull() ?: 0.0
                                )
                                sets = ""; reps = ""; weight = ""
                                // Navegamos al timer usando la variable de tiempo actual
                                navController.navigate("timer/$currentRestTime/Descanso")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GUARDAR Y DESCANSAR (${currentRestTime}s)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LISTA AGRUPADA POR DÍAS (OPTIMIZADA)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Iteramos sobre el mapa que ya viene LISTO del ViewModel
                groupedExercises.forEach { (dateHeader, exercisesForDay) ->
                    item {
                        Text(
                            text = dateHeader.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    items(exercisesForDay) { exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            onDelete = { viewModel.deleteExercise(exercise.id) },
                            onGraphClick = { navController.navigate("graph/${exercise.name}") },
                            onEditClick = { exerciseToEdit = exercise }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(50.dp)) }
            }
        }
    }
}

// --- Componentes ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseItem(
    exercise: ExerciseEntity,
    onDelete: () -> Unit,
    onGraphClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onGraphClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${exercise.targetSets} X ${exercise.targetReps} • ${exercise.currentWeight} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = exercise.muscleGroup, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun EditExerciseDialog(exercise: ExerciseEntity, onDismiss: () -> Unit, onConfirm: (ExerciseEntity) -> Unit) {
    var sets by remember { mutableStateOf(exercise.targetSets.toString()) }
    var reps by remember { mutableStateOf(exercise.targetReps) }
    var weight by remember { mutableStateOf(exercise.currentWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Editar ${exercise.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = sets, onValueChange = { if (it.all { char -> char.isDigit() }) sets = it },
                    label = { Text("Series") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("Peso (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = exercise.copy(
                    targetSets = sets.toIntOrNull() ?: exercise.targetSets,
                    targetReps = reps,
                    currentWeight = weight.toDoubleOrNull() ?: exercise.currentWeight
                )
                onConfirm(updated)
            }) { Text("GUARDAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}