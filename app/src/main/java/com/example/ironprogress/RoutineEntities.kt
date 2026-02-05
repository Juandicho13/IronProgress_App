package com.example.ironprogress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una Rutina de Entrenamiento (Cabecera).
 * Ej: "Pierna Lunes", "Empuje A", "Torso".
 *
 * @property restTimeSeconds Tiempo de descanso global por defecto para esta rutina.
 * @property folder Carpeta organizativa (ej. "GIMNASIO", "CASA") para agrupar rutinas.
 */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    /**
     * Tiempo de descanso base (en segundos) que se aplicará al temporizador
     * si el ejercicio específico no tiene uno propio configurado.
     */
    val restTimeSeconds: Int = 90,

    /**
     * Etiqueta de agrupación. Se normaliza a mayúsculas en el ViewModel
     * para mantener el orden en la UI.
     */
    val folder: String = "GENERAL"
)

/**
 * Entidad que representa un Ejercicio dentro de una Rutina (Plantilla).
 *
 * Configurada con [ForeignKey] para asegurar la integridad referencial:
 * Si se borra la Rutina padre, se borran automáticamente sus ejercicios (CASCADE).
 *
 * @property routineId ID de la rutina a la que pertenece (Clave Foránea).
 * @property weight Último peso registrado. Se actualiza automáticamente al finalizar un set
 * para implementar la "Sobrecarga Progresiva".
 */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE // Si borras la rutina, se borran sus ejercicios
        )
    ],
    indices = [Index(value = ["routineId"])] // Optimiza las consultas por rutina
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val routineId: Int, // Enlace con el padre

    val name: String,
    val sets: Int,
    val reps: String,
    val weight: Double,
    val isKg: Boolean = true,

    /**
     * Tiempo de descanso específico para este ejercicio.
     * Si es null, el sistema usará el [RoutineEntity.restTimeSeconds] global.
     * Útil para ejercicios pesados (Sentadilla) que requieren más descanso que accesorios.
     */
    val targetRestTime: Int? = null
)