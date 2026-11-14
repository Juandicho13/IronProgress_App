package com.olarte.ironprogress.data.feedback

import com.olarte.ironprogress.data.local.FeedbackDao
import com.olarte.ironprogress.data.local.SessionFeedbackEntity
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.FeedbackId
import com.olarte.ironprogress.domain.feedback.FeedbackRepository
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.latestByMuscle
import com.olarte.ironprogress.domain.session.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/** Feedback por músculo en Room (tabla session_feedback, versión 6). */
class RoomFeedbackRepository(private val dao: FeedbackDao, private val clock: Clock) : FeedbackRepository {

    override fun observeAll(): Flow<List<SessionFeedback>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun save(feedback: List<SessionFeedback>) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(feedback.map { it.toEntity(now) })
    }

    override suspend fun forSession(sessionId: SessionId): List<SessionFeedback> = dao.forSession(sessionId.value).mapNotNull {
        it.toDomain()
    }

    override suspend fun latestByMuscle(): Map<Muscle, SessionFeedback> = dao.all().mapNotNull { it.toDomain() }.latestByMuscle()

    override suspend fun setSoreness(id: FeedbackId, soreness: Int, at: Instant) {
        dao.setSoreness(id.value, soreness.coerceIn(0, 3), at.toEpochMilliseconds())
    }
}

/** Un músculo que ya no existe en el catálogo fijo se salta en vez de romper la lectura. */
internal fun SessionFeedbackEntity.toDomain(): SessionFeedback? {
    val muscle = Muscle.fromId(muscleId) ?: return null
    return SessionFeedback(
        id = FeedbackId(id),
        sessionId = SessionId(sessionId),
        muscle = muscle,
        pump = pump?.coerceIn(0, 3),
        soreness = soreness?.coerceIn(0, 3),
        jointPain = jointPain.coerceIn(0, 2),
        load = load?.coerceIn(0, 3),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        sorenessAt = sorenessAt?.let { Instant.fromEpochMilliseconds(it) },
    )
}

internal fun SessionFeedback.toEntity(now: Long): SessionFeedbackEntity = SessionFeedbackEntity(
    id = id.value,
    sessionId = sessionId.value,
    muscleId = muscle.id,
    pump = pump,
    soreness = soreness,
    jointPain = jointPain,
    load = load,
    createdAt = createdAt.toEpochMilliseconds(),
    sorenessAt = sorenessAt?.toEpochMilliseconds(),
    updatedAt = now,
)
