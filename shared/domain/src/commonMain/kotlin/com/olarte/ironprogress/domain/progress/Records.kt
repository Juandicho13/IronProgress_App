package com.olarte.ironprogress.domain.progress

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.progression.OneRepMax
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.math.round
import kotlin.time.Instant

/**
 * Una serie de trabajo hecha de un ejercicio, con su sesión y la hora en que se completó. Es la
 * materia prima de los récords (F2-03) y de las gráficas del ejercicio (F2-04).
 */
data class ExerciseHistoryEntry(val sessionId: SessionId, val at: Instant, val set: WorkoutSet)

/** Los cuatro récords de un ejercicio de fuerza (ANA-02). */
enum class RecordKind {
    /** El mayor 1RM estimado (11.1). En el modo básico se llama "Fuerza estimada". */
    ESTIMATED_ONE_REP_MAX,

    /** El mayor peso levantado al menos una vez. */
    HEAVIEST_WEIGHT,

    /** Más reps con un mismo peso (o sin peso, en los de peso corporal). */
    MOST_REPS,

    /** El mayor volumen del ejercicio en una sola sesión. */
    BEST_SESSION_VOLUME,
}

/**
 * Un récord batido. [value] está en kg para el 1RM, el peso y el volumen, y en reps para
 * [RecordKind.MOST_REPS]. [weightKg] y [reps] son los de la serie que lo batió; [previous] es la
 * marca que había antes, en la misma unidad que [value].
 */
data class RecordHit(
    val exerciseId: ExerciseId,
    val kind: RecordKind,
    val value: Double,
    val previous: Double,
    val weightKg: Double?,
    val reps: Int?,
    val setId: WorkoutSetId,
    val sessionId: SessionId,
    val at: Instant,
)

/** La mejor marca de un tipo, con la serie y el día en que se hizo. */
data class RecordMark(val value: Double, val weightKg: Double?, val reps: Int?, val sessionId: SessionId, val at: Instant)

/** Los récords vigentes de un ejercicio. [mostRepsByWeight] usa el peso redondeado a 10 g (0 sin peso). */
data class ExerciseRecords(
    val estimatedOneRepMax: RecordMark? = null,
    val heaviestWeight: RecordMark? = null,
    val bestSessionVolume: RecordMark? = null,
    val mostRepsByWeight: Map<Double, RecordMark> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = estimatedOneRepMax == null && heaviestWeight == null && bestSessionVolume == null && mostRepsByWeight.isEmpty()
}

/**
 * Récords automáticos (F2-03). Solo cuentan series de trabajo completadas, sin las rondas de
 * intervalos y bloques funcionales, y con al menos una rep. La primera vez que se hace un ejercicio
 * no hay récord: hace falta una marca anterior de otra sesión para batirla.
 */
object Records {
    /** El 1RM estimado solo vale si reps + RIR no pasa de esto (11.1). */
    const val MAX_REPS_FOR_ESTIMATE = OneRepMax.MAX_REPS_IN_FORMULA

    /** Lo que hay que superar para que cuente: evita récords por redondeo. */
    private const val EPSILON = 0.005

    /** Orden de importancia: el aviso en vivo muestra el primero que se bata. */
    val priority: List<RecordKind> = RecordKind.entries

    /**
     * 1RM estimado con la misma fórmula del motor de progresión ([OneRepMax], HANDOFF 11.1), para
     * que los récords y las sugerencias nunca den números distintos.
     */
    fun estimatedOneRepMax(weightKg: Double?, reps: Int?, rir: Int? = null): Double? =
        OneRepMax.estimate(weightKg ?: return null, reps ?: return null, rir)

    fun estimatedOneRepMax(set: WorkoutSet): Double? = estimatedOneRepMax(set.weightKg, set.reps, set.rir)

    /** Si la serie entra en los récords: de trabajo, completada, con reps y fuera de una ronda. */
    fun counts(set: WorkoutSet): Boolean = set.isCompleted && set.setType.countsAsWorking && set.round == null && (set.reps ?: 0) > 0

    /** Llave del peso para "más reps con este peso": redondeado a 10 g; sin peso es 0. */
    fun weightKey(weightKg: Double?): Double = round((weightKg ?: 0.0) * 100) / 100

    /** Volumen de una serie: peso por reps. */
    fun volumeOf(set: WorkoutSet): Double = (set.weightKg ?: 0.0) * (set.reps ?: 0)

    /** Los récords vigentes con toda la historia de un ejercicio. */
    fun bests(history: List<ExerciseHistoryEntry>): ExerciseRecords {
        val entries = history.filter { counts(it.set) }.sortedBy { it.at }
        var oneRm: RecordMark? = null
        var heaviest: RecordMark? = null
        val reps = mutableMapOf<Double, RecordMark>()
        entries.forEach { entry ->
            val set = entry.set
            estimatedOneRepMax(set)?.let { value ->
                val best = oneRm
                if (best == null || value > best.value + EPSILON) oneRm = mark(value, entry)
            }
            val weight = set.weightKg?.takeIf { it > 0 }
            val top = heaviest
            if (weight != null && (top == null || weight > top.value + EPSILON)) heaviest = mark(weight, entry)
            val key = weightKey(set.weightKg)
            val count = (set.reps ?: 0).toDouble()
            val current = reps[key]
            if (current == null || count > current.value) reps[key] = mark(count, entry)
        }
        val volume = entries.groupBy { it.sessionId }.mapNotNull { (sessionId, items) ->
            val total = items.sumOf { volumeOf(it.set) }
            if (total <= 0) null else RecordMark(total, null, null, sessionId, items.maxOf { it.at })
        }.maxByOrNull { it.value }
        return ExerciseRecords(oneRm, heaviest, volume, reps)
    }

