package com.example.ironprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) principal de IronProgress.
 *
 * Define la interfaz de comunicación con la base de datos SQLite (vía Room).
 * Contiene todas las operaciones CRUD y consultas complejas necesarias para
 * la gestión de Rutinas, Historial de Ejercicios y Analíticas.
 *
 * Las funciones que retornan [Flow] son reactivas y notifican cambios en tiempo real.
 * Las funciones [suspend] son operaciones "one-shot" que se ejecutan en corrutinas.
 */
@Dao
interface IronDao {

    //region --- Historial de Entrenamiento (Logbook) ---

    /**
     * Obtiene el historial completo de ejercicios realizados.
     * Ordenado por fecha descendente (lo más reciente primero) para el feed principal.
     */
    @Query("SELECT * FROM exercises ORDER BY date DESC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Obtiene el historial de un ejercicio específico para generar gráficas.
     * Ordenado por fecha ASCENDENTE (del más antiguo al nuevo) para trazar la línea de tiempo.
     */
    @Query("SELECT * FROM exercises WHERE name = :exerciseName ORDER BY date ASC")
    fun getHistoryByName(exerciseName: String): Flow<List<ExerciseEntity>>

    /**
     * Filtra el historial por nombre y rango de fechas (ej. "Hoy", "Esta semana").
     * Utilizado para validar si ya se entrenó un ejercicio en la sesión actual.
     */
    @Query("SELECT * FROM exercises WHERE name = :exerciseName AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getHistoryByDateRange(exerciseName: String, startDate: Long, endDate: Long): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Int)
    //endregion

    //region --- Gestión de Rutinas (Routines) ---

    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Int): RoutineEntity?

    /**
     * Inserta una nueva rutina y retorna su ID (RowID).
     * El ID es necesario para insertar los ejercicios hijos inmediatamente después.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Int)
    //endregion

    //region --- Detalle de Rutinas (Routine Exercises/Templates) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(exercise: RoutineExerciseEntity)

    /**
     * Obtiene la lista de ejercicios plantilla asociados a una rutina.
     */
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId")
    suspend fun getRoutineExercises(routineId: Int): List<RoutineExerciseEntity>

    /**
     * Actualiza la "previsualización" de peso y reps en la plantilla de rutina.
     * Esto implementa la lógica de "Recordar último peso usado" para la próxima sesión.
     */
    @Query("UPDATE routine_exercises SET weight = :weight, reps = :reps WHERE id = :id")
    suspend fun updateRoutineExercisePreview(id: Int, weight: Double, reps: String)

    /**
     * Eliminación en cascada manual: Borra los ejercicios de una rutina antes de re-insertarlos
     * durante una edición completa.
     */
    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Int)
    //endregion

    //region --- Analíticas y Utilidades ---

    /**
     * Obtiene una lista de nombres únicos de ejercicios ya registrados.
     * Utilizado para sugerencias de autocompletado o filtrado en gráficas.
     */
    @Query("SELECT DISTINCT name FROM exercises ORDER BY name ASC")
    fun getUniqueExerciseNames(): Flow<List<String>>
    //endregion
}