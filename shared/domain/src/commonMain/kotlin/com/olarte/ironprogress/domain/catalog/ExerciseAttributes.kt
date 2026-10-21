package com.olarte.ironprogress.domain.catalog

enum class Equipment {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    SMITH_MACHINE,
    EZ_BAR,
    KETTLEBELL,
    BAND,
    BODYWEIGHT,
    OTHER,
}

/** Patrón de movimiento: agrupa ejercicios intercambiables para sugerir sustitutos. */
enum class MovementPattern {
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    SQUAT,
    HINGE,
    LUNGE,
    HIP_EXTENSION,
    HIP_ABDUCTION,
    CHEST_FLY,
    SHOULDER_ISOLATION,
    SHRUG,
    ELBOW_FLEXION,
    ELBOW_EXTENSION,
    KNEE_EXTENSION,
    KNEE_FLEXION,
    CALF_RAISE,
    CORE,
    CARRY,
    CARDIO,
    OTHER,
}

/** Qué se anota en cada serie de un ejercicio. */
enum class TrackingType(val fields: Set<SetField>) {
    /** Peso y repeticiones: press banca, curl. */
    WEIGHT_REPS(setOf(SetField.WEIGHT, SetField.REPS)),

    /** Solo repeticiones: flexiones, dominadas sin lastre. */
    REPS(setOf(SetField.REPS)),

    /** El peso es el lastre que se suma al cuerpo: fondos lastrados. */
    WEIGHTED_BODYWEIGHT(setOf(SetField.WEIGHT, SetField.REPS)),

    /** El peso es la asistencia que se resta: dominadas asistidas. */
    ASSISTED_BODYWEIGHT(setOf(SetField.WEIGHT, SetField.REPS)),

    /** Solo tiempo: plancha. */
    DURATION(setOf(SetField.DURATION)),

    /** Distancia y tiempo: cinta, bicicleta, remo. */
    DISTANCE_DURATION(setOf(SetField.DISTANCE, SetField.DURATION)),

    /** Peso y distancia: paseo del granjero. */
    WEIGHT_DISTANCE(setOf(SetField.WEIGHT, SetField.DISTANCE)),
}

enum class SetField { WEIGHT, REPS, DURATION, DISTANCE }

/** Unidad para mostrar pesos. Todo se guarda en kilos. */
enum class WeightUnit { KG, LB }