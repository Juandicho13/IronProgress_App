package com.example.ironprogress

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Pantalla de Temporizador de Descanso (Rest Timer).
 *
 * Esta pantalla se muestra entre series para gestionar la recuperación del atleta.
 * Características principales:
 * 1. Cuenta regresiva visual de alto contraste.
 * 2. Feedback háptico (vibración) y sonoro al finalizar.
 * 3. Controles de reproducción (Pausa/Play) y extensión rápida (+30s).
 * 4. Espacios publicitarios de alta visibilidad (Banner + MREC) aprovechando el tiempo de espera.
 *
 * @param durationSeconds Duración inicial del descanso.
 * @param nextExerciseName Nombre del siguiente ejercicio (Previsualización).
 * @param onFinish Callback ejecutado cuando el tiempo termina o el usuario salta el descanso.
 */
@Composable
fun TimerScreen(
    durationSeconds: Int,
    nextExerciseName: String,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    //region --- Estado del Temporizador ---
    // Tiempo restante en segundos para visualización
    var timeLeft by remember { mutableLongStateOf(durationSeconds.toLong()) }

    // Marca de tiempo objetivo (Epoch millis). Si es null, el timer está PAUSADO.
    var targetEndTime by remember { mutableStateOf<Long?>(null) }

    // Control del diálogo de confirmación de salida
    var showExitDialog by remember { mutableStateOf(false) }
    //endregion

    // 1. Inicialización: Establecer el tiempo objetivo al montar el componente
    LaunchedEffect(Unit) {
        targetEndTime = System.currentTimeMillis() + (durationSeconds * 1000)
    }

    // 2. Bucle de Cronómetro (Game Loop pattern)
    // Se ejecuta cada vez que targetEndTime cambia o se recompone
    LaunchedEffect(targetEndTime) {
        while (targetEndTime != null && timeLeft > 0) {
            val now = System.currentTimeMillis()
            val remaining = targetEndTime!! - now

            if (remaining <= 0) {
                // --- FIN DEL TEMPORIZADOR ---
                timeLeft = 0

                // A. Feedback Auditivo (Sonido de notificación predeterminado)
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // B. Feedback Háptico (Vibración)
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // API 26+: Vibración con amplitud controlada (255 = Máxima intensidad)
                        vibrator.vibrate(VibrationEffect.createOneShot(1500, 255))
                    } else {
                        // API < 26: Vibración estándar (Legacy)
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(1500)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Navegar al siguiente estado
                onFinish()
                break
            } else {
                // Actualización de UI y espera
                timeLeft = (remaining / 1000) + 1
                delay(100) // Polling cada 100ms para precisión aceptable
            }
        }
    }

    // 3. Intercepción del botón "Atrás"
    // Evita saltar el descanso accidentalmente sin confirmación.
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // --- Monetización: Banner Superior ---
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            AdmobBanner()
        }

        // --- Cuerpo Principal ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Previsualización del Siguiente Ejercicio
            if (nextExerciseName.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "A continuación:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nextExerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Visualización del Tiempo (Big Number)
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$timeLeft",
                    fontSize = 140.sp,
                    fontWeight = FontWeight.Black,
                    // Cambia a gris si está pausado para feedback visual
                    color = if (targetEndTime == null) Color.Gray else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 140.sp,
                    letterSpacing = (-4).sp
                )
            }

            // Controles de Reproducción
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonSize = 70.dp
                val buttonShape = RoundedCornerShape(20.dp)

                // Botón Extender Tiempo (+30s)
                FilledTonalButton(
                    onClick = {
                        if (targetEndTime != null) {
                            targetEndTime = targetEndTime!! + 30000
                        } else {
                            timeLeft += 30
                        }
                    },
                    shape = buttonShape,
                    modifier = Modifier.size(buttonSize),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("30s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(32.dp))

                // Botón Play/Pause
                Button(
                    onClick = {
                        if (targetEndTime != null) {
                            targetEndTime = null // Acción: PAUSAR
                        } else {
                            // Acción: REANUDAR (Recalcular target basado en tiempo restante actual)
                            targetEndTime = System.currentTimeMillis() + (timeLeft * 1000)
                        }
                    },
                    shape = buttonShape,
                    modifier = Modifier.size(buttonSize),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (targetEndTime == null) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (targetEndTime == null) "Reanudar" else "Pausar",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // --- Monetización: Rectángulo Inferior (MREC) ---
        // Este formato (300x250) tiene mayor CPM y es ideal para pantallas de espera.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            AdmobRectangle()
        }
    }

    // --- Diálogo de Confirmación de Salida ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "¿Saltar descanso?") },
            text = { Text(text = "Si sales ahora, pasarás inmediatamente al siguiente ejercicio.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("SALTAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CANCELAR", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}