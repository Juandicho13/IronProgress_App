package com.example.ironprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IronDao {
    // --- DIARIO (Exercises) ---
    @Query("SELECT * FROM exercises ORDER BY date DESC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name = :exerciseName ORDER BY date ASC")
    fun getHistoryByName(exerciseName: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Int)

    // --- RUTINAS (Routines) ---
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    // 👇 NUEVO: Para actualizar el nombre/tiempo del padre
    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Int)

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Int): RoutineEntity?

    // --- DETALLE DE RUTINAS (Routine Exercises) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(exercise: RoutineExerciseEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId")
    suspend fun getRoutineExercises(routineId: Int): List<RoutineExerciseEntity>

    // 👇 NUEVO: Para borrar los ejercicios viejos al editar una rutina (limpieza)
    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Int)


    // 👇 1. Para el Progreso: Obtener lista de nombres únicos de ejercicios realizados
    @Query("SELECT DISTINCT name FROM exercises ORDER BY name ASC")
    fun getUniqueExerciseNames(): Flow<List<String>>

    // 👇 2. Para la Evolución: Actualizar el peso/reps base de un ejercicio de rutina
    @Query("UPDATE routine_exercises SET weight = :weight, reps = :reps WHERE id = :id")
    suspend fun updateRoutineExercisePreview(id: Int, weight: Double, reps: String)

    // 👇 OPTIMIZACIÓN: Traer historial solo de un rango de tiempo (Ej: Hoy)
    @Query("SELECT * FROM exercises WHERE name = :exerciseName AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getHistoryByDateRange(exerciseName: String, startDate: Long, endDate: Long): Flow<List<ExerciseEntity>>
}