package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.domain.catalog.ExerciseDemand
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseProgression
import com.olarte.ironprogress.domain.catalog.Joint
import com.olarte.ironprogress.domain.catalog.JointImpact
import com.olarte.ironprogress.domain.catalog.Muscle

/**
 * Datos de la versión 2 del catálogo semilla (F1-06, HANDOFF 11.10): exigencia, porcentaje de
 * cada músculo y escalera de los 77. Borrador revisado por Juan antes de cerrar el ticket.
 * Los MET salen del Compendio de actividades físicas 2024: 02054 pesas (3,5), 02022 calistenia
 * moderada (3,8), 02024 calistenia ligera (2,8), 02058 swing con pesa rusa (9,8) y 02071 remo
 * ergómetro moderado (5,0). Cinta, bicicleta y paseo del granjero quedan sin MET: no hay un código
 * claro sin velocidad, vatios o carga.
 */
internal object SeedCatalogFacts {

    /** Fuente y licencia de los 77: los escribió el equipo de IronProgress. */
    const val SOURCE = "IronProgress"
    const val LICENSE = "Propio"

    val workShares: Map<String, Map<Muscle, Int>> = mapOf(
        "barbell-bench-press" to mapOf(Muscle.CHEST to 55, Muscle.TRICEPS to 25, Muscle.SHOULDERS to 20),
        "incline-barbell-bench-press" to mapOf(Muscle.CHEST to 50, Muscle.SHOULDERS to 30, Muscle.TRICEPS to 20),
        "dumbbell-bench-press" to mapOf(Muscle.CHEST to 60, Muscle.TRICEPS to 20, Muscle.SHOULDERS to 20),
        "incline-dumbbell-bench-press" to mapOf(Muscle.CHEST to 55, Muscle.SHOULDERS to 30, Muscle.TRICEPS to 15),
        "chest-press-machine" to mapOf(Muscle.CHEST to 60, Muscle.TRICEPS to 20, Muscle.SHOULDERS to 20),
        "cable-chest-fly" to mapOf(Muscle.CHEST to 85, Muscle.SHOULDERS to 15),
        "pec-deck-fly" to mapOf(Muscle.CHEST to 100),
        "push-up" to mapOf(Muscle.CHEST to 55, Muscle.TRICEPS to 25, Muscle.SHOULDERS to 20),
        "lat-pulldown" to mapOf(Muscle.LATS to 65, Muscle.BICEPS to 20, Muscle.TRAPS to 15),
        "close-grip-lat-pulldown" to mapOf(Muscle.LATS to 70, Muscle.BICEPS to 30),
        "pull-up" to mapOf(Muscle.LATS to 65, Muscle.BICEPS to 25, Muscle.FOREARMS to 10),
        "assisted-pull-up" to mapOf(Muscle.LATS to 70, Muscle.BICEPS to 30),
        "barbell-row" to mapOf(Muscle.LATS to 50, Muscle.TRAPS to 25, Muscle.BICEPS to 15, Muscle.LOWER_BACK to 10),
        "one-arm-dumbbell-row" to mapOf(Muscle.LATS to 60, Muscle.TRAPS to 25, Muscle.BICEPS to 15),
        "seated-cable-row" to mapOf(Muscle.LATS to 60, Muscle.TRAPS to 25, Muscle.BICEPS to 15),
        "chest-supported-machine-row" to mapOf(Muscle.LATS to 60, Muscle.TRAPS to 25, Muscle.BICEPS to 15),
        "overhead-barbell-press" to mapOf(Muscle.SHOULDERS to 65, Muscle.TRICEPS to 25, Muscle.TRAPS to 10),
        "seated-dumbbell-shoulder-press" to mapOf(Muscle.SHOULDERS to 70, Muscle.TRICEPS to 30),
        "shoulder-press-machine" to mapOf(Muscle.SHOULDERS to 70, Muscle.TRICEPS to 30),
        "dumbbell-lateral-raise" to mapOf(Muscle.SHOULDERS to 100),
        "cable-lateral-raise" to mapOf(Muscle.SHOULDERS to 100),
        "reverse-pec-deck" to mapOf(Muscle.SHOULDERS to 75, Muscle.TRAPS to 25),
        "face-pull" to mapOf(Muscle.SHOULDERS to 65, Muscle.TRAPS to 35),
        "dumbbell-front-raise" to mapOf(Muscle.SHOULDERS to 100),
        "barbell-bicep-curl" to mapOf(Muscle.BICEPS to 80, Muscle.FOREARMS to 20),
        "ez-bar-bicep-curl" to mapOf(Muscle.BICEPS to 80, Muscle.FOREARMS to 20),
        "dumbbell-bicep-curl" to mapOf(Muscle.BICEPS to 85, Muscle.FOREARMS to 15),
        "incline-dumbbell-curl" to mapOf(Muscle.BICEPS to 100),
        "hammer-curl" to mapOf(Muscle.BICEPS to 60, Muscle.FOREARMS to 40),
        "cable-bicep-curl" to mapOf(Muscle.BICEPS to 100),
        "cable-tricep-pushdown" to mapOf(Muscle.TRICEPS to 100),
        "overhead-cable-tricep-extension" to mapOf(Muscle.TRICEPS to 100),
        "ez-bar-skull-crusher" to mapOf(Muscle.TRICEPS to 100),
        "close-grip-bench-press" to mapOf(Muscle.TRICEPS to 50, Muscle.CHEST to 35, Muscle.SHOULDERS to 15),
        "tricep-dip" to mapOf(Muscle.TRICEPS to 55, Muscle.CHEST to 30, Muscle.SHOULDERS to 15),
        "dumbbell-overhead-tricep-extension" to mapOf(Muscle.TRICEPS to 100),
        "barbell-back-squat" to mapOf(Muscle.QUADS to 55, Muscle.GLUTES to 30, Muscle.LOWER_BACK to 15),
        "barbell-front-squat" to mapOf(Muscle.QUADS to 65, Muscle.GLUTES to 20, Muscle.ABS to 15),
        "hack-squat" to mapOf(Muscle.QUADS to 75, Muscle.GLUTES to 25),
        "leg-press" to mapOf(Muscle.QUADS to 70, Muscle.GLUTES to 30),
        "bulgarian-split-squat" to mapOf(Muscle.QUADS to 60, Muscle.GLUTES to 40),
        "dumbbell-walking-lunge" to mapOf(Muscle.QUADS to 55, Muscle.GLUTES to 30, Muscle.HAMSTRINGS to 15),
        "leg-extension" to mapOf(Muscle.QUADS to 100),
        "smith-machine-squat" to mapOf(Muscle.QUADS to 70, Muscle.GLUTES to 30),
        "romanian-deadlift" to mapOf(Muscle.HAMSTRINGS to 55, Muscle.GLUTES to 30, Muscle.LOWER_BACK to 15),
        "dumbbell-romanian-deadlift" to mapOf(Muscle.HAMSTRINGS to 60, Muscle.GLUTES to 40),
        "lying-leg-curl" to mapOf(Muscle.HAMSTRINGS to 100),
        "seated-leg-curl" to mapOf(Muscle.HAMSTRINGS to 100),
        "nordic-hamstring-curl" to mapOf(Muscle.HAMSTRINGS to 100),
        "barbell-good-morning" to mapOf(Muscle.HAMSTRINGS to 50, Muscle.LOWER_BACK to 30, Muscle.GLUTES to 20),
        "barbell-hip-thrust" to mapOf(Muscle.GLUTES to 75, Muscle.HAMSTRINGS to 25),
        "hip-thrust-machine" to mapOf(Muscle.GLUTES to 75, Muscle.HAMSTRINGS to 25),
        "cable-glute-kickback" to mapOf(Muscle.GLUTES to 100),
        "hip-abduction-machine" to mapOf(Muscle.GLUTES to 100),
        "glute-bridge" to mapOf(Muscle.GLUTES to 100),
        "kettlebell-swing" to mapOf(Muscle.GLUTES to 55, Muscle.HAMSTRINGS to 30, Muscle.LOWER_BACK to 15),
        "conventional-deadlift" to mapOf(Muscle.LOWER_BACK to 35, Muscle.HAMSTRINGS to 25, Muscle.GLUTES to 25, Muscle.TRAPS to 15),
        "sumo-deadlift" to mapOf(Muscle.LOWER_BACK to 30, Muscle.GLUTES to 25, Muscle.QUADS to 25, Muscle.HAMSTRINGS to 20),
        "back-extension" to mapOf(Muscle.LOWER_BACK to 50, Muscle.GLUTES to 30, Muscle.HAMSTRINGS to 20),
        "barbell-rack-pull" to mapOf(Muscle.LOWER_BACK to 45, Muscle.TRAPS to 30, Muscle.GLUTES to 25),
        "barbell-shrug" to mapOf(Muscle.TRAPS to 85, Muscle.FOREARMS to 15),
        "dumbbell-shrug" to mapOf(Muscle.TRAPS to 85, Muscle.FOREARMS to 15),
        "farmers-walk" to mapOf(Muscle.TRAPS to 50, Muscle.FOREARMS to 35, Muscle.ABS to 15),
        "barbell-wrist-curl" to mapOf(Muscle.FOREARMS to 100),
        "reverse-barbell-curl" to mapOf(Muscle.FOREARMS to 55, Muscle.BICEPS to 45),
        "dead-hang" to mapOf(Muscle.FOREARMS to 100),
        "standing-calf-raise" to mapOf(Muscle.CALVES to 100),
        "seated-calf-raise" to mapOf(Muscle.CALVES to 100),
        "leg-press-calf-raise" to mapOf(Muscle.CALVES to 100),
        "cable-crunch" to mapOf(Muscle.ABS to 100),
        "hanging-leg-raise" to mapOf(Muscle.ABS to 100),
        "ab-wheel-rollout" to mapOf(Muscle.ABS to 100),
        "plank" to mapOf(Muscle.ABS to 100),
        "pallof-press" to mapOf(Muscle.ABS to 100),
    )

