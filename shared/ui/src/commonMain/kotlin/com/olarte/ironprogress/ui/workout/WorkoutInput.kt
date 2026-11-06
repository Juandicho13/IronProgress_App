package com.olarte.ironprogress.ui.workout

import com.olarte.ironprogress.domain.session.WorkoutSet

fun WorkoutSet.value(field: WorkoutField): Double? = when (field) {
    WorkoutField.WEIGHT -> weightKg
    WorkoutField.REPS -> reps?.toDouble()
    WorkoutField.DURATION -> durationSeconds?.toDouble()
    WorkoutField.DISTANCE -> distanceMeters
    WorkoutField.CALORIES -> calories?.toDouble()
    WorkoutField.HEIGHT -> heightCm
    WorkoutField.BREATHS -> breaths?.toDouble()
    WorkoutField.WATTS -> watts?.toDouble()
    WorkoutField.RIR -> rir?.toDouble()
    WorkoutField.RPE -> rpe
}

/** Devuelve null si el texto no puede guardarse. Vacío borra el dato opcional. */
fun WorkoutSet.withInput(field: WorkoutField, text: String): WorkoutSet? {
    val number = if (text.isBlank()) null else text.replace(',', '.').toDoubleOrNull() ?: return null
    if (number != null && (!number.isFinite() || number < 0 || number > 1_000_000)) return null
    val decimal = field in listOf(WorkoutField.WEIGHT, WorkoutField.DISTANCE, WorkoutField.HEIGHT, WorkoutField.RPE)
    if (!decimal && number != null && number % 1.0 != 0.0) return null
    if (field == WorkoutField.RIR && number != null && number !in 0.0..10.0) return null
    if (field == WorkoutField.RPE && number != null && (number !in 1.0..10.0 || number * 2 % 1 != 0.0)) return null
    return when (field) {
        WorkoutField.WEIGHT -> copy(weightKg = number)
        WorkoutField.REPS -> copy(reps = number?.toInt())
        WorkoutField.DURATION -> copy(durationSeconds = number?.toInt())
        WorkoutField.DISTANCE -> copy(distanceMeters = number)
        WorkoutField.CALORIES -> copy(calories = number?.toInt())
        WorkoutField.HEIGHT -> copy(heightCm = number)
        WorkoutField.BREATHS -> copy(breaths = number?.toInt())
        WorkoutField.WATTS -> copy(watts = number?.toInt())
        WorkoutField.RIR -> copy(rir = number?.toInt(), rpe = null)
        WorkoutField.RPE -> copy(rpe = number, rir = null)
    }
}
