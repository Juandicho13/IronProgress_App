package com.example.ironprogress

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Base de Datos: Registro de Ejercicio (Historial).
 *
 * Representa una unidad atómica de entrenamiento (un set o ejercicio completado).
 * Esta clase define la estructura de la tabla "exercises" en la base de datos SQLite (vía Room).
 *
 * Se utiliza para:
 * 1. Persistir el historial de entrenamiento del usuario (Logbook).
 * 2. Alimentar las gráficas de progreso y volumen.
 * 3. Calcular métricas avanzadas como el 1RM estimado.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    /**
     * Clave primaria autogenerada. Identifica de forma única cada registro.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * Nombre del ejercicio (ej. "Press de Banca", "Sentadilla").
     * Actúa como clave foránea lógica para agrupar historiales.
     */
    val name: String,

    /**
     * Grupo muscular principal involucrado (ej. "Pecho", "Piernas").
     * Utilizado para análisis de volumen por grupo muscular.
     */
    val muscleGroup: String,

    /**
     * Número de la serie (Set Index) o cantidad de series objetivo.
     */
    val targetSets: Int,

    /**
     * Repeticiones realizadas.
     * Se almacena como [String] para permitir formatos flexibles (ej. "8-12", "Fallo"),
     * aunque los cálculos de 1RM parsean el valor numérico principal.
     */
    val targetReps: String,

    /**
     * Carga utilizada (Peso).
     * La unidad (kg/lb) depende de la configuración global del usuario al momento de guardar.
     */
    val currentWeight: Double,

    /**
     * Marca de tiempo (Timestamp en milisegundos).
     * Captura el momento exacto de la finalización del set (`System.currentTimeMillis()`).
     * Esencial para el ordenamiento cronológico y filtrado por fecha.
     */
    val date: Long = System.currentTimeMillis()
)