package com.olarte.ironprogress.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.ui.format.LocalAppLanguageTag
import com.olarte.ironprogress.ui.format.resolvedLanguageTag
import com.olarte.ironprogress.ui.format.systemRegion
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.resolved_language
import com.olarte.ironprogress.ui.theme.ForjaTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Ajustes vigentes para cualquier pantalla: tema, vibración, voz y ocultar peso corporal y calorías. */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/**
 * A dónde ir justo después de la bienvenida ("Crear mi primera rutina" o "Ir a Hoy"). La app lo
 * lee una vez y lo limpia.
 */
val LocalOnboardingExit = staticCompositionLocalOf<MutableState<OnboardingExit?>> { mutableStateOf(null) }

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
    val exit = remember { mutableStateOf<OnboardingExit?>(null) }
    if (state.loading) return
    val settings = state.settings
    CompositionLocalProvider(appLocaleProvider(settings.language.tag)) {
        key(settings.language) {
            val resolved = stringResource(Res.string.resolved_language)
            CompositionLocalProvider(
                LocalAppSettings provides settings,
                LocalSettingsIntents provides model::onIntent,
                LocalAppLanguageTag provides resolvedLanguageTag(settings.language.tag ?: resolved, systemRegion()),
                LocalOnboardingExit provides exit,
            ) {
                if (settings.onboardingDone) {
                    content()
                } else {
                    ForjaTheme(darkTheme = settings.darkTheme(), hapticsEnabled = settings.vibration) {
                        OnboardingScreen { result ->
                            exit.value = result.exit
                            model.onIntent(SettingsIntent.FinishOnboarding(result.profile, result.proMode, result.audio))
                        }
                    }
                }
            }
        }
    }
}
