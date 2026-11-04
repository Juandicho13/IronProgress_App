package com.olarte.ironprogress.domain.routine

import kotlinx.coroutines.flow.Flow

/** Carpetas y rutinas. Domain la define y la capa data la implementa (F1-04). */
interface RoutineRepository {
    fun observeFolders(): Flow<List<Folder>>

    /** Rutinas sin sus bloques, en el orden de la lista. */
    fun observeRoutines(): Flow<List<Routine>>

    suspend fun findRoutine(id: RoutineId): RoutineDetail?

    /** Guarda la rutina entera en una transacción: lo que no viene en [detail] se borra. */
    suspend fun saveRoutine(detail: RoutineDetail)

    suspend fun deleteRoutine(id: RoutineId)

    suspend fun saveFolder(folder: Folder)

    /** Borra la carpeta; sus rutinas quedan sin carpeta. */
    suspend fun deleteFolder(id: FolderId)
}
