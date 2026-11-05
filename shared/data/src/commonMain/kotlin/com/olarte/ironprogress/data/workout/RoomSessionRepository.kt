package com.olarte.ironprogress.data.workout

import com.olarte.ironprogress.data.local.SessionDao
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionRepository
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * [SessionRepository] con Room. Cada guardado es una escritura propia, así que una serie
 * completada queda en disco aunque la app se cierre un segundo después.
 */
class RoomSessionRepository(private val dao: SessionDao, private val clock: Clock) : SessionRepository {

    private fun now(): Long = clock.now().toEpochMilliseconds()

    /**
     * Emite de nuevo cuando cambia la sesión, una serie o un ejercicio de la sesión: la consulta
     * de series cruza session_exercises, así que Room avisa por las dos tablas.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSession(): Flow<SessionDetail?> = dao.observeActiveSession().flatMapLatest { session ->
        if (session == null) {
            flowOf(null)
        } else {
            dao.observeSetsOfSession(session.id).map { sets ->
                SessionDetail(
                    session = session.toDomain(),
                    blocks = dao.blocksOfSession(session.id).map { it.toSessionBlock() },
                    exercises = dao.exercisesOfSession(session.id).map { it.toDomain() },
                    sets = sets.map { it.toDomain() },
                )
            }
        }
    }

    override suspend fun findSession(id: SessionId): SessionDetail? {
        val session = dao.findSession(id.value) ?: return null
        return SessionDetail(
            session = session.toDomain(),
            blocks = dao.blocksOfSession(id.value).map { it.toSessionBlock() },
            exercises = dao.exercisesOfSession(id.value).map { it.toDomain() },
            sets = dao.setsOfSession(id.value).map { it.toDomain() },
        )
    }

    override fun observeFinishedSessions(): Flow<List<WorkoutSession>> =
        dao.observeFinishedSessions().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createSession(detail: SessionDetail): SessionDetail {
        val now = now()
        val id = dao.createSession(
            detail.session.toEntity(now, now),
            detail.blocks.map { it.toEntity(now, now) },
            detail.exercises.map { it.toEntity(now, now) },
            detail.sets.map { it.toEntity(now, now) },
        )
        return requireNotNull(findSession(SessionId(id)))
    }

    override suspend fun appendExercise(block: SessionBlock, exercise: SessionExercise, sets: List<WorkoutSet>) {
        val now = now()
        dao.appendExercise(
            block.toEntity(dao.blockCreatedAt(block.id.value) ?: now, now),
            exercise.toEntity(now, now),
            sets.map { it.toEntity(now, now) },
        )
    }

    override suspend fun previousSets(exerciseId: ExerciseId): List<WorkoutSet> = dao.previousSets(exerciseId.value).map { it.toDomain() }

    override suspend fun saveSession(session: WorkoutSession) {
        val now = now()
        dao.upsertSession(session.toEntity(dao.sessionCreatedAt(session.id.value) ?: now, now))
    }

    override suspend fun saveBlock(block: SessionBlock) {
        val now = now()
        dao.upsertBlock(block.toEntity(dao.blockCreatedAt(block.id.value) ?: now, now))
    }

    override suspend fun saveExercise(exercise: SessionExercise) {
        val now = now()
        dao.upsertExercise(exercise.toEntity(dao.exerciseCreatedAt(exercise.id.value) ?: now, now))
    }

    override suspend fun saveSet(set: WorkoutSet) {
        val now = now()
        dao.upsertSet(set.toEntity(dao.setCreatedAt(set.id.value) ?: now, now))
    }

    override suspend fun deleteSet(id: WorkoutSetId) {
        dao.markSetDeleted(id.value, now())
    }

    override suspend fun deleteBlock(detail: SessionDetail, id: SessionBlockId) {
        dao.deleteBlock(id.value, now())
    }
}
