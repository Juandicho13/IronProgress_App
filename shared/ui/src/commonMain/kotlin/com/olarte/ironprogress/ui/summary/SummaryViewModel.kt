package com.olarte.ironprogress.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.common.UuidV7IdGenerator
import com.olarte.ironprogress.domain.feedback.FeedbackAnswers
import com.olarte.ironprogress.domain.feedback.FeedbackRepository
import com.olarte.ironprogress.domain.feedback.Joint
import com.olarte.ironprogress.domain.feedback.MuscleAnswer
import com.olarte.ironprogress.domain.feedback.PerceivedLoad
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.SessionFeedbackRules
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.domain.progress.MuscleSets
import com.olarte.ironprogress.domain.routine.RoutineRepository
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionSummary
import com.olarte.ironprogress.domain.session.SummarizeSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Feedback del Resumen (F2-06, PANTALLAS 1.6). Todo viene en "Justo" y sin molestias, así que
 * guardar es un toque. El bombeo solo se pregunta en modo Pro.
 */
data class FeedbackForm(
    val muscles: List<Muscle> = emptyList(),
    val loads: Map<Muscle, PerceivedLoad> = emptyMap(),
    val pumps: Map<Muscle, Int> = emptyMap(),
    val joints: Set<Joint> = emptySet(),
    val strongPain: Boolean = false,
    /** Series efectivas de los últimos 7 días por músculo: con el tope no se suma otra. */
    val weeklySets: Map<Muscle, Double> = emptyMap(),
    val saving: Boolean = false,
) {
    fun loadOf(muscle: Muscle): PerceivedLoad = loads[muscle] ?: PerceivedLoad.RIGHT

    fun answers(withPump: Boolean): FeedbackAnswers = FeedbackAnswers(
        muscles = muscles.map { MuscleAnswer(it, loadOf(it), if (withPump) pumps[it] else null) },
        joints = joints,
        strongPain = strongPain,
    )

    /**
     * Lo que pasaría la próxima semana si llega sin agujetas: la misma regla de 11.4 con las agujetas
     * curadas.
     */
    fun preview(withPump: Boolean): Map<Muscle, SetChange> = muscles.associateWith { muscle ->
        val row = SessionFeedback(
            id = com.olarte.ironprogress.domain.feedback.FeedbackId("preview"),
            sessionId = SessionId("preview"),
            muscle = muscle,
            pump = if (withPump) pumps[muscle] else null,
            soreness = 0,
            load = loadOf(muscle).score,
            createdAt = kotlin.time.Instant.DISTANT_PAST,
        )
        SessionFeedbackRules.weekChange(row, weeklySets[muscle] ?: 0.0)
    }
}

data class SummaryState(
    val loading: Boolean = true,
    val summary: SessionSummary? = null,
    val routineName: String? = null,
    val exercises: Map<ExerciseId, Exercise> = emptyMap(),
    val failed: Boolean = false,
    /** null cuando no hay nada que calificar o ya se calificó esta sesión. */
    val feedback: FeedbackForm? = null,
) {
    fun exerciseName(id: ExerciseId, languageCode: String): String = exercises[id]?.name?.resolve(languageCode) ?: id.value
}

/** Resumen de una sesión terminada (F1-21) con el feedback por músculo (F2-06). */
class SummaryViewModel(
    private val summarize: SummarizeSession,
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val sessionId: String,
    private val feedback: FeedbackRepository? = null,
    private val sessions: SessionRepository? = null,
    private val ids: IdGenerator = UuidV7IdGenerator,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SummaryState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun setLoad(muscle: Muscle, load: PerceivedLoad) = updateForm { it.copy(loads = it.loads + (muscle to load)) }

    fun setPump(muscle: Muscle, pump: Int) = updateForm { it.copy(pumps = it.pumps + (muscle to pump.coerceIn(0, 3))) }

    /** "Ninguna" limpia las molestias; una articulación se prende o se apaga. */
    fun toggleJoint(joint: Joint?) = updateForm { form ->
        when {
            joint == null -> form.copy(joints = emptySet(), strongPain = false)
            joint in form.joints -> form.copy(joints = form.joints - joint).let {
                if (it.joints.isEmpty()) it.copy(strongPain = false) else it
            }
            else -> form.copy(joints = form.joints + joint)
        }
    }

    fun setStrongPain(strong: Boolean) = updateForm { it.copy(strongPain = strong && it.joints.isNotEmpty()) }

    /** Guarda el feedback y sigue. Si falla, sigue igual: el feedback es opcional. */
    fun save(withPump: Boolean, onDone: () -> Unit) {
        val form = state.value.feedback
        val repository = feedback
        val summary = state.value.summary
        if (form == null || repository == null || summary == null || form.saving) {
            onDone()
            return
        }
        mutableState.update { it.copy(feedback = form.copy(saving = true)) }
        viewModelScope.launch {
            try {
                val primary = summary.exercises.flatMap { state.value.exercises[it.exerciseId]?.primaryMuscles.orEmpty() }.toSet()
                val rows = SessionFeedbackRules.build(summary.session.id, form.answers(withPump), primary, clock.now(), ids::newId)
                repository.save(rows)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Sin feedback la app funciona igual; no se bloquea el cierre.
            }
            mutableState.update { it.copy(feedback = null) }
            onDone()
        }
    }

    private fun updateForm(transform: (FeedbackForm) -> FeedbackForm) {
        mutableState.update { state -> state.feedback?.let { state.copy(feedback = transform(it)) } ?: state }
    }

    private fun load() {
        mutableState.value = SummaryState()
        viewModelScope.launch {
            try {
                val summary = summarize(SessionId(sessionId))
                val name = summary?.session?.routineId?.let { routines.findRoutine(it)?.routine?.name }
                val catalog = exercises.observeExercises().first().associateBy { it.id }
                mutableState.value = SummaryState(
                    loading = false,
                    summary = summary,
                    routineName = name,
                    exercises = catalog,
                    failed = summary == null,
                    feedback = summary?.let { feedbackForm(it, catalog) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = SummaryState(loading = false, failed = true)
            }
        }
    }

    private suspend fun feedbackForm(summary: SessionSummary, catalog: Map<ExerciseId, Exercise>): FeedbackForm? {
        val repository = feedback ?: return null
        if (repository.forSession(summary.session.id).isNotEmpty()) return null
        val muscles = SessionFeedbackRules.trainedMuscles(summary.exercises.associate { it.exerciseId to it.sets.size }, catalog)
        if (muscles.isEmpty()) return null
        return FeedbackForm(muscles = muscles, weeklySets = weeklySets(catalog))
    }

    private suspend fun weeklySets(catalog: Map<ExerciseId, Exercise>): Map<Muscle, Double> {
        val repository = sessions ?: return emptyMap()
        val now = clock.now()
        val details = repository.observeFinishedSessions().first()
            .filter { (it.endedAt ?: it.startedAt) >= now - 8.days }
            .mapNotNull { repository.findSession(it.id) }
        return MuscleSets.lastSevenDays(details, catalog, now)
    }
}
