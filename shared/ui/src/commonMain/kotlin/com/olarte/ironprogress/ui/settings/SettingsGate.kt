package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.ui.format.LocalAppLanguageTag
import com.olarte.ironprogress.ui.format.systemLanguageTag
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.koin.compose.viewmodel.koinViewModel

/** Ajustes vigentes para cualquier pantalla: tema, vibración, voz y ocultar peso corporal y calorías. */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/** Lo usa Ajustes para cambiar un valor sin pedir su propio ViewModel. */
val LocalSettingsIntents = staticCompositionLocalOf<(SettingsIntent) -> Unit> { {} }

/** Claro u oscuro según el ajuste; con "Sistema", el del teléfono. */
@Composable
fun AppSettings.darkTheme(): Boolean = when (theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

/**
 * Envuelve la app: aplica idioma y tema sin reiniciar y muestra la bienvenida solo la primera
 * vez. Mientras DataStore carga no pinta nada para no mostrar la bienvenida un instante.
 */
@Composable
fun SettingsGate(content: @Composable () -> Unit) {
    val model = koinViewModel<SettingsViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    if (state.loading) return
    val settings = state.settings
    CompositionLocalProvider(appLocaleProvider(settings.language.tag)) {
        key(settings.language) {
            CompositionLocalProvider(
                LocalAppSettings provides settings,
                LocalSettingsIntents provides model::onIntent,
                LocalAppLanguageTag provides (settings.language.tag ?: systemLanguageTag()),
            ) {
                if (settings.onboardingDone) {
                    content()
                } else {
                    ForjaTheme(darkTheme = settings.darkTheme(), hapticsEnabled = settings.vibration) {
                        OnboardingScreen { model.onIntent(SettingsIntent.FinishOnboarding(it)) }
                    }
                }
            }
        }
    }
}
