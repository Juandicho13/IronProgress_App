package com.olarte.ironprogress.domain.feedback

import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Cambio de series de un músculo para la próxima vez que se entrena (11.4). */
enum class SetChange(val delta: Int) {
    ADD(1),
    KEEP(0),
    REMOVE(-1),
}

/** Lo que se contestó en el Resumen para un músculo. */
data class MuscleAnswer(val muscle: Muscle, val load: PerceivedLoad = PerceivedLoad.RIGHT, val pump: Int? = null)

/**
 * Respuestas del Resumen: la carga (y el bombeo en Pro) por músculo y las molestias, que son por
 * articulación. [strongPain] marca las molestias elegidas como fuertes (2) en vez de leves (1).
 */
data class FeedbackAnswers(val muscles: List<MuscleAnswer>, val joints: Set<Joint> = emptySet(), val strongPain: Boolean = false)

/** Series que se agregan y se quitan en la sesión de hoy, y lo que quedó por músculo. */
data class SetChangePlan(
    val added: List<WorkoutSet> = emptyList(),
    val removed: List<WorkoutSetId> = emptyList(),
    val applied: Map<Muscle, SetChange> = emptyMap(),
)

/**
 * Reglas del feedback después de entrenar (HANDOFF 11.4).
 *
 * - Agujetas ya curadas, bombeo bajo y carga fácil: una serie más la próxima vez, hasta el tope del
 *   rango semanal ([MuscleSets.TARGET_MAX]).
 * - Agujetas presentes al volver (2 o más) o carga muy dura: una serie menos. Agujetas leves (1):
 *   se mantiene.
 * - Molestias de 2: proponer un sustituto del ejercicio que las causó ([exercisesToSwap]).
 *
 * El rango de reps y las rutinas se repiten cada semana, así que "la semana siguiente" es la
 * próxima sesión que entrena ese músculo: el cambio se aplica al empezarla, después de preguntar
 * por las agujetas.
 */
object SessionFeedbackRules {
    /** Como mucho cinco músculos en el Resumen, los más trabajados. */
    const val MAX_MUSCLES = 5

    /** Las agujetas se preguntan si el feedback tiene menos de dos semanas. */
    val SORENESS_WINDOW = 14.days

    /** Una molestia fuerte se tiene en cuenta un mes. */
    val PAIN_WINDOW = 30.days

    /**
     * Músculos que se califican: los que sumaron al menos una serie efectiva en la sesión, de más a
     * menos trabajado (empuje: pecho, tríceps y hombros).
     */
    fun trainedMuscles(detail: SessionDetail, exercises: Map<ExerciseId, Exercise>): List<Muscle> =
        rank(MuscleSets.between(listOf(detail), exercises, Instant.DISTANT_PAST, Instant.DISTANT_FUTURE))

    /** Lo mismo desde el Resumen: cuántas series de trabajo hechas tuvo cada ejercicio. */
    fun trainedMuscles(done: Map<ExerciseId, Int>, exercises: Map<ExerciseId, Exercise>): List<Muscle> {
        val totals = mutableMapOf<Muscle, Double>()
        done.forEach { (id, count) ->
            val exercise = exercises[id] ?: return@forEach
            MuscleSets.setWeights(exercise).forEach { (muscle, weight) -> totals[muscle] = (totals[muscle] ?: 0.0) + count * weight }
        }
        return rank(totals)
    }

    private fun rank(totals: Map<Muscle, Double>): List<Muscle> = totals
        .filterValues { it >= 1.0 }
        .entries.sortedWith(compareByDescending<Map.Entry<Muscle, Double>> { it.value }.thenBy { it.key.ordinal })
        .map { it.key }
        .take(MAX_MUSCLES)

    /** Arma una fila por músculo calificado. Las molestias van a los músculos de cada articulación. */
    fun build(
        sessionId: SessionId,
        answers: FeedbackAnswers,
        primary: Collection<Muscle>,
        now: Instant,
        newId: () -> String,
    ): List<SessionFeedback> {
        val rated = answers.muscles.map { it.muscle }
        val pain = mutableMapOf<Muscle, Int>()
        val level = if (answers.strongPain) 2 else 1
        answers.joints.forEach { joint ->
            val hit = rated.filter { it in joint.muscles }.ifEmpty { rated.filter { it in primary }.ifEmpty { rated.take(1) } }
            hit.forEach { pain[it] = maxOf(pain[it] ?: 0, level) }
        }
        return answers.muscles.map { answer ->
            SessionFeedback(
                id = FeedbackId(newId()),
                sessionId = sessionId,
                muscle = answer.muscle,
                pump = answer.pump,
                jointPain = pain[answer.muscle] ?: 0,
                load = answer.load.score,
                createdAt = now,
            )
        }
    }

    /**
     * El cambio para la próxima vez. [weeklySets] son las series efectivas de los últimos 7 días:
     * con ellas en el tope no se suma otra.
     */
    fun weekChange(feedback: SessionFeedback, weeklySets: Double): SetChange {
        val sore = feedback.soreness
        if ((sore ?: 0) >= 2 || feedback.load == PerceivedLoad.HARD.score) return SetChange.REMOVE
        if (sore == 1) return SetChange.KEEP
        val easy = feedback.load != null && feedback.load <= PerceivedLoad.LIGHT.score
        val lowPump = feedback.pump == null || feedback.pump <= 1
        if (!easy || !lowPump) return SetChange.KEEP
        return if (weeklySets + 1 > MuscleSets.TARGET_MAX) SetChange.KEEP else SetChange.ADD
    }

