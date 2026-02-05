package com.example.ironprogress

import android.content.Context // Importante para vibrar
import android.media.RingtoneManager
import android.net.Uri
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

@Composable
fun TimerScreen(
    durationSeconds: Int,
    nextExerciseName: String,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    // --- ESTADO ---
    var timeLeft by remember { mutableLongStateOf(durationSeconds.toLong()) }
    var targetEndTime by remember { mutableStateOf<Long?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 1. INICIALIZAR HORA FIN
    LaunchedEffect(Unit) {
        targetEndTime = System.currentTimeMillis() + (durationSeconds * 1000)
    }

    // 2. BUCLE DE TIEMPO REAL + SONIDO + VIBRACIÓN
    LaunchedEffect(targetEndTime) {
        while (targetEndTime != null && timeLeft > 0) {
            val now = System.currentTimeMillis()
            val remaining = targetEndTime!! - now

            if (remaining <= 0) {
                timeLeft = 0

                // --- 🔔 1. SONIDO (Notificación suave) ---
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // --- 📳 2. VIBRACIÓN FUERTE (1.5 seg) ---
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= 26) {
                        // El 255 es la AMPLITUD MÁXIMA (Vibra duro)
                        vibrator.vibrate(VibrationEffect.createOneShot(1500, 255))
                    } else {
                        // Versiones viejas de Android
                        vibrator.vibrate(1500)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                onFinish() // Avanzamos
                break
            } else {
                timeLeft = (remaining / 1000) + 1
                delay(100)
            }
        }
    }

    // 3. INTERCEPTAR GESTO ATRÁS
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // PUBLICIDAD SUPERIOR
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            AdmobBanner()
        }

        // ZONA DEL RELOJ
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // AVISO SIGUIENTE EJERCICIO
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

            // NÚMEROS GIGANTES
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$timeLeft",
                    fontSize = 140.sp,
                    fontWeight = FontWeight.Black,
                    color = if (targetEndTime == null) Color.Gray else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 140.sp,
                    letterSpacing = (-4).sp
                )
            }

            // BOTONES DE CONTROL (SOLO 2)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonSize = 70.dp
                val buttonShape = RoundedCornerShape(20.dp)

                // Botón +30s
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

                // Botón Pausa/Play
                Button(
                    onClick = {
                        if (targetEndTime != null) {
                            targetEndTime = null // PAUSA
                        } else {
                            targetEndTime = System.currentTimeMillis() + (timeLeft * 1000) // PLAY
                        }
                    },
                    shape = buttonShape,
                    modifier = Modifier.size(buttonSize),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (targetEndTime == null) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pausar",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // PUBLICIDAD INFERIOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            AdmobRectangle()
        }
    }

    // DIÁLOGO DE SALIDA
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