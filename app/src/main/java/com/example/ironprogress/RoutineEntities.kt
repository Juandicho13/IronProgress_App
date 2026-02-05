package com.example.ironprogress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// 1. LA RUTINA (Padre)
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val restTimeSeconds: Int,
    val folder: String = "General" // 🔥 ESTE CAMPO ES VITAL
)

// 2. LOS EJERCICIOS (Hijos)
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val routineId: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val weight: Double,
    val targetRestTime: Int? = null,
    val isKg: Boolean = true
)