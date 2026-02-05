package com.example.ironprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla de Perfil de Usuario y Configuración.
 *
 * Permite la gestión de datos persistentes del usuario y preferencias globales de la app.
 * Funcionalidades:
 * 1. Edición de identidad (Nombre).
 * 2. Registro de métricas físicas (Peso/Estatura) para futuros cálculos de salud.
 * 3. Configuración de UX (Unidades métricas/imperiales, Feedback háptico y sonoro).
 *
 * @param viewModel Instancia del [ExerciseViewModel] para la persistencia de datos (DataStore).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ExerciseViewModel = viewModel()
) {

    //region --- Recolección de Estado ---
    // Obtenemos el flujo de datos del usuario.
    val userProfile by viewModel.userProfile.collectAsState()

    // Estados locales para el formulario (buffer de edición).
    // Usamos 'remember(userProfile)' para inicializar los campos con los datos actuales
    // pero permitir la edición sin modificar el StateFlow inmediatamente.
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var currentWeight by remember(userProfile) { mutableStateOf(userProfile.currentWeight) }
    var height by remember(userProfile) { mutableStateOf(userProfile.height) }

    // Preferencias de UI
    var isKg by remember(userProfile) { mutableStateOf(userProfile.isKg) }
    var soundEnabled by remember(userProfile) { mutableStateOf(userProfile.soundEnabled) }
    var vibrationEnabled by remember(userProfile) { mutableStateOf(userProfile.vibrationEnabled) }
    //endregion

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MI PERFIL", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- SECCIÓN 1: IDENTIDAD ---

            // Avatar Placeholder (Círculo con icono)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Nombre
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre de Atleta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN 2: MÉTRICAS FÍSICAS ---

            Text(
                "Métricas Físicas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tarjeta de PESO
                // Nota: Icons.Default.MonitorWeight requiere la librería 'material-icons-extended'.
                // Si falla, usar Icons.Default.Info o agregar la dependencia en build.gradle.
                MetricCard(
                    title = "Peso Actual",
                    value = currentWeight,
                    unit = if (isKg) "kg" else "lb",
                    icon = Icons.Default.Info, // Icono genérico seguro
                    onValueChange = { currentWeight = it },
                    modifier = Modifier.weight(1f)
                )
                // Tarjeta de ESTATURA
                MetricCard(
                    title = "Estatura",
                    value = height,
                    unit = "cm",
                    icon = Icons.Default.Info, // Icono genérico seguro
                    onValueChange = { height = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN 3: PREFERENCIAS GLOBALES ---

            Text(
                "Preferencias Globales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Selector de Sistema de Unidades (Toggle Custom)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unidad de Peso", style = MaterialTheme.typography.bodyLarge)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            UnitToggleOption("KG", isKg) { isKg = true }
                            UnitToggleOption("LB", !isKg) { isKg = false }
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(0.1f))

                    // Switches de Feedback
                    SettingSwitchItem("Sonido al finalizar", soundEnabled) { soundEnabled = it }
                    HorizontalDivider(color = Color.Gray.copy(0.1f))
                    SettingSwitchItem("Vibración", vibrationEnabled) { vibrationEnabled = it }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- ACCIÓN PRINCIPAL: GUARDAR ---
            Button(
                onClick = {
                    // Empaquetamos los datos en el Data Class
                    val newProfile = UserProfile(
                        name = name,
                        currentWeight = currentWeight,
                        height = height,
                        isKg = isKg,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled
                    )
                    // Persistencia vía ViewModel
                    viewModel.saveUserProfile(newProfile)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

//region --- Componentes Reutilizables (UI Components) ---

/**
 * Tarjeta para ingreso de métricas numéricas (Peso, Altura).
 * Incluye validación básica para permitir solo números y puntos decimales.
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header de la tarjeta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Input Numérico
            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        // Validación: Solo dígitos y puntos
                        if (it.all { char -> char.isDigit() || char == '.' }) onValueChange(it)
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * Fila de configuración con interruptor (Switch).
 */
@Composable
fun SettingSwitchItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Botón de selección para el toggle de unidades (KG/LB).
 */
@Composable
fun UnitToggleOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp
        )
    }
}
//endregion