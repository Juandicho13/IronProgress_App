package com.olarte.ironprogress.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.routine.SeedDemoRoutines
import com.olarte.ironprogress.domain.settings.AppLanguage
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.CompleteOnboarding
import com.olarte.ironprogress.domain.settings.SettingsRepository
import com.olarte.ironprogress.domain.settings.ThemeMode
import com.olarte.ironprogress.domain.settings.TrainingProfile
import com.olarte.ironprogress.domain.settings.WorkoutAudio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val loading: Boolean = true, val settings: AppSettings = AppSettings())

sealed interface SettingsIntent {
    data class Language(val value: AppLanguage) : SettingsIntent

    data class Theme(val value: ThemeMode) : SettingsIntent

    data class Sound(val on: Boolean) : SettingsIntent

    data class Vibration(val on: Boolean) : SettingsIntent

    data class Voice(val on: Boolean) : SettingsIntent

    data class HideBodyMetrics(val on: Boolean) : SettingsIntent

    data class ProMode(val on: Boolean) : SettingsIntent

    data class Audio(val value: WorkoutAudio) : SettingsIntent

    data class FinishOnboarding(val profile: TrainingProfile, val proMode: Boolean? = null, val audio: WorkoutAudio? = null) :
        SettingsIntent
}

/** Ajustes y bienvenida (CFG-01 a CFG-03). Cada cambio se guarda al momento en DataStore. */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val completeOnboarding: CompleteOnboarding,
    private val seedDemoRoutines: SeedDemoRoutines? = null,
) : ViewModel() {

    val state: StateFlow<SettingsState> = repository.settings
        .map { SettingsState(loading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    init {
        // Con la bienvenida ya hecha (o al terminarla) siembra las rutinas demo una sola vez.
        viewModelScope.launch {
            val done = repository.settings.first { it.onboardingDone }
            if (!done.demoRoutinesSeeded) runCatching { seedDemoRoutines?.invoke() }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.FinishOnboarding -> completeOnboarding(intent.profile, intent.proMode, intent.audio)
                else -> repository.update { it.apply(intent) }
            }
        }
    }

    private fun AppSettings.apply(intent: SettingsIntent): AppSettings = when (intent) {
        is SettingsIntent.Language -> copy(language = intent.value)
        is SettingsIntent.Theme -> copy(theme = intent.value)
        is SettingsIntent.Sound -> copy(sound = intent.on)
        is SettingsIntent.Vibration -> copy(vibration = intent.on)
        is SettingsIntent.Voice -> copy(voice = intent.on)
        is SettingsIntent.HideBodyMetrics -> copy(hideBodyMetrics = intent.on)
        is SettingsIntent.ProMode -> copy(proMode = intent.on)
        is SettingsIntent.Audio -> withWorkoutAudio(intent.value)
        is SettingsIntent.FinishOnboarding -> this
    }
}
