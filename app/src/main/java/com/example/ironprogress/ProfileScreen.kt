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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: ExerciseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    // Obtenemos el perfil actual del ViewModel
    val userProfile by viewModel.userProfile.collectAsState()

    // Estados locales para el formulario (se inicializan con los datos guardados)
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var currentWeight by remember(userProfile) { mutableStateOf(userProfile.currentWeight) }
    var height by remember(userProfile) { mutableStateOf(userProfile.height) }

    // Configuración
    var isKg by remember(userProfile) { mutableStateOf(userProfile.isKg) }
    var soundEnabled by remember(userProfile) { mutableStateOf(userProfile.soundEnabled) }
    var vibrationEnabled by remember(userProfile) { mutableStateOf(userProfile.vibrationEnabled) }

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

            // FOTO DE PERFIL (Icono)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // NOMBRE
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre de Atleta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN FÍSICA
            Text("Métricas Físicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tarjeta de PESO
                MetricCard(
                    title = "Peso Actual",
                    value = currentWeight,
                    unit = if (isKg) "kg" else "lb",
                    icon = Icons.Default.MonitorWeight, // O Icons.Default.Info si da error
                    onValueChange = { currentWeight = it },
                    modifier = Modifier.weight(1f)
                )
                // Tarjeta de ESTATURA
                MetricCard(
                    title = "Estatura",
                    value = height,
                    unit = "cm",
                    icon = Icons.Default.Height, // O Icons.Default.Info si da error
                    onValueChange = { height = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN AJUSTES
            Text("Preferencias Globales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // SWITCH DE UNIDADES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unidad de Peso", style = MaterialTheme.typography.bodyLarge)
                        // Toggle personalizado visual
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

                    SettingSwitchItem("Sonido al finalizar", soundEnabled) { soundEnabled = it }

                    HorizontalDivider(color = Color.Gray.copy(0.1f))

                    SettingSwitchItem("Vibración", vibrationEnabled) { vibrationEnabled = it }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN GUARDAR
            Button(
                onClick = {
                    val newProfile = UserProfile(
                        name = name,
                        currentWeight = currentWeight,
                        height = height,
                        isKg = isKg,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled
                    )
                    viewModel.saveUserProfile(newProfile)
                    navController.popBackStack() // Volver al inicio al guardar
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

// --- COMPONENTES AUXILIARES ---

@Composable
fun MetricCard(title: String, value: String, unit: String, icon: ImageVector, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onValueChange(it) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

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