    /**
     * Lo que se aplica al empezar: solo cuando ya se preguntaron las agujetas, porque la serie de
     * más pide que estén curadas.
     */
    fun changeAtStart(feedback: SessionFeedback, weeklySets: Double): SetChange =
        if (feedback.soreness == null) SetChange.KEEP else weekChange(feedback, weeklySets)

    /**
     * Agujetas por preguntar al empezar [detail]: músculos principales de sus ejercicios con un
     * feedback anterior sin agujetas, de menos de [SORENESS_WINDOW]. Solo antes de la primera serie.
     */
    fun sorenessQuestions(
        detail: SessionDetail,
        exercises: Map<ExerciseId, Exercise>,
        latest: Map<Muscle, SessionFeedback>,
        now: Instant,
    ): List<SessionFeedback> {
        if (detail.sets.any { it.isCompleted }) return emptyList()
        val muscles = detail.exercises.sortedBy { it.sortOrder }
            .flatMap { exercises[it.exerciseId]?.primaryMuscles.orEmpty() }
            .distinct()
        return muscles.mapNotNull { muscle ->
            latest[muscle]?.takeIf {
                it.sessionId != detail.session.id && it.soreness == null && now - it.createdAt <= SORENESS_WINDOW
            }
        }.take(4)
    }

    /**
     * Agrega o quita series de trabajo en la sesión de hoy. Una serie de más va al primer ejercicio
     * donde el músculo es principal, copiando su última serie de trabajo; una de menos sale del
     * último ejercicio que tenga al menos dos pendientes. Un ejercicio que ya recibió una serie no
     * pierde otra, y si dos músculos piden sumar en el mismo ejercicio, basta una.
     */
    fun planSetChanges(
        detail: SessionDetail,
        exercises: Map<ExerciseId, Exercise>,
        changes: Map<Muscle, SetChange>,
        newId: () -> String,
    ): SetChangePlan {
        val ordered = detail.exercises.sortedBy { it.sortOrder }
        val added = mutableListOf<WorkoutSet>()
        val removed = mutableListOf<WorkoutSetId>()
        val applied = mutableMapOf<Muscle, SetChange>()
        val grown = mutableSetOf<SessionExerciseId>()
        val shrunk = mutableSetOf<SessionExerciseId>()
        changes.forEach { (muscle, change) ->
            val candidates = ordered.filter { muscle in exercises[it.exerciseId]?.primaryMuscles.orEmpty() }
            when (change) {
                SetChange.ADD -> {
                    val target = candidates.firstOrNull { ex -> detail.setsOf(ex.id).any { it.setType.countsAsWorking } }
                    if (target == null) {
                        applied[muscle] = SetChange.KEEP
                    } else {
                        if (target.id !in grown) {
                            val sets = detail.setsOf(target.id)
                            val last = sets.last { it.setType.countsAsWorking }
                            added += last.copy(
                                id = WorkoutSetId(newId()),
                                sortOrder = sets.maxOf { it.sortOrder } + 1,
                                completedAt = null,
                                rir = null,
                                rpe = null,
                                suggestion = null,
                                endHeartRate = null,
                                recoverySeconds = null,
                                notes = "",
                            )
                            grown += target.id
                        }
                        applied[muscle] = SetChange.ADD
                    }
                }
                SetChange.REMOVE -> {
                    val target = candidates.reversed().firstOrNull { ex ->
                        ex.id !in grown &&
                            ex.id !in shrunk &&
                            detail.setsOf(ex.id).count { !it.isCompleted && it.setType.countsAsWorking } >= 2
                    }
                    if (target == null) {
                        applied[muscle] = SetChange.KEEP
                    } else {
                        removed += detail.setsOf(target.id).last { !it.isCompleted && it.setType.countsAsWorking }.id
                        shrunk += target.id
                        applied[muscle] = SetChange.REMOVE
                    }
                }
                SetChange.KEEP -> applied[muscle] = SetChange.KEEP
            }
        }
        return SetChangePlan(added, removed, applied)
    }

    /**
     * Ejercicios de [current] que conviene cambiar por molestias: la última sesión terminada que los
     * tuvo dejó una molestia fuerte (2) en uno de sus músculos, dentro de [PAIN_WINDOW].
     * [recent] son las sesiones terminadas recientes y [feedback] el de cada una.
     */
    fun exercisesToSwap(
        current: SessionDetail,
        exercises: Map<ExerciseId, Exercise>,
        recent: List<SessionDetail>,
        feedback: Map<SessionId, List<SessionFeedback>>,
        now: Instant,
    ): Set<SessionExerciseId> = current.exercises.filter { exercise ->
        val info = exercises[exercise.exerciseId] ?: return@filter false
        val last = recent.filter { past ->
            past.session.id != current.session.id && past.exercises.any { it.exerciseId == exercise.exerciseId }
        }.maxByOrNull { it.session.startedAt } ?: return@filter false
        if (now - (last.session.endedAt ?: last.session.startedAt) > PAIN_WINDOW) return@filter false
        val muscles = info.muscles.map { it.muscle }.toSet()
        feedback[last.session.id].orEmpty().any { it.jointPain >= 2 && it.muscle in muscles }
    }.map { it.id }.toSet()
}
