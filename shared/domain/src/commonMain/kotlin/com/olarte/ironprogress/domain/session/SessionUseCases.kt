package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.progression.SuggestionReason
import com.olarte.ironprogress.domain.progression.performances
import com.olarte.ironprogress.domain.progression.recordFor
import com.olarte.ironprogress.domain.progression.suggestToday
import com.olarte.ironprogress.domain.routine.RoutineDetail
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Empieza una sesión, vacía o desde una rutina. Desde una rutina copia sus bloques, ejercicios
 * y una serie pendiente por cada plantilla. Si ya hay una sesión en curso, la devuelve sin
 * crear otra. Con el catálogo, las series de trabajo arrancan con la sugerencia del motor de
 * progresión y la guardan en `WorkoutSet.suggestion` (F2-02); sin él, con lo de la última vez.
 */
class StartSession(
    private val repository: SessionRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val catalog: ExerciseRepository? = null,
) {

    suspend operator fun invoke(routine: RoutineDetail? = null, discipline: Discipline = Discipline.STRENGTH): SessionDetail {
        repository.observeActiveSession().first()?.let { return it }

        val session = WorkoutSession(
            id = SessionId(ids.newId()),
            routineId = routine?.routine?.id,
            discipline = discipline,
            startedAt = clock.now(),
        )
        val blocks = mutableListOf<SessionBlock>()
        val exercises = mutableListOf<SessionExercise>()
        val sets = mutableListOf<WorkoutSet>()
        routine?.blocks?.sortedBy { it.sortOrder }?.forEach { block ->
            val sessionBlock = SessionBlock(SessionBlockId(ids.newId()), session.id, blocks.size, block.format)
            blocks += sessionBlock
            routine.exercisesOf(block.id).forEach { planned ->
                val exercise = SessionExercise(
                    id = SessionExerciseId(ids.newId()),
                    sessionId = session.id,
                    blockId = sessionBlock.id,
                    exerciseId = planned.exerciseId,
                    sortOrder = exercises.size,
                    supersetGroup = planned.supersetGroup,
                    notes = planned.notes,
                    restSeconds = planned.restSeconds,
                    timedSeconds = planned.timedSeconds,
                )
                exercises += exercise
                val previous = repository.previousSets(planned.exerciseId)
                val templates = routine.setsOf(planned.id)
                val suggestion = catalog?.findExercise(planned.exerciseId)?.let { exercise ->
                    suggestToday(
                        exercise = exercise,
                        templates = templates,
                        history = repository.performances(planned.exerciseId),
                        unit = exercise.preferredUnit ?: planned.unit ?: WeightUnit.KG,
                        timed = planned.timedSeconds != null,
                    )
                }?.takeIf { it.reason != SuggestionReason.SAME_AS_LAST }
                val working = templates.filter { it.setType.countsAsWorking }
                templates.forEach { template ->
                    val index = templates.filter { it.setType == template.setType }.indexOf(template)
                    val last = previous.filter { it.setType == template.setType }.getOrNull(index)
                    val workingIndex = working.indexOf(template)
                    val suggested = suggestion?.takeIf { workingIndex >= 0 }
                    sets += WorkoutSet(
                        id = WorkoutSetId(ids.newId()),
                        sessionExerciseId = exercise.id,
                        sortOrder = template.sortOrder,
                        setType = template.setType,
                        weightKg = suggested?.weightKg ?: last?.weightKg,
                        reps = suggested?.reps?.getOrNull(workingIndex) ?: last?.reps ?: template.repsMin ?: template.repsMax,
                        rir = template.targetRir,
                        durationSeconds = last?.durationSeconds,
                        distanceMeters = last?.distanceMeters,
                        suggestion = suggested?.recordFor(workingIndex)?.encode(),
                    )
                }
            }
        }

        return repository.createSession(SessionDetail(session, blocks, exercises, sets))
    }
}

/** Marca una serie como hecha con sus valores y la guarda en el momento. */
class CompleteSet(private val repository: SessionRepository, private val clock: Clock) {

    suspend operator fun invoke(set: WorkoutSet): WorkoutSet {
        val done = set.copy(completedAt = clock.now())
        repository.saveSet(done)
        return done
    }
}

/** Termina o descarta la sesión en curso. Una sesión descartada no cuenta en el progreso. */
class EndSession(private val repository: SessionRepository, private val clock: Clock) {

    suspend operator fun invoke(session: WorkoutSession, discard: Boolean = false): WorkoutSession {
        require(session.status == SessionStatus.IN_PROGRESS) { "La sesión ya terminó" }
        val ended = session.copy(
            endedAt = clock.now(),
            status = if (discard) SessionStatus.DISCARDED else SessionStatus.FINISHED,
        )
        repository.saveSession(ended)
        return ended
    }
}
