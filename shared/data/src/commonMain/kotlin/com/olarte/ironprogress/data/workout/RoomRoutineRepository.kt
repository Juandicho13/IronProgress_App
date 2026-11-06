package com.olarte.ironprogress.data.workout

import com.olarte.ironprogress.data.local.RoutineDao
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** [RoutineRepository] con Room. Conserva created_at de lo que ya existía y pone updated_at. */
class RoomRoutineRepository(private val dao: RoutineDao, private val clock: Clock) : RoutineRepository {

    private fun now(): Long = clock.now().toEpochMilliseconds()

    override fun observeFolders(): Flow<List<Folder>> = dao.observeFolders().map { rows -> rows.map { it.toDomain() } }

    override fun observeRoutines(): Flow<List<Routine>> = dao.observeRoutines().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findRoutine(id: RoutineId): RoutineDetail? {
        val routine = dao.findRoutine(id.value) ?: return null
        return RoutineDetail(
            routine = routine.toDomain(),
            blocks = dao.blocksOfRoutine(id.value).map { it.toRoutineBlock() },
            exercises = dao.exercisesOfRoutine(id.value).map { it.toDomain() },
            setTemplates = dao.setTemplatesOfRoutine(id.value).map { it.toDomain() },
        )
    }

    override suspend fun saveRoutine(detail: RoutineDetail) {
        val now = now()
        val routineId = detail.routine.id
        val routineCreated = dao.findRoutine(routineId.value)?.createdAt ?: now
        val blocksCreated = dao.blocksOfRoutine(routineId.value).associate { it.id to it.createdAt }
        val exercisesCreated = dao.exercisesOfRoutine(routineId.value).associate { it.id to it.createdAt }
        val templatesCreated = dao.setTemplatesOfRoutine(routineId.value).associate { it.id to it.createdAt }
        dao.replaceRoutine(
            routine = detail.routine.toEntity(routineCreated, now),
            blocks = detail.blocks.map { it.toEntity(routineId, blocksCreated[it.id.value] ?: now, now) },
            exercises = detail.exercises.map { it.toEntity(routineId, exercisesCreated[it.id.value] ?: now, now) },
            templates = detail.setTemplates.map { it.toEntity(templatesCreated[it.id.value] ?: now, now) },
            now = now,
        )
    }

    override suspend fun reorderRoutines(ids: List<RoutineId>) = dao.reorderRoutines(ids.map { it.value }, now())

    override suspend fun reorderFolders(ids: List<FolderId>) = dao.reorderFolders(ids.map { it.value }, now())

    override suspend fun deleteRoutine(id: RoutineId) {
        dao.markRoutineDeleted(id.value, now())
    }

    override suspend fun saveFolder(folder: Folder) {
        val now = now()
        val created = dao.observeFolders().first().firstOrNull { it.id == folder.id.value }?.createdAt ?: now
        dao.upsertFolder(folder.toEntity(created, now))
    }

    override suspend fun deleteFolder(id: FolderId) {
        dao.deleteFolder(id.value, now())
    }
}
