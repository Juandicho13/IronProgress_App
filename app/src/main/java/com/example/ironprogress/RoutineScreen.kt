package com.example.ironprogress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Pantalla de Gestión de Rutinas y Zonas (Routine Management).
 *
 * Implementa un sistema de navegación jerárquico de dos niveles:
 * 1. Nivel Superior: Grid de Carpetas/Zonas (Ej: Gimnasio, Casa, Hotel).
 * 2. Nivel Inferior: Lista de Rutinas dentro de una carpeta seleccionada.
 *
 * Incluye un "Wizard" (Asistente) de pantalla completa para la creación
 * y diseño de rutinas complejas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(viewModel: ExerciseViewModel, navController: NavController) {

    //region --- Recolección de Estado (State Collection) ---
    val allRoutines by viewModel.allRoutines.collectAsState(initial = emptyList())
    val userProfile by viewModel.userProfile.collectAsState()

    // Sistema de Carpetas
    val currentFolder by viewModel.currentFolder.collectAsState()
    val folders by viewModel.folders.collectAsState()

    // Filtrado reactivo: Solo mostramos rutinas de la carpeta actual
    val filteredRoutines = remember(allRoutines, currentFolder) {
        if (currentFolder == null) emptyList()
        else allRoutines.filter { it.folder == currentFolder }
    }
    //endregion

    //region --- Estados de UI (Dialogs & Navigation) ---
    var showCreateDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    // Estados para edición/borrado temporal
    var routineToEdit by remember { mutableStateOf<RoutineEntity?>(null) }
    var initialExercises by remember { mutableStateOf<List<RoutineExerciseEntity>>(emptyList()) }
    var routineToDelete by remember { mutableStateOf<RoutineEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    //endregion

    Scaffold(
        topBar = {
            // Barra superior dinámica según el nivel de navegación
            if (currentFolder != null) {
                CenterAlignedTopAppBar(
                    title = { Text(currentFolder!!.uppercase(), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setFolder(null) }) { // Volver a carpetas
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("ZONAS", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) { // Salir al Dashboard
                            Icon(Icons.Default.ArrowBack, contentDescription = "Salir")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- CONTENIDO PRINCIPAL (Swap entre Grid y Lista) ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

                if (currentFolder == null) {
                    //region --- VISTA 1: GRID DE CARPETAS ---
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Selecciona tu entorno:", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(folders) { folder ->
                                FolderCard(
                                    name = folder,
                                    onClick = { viewModel.setFolder(folder) },
                                    onDelete = { folderToDelete = folder }
                                )
                            }
                            // Tarjeta especial para agregar nueva carpeta
                            item {
                                NewFolderCard { showNewFolderDialog = true }
                            }
                        }
                    }
                    //endregion
                } else {
                    //region --- VISTA 2: LISTA DE RUTINAS ---
                    Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {

                        // Lista Scrollable
                        LazyColumn(
                            contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f) // Empuja la barra de control hacia abajo
                        ) {
                            if (filteredRoutines.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No hay rutinas aquí", color = Color.Gray)
                                    }
                                }
                            }

                            items(filteredRoutines) { routine ->
                                RoutineItem(
                                    routine = routine,
                                    onDelete = { routineToDelete = routine },
                                    onEdit = {
                                        // Cargamos los datos de la BD antes de abrir el editor
                                        scope.launch {
                                            val exercises = viewModel.getExercisesForRoutine(routine.id)
                                            routineToEdit = routine
                                            initialExercises = exercises
                                            showCreateDialog = true
                                        }
                                    },
                                    onLoad = {
                                        // Iniciar entrenamiento en vivo
                                        viewModel.startLiveWorkout(routine.id)
                                        navController.navigate("live_workout")
                                    }
                                )
                            }
                        }

                        // Barra de Control Inferior (Crear / Cambiar Zona)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Área de texto (Clickeable para crear)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { routineToEdit = null; initialExercises = emptyList(); showCreateDialog = true }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Crear en ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    Text(currentFolder ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                // Botón: Cambiar Zona Rápido
                                IconButton(
                                    onClick = { viewModel.setFolder(null) },
                                    modifier = Modifier.background(Color.Gray.copy(alpha = 0.2f), CircleShape).size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, "Cambiar Zona", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Botón: Agregar (+)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { routineToEdit = null; initialExercises = emptyList(); showCreateDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, "Nueva Rutina", tint = Color.White)
                                }
                            }
                        }
                    }
                    //endregion
                }
            }

            // --- PUBLICIDAD ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AdmobBanner()
            }
        }
    }

    //region --- DIÁLOGOS MODALES ---

    // 1. Confirmar borrado de carpeta
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("¿Eliminar Zona?") },
            text = { Text("¿Seguro que quieres borrar la carpeta \"$folderToDelete\"? Las rutinas dentro se perderán visualmente.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteFolder(folderToDelete!!)
                    folderToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("ELIMINAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("CANCELAR") }
            }
        )
    }

    // 2. Crear nueva carpeta
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Nueva Zona") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nombre (Ej: Hotel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotEmpty()) {
                        viewModel.createFolder(newFolderName)
                        showNewFolderDialog = false
                    }
                }) { Text("CREAR") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("CANCELAR") } }
        )
    }

    // 3. WIZARD DE RUTINA (Pantalla Completa)
    if (showCreateDialog) {
        FullScreenRoutineDialog(
            routineToEdit = routineToEdit,
            initialExercises = initialExercises,
            defaultIsKg = userProfile.isKg,
            currentFolder = currentFolder ?: "General",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, restTime, folder, exercises ->
                if (routineToEdit == null) {
                    // Crear nueva
                    viewModel.addRoutineComplex(name, restTime, folder, exercises)
                } else {
                    // Actualizar existente
                    viewModel.updateRoutineComplex(routineToEdit!!.id, name, restTime, exercises)
                }
                showCreateDialog = false
            }
        )
    }

    // 4. Confirmar borrado de rutina
    if (routineToDelete != null) {
        DeleteConfirmationDialog(
            routineName = routineToDelete!!.name,
            onConfirm = {
                viewModel.deleteRoutine(routineToDelete!!.id)
                routineToDelete = null
            },
            onDismiss = { routineToDelete = null }
        )
    }
    //endregion
}

