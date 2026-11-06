package com.olarte.ironprogress.domain.wod

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Guarda un bloque funcional como sesión FUNCTIONAL: un bloque con su formato y resultado, y por
 * cada movimiento una serie por ronda completa (más la parcial en AMRAP). El nombre del bloque va
 * en las notas de la sesión: así se encuentra la marca anterior.
 */
class SaveWodResult(private val sessions: SessionRepository, private val ids: IdGenerator, private val clock: Clock) {

    suspend operator fun invoke(wod: Wod, startedAt: Instant, score: WodScore, elapsedSeconds: Int): SessionId {
        val session = WorkoutSession(
            id = SessionId(ids.newId()),
            routineId = null,
            discipline = Discipline.FUNCTIONAL,
            startedAt = startedAt,
            endedAt = clock.now(),
            status = SessionStatus.FINISHED,
            notes = wod.name,
        )
        val block = SessionBlock(SessionBlockId(ids.newId()), session.id, 0, wod.format, score.result(wod, elapsedSeconds))
        sessions.saveSession(session)
        sessions.saveBlock(block)
        wod.movements.forEachIndexed { index, movement ->
            val exercise = SessionExercise(SessionExerciseId(ids.newId()), session.id, block.id, movement.exerciseId, sortOrder = index)
            sessions.saveExercise(exercise)
            val partial = if (index < score.movementsDone) 1 else 0
            repeat(score.rounds + partial) { round ->
                sessions.saveSet(
                    WorkoutSet(
                        id = WorkoutSetId(ids.newId()),
                        sessionExerciseId = exercise.id,
                        sortOrder = round,
                        reps = movement.reps,
                        round = round + 1,
                        completedAt = session.endedAt,
                    ),
                )
            }
        }
        return session.id
    }
}

/** La mejor marca guardada de un bloque con el mismo nombre y formato ("Tu marca 8 + 12"). */
class FindWodBest(private val sessions: SessionRepository) {

    suspend operator fun invoke(wod: Wod): BlockResult? {
        var best: BlockResult? = null
        for (session in sessions.observeFinishedSessions().first()) {
            if (session.discipline != Discipline.FUNCTIONAL || session.notes != wod.name) continue
            val result = sessions.findSession(session.id)?.blocks?.firstOrNull { it.format == wod.format }?.result ?: continue
            if (isBetter(result, best)) best = result
        }
        return best
    }
}