    /**
     * Los récords que bate [set] al completarse. [history] son las series de sesiones anteriores y
     * [earlierToday] las de esta misma sesión hechas antes que [set]: la marca a superar es la mejor
     * de las dos. Sin historia de otras sesiones no hay récord.
     */
    fun detect(
        exerciseId: ExerciseId,
        history: List<ExerciseHistoryEntry>,
        earlierToday: List<ExerciseHistoryEntry>,
        set: ExerciseHistoryEntry,
    ): List<RecordHit> {
        if (!counts(set.set)) return emptyList()
        val past = history.filter { counts(it.set) && it.sessionId != set.sessionId }
        if (past.isEmpty()) return emptyList()
        val today = earlierToday.filter { counts(it.set) && it.set.id != set.set.id }
        val before = bests(past + today)
        val hits = mutableListOf<RecordHit>()
        fun hit(kind: RecordKind, value: Double, previous: Double) {
            hits += RecordHit(exerciseId, kind, value, previous, set.set.weightKg, set.set.reps, set.set.id, set.sessionId, set.at)
        }
        estimatedOneRepMax(set.set)?.let { value ->
            val best = before.estimatedOneRepMax?.value
            if (best != null && value > best + EPSILON) hit(RecordKind.ESTIMATED_ONE_REP_MAX, value, best)
        }
        set.set.weightKg?.takeIf { it > 0 }?.let { weight ->
            val best = before.heaviestWeight?.value
            if (best != null && weight > best + EPSILON) hit(RecordKind.HEAVIEST_WEIGHT, weight, best)
        }
        before.mostRepsByWeight[weightKey(set.set.weightKg)]?.let { best ->
            val reps = (set.set.reps ?: 0).toDouble()
            if (reps > best.value) hit(RecordKind.MOST_REPS, reps, best.value)
        }
        val bestVolume = past.groupBy { it.sessionId }.values.maxOfOrNull { items -> items.sumOf { volumeOf(it.set) } }
        if (bestVolume != null && bestVolume > 0) {
            val volumeBefore = today.sumOf { volumeOf(it.set) }
            val volumeAfter = volumeBefore + volumeOf(set.set)
            if (volumeBefore <= bestVolume + EPSILON && volumeAfter > bestVolume + EPSILON) {
                hit(RecordKind.BEST_SESSION_VOLUME, volumeAfter, bestVolume)
            }
        }
        return hits.sortedBy { priority.indexOf(it.kind) }
    }

    /**
     * Repite la sesión serie por serie y devuelve los récords que quedaron al final: si un tipo se
     * batió dos veces, solo el último (el mejor). Es lo que lista el resumen.
     */
    fun replay(exerciseId: ExerciseId, history: List<ExerciseHistoryEntry>, session: List<ExerciseHistoryEntry>): List<RecordHit> {
        val ordered = session.filter { counts(it.set) }.sortedBy { it.at }
        val hits = linkedMapOf<RecordKind, RecordHit>()
        ordered.forEachIndexed { index, entry ->
            detect(exerciseId, history, ordered.take(index), entry).forEach { hits[it.kind] = it }
        }
        // El volumen se bate una vez, al cruzar la marca; el resumen muestra el total de la sesión.
        hits[RecordKind.BEST_SESSION_VOLUME]?.let { hit ->
            hits[RecordKind.BEST_SESSION_VOLUME] = hit.copy(value = ordered.sumOf { volumeOf(it.set) })
        }
        return hits.values.sortedBy { priority.indexOf(it.kind) }
    }

    private fun mark(value: Double, entry: ExerciseHistoryEntry) =
        RecordMark(value, entry.set.weightKg, entry.set.reps, entry.sessionId, entry.at)
}

/** Las series de trabajo de un ejercicio en una sesión, como entradas de historia. */
fun SessionDetail.historyOf(exerciseId: ExerciseId): List<ExerciseHistoryEntry> {
    val ids = exercises.filter { it.exerciseId == exerciseId }.map { it.id }.toSet()
    return sets.filter { it.sessionExerciseId in ids && it.completedAt != null }
        .map { ExerciseHistoryEntry(session.id, requireNotNull(it.completedAt), it) }
}

/**
 * Récords que bate una serie recién completada en la sesión activa (F2-03). Es la entrada que usan
 * el aviso del entreno y, más adelante, la Live Activity y la Live Update (F2-15).
 */
class DetectRecords(private val sessions: SessionRepository) {
    suspend operator fun invoke(detail: SessionDetail, setId: WorkoutSetId): List<RecordHit> {
        val set = detail.sets.firstOrNull { it.id == setId } ?: return emptyList()
        val completedAt = set.completedAt ?: return emptyList()
        val exercise = detail.exercises.firstOrNull { it.id == set.sessionExerciseId } ?: return emptyList()
        val today = detail.historyOf(exercise.exerciseId).filter { it.set.id != setId && it.at <= completedAt }
        val history = sessions.exerciseHistory(exercise.exerciseId).filter { it.sessionId != detail.session.id }
        return Records.detect(exercise.exerciseId, history, today, ExerciseHistoryEntry(detail.session.id, completedAt, set))
    }
}