//region --- COMPONENTES UI (Widgets) ---

@Composable
fun FolderCard(name: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Contenido Central
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(name, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Botón Borrar (Esquina superior derecha)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun NewFolderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Nueva Zona", fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

@Composable
fun RoutineItem(routine: RoutineEntity, onDelete: () -> Unit, onEdit: () -> Unit, onLoad: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Botón de Acción Principal (Play)
            Button(
                onClick = onLoad,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("INICIAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Menú contextual (Más opciones)
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { expanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Borrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(routineName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar Rutina?") },
        text = { Text("Se eliminará '$routineName' y su historial asociado.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("BORRAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}
//endregion

//region --- WIZARD DE CREACIÓN (Full Screen Dialog) ---

/**
 * Diálogo de Pantalla Completa para Crear/Editar Rutinas.
 * Funciona como un Wizard de 2 pasos:
 * 1. Definición de Identidad (Nombre).
 * 2. Diseño de Entrenamiento (Lista de Ejercicios).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenRoutineDialog(
    routineToEdit: RoutineEntity?,
    initialExercises: List<RoutineExerciseEntity>,
    defaultIsKg: Boolean,
    currentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, List<RoutineExerciseEntity>) -> Unit
) {
    // Control del paso actual (1: Nombre, 2: Ejercicios)
    var currentStep by remember { mutableIntStateOf(if (routineToEdit != null) 2 else 1) }

    // Datos de cabecera
    var name by remember { mutableStateOf(routineToEdit?.name ?: "") }
    val defaultRestTime = 90
    val folder = routineToEdit?.folder ?: currentFolder

    // Lista temporal de ejercicios (En memoria hasta confirmar)
    val tempExercises = remember { mutableStateListOf<RoutineExerciseEntity>().apply { addAll(initialExercises) } }

    // Campos de formulario para el ejercicio actual
    var exName by remember { mutableStateOf("") }
    var exSets by remember { mutableStateOf("") }
    var isRangeMode by remember { mutableStateOf(false) } // Toggle para rango de reps (ej: 8-12)
    var exRepsNormal by remember { mutableStateOf("") }
    var exRepsMin by remember { mutableStateOf("") }
    var exRepsMax by remember { mutableStateOf("") }
    var exWeight by remember { mutableStateOf("") }
    var exRest by remember { mutableStateOf("") }
    var isKg by remember { mutableStateOf(defaultIsKg) }

    // Índice de edición (-1 significa modo Agregar nuevo)
    var editingIndex by remember { mutableStateOf(-1) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            if (currentStep == 1) {
                // --- PASO 1: NOMBRE DE LA RUTINA ---
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("NUEVA RUTINA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Carpeta: $folder", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { if (name.isNotEmpty()) currentStep = 2 },
                        enabled = name.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("DISEÑAR EJERCICIOS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) }
                }
            } else {
                // --- PASO 2: DISEÑO DE EJERCICIOS ---
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                    // Cabecera con Botón Finalizar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (name.isNotEmpty()) {
                                    onConfirm(name, defaultRestTime, folder, tempExercises.toList())
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FINALIZAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Formulario de Ejercicio ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = exName, onValueChange = { exName = it }, label = { Text("Nombre") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(text = if (isKg) "KG" else "LB", modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Chip para cambiar modo de Rango
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isRangeMode,
                            onClick = { isRangeMode = !isRangeMode },
                            label = { Text("Rango de Reps (8-12)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.tertiary, selectedLabelColor = Color.White),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Inputs numéricos en fila
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val smallTextStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.White)

                        // Input Series
                        OutlinedTextField(
                            value = exSets, onValueChange = { exSets = it },
                            label = { Text("Series", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f).height(60.dp),
                            textStyle = smallTextStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Input Reps (Condicional)
                        if (isRangeMode) {
                            Row(modifier = Modifier.weight(1.4f).height(60.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = exRepsMin, onValueChange = { exRepsMin = it },
                                    placeholder = { Text("Min", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    textStyle = smallTextStyle.copy(fontSize = 12.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Text("-", color = Color.Gray)
                                OutlinedTextField(
                                    value = exRepsMax, onValueChange = { exRepsMax = it },
                                    placeholder = { Text("Max", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    textStyle = smallTextStyle.copy(fontSize = 12.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = exRepsNormal, onValueChange = { exRepsNormal = it },
                                label = { Text("Reps", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).height(60.dp),
                                textStyle = smallTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Input Peso
                        OutlinedTextField(
                            value = exWeight, onValueChange = { exWeight = it },
                            label = { Text("Peso", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(60.dp),
                            textStyle = smallTextStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Input Descanso
                        OutlinedTextField(
                            value = exRest, onValueChange = { if(it.all { c -> c.isDigit() }) exRest = it },
                            label = { Text("Desc.", fontSize = 10.sp) },
                            placeholder = { Text("90s", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp) },
                            modifier = Modifier.weight(0.9f).height(60.dp),
                            textStyle = smallTextStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón Agregar/Actualizar
                    Button(
                        onClick = {
                            val finalReps = if (isRangeMode) "$exRepsMin-$exRepsMax" else exRepsNormal

                            if (exName.isNotEmpty() && exSets.isNotEmpty() && finalReps.isNotEmpty()) {
                                val newItem = RoutineExerciseEntity(
                                    routineId = 0,
                                    name = exName,
                                    sets = exSets.toIntOrNull() ?: 3,
                                    reps = finalReps,
                                    weight = exWeight.toDoubleOrNull() ?: 0.0,
                                    targetRestTime = exRest.toIntOrNull(),
                                    isKg = isKg
                                )

                                if (editingIndex >= 0) {
                                    tempExercises[editingIndex] = newItem
                                    editingIndex = -1 // Reset a modo agregar
                                } else {
                                    tempExercises.add(newItem)
                                }

                                // Resetear campos
                                exName = ""; exSets = ""; exRepsNormal = ""; exRepsMin = ""; exRepsMax = ""; exWeight = ""; exRest = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editingIndex >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (editingIndex >= 0) Color.White else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (editingIndex >= 0) Icons.Default.Check else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (editingIndex >= 0) "ACTUALIZAR EJERCICIO" else "AGREGAR A LA LISTA")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- LISTA DE EJERCICIOS AGREGADOS ---
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(tempExercises, key = { index, ex -> ex.id.toString() + ex.name + ex.hashCode() }) { index, ex ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (editingIndex == index) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, fontWeight = FontWeight.Bold, color = Color.White)
                                        val unitText = if (ex.isKg) "kg" else "lb"
                                        val restText = if (ex.targetRestTime != null) "${ex.targetRestTime}s" else "(90s)"
                                        Text("${ex.sets} x ${ex.reps} • ${ex.weight}$unitText • $restText", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }

                                    // Controles de Reordenamiento
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (index > 0) {
                                            Icon(Icons.Default.KeyboardArrowUp, "Subir", modifier = Modifier.size(24.dp).clickable { Collections.swap(tempExercises, index, index - 1) }, tint = Color.Gray)
                                        }
                                        if (index < tempExercises.size - 1) {
                                            Icon(Icons.Default.KeyboardArrowDown, "Bajar", modifier = Modifier.size(24.dp).clickable { Collections.swap(tempExercises, index, index + 1) }, tint = Color.Gray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Botón Editar
                                    IconButton(onClick = {
                                        exName = ex.name; exSets = ex.sets.toString(); exWeight = ex.weight.toString()
                                        exRest = ex.targetRestTime?.toString() ?: ""; isKg = ex.isKg

                                        if (ex.reps.contains("-")) {
                                            val parts = ex.reps.split("-")
                                            if (parts.size >= 2) {
                                                isRangeMode = true; exRepsMin = parts[0]; exRepsMax = parts[1]
                                            }
                                        } else {
                                            isRangeMode = false; exRepsNormal = ex.reps
                                        }

                                        editingIndex = index
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Botón Borrar
                                    IconButton(onClick = {
                                        if (editingIndex == index) { editingIndex = -1 }
                                        tempExercises.removeAt(index)
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
//endregion