package com.olarte.ironprogress.domain.settings

import com.olarte.ironprogress.domain.catalog.Discipline
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Idioma de la app. SYSTEM sigue el del teléfono. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
}

/** Tema de la app. SYSTEM sigue el claro u oscuro del teléfono. */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

enum class TrainingGoal {
    STRENGTH,
    MUSCLE,
    ENDURANCE,
    HEALTH,
    WEIGHT,
}

enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
}

/** Unidad de peso por defecto. Cada ejercicio puede cambiarla (F1-22); la base guarda siempre kg. */
enum class WeightUnit {
    KG,
    LB,
}

/**
 * Qué pasa con la música del teléfono cuando la app habla durante el entreno. Por defecto la música
 * sigue igual; [NO_VOICE] apaga la voz y la música no se toca nunca.
 */
enum class WorkoutAudio {
    KEEP_VOLUME,
    DUCK,
    PAUSE_MUSIC,
    NO_VOICE,
}

/** Lo que se responde en la bienvenida (CFG-01). */
@Serializable
data class TrainingProfile(
    val goal: TrainingGoal = TrainingGoal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val unit: WeightUnit = WeightUnit.KG,
    val daysPerWeek: Int = 3,
    val disciplines: Set<Discipline> = setOf(Discipline.STRENGTH),
) {
    init {
        require(daysPerWeek in DAYS_RANGE) { "daysPerWeek fuera de $DAYS_RANGE" }
    }

    companion object {
        val DAYS_RANGE = 1..7
    }
}

/**
 * Ajustes de la app (CFG-02 y CFG-03). Viven en DataStore hasta F3, cuando pasan a la cuenta.
 * [hideBodyMetrics] oculta todo peso corporal y toda caloría en la app y en los avisos.
 */
@Serializable
data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val voice: Boolean = true,
    val hideBodyMetrics: Boolean = false,
    /** Modo Pro: muestra RIR y RPE para quien entrena por esfuerzo; apagado, solo peso y reps. */
    val proMode: Boolean = false,
    /** Música y voz durante el entreno. Con [voice] apagado cuenta como [WorkoutAudio.NO_VOICE]. */
    val audio: WorkoutAudio = WorkoutAudio.KEEP_VOLUME,
    val onboardingDone: Boolean = false,
    val profile: TrainingProfile? = null,
    /** Las rutinas demo ya se sembraron (o no hacían falta): no vuelven aunque se borren. */
    val demoRoutinesSeeded: Boolean = false,
) {
    /** Lo que de verdad se usa: la voz apagada de antes de este ajuste también cuenta como "Sin voz". */
    val workoutAudio: WorkoutAudio get() = if (!voice) WorkoutAudio.NO_VOICE else audio

    /** Elegir "Sin voz" apaga la voz; cualquier otra opción la prende. */
    fun withWorkoutAudio(value: WorkoutAudio): AppSettings = copy(audio = value, voice = value != WorkoutAudio.NO_VOICE)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** Cambia los ajustes de forma atómica: [transform] recibe los vigentes. */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Guarda lo elegido en la bienvenida y la marca como hecha: no vuelve a salir. */
class CompleteOnboarding(private val repository: SettingsRepository) {
    suspend operator fun invoke(profile: TrainingProfile, proMode: Boolean? = null, audio: WorkoutAudio? = null) {
        repository.update { current ->
            var next = current.copy(profile = profile, onboardingDone = true)
            if (proMode != null) next = next.copy(proMode = proMode)
            if (audio != null) next = next.withWorkoutAudio(audio)
            next
        }
    }
}
