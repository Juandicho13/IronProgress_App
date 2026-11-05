package com.olarte.ironprogress.domain.interval

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.routine.BlockFormat
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Guarda unos intervalos terminados como una sesión de HIIT: un bloque INTERVALS con las reps de
 * cada ronda como resultado, y una serie por ronda en el ejercicio de esa ronda (con su número de
 * ronda y la hora en que terminó). Así salen en el historial y en "la última vez".
 */
class SaveIntervalResult(private val sessions: SessionRepository, private val ids: IdGenerator, private val clock: Clock) {

    /** [repsPerRound] tiene una entrada por ronda hecha; null si esa ronda no se anotó. */
    suspend operator fun invoke(template: IntervalTemplate, startedAt: Instant, repsPerRound: List<Int?>): SessionId {
        val endedAt = clock.now()
        val session = WorkoutSession(
            id = SessionId(ids.newId()),
            routineId = null,
            discipline = Discipline.HIIT,
            startedAt = startedAt,
            endedAt = endedAt,
            status = SessionStatus.FINISHED,
            notes = template.name,
        )
        val block = SessionBlock(
            id = SessionBlockId(ids.newId()),
            sessionId = session.id,
            sortOrder = 0,
            format = BlockFormat.INTERVALS,
            result = BlockResult.RepsPerRound(repsPerRound.map { it ?: 0 }),
        )
        sessions.saveSession(session)
        sessions.saveBlock(block)

        val exercises = template.exerciseIds.distinct().mapIndexed { index, exerciseId ->
            SessionExercise(SessionExerciseId(ids.newId()), session.id, block.id, exerciseId, sortOrder = index)
        }
        exercises.forEach { sessions.saveExercise(it) }
        val byExercise = exercises.associateBy { it.exerciseId }
        repsPerRound.forEachIndexed { index, reps ->
            val round = index + 1
            val exercise = template.exerciseFor(round)?.let(byExercise::get) ?: return@forEachIndexed
            val roundEnd = startedAt + (round * template.workSeconds + index * template.restSeconds).seconds
            sessions.saveSet(
                WorkoutSet(
                    id = WorkoutSetId(ids.newId()),
                    sessionExerciseId = exercise.id,
                    sortOrder = index,
                    reps = reps,
                    durationSeconds = template.workSeconds,
                    round = round,
                    completedAt = minOf(roundEnd, endedAt),
                ),
            )
        }
        return session.id
    }
}
