package com.olarte.ironprogress.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Carpetas, rutinas, sus bloques, ejercicios y plantillas. Ninguna lectura trae filas borradas. */
@Dao
interface RoutineDao {

    @Query("SELECT * FROM folders WHERE deleted_at IS NULL ORDER BY sort_order ASC, name ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("UPDATE routines SET sort_order = :position, updated_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun setRoutineOrder(id: String, position: Int, now: Long)

    @Query("UPDATE folders SET sort_order = :position, updated_at = :now WHERE id = :id AND deleted_at IS NULL")
    suspend fun setFolderOrder(id: String, position: Int, now: Long)

    @Transaction
    suspend fun reorderRoutines(ids: List<String>, now: Long) {
        ids.forEachIndexed { index, id -> setRoutineOrder(id, index, now) }
    }

    @Transaction
    suspend fun reorderFolders(ids: List<String>, now: Long) {
        ids.forEachIndexed { index, id -> setFolderOrder(id, index, now) }
    }

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET deleted_at = :now, updated_at = :now WHERE id = :folderId")
    suspend fun markFolderDeleted(folderId: String, now: Long)

    @Query("UPDATE routines SET folder_id = NULL, updated_at = :now WHERE folder_id = :folderId")
    suspend fun clearFolder(folderId: String, now: Long)

    /** Borra la carpeta y deja sus rutinas sin carpeta. */
    @Transaction
    suspend fun deleteFolder(folderId: String, now: Long) {
        clearFolder(folderId, now)
        markFolderDeleted(folderId, now)
    }

    @Query("SELECT * FROM routines WHERE deleted_at IS NULL ORDER BY sort_order ASC, name ASC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :routineId AND deleted_at IS NULL LIMIT 1")
    suspend fun findRoutine(routineId: String): RoutineEntity?

    @Query("SELECT * FROM workout_blocks WHERE routine_id = :routineId AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun blocksOfRoutine(routineId: String): List<WorkoutBlockEntity>

    @Query("SELECT * FROM routine_exercises WHERE routine_id = :routineId AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun exercisesOfRoutine(routineId: String): List<RoutineExerciseEntity>

    @Query(
        "SELECT t.* FROM set_templates t " +
            "JOIN routine_exercises e ON e.id = t.routine_exercise_id " +
            "WHERE e.routine_id = :routineId AND t.deleted_at IS NULL AND e.deleted_at IS NULL " +
            "ORDER BY t.sort_order ASC",
    )
    suspend fun setTemplatesOfRoutine(routineId: String): List<SetTemplateEntity>

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertBlocks(blocks: List<WorkoutBlockEntity>)

    @Upsert
    suspend fun upsertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertSetTemplates(templates: List<SetTemplateEntity>)

    @Query(
        "UPDATE workout_blocks SET deleted_at = :now, updated_at = :now " +
            "WHERE routine_id = :routineId AND deleted_at IS NULL AND id NOT IN (:keepIds)",
    )
    suspend fun markBlocksDeletedExcept(routineId: String, keepIds: List<String>, now: Long)

    @Query(
        "UPDATE routine_exercises SET deleted_at = :now, updated_at = :now " +
            "WHERE routine_id = :routineId AND deleted_at IS NULL AND id NOT IN (:keepIds)",
    )
    suspend fun markExercisesDeletedExcept(routineId: String, keepIds: List<String>, now: Long)

    @Query(
        "UPDATE set_templates SET deleted_at = :now, updated_at = :now " +
            "WHERE deleted_at IS NULL AND id NOT IN (:keepIds) AND routine_exercise_id IN " +
            "(SELECT id FROM routine_exercises WHERE routine_id = :routineId)",
    )
    suspend fun markSetTemplatesDeletedExcept(routineId: String, keepIds: List<String>, now: Long)

    /**
     * Guarda la rutina entera en una transacción. Lo que ya no viene se marca como borrado (no se
     * elimina) para que el borrado viaje en la sincronización.
     */
    @Transaction
    suspend fun replaceRoutine(
        routine: RoutineEntity,
        blocks: List<WorkoutBlockEntity>,
        exercises: List<RoutineExerciseEntity>,
        templates: List<SetTemplateEntity>,
        now: Long,
    ) {
        upsertRoutine(routine)
        markSetTemplatesDeletedExcept(routine.id, templates.map { it.id }, now)
        markExercisesDeletedExcept(routine.id, exercises.map { it.id }, now)
        markBlocksDeletedExcept(routine.id, blocks.map { it.id }, now)
        upsertBlocks(blocks)
        upsertRoutineExercises(exercises)
        upsertSetTemplates(templates)
    }

    @Query("UPDATE routines SET deleted_at = :now, updated_at = :now WHERE id = :routineId")
    suspend fun markRoutineDeleted(routineId: String, now: Long)
}