    val demands: Map<String, ExerciseDemand> = mapOf(
        "barbell-bench-press" to
            ExerciseDemand(
                4,
                3,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.HIGH,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "incline-barbell-bench-press" to
            ExerciseDemand(
                4,
                3,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.HIGH,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "dumbbell-bench-press" to ExerciseDemand(3, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "incline-dumbbell-bench-press" to ExerciseDemand(3, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "chest-press-machine" to ExerciseDemand(2, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "cable-chest-fly" to ExerciseDemand(2, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "pec-deck-fly" to ExerciseDemand(2, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "push-up" to
            ExerciseDemand(
                2,
                2,
                1,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.HIGH,
                ),
                3.8,
            ),
        "lat-pulldown" to ExerciseDemand(3, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "close-grip-lat-pulldown" to
            ExerciseDemand(3, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "pull-up" to
            ExerciseDemand(
                4,
                3,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.HIGH,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.8,
            ),
        "assisted-pull-up" to ExerciseDemand(3, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.8),
        "barbell-row" to
            ExerciseDemand(
                3,
                4,
                2,
                mapOf(
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "one-arm-dumbbell-row" to
            ExerciseDemand(
                3,
                2,
                1,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "seated-cable-row" to
            ExerciseDemand(
                3,
                2,
                1,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "chest-supported-machine-row" to
            ExerciseDemand(3, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "overhead-barbell-press" to
            ExerciseDemand(
                4,
                4,
                2,
                mapOf(
                    Joint.SHOULDER to JointImpact.HIGH,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "seated-dumbbell-shoulder-press" to
            ExerciseDemand(3, 2, 1, mapOf(Joint.SHOULDER to JointImpact.HIGH, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "shoulder-press-machine" to
            ExerciseDemand(3, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "dumbbell-lateral-raise" to ExerciseDemand(2, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "cable-lateral-raise" to ExerciseDemand(2, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "reverse-pec-deck" to ExerciseDemand(2, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "face-pull" to ExerciseDemand(2, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "dumbbell-front-raise" to ExerciseDemand(2, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM), 3.5),
        "barbell-bicep-curl" to ExerciseDemand(1, 2, 1, mapOf(Joint.ELBOW to JointImpact.MEDIUM, Joint.WRIST to JointImpact.MEDIUM), 3.5),
        "ez-bar-bicep-curl" to ExerciseDemand(1, 1, 1, mapOf(Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "dumbbell-bicep-curl" to ExerciseDemand(1, 1, 0, mapOf(Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "incline-dumbbell-curl" to
            ExerciseDemand(1, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "hammer-curl" to ExerciseDemand(1, 1, 0, mapOf(Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "cable-bicep-curl" to ExerciseDemand(1, 1, 0, mapOf(Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "cable-tricep-pushdown" to ExerciseDemand(1, 1, 0, mapOf(Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "overhead-cable-tricep-extension" to
            ExerciseDemand(1, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "ez-bar-skull-crusher" to
            ExerciseDemand(
                1,
                2,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.HIGH,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "close-grip-bench-press" to
            ExerciseDemand(
                3,
                3,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.HIGH,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "tricep-dip" to
            ExerciseDemand(
                3,
                3,
                0,
                mapOf(
                    Joint.SHOULDER to JointImpact.HIGH,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                ),
                3.8,
            ),
        "dumbbell-overhead-tricep-extension" to
            ExerciseDemand(1, 2, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.ELBOW to JointImpact.MEDIUM), 3.5),
        "barbell-back-squat" to
            ExerciseDemand(
                5,
                4,
                3,
                mapOf(
                    Joint.KNEE to JointImpact.HIGH,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "barbell-front-squat" to
            ExerciseDemand(
                5,
                5,
                2,
                mapOf(
                    Joint.KNEE to JointImpact.HIGH,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "hack-squat" to ExerciseDemand(4, 1, 1, mapOf(Joint.KNEE to JointImpact.HIGH, Joint.HIP to JointImpact.MEDIUM), 3.5),
        "leg-press" to
            ExerciseDemand(
                4,
                1,
                1,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "bulgarian-split-squat" to ExerciseDemand(3, 3, 1, mapOf(Joint.KNEE to JointImpact.HIGH, Joint.HIP to JointImpact.MEDIUM), 3.5),
        "dumbbell-walking-lunge" to ExerciseDemand(3, 2, 1, mapOf(Joint.KNEE to JointImpact.HIGH, Joint.HIP to JointImpact.MEDIUM), 3.8),
        "leg-extension" to ExerciseDemand(2, 1, 0, mapOf(Joint.KNEE to JointImpact.HIGH), 3.5),
        "smith-machine-squat" to
            ExerciseDemand(
                4,
                2,
                2,
                mapOf(
                    Joint.KNEE to JointImpact.HIGH,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "romanian-deadlift" to
            ExerciseDemand(
                5,
                4,
                3,
                mapOf(
                    Joint.HIP to JointImpact.HIGH,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "dumbbell-romanian-deadlift" to
            ExerciseDemand(4, 3, 2, mapOf(Joint.HIP to JointImpact.MEDIUM, Joint.LOWER_BACK to JointImpact.MEDIUM), 3.5),
        "lying-leg-curl" to ExerciseDemand(2, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM), 3.5),
        "seated-leg-curl" to ExerciseDemand(2, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM), 3.5),
        "nordic-hamstring-curl" to ExerciseDemand(3, 3, 0, mapOf(Joint.KNEE to JointImpact.HIGH), 3.8),
        "barbell-good-morning" to
            ExerciseDemand(
                5,
                4,
                3,
                mapOf(
                    Joint.HIP to JointImpact.HIGH,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "barbell-hip-thrust" to
            ExerciseDemand(
                3,
                2,
                1,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "hip-thrust-machine" to ExerciseDemand(3, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM, Joint.HIP to JointImpact.MEDIUM), 3.5),
        "cable-glute-kickback" to
            ExerciseDemand(2, 2, 1, mapOf(Joint.HIP to JointImpact.MEDIUM, Joint.LOWER_BACK to JointImpact.MEDIUM), 3.5),
        "hip-abduction-machine" to ExerciseDemand(2, 1, 0, mapOf(Joint.HIP to JointImpact.MEDIUM), 3.5),
        "glute-bridge" to ExerciseDemand(2, 1, 0, mapOf(Joint.HIP to JointImpact.MEDIUM), 3.5),
        "kettlebell-swing" to
            ExerciseDemand(
                4,
                4,
                2,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.HIGH,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                9.8,
            ),
        "conventional-deadlift" to
            ExerciseDemand(
                5,
                5,
                3,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.HIGH,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "sumo-deadlift" to
            ExerciseDemand(
                5,
                5,
                3,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.HIGH,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "back-extension" to ExerciseDemand(2, 2, 2, mapOf(Joint.HIP to JointImpact.MEDIUM, Joint.LOWER_BACK to JointImpact.MEDIUM), 3.5),
        "barbell-rack-pull" to
            ExerciseDemand(
                4,
                3,
                3,
                mapOf(
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.HIGH,
                ),
                3.5,
            ),
        "barbell-shrug" to
            ExerciseDemand(
                2,
                1,
                2,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                3.5,
            ),
        "dumbbell-shrug" to ExerciseDemand(2, 1, 1, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.WRIST to JointImpact.MEDIUM), 3.5),
        "farmers-walk" to
            ExerciseDemand(
                3,
                1,
                2,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                null,
            ),
        "barbell-wrist-curl" to ExerciseDemand(1, 1, 0, mapOf(Joint.WRIST to JointImpact.MEDIUM), 3.5),
        "reverse-barbell-curl" to ExerciseDemand(1, 2, 0, mapOf(Joint.ELBOW to JointImpact.MEDIUM, Joint.WRIST to JointImpact.MEDIUM), 3.5),
        "dead-hang" to ExerciseDemand(1, 1, 0, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.WRIST to JointImpact.MEDIUM), null),
        "standing-calf-raise" to
            ExerciseDemand(1, 1, 2, mapOf(Joint.KNEE to JointImpact.MEDIUM, Joint.LOWER_BACK to JointImpact.MEDIUM), 3.5),
        "seated-calf-raise" to ExerciseDemand(1, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM), 3.5),
        "leg-press-calf-raise" to ExerciseDemand(1, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM), 3.5),
        "cable-crunch" to ExerciseDemand(2, 2, 1, mapOf(Joint.LOWER_BACK to JointImpact.MEDIUM), 3.5),
        "hanging-leg-raise" to
            ExerciseDemand(
                2,
                3,
                1,
                mapOf(
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                2.8,
            ),
        "ab-wheel-rollout" to
            ExerciseDemand(
                2,
                3,
                2,
                mapOf(
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.WRIST to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                2.8,
            ),
        "plank" to ExerciseDemand(2, 1, 1, mapOf(Joint.SHOULDER to JointImpact.MEDIUM, Joint.WRIST to JointImpact.MEDIUM), 2.8),
        "pallof-press" to ExerciseDemand(2, 2, 1, mapOf(), 3.5),
        "treadmill-run" to
            ExerciseDemand(
                3,
                1,
                1,
                mapOf(
                    Joint.KNEE to JointImpact.HIGH,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                null,
            ),
        "stationary-bike" to ExerciseDemand(2, 1, 0, mapOf(Joint.KNEE to JointImpact.MEDIUM), null),
        "rowing-ergometer" to
            ExerciseDemand(
                3,
                3,
                1,
                mapOf(
                    Joint.KNEE to JointImpact.MEDIUM,
                    Joint.HIP to JointImpact.MEDIUM,
                    Joint.SHOULDER to JointImpact.MEDIUM,
                    Joint.ELBOW to JointImpact.MEDIUM,
                    Joint.LOWER_BACK to JointImpact.MEDIUM,
                ),
                5.0,
            ),
    )

    val progressions: Map<String, ExerciseProgression> = mapOf(
        "barbell-bench-press" to ExerciseProgression(ExerciseId("dumbbell-bench-press"), null),
        "incline-barbell-bench-press" to ExerciseProgression(ExerciseId("incline-dumbbell-bench-press"), null),
        "dumbbell-bench-press" to ExerciseProgression(ExerciseId("chest-press-machine"), ExerciseId("barbell-bench-press")),
        "incline-dumbbell-bench-press" to ExerciseProgression(null, ExerciseId("incline-barbell-bench-press")),
        "chest-press-machine" to ExerciseProgression(null, ExerciseId("dumbbell-bench-press")),
        "cable-chest-fly" to ExerciseProgression(ExerciseId("pec-deck-fly"), null),
        "pec-deck-fly" to ExerciseProgression(null, ExerciseId("cable-chest-fly")),
        "lat-pulldown" to ExerciseProgression(null, ExerciseId("assisted-pull-up")),
        "close-grip-lat-pulldown" to ExerciseProgression(null, ExerciseId("assisted-pull-up")),
        "pull-up" to ExerciseProgression(ExerciseId("assisted-pull-up"), null),
        "assisted-pull-up" to ExerciseProgression(ExerciseId("lat-pulldown"), ExerciseId("pull-up")),
        "barbell-row" to ExerciseProgression(ExerciseId("chest-supported-machine-row"), null),
        "one-arm-dumbbell-row" to ExerciseProgression(ExerciseId("seated-cable-row"), ExerciseId("barbell-row")),
        "seated-cable-row" to ExerciseProgression(ExerciseId("chest-supported-machine-row"), ExerciseId("one-arm-dumbbell-row")),
        "chest-supported-machine-row" to ExerciseProgression(null, ExerciseId("seated-cable-row")),
        "overhead-barbell-press" to ExerciseProgression(ExerciseId("seated-dumbbell-shoulder-press"), null),
        "seated-dumbbell-shoulder-press" to ExerciseProgression(ExerciseId("shoulder-press-machine"), ExerciseId("overhead-barbell-press")),
        "shoulder-press-machine" to ExerciseProgression(null, ExerciseId("seated-dumbbell-shoulder-press")),
        "dumbbell-lateral-raise" to ExerciseProgression(ExerciseId("cable-lateral-raise"), null),
        "cable-lateral-raise" to ExerciseProgression(null, ExerciseId("dumbbell-lateral-raise")),
        "reverse-pec-deck" to ExerciseProgression(null, ExerciseId("face-pull")),
        "face-pull" to ExerciseProgression(ExerciseId("reverse-pec-deck"), null),
        "barbell-bicep-curl" to ExerciseProgression(ExerciseId("ez-bar-bicep-curl"), null),
        "ez-bar-bicep-curl" to ExerciseProgression(ExerciseId("dumbbell-bicep-curl"), ExerciseId("barbell-bicep-curl")),
        "dumbbell-bicep-curl" to ExerciseProgression(ExerciseId("cable-bicep-curl"), ExerciseId("ez-bar-bicep-curl")),
        "incline-dumbbell-curl" to ExerciseProgression(ExerciseId("dumbbell-bicep-curl"), null),
        "cable-bicep-curl" to ExerciseProgression(null, ExerciseId("dumbbell-bicep-curl")),
        "cable-tricep-pushdown" to ExerciseProgression(null, ExerciseId("overhead-cable-tricep-extension")),
        "overhead-cable-tricep-extension" to ExerciseProgression(ExerciseId("cable-tricep-pushdown"), null),
        "ez-bar-skull-crusher" to ExerciseProgression(ExerciseId("cable-tricep-pushdown"), null),
        "tricep-dip" to ExerciseProgression(ExerciseId("cable-tricep-pushdown"), null),
        "dumbbell-overhead-tricep-extension" to ExerciseProgression(ExerciseId("cable-tricep-pushdown"), null),
        "barbell-back-squat" to ExerciseProgression(ExerciseId("hack-squat"), null),
        "barbell-front-squat" to ExerciseProgression(ExerciseId("hack-squat"), null),
        "hack-squat" to ExerciseProgression(ExerciseId("leg-press"), ExerciseId("barbell-back-squat")),
        "leg-press" to ExerciseProgression(null, ExerciseId("hack-squat")),
        "bulgarian-split-squat" to ExerciseProgression(ExerciseId("dumbbell-walking-lunge"), null),
        "dumbbell-walking-lunge" to ExerciseProgression(null, ExerciseId("bulgarian-split-squat")),
        "smith-machine-squat" to ExerciseProgression(ExerciseId("leg-press"), ExerciseId("barbell-back-squat")),
        "romanian-deadlift" to ExerciseProgression(ExerciseId("dumbbell-romanian-deadlift"), null),
        "dumbbell-romanian-deadlift" to ExerciseProgression(ExerciseId("seated-leg-curl"), ExerciseId("romanian-deadlift")),
        "lying-leg-curl" to ExerciseProgression(null, ExerciseId("nordic-hamstring-curl")),
        "seated-leg-curl" to ExerciseProgression(null, ExerciseId("lying-leg-curl")),
        "nordic-hamstring-curl" to ExerciseProgression(ExerciseId("lying-leg-curl"), null),
        "barbell-good-morning" to ExerciseProgression(ExerciseId("romanian-deadlift"), null),
        "barbell-hip-thrust" to ExerciseProgression(ExerciseId("glute-bridge"), null),
        "hip-thrust-machine" to ExerciseProgression(ExerciseId("glute-bridge"), ExerciseId("barbell-hip-thrust")),
        "glute-bridge" to ExerciseProgression(null, ExerciseId("barbell-hip-thrust")),
        "conventional-deadlift" to ExerciseProgression(ExerciseId("barbell-rack-pull"), null),
        "sumo-deadlift" to ExerciseProgression(ExerciseId("barbell-rack-pull"), null),
        "back-extension" to ExerciseProgression(null, ExerciseId("barbell-good-morning")),
        "barbell-rack-pull" to ExerciseProgression(null, ExerciseId("conventional-deadlift")),
        "barbell-shrug" to ExerciseProgression(ExerciseId("dumbbell-shrug"), null),
        "dumbbell-shrug" to ExerciseProgression(null, ExerciseId("barbell-shrug")),
        "dead-hang" to ExerciseProgression(null, ExerciseId("pull-up")),
        "standing-calf-raise" to ExerciseProgression(ExerciseId("seated-calf-raise"), null),
        "seated-calf-raise" to ExerciseProgression(null, ExerciseId("standing-calf-raise")),
        "leg-press-calf-raise" to ExerciseProgression(null, ExerciseId("standing-calf-raise")),
        "hanging-leg-raise" to ExerciseProgression(ExerciseId("plank"), ExerciseId("ab-wheel-rollout")),
        "ab-wheel-rollout" to ExerciseProgression(ExerciseId("plank"), null),
        "plank" to ExerciseProgression(null, ExerciseId("ab-wheel-rollout")),
        "pallof-press" to ExerciseProgression(ExerciseId("plank"), null),
    )
}
