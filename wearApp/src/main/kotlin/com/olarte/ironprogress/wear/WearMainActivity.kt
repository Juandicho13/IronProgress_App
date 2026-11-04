package com.olarte.ironprogress.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.olarte.ironprogress.ui.theme.ForjaDarkColors

/**
 * Esqueleto de la app del Galaxy Watch (F0-11). Las caras de serie, descanso e intervalos
 * vuelven en F4-13, movidas por el motor de tiempo; el pulso llega en F4-07.
 */
class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForjaWearTheme {
                WearHome()
            }
        }
    }
}

/** Tema del reloj: Material 3 de Wear con los colores oscuros de Forja sobre negro puro. */
@Composable
fun ForjaWearTheme(content: @Composable () -> Unit) {
    val forja = ForjaDarkColors
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = forja.ember,
            onPrimary = forja.onEmber,
            secondary = forja.ice,
            background = Color.Black,
            onBackground = forja.text1,
            onSurface = forja.text1,
            onSurfaceVariant = forja.text2,
            error = forja.danger,
            onError = forja.onDanger,
        ),
        content = content,
    )
}

@Composable
private fun WearHome() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = ForjaDarkColors.ember,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
