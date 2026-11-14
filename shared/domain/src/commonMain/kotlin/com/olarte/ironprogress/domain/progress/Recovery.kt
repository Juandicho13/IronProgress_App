package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.session.SessionDetail
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Verde, ámbar o rojo del mapa (HANDOFF 11.5). */
enum class RecoveryLevel { FRESH, RECOVERING, LOADED }

/** Estado de un músculo ahora. [heat] va de 0 (fresco) a 4 (cargado), como heat0 a heat4. */
data class MuscleRecovery(
    val muscle: Muscle,
    val fatigue: Double,
    val level: RecoveryLevel,
    val heat: Int,
    /** Cuándo baja de [Recovery.AMBER_FROM]; null si ya está listo. */
    val readyAt: Instant?,
    val lastTrainedAt: Instant?,
    /** Agujetas de 2 o más contadas en las últimas 48 h: el músculo queda por lo menos en ámbar. */
    val sore: Boolean = false,
)

/**
 * Mapa de recuperación (HANDOFF 11.5):
 *
 * ```
 * F_m(t) = Σ_i s_i · 0,5^((t - t_i) / h_m)
 * ```
 *
 * Cada serie de trabajo completada suma a un músculo su peso de [MuscleSets.setWeights] (1 el
 * principal, 0,5 el secundario o el porcentaje de 11.10) y esa fatiga baja a la mitad cada
 * [com.olarte.ironprogress.domain.catalog.MuscleSize.recoveryHalfLifeHours] horas. Verde por
 * debajo de 3, ámbar de 3 a 6 y rojo por encima de 6. Las agujetas de F2-06 (2 o más en las
 * últimas 48 h) dejan el músculo por lo menos en ámbar aunque la cuenta diga verde.
 */
object Recovery {
    const val AMBER_FROM = 3.0
    const val RED_ABOVE = 6.0

    /** Más atrás de dos semanas la fatiga ya no se nota (siete vidas medias de 48 h). */
    val WINDOW = 14.days

    /** Las agujetas contestadas cuentan durante dos días. */
    val SORENESS_WINDOW = 48.hours

    fun level(fatigue: Double): RecoveryLevel = when {
        fatigue > RED_ABOVE -> RecoveryLevel.LOADED
        fatigue >= AMBER_FROM -> RecoveryLevel.RECOVERING
        else -> RecoveryLevel.FRESH
    }

    /** heat0 sin carga, heat1 verde, heat2 y heat3 las dos mitades del ámbar, heat4 rojo. */
    fun heat(fatigue: Double): Int = when {
        fatigue < 0.5 -> 0
        fatigue < AMBER_FROM -> 1
        fatigue < 4.5 -> 2
        fatigue <= RED_ABOVE -> 3
        else -> 4
    }

    /** Fatiga ahora de unas series hechas en [contributions] (hora y cuánto suman). */
    fun fatigue(contributions: List<Pair<Instant, Double>>, halfLifeHours: Int, now: Instant): Double =
        contributions.sumOf { (at, weight) ->
            if (at > now) weight else weight * 0.5.pow((now - at).inWholeMinutes / (halfLifeHours * 60.0))
        }

    /**
     * Cuándo la fatiga baja de [AMBER_FROM]. Como todas las series de un músculo bajan al mismo
     * ritmo, F(t) = F(ahora) · 0,5^(Δ/h) y Δ = h · log2(F / 3).
     */
    fun readyAt(fatigue: Double, halfLifeHours: Int, now: Instant): Instant? {
        if (fatigue < AMBER_FROM) return null
        val hours = halfLifeHours * ln(fatigue / AMBER_FROM) / ln(2.0)
        return now + (hours * 60).toLong().coerceAtLeast(1).minutes
    }

    /**
     * El estado de cada músculo del catálogo con las sesiones de [details] (solo cuentan las series
     * de las últimas dos semanas) y las agujetas de [feedback].
     */
    fun of(
        details: List<SessionDetail>,
        exercises: Map<ExerciseId, Exercise>,
        now: Instant,
        feedback: List<SessionFeedback> = emptyList(),
    ): Map<Muscle, MuscleRecovery> {
        val contributions = mutableMapOf<Muscle, MutableList<Pair<Instant, Double>>>()
        details.forEach { detail ->
            detail.exercises.forEach { sessionExercise ->
                val exercise = exercises[sessionExercise.exerciseId] ?: return@forEach
                val weights = MuscleSets.setWeights(exercise)
                detail.setsOf(sessionExercise.id).forEach { set ->
                    val at = set.completedAt ?: return@forEach
                    if (!set.setType.countsAsWorking || now - at > WINDOW) return@forEach
                    weights.forEach { (muscle, weight) -> contributions.getOrPut(muscle) { mutableListOf() } += at to weight }
                }
            }
        }
        val sore = feedback.filter { row ->
            val at = row.sorenessAt ?: return@filter false
            (row.soreness ?: 0) >= 2 && now - at in Duration.ZERO..SORENESS_WINDOW
        }.map { it.muscle }.toSet()
        return Muscle.entries.associateWith { muscle ->
            val items = contributions[muscle].orEmpty()
            val halfLife = muscle.size.recoveryHalfLifeHours
            val value = fatigue(items, halfLife, now)
            var level = level(value)
            var heat = heat(value)
            val isSore = muscle in sore
            if (isSore && level == RecoveryLevel.FRESH) {
                level = RecoveryLevel.RECOVERING
                heat = maxOf(heat, 2)
            }
            val readyAt = readyAt(value, halfLife, now)
                ?: if (isSore) {
                    feedback.filter {
                        it.muscle == muscle
                    }.mapNotNull { it.sorenessAt }.maxOrNull()?.plus(SORENESS_WINDOW)
                } else {
                    null
                }
            MuscleRecovery(
                muscle = muscle,
                fatigue = value,
                level = level,
                heat = heat,
                readyAt = readyAt,
                lastTrainedAt = items.maxOfOrNull { it.first },
                sore = isSore,
            )
        }
    }

    /** Los músculos que no están listos, del más cargado al menos. */
    fun notReady(states: Map<Muscle, MuscleRecovery>): List<MuscleRecovery> =
        states.values.filter { it.level != RecoveryLevel.FRESH }.sortedByDescending { it.fatigue }
}
