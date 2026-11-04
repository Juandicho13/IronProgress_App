package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.common.IdGenerator
import kotlinx.coroutines.flow.first

/** Operaciones de biblioteca sobre los repositorios existentes. */
class EditRoutines(private val repository: RoutineRepository, private val ids: IdGenerator) {
    fun draft(name: String, folder: FolderId? = null): RoutineDetail = RoutineDetail(
        Routine(RoutineId(ids.newId()), folder, name.trim()),
        listOf(WorkoutBlock(BlockId(ids.newId()), 0, BlockFormat.SETS)),
        emptyList(),
        emptyList(),
    )

    suspend fun save(detail: RoutineDetail) {
        require(detail.routine.name.isNotBlank())
        require(detail.exercises.all { it.restSeconds == null || it.restSeconds >= 0 })
        require(detail.setTemplates.all { it.repsMax == null || it.repsMax > 0 })
        repository.saveRoutine(detail.copy(routine = detail.routine.copy(name = detail.routine.name.trim())))
    }

    suspend fun duplicate(id: RoutineId, name: String): RoutineDetail {
        val source = requireNotNull(repository.findRoutine(id))
        val blocks = source.blocks.associate { it.id to BlockId(ids.newId()) }
        val exercises = source.exercises.associate { it.id to RoutineExerciseId(ids.newId()) }
        val copy = source.copy(
            routine = source.routine.copy(
                id = RoutineId(ids.newId()),
                name = name.trim(),
                sortOrder = repository.observeRoutines().first().size,
                createdBy = CreatedBy.User,
                sourceProgramVersionId = null,
            ),
            blocks = source.blocks.map { it.copy(id = blocks.getValue(it.id)) },
            exercises = source.exercises.map { it.copy(id = exercises.getValue(it.id), blockId = blocks.getValue(it.blockId)) },
            setTemplates = source.setTemplates.map {
                it.copy(id = SetTemplateId(ids.newId()), routineExerciseId = exercises.getValue(it.routineExerciseId))
            },
        )
        save(copy)
        return copy
    }

    suspend fun folder(name: String, existing: Folder? = null) {
        require(name.isNotBlank())
        repository.saveFolder(
            existing?.copy(name = name.trim()) ?: Folder(FolderId(ids.newId()), name.trim(), repository.observeFolders().first().size),
        )
    }

    suspend fun move(id: RoutineId, target: Int) {
        val routines = repository.observeRoutines().first().toMutableList()
        val moving = routines.first { it.id == id }
        routines.remove(moving)
        routines.add(target.coerceIn(0, routines.size), moving)
        repository.reorderRoutines(routines.map { it.id })
    }

    suspend fun moveFolder(id: FolderId, target: Int) {
        val folders = repository.observeFolders().first().toMutableList()
        val moving = folders.first { it.id == id }
        folders.remove(moving)
        folders.add(target.coerceIn(0, folders.size), moving)
        repository.reorderFolders(folders.map { it.id })
    }
}
