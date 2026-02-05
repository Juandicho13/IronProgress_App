package com.example.ironprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRMCalculatorScreen(navController: NavController) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var oneRM by remember { mutableStateOf(0.0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CALCULADORA 1RM", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
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
            // INPUTS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Datos del levantamiento", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Peso (kg/lb)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = reps,
                            onValueChange = { reps = it },
                            label = { Text("Repeticiones") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val w = weight.toDoubleOrNull() ?: 0.0
                            val r = reps.toDoubleOrNull() ?: 0.0
                            if (w > 0 && r > 0) {
                                // Fórmula Epley: w * (1 + r/30)
                                oneRM = w * (1 + r / 30.0)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Calculate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("CALCULAR")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // RESULTADOS
            if (oneRM > 0) {
                Text("Tu 1RM Estimado", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Text(
                    "${oneRM.roundToInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Tabla de Porcentajes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val percentages = listOf(95, 90, 85, 80, 75, 70, 65, 60, 50)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(percentages) { pct ->
                        PercentageRow(pct, oneRM)
                    }
                }
            }
        }
    }
}

@Composable
fun PercentageRow(percent: Int, oneRM: Double) {
    val value = (oneRM * percent / 100.0).roundToInt()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$percent%", fontWeight = FontWeight.Bold, color = Color.White)
            Text("$value", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}