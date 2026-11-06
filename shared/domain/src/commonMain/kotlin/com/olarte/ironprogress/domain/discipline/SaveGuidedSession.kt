package com.olarte.ironprogress.domain.discipline

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Guarda un flujo o una respiración terminados como sesión de su disciplina, con un bloque FLOW o
 * BREATHING y el tiempo hecho como resultado. Salud y Health Connect llegan en F4.
 */
class SaveGuidedSession(private val sessions: SessionRepository, private val ids: IdGenerator, private val clock: Clock) {

    suspend operator fun invoke(discipline: Discipline, name: String, startedAt: Instant, activeSeconds: Int): SessionId {
        val endedAt = maxOf(clock.now(), startedAt)
        val session = WorkoutSession(
            id = SessionId(ids.newId()),
            routineId = null,
            discipline = discipline,
            startedAt = startedAt,
            endedAt = endedAt,
            status = SessionStatus.FINISHED,
            notes = name,
        )
        sessions.saveSession(session)
        sessions.saveBlock(
            SessionBlock(
                id = SessionBlockId(ids.newId()),
                sessionId = session.id,
                sortOrder = 0,
                format = if (discipline == Discipline.BREATHING) BlockFormat.BREATHING else BlockFormat.FLOW,
                result = BlockResult.Time(activeSeconds),
            ),
        )
        return session.id
    }
}
