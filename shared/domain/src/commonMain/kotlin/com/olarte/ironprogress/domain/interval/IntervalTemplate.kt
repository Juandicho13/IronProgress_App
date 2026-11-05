package com.olarte.ironprogress.domain.interval

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.time.TimerPlan
import com.olarte.ironprogress.domain.time.TimerPlans
import com.olarte.ironprogress.domain.voice.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline

@JvmInline
value class IntervalTemplateId(val value: String)

/**
 * Intervalos a tu medida (PANTALLAS 5.2): trabajo y descanso en segundos, rondas y los ejercicios
 * que se alternan en cada ronda. Los límites son los de los botones de la pantalla.
 */
data class IntervalTemplate(
    val id: IntervalTemplateId,
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val exerciseIds: List<ExerciseId> = emptyList(),
    val voice: VoiceSettings = VoiceSettings(),
) {
    init {
        require(name.isNotBlank()) { "El intervalo necesita un nombre" }
        require(workSeconds in WORK_RANGE && workSeconds % STEP == 0) { "El trabajo va de 5 s a 5 min, de 5 en 5" }
        require(restSeconds in REST_RANGE && restSeconds % STEP == 0) { "El descanso va de 0 a 5 min, de 5 en 5" }
        require(rounds in ROUNDS_RANGE) { "Las rondas van de 1 a 50" }
    }

    /** Una ronda: trabajo más descanso. */
    val roundSeconds: Int get() = workSeconds + restSeconds

    /** Total = rondas × trabajo + (rondas - 1) × descanso: el último descanso no cuenta. */
    val totalSeconds: Int get() = rounds * workSeconds + (rounds - 1) * restSeconds

    /** El ejercicio de una ronda (empieza en 1), o null si no hay ejercicios. */
    fun exerciseFor(round: Int): ExerciseId? = exerciseIds.takeIf { it.isNotEmpty() }?.let { it[(round - 1) % it.size] }

    /** La base que coincide con estos tiempos, o null si es "A tu medida". */
    val preset: IntervalPreset?
        get() = IntervalPreset.entries.firstOrNull { it.workSeconds == workSeconds && it.restSeconds == restSeconds }

    fun toPlan(): TimerPlan = TimerPlans.intervals(
        workSeconds = workSeconds,
        restSeconds = restSeconds,
        rounds = rounds,
        exerciseIds = exerciseIds.map { it.value },
    )

    fun withPreset(preset: IntervalPreset): IntervalTemplate =
        copy(workSeconds = preset.workSeconds, restSeconds = preset.restSeconds, rounds = preset.rounds)

    companion object {
        const val STEP = 5
        val WORK_RANGE = 5..300
        val REST_RANGE = 0..300
        val ROUNDS_RANGE = 1..50
    }
}

/** Las bases de los chips de 5.2. */
enum class IntervalPreset(val workSeconds: Int, val restSeconds: Int, val rounds: Int) {
    TABATA(20, 10, 8),
    FIFTEEN_FORTY_FIVE(15, 45, 10),
    THIRTY_THIRTY(30, 30, 10),
    FORTY_TWENTY(40, 20, 10),
}

interface IntervalTemplateRepository {
    fun observeTemplates(): Flow<List<IntervalTemplate>>

    suspend fun findTemplate(id: IntervalTemplateId): IntervalTemplate?

    suspend fun saveTemplate(template: IntervalTemplate)

    suspend fun deleteTemplate(id: IntervalTemplateId)
}
