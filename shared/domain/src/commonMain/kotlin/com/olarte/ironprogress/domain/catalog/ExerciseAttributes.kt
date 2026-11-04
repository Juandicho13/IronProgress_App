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

    // v3 (F1-01): equipo de las demás disciplinas. Van al final para no cambiar lo guardado.
    TRAP_BAR,
    MEDICINE_BALL,
    SANDBAG,
    SLED,
    PLYO_BOX,
    JUMP_ROPE,
    RINGS,
    PULL_UP_BAR,
    PARALLEL_BARS,
    SUSPENSION_TRAINER,
    FOAM_ROLLER,
    MAT,
    YOGA_BLOCK,
    YOGA_STRAP,
    REFORMER,
    ROWER,
    BIKE,
    TREADMILL,
    SKI_ERG,
    AIR_BIKE,
    HEAVY_BAG,
    HANGBOARD,
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

    // v3 (F1-01): patrones de funcional, calistenia, yoga y respiración.
    ROTATION,
    ANTI_ROTATION,
    JUMP,
    OLYMPIC_LIFT,
    LOCOMOTION,
    INVERSION,
    POSE,
    BREATHING,
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

    // v3 (F1-01). "Por lado" no es un tipo: es Exercise.unilateral con el lado en cada serie.

    /** Solo calorías: air bike, remo por calorías. */
    CALORIES(setOf(SetField.CALORIES)),

    /** Solo distancia: 400 m de carrera dentro de una ronda. */
    DISTANCE(setOf(SetField.DISTANCE)),

    /** Altura y reps: salto al cajón. */
    HEIGHT_REPS(setOf(SetField.HEIGHT, SetField.REPS)),

    /** Respiraciones: posturas de yoga contadas por respiración. */
    BREATHS(setOf(SetField.BREATHS)),

    /** Vatios y tiempo: bici o remo por potencia. */
    WATTS_DURATION(setOf(SetField.WATTS, SetField.DURATION)),
}

enum class SetField { WEIGHT, REPS, DURATION, DISTANCE, CALORIES, HEIGHT, BREATHS, WATTS }

/** Unidad para mostrar pesos. Todo se guarda en kilos. */
enum class WeightUnit { KG, LB }
