package com.olarte.ironprogress.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.olarte.ironprogress.ui.theme.ForjaTheme
import kotlinx.coroutines.delay

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForjaTheme(darkTheme = true) {
                ForjaWatchApp()
            }
        }
    }
}

/**
 * Las 2 caras del Galaxy Watch4 (196 x 196 dp) definidas en la pagina 43 del sistema Forja:
 * 1) Serie actual con boton Completar en brasa.
 * 2) Descanso con anillo perimetral en hielo, cuenta regresiva, pulso y botones +15 s / Listo.
 */
@Composable
fun ForjaWatchApp() {
    val colors = ForjaTheme.colors
    val typography = ForjaTheme.typography
    val haptics = ForjaTheme.haptics

    var isResting by rememberSaveable { mutableStateOf(false) }
    var currentSet by rememberSaveable { mutableIntStateOf(3) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(72) }

    LaunchedEffect(isResting, remainingSeconds) {
        if (isResting && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
            if (remainingSeconds in 1..3) {
                haptics.restCountdownTick()
            } else if (remainingSeconds == 0) {
                haptics.restFinished()
                isResting = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        if (isResting) {
            val minutes = remainingSeconds / 60
            val seconds = (remainingSeconds % 60).toString().padStart(2, '0')

            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val strokeWidth = 5.dp.toPx()
                drawArc(
                    color = colors.surface3,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = colors.ice,
                    startAngle = -90f,
                    sweepAngle = 360f * (remainingSeconds / 90f).coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Descanso · ♥ 128",
                    style = typography.caption,
                    color = colors.iceInk,
                )
                Text(
                    text = "$minutes:$seconds",
                    style = typography.largeFigure,
                    color = colors.text1,
                )
                Text(
                    text = "Siguiente · serie $currentSet de 4",
                    style = typography.caption,
                    color = colors.text2,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 36.dp)
                            .clip(CircleShape)
                            .background(colors.surface3)
                            .clickable {
                                haptics.segmentTick()
                                remainingSeconds += 15
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+15 s",
                            style = typography.label,
                            color = colors.text1,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 36.dp)
                            .clip(CircleShape)
                            .background(colors.ember)
                            .clickable {
                                isResting = false
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Listo",
                            style = typography.label,
                            color = colors.onEmber,
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "serie $currentSet de 4 · ♥ 100",
                    style = typography.caption,
                    color = colors.text2,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "62,5",
                        style = typography.figure,
                        color = colors.text1,
                    )
                    Text(
                        text = "kg",
                        style = typography.caption,
                        color = colors.text2,
                    )
                    Text(
                        text = "×",
                        style = typography.caption,
                        color = colors.text2,
                    )
                    Text(
                        text = "10",
                        style = typography.figure,
                        color = colors.text1,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(width = 124.dp, height = 44.dp)
                        .clip(CircleShape)
                        .background(colors.ember)
                        .clickable {
                            haptics.setCompleted()
                            if (currentSet < 4) currentSet += 1 else currentSet = 1
                            remainingSeconds = 72
                            isResting = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Completar",
                        style = typography.label,
                        color = colors.onEmber,
                    )
                }
            }
        }
    }
}