package com.olarte.ironprogress.domain.feedback

import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.session.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class FeedbackId(val value: String)

/**
 * Lo que sintió un músculo en una sesión (HANDOFF 11.4 y 10.2 SessionFeedback). Al terminar se
 * califican el bombeo (0 a 3), las molestias articulares (0 a 2) y la carga percibida (0 a 3).
 * Las agujetas (0 a 3) se preguntan antes de volver a entrenar el músculo y se guardan en esta
 * misma fila, con la hora en que se contestaron. Un valor null es "no se preguntó": el bombeo
 * solo se pregunta en modo Pro.
 */
data class SessionFeedback(
    val id: FeedbackId,
    val sessionId: SessionId,
    val muscle: Muscle,
    val pump: Int? = null,
    val soreness: Int? = null,
    val jointPain: Int = 0,
    val load: Int? = null,
    val createdAt: Instant,
    val sorenessAt: Instant? = null,
) {
    init {
        require(pump == null || pump in 0..3) { "El bombeo va de 0 a 3" }
        require(soreness == null || soreness in 0..3) { "Las agujetas van de 0 a 3" }
        require(jointPain in 0..2) { "Las molestias van de 0 a 2" }
        require(load == null || load in 0..3) { "La carga va de 0 a 3" }
    }
}

/**
 * Carga percibida del Resumen (PANTALLAS 1.6): Poco, Justo o Mucho. Se guarda como 1, 2 o 3 en
 * la escala de 0 a 3 de 11.4; el 0 queda para "no sintió nada" si algún día se pregunta.
 */
enum class PerceivedLoad(val score: Int) {
    LIGHT(1),
    RIGHT(2),
    HARD(3),
    ;

    companion object {
        fun of(score: Int?): PerceivedLoad? = entries.firstOrNull { it.score == score }
    }
}

/**
 * Articulaciones del Resumen ("Molestias"). Una molestia se anota en los músculos trabajados que
 * mueven esa articulación; si ninguno la mueve, en los principales de la sesión.
 */
enum class Joint(val muscles: Set<Muscle>) {
    SHOULDER(setOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.TRAPS, Muscle.LATS)),
    ELBOW(setOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS)),
    WRIST(setOf(Muscle.FOREARMS, Muscle.BICEPS)),
    KNEE(setOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.CALVES, Muscle.GLUTES)),
    LOWER_BACK(setOf(Muscle.LOWER_BACK, Muscle.GLUTES, Muscle.HAMSTRINGS)),
}

/** Feedback guardado. Room en la app; en memoria en las pruebas. */
interface FeedbackRepository {
    fun observeAll(): Flow<List<SessionFeedback>>

    suspend fun save(feedback: List<SessionFeedback>)

    suspend fun forSession(sessionId: SessionId): List<SessionFeedback>

    /** La fila más reciente de cada músculo (por hora de creación). */
    suspend fun latestByMuscle(): Map<Muscle, SessionFeedback>

    suspend fun setSoreness(id: FeedbackId, soreness: Int, at: Instant)
}

/** Repositorio en memoria: pruebas y vistas previas. */
class InMemoryFeedbackRepository(initial: List<SessionFeedback> = emptyList()) : FeedbackRepository {
    private val rows = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<SessionFeedback>> = rows.asStateFlow()

    override suspend fun save(feedback: List<SessionFeedback>) {
        val ids = feedback.map { it.id }.toSet()
        rows.update { current -> current.filter { it.id !in ids } + feedback }
    }

    override suspend fun forSession(sessionId: SessionId): List<SessionFeedback> = rows.value.filter { it.sessionId == sessionId }

    override suspend fun latestByMuscle(): Map<Muscle, SessionFeedback> = rows.value.latestByMuscle()

    override suspend fun setSoreness(id: FeedbackId, soreness: Int, at: Instant) {
        rows.update { current -> current.map { if (it.id == id) it.copy(soreness = soreness, sorenessAt = at) else it } }
    }
}

/** La fila más reciente por músculo. */
fun List<SessionFeedback>.latestByMuscle(): Map<Muscle, SessionFeedback> =
    groupBy { it.muscle }.mapValues { (_, rows) -> rows.maxBy { it.createdAt } }
