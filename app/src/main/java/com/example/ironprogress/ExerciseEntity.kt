package com.example.ironprogress

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val targetSets: Int,
    val targetReps: String,
    val currentWeight: Double,
    val date: Long = System.currentTimeMillis() // <--- ¡NUEVO! Guarda la hora actual automática
)