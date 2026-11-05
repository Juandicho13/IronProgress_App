package com.olarte.ironprogress

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.olarte.ironprogress.di.resolveRestController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Sin estilos fijos: los iconos de las barras siguen el tema claro u oscuro del sistema, como ForjaTheme.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleWorkoutIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWorkoutIntent(intent)
    }

    private fun handleWorkoutIntent(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_WORKOUT) return
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        intent.removeExtra(EXTRA_SESSION_ID)
        resolveRestController().requestOpenWorkout(sessionId)
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.olarte.ironprogress.action.OPEN_WORKOUT"
        const val EXTRA_SESSION_ID = "com.olarte.ironprogress.extra.SESSION_ID"
    }
}
