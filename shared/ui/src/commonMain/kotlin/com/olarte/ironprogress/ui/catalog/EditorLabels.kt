package com.olarte.ironprogress.ui.catalog

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.discipline_breathing
import com.olarte.ironprogress.ui.resources.discipline_calisthenics
import com.olarte.ironprogress.ui.resources.discipline_cardio
import com.olarte.ironprogress.ui.resources.discipline_climbing
import com.olarte.ironprogress.ui.resources.discipline_combat
import com.olarte.ironprogress.ui.resources.discipline_functional
import com.olarte.ironprogress.ui.resources.discipline_hiit
import com.olarte.ironprogress.ui.resources.discipline_mobility
import com.olarte.ironprogress.ui.resources.discipline_pilates
import com.olarte.ironprogress.ui.resources.discipline_rehab
import com.olarte.ironprogress.ui.resources.discipline_sport
import com.olarte.ironprogress.ui.resources.discipline_strength
import com.olarte.ironprogress.ui.resources.discipline_weightlifting
import com.olarte.ironprogress.ui.resources.discipline_yoga
import com.olarte.ironprogress.ui.resources.movementpattern_anti_rotation
import com.olarte.ironprogress.ui.resources.movementpattern_breathing
import com.olarte.ironprogress.ui.resources.movementpattern_calf_raise
import com.olarte.ironprogress.ui.resources.movementpattern_cardio
import com.olarte.ironprogress.ui.resources.movementpattern_carry
import com.olarte.ironprogress.ui.resources.movementpattern_chest_fly
import com.olarte.ironprogress.ui.resources.movementpattern_core
import com.olarte.ironprogress.ui.resources.movementpattern_elbow_extension
import com.olarte.ironprogress.ui.resources.movementpattern_elbow_flexion
import com.olarte.ironprogress.ui.resources.movementpattern_hinge
import com.olarte.ironprogress.ui.resources.movementpattern_hip_abduction
import com.olarte.ironprogress.ui.resources.movementpattern_hip_extension
import com.olarte.ironprogress.ui.resources.movementpattern_horizontal_pull
import com.olarte.ironprogress.ui.resources.movementpattern_horizontal_push
import com.olarte.ironprogress.ui.resources.movementpattern_inversion
import com.olarte.ironprogress.ui.resources.movementpattern_jump
import com.olarte.ironprogress.ui.resources.movementpattern_knee_extension
import com.olarte.ironprogress.ui.resources.movementpattern_knee_flexion
import com.olarte.ironprogress.ui.resources.movementpattern_locomotion
import com.olarte.ironprogress.ui.resources.movementpattern_lunge
import com.olarte.ironprogress.ui.resources.movementpattern_olympic_lift
import com.olarte.ironprogress.ui.resources.movementpattern_other
import com.olarte.ironprogress.ui.resources.movementpattern_pose
import com.olarte.ironprogress.ui.resources.movementpattern_rotation
import com.olarte.ironprogress.ui.resources.movementpattern_shoulder_isolation
import com.olarte.ironprogress.ui.resources.movementpattern_shrug
import com.olarte.ironprogress.ui.resources.movementpattern_squat
import com.olarte.ironprogress.ui.resources.movementpattern_vertical_pull
import com.olarte.ironprogress.ui.resources.movementpattern_vertical_push
import com.olarte.ironprogress.ui.resources.trackingtype_assisted_bodyweight
import com.olarte.ironprogress.ui.resources.trackingtype_breaths
import com.olarte.ironprogress.ui.resources.trackingtype_calories
import com.olarte.ironprogress.ui.resources.trackingtype_distance
import com.olarte.ironprogress.ui.resources.trackingtype_distance_duration
import com.olarte.ironprogress.ui.resources.trackingtype_duration
import com.olarte.ironprogress.ui.resources.trackingtype_height_reps
import com.olarte.ironprogress.ui.resources.trackingtype_reps
import com.olarte.ironprogress.ui.resources.trackingtype_watts_duration
import com.olarte.ironprogress.ui.resources.trackingtype_weight_distance
import com.olarte.ironprogress.ui.resources.trackingtype_weight_reps
import com.olarte.ironprogress.ui.resources.trackingtype_weighted_bodyweight
import org.jetbrains.compose.resources.stringResource

@Composable
fun Discipline.editorLabel(): String = stringResource(
    when (this) {
        Discipline.STRENGTH -> Res.string.discipline_strength
        Discipline.WEIGHTLIFTING -> Res.string.discipline_weightlifting
        Discipline.CALISTHENICS -> Res.string.discipline_calisthenics
        Discipline.FUNCTIONAL -> Res.string.discipline_functional
        Discipline.HIIT -> Res.string.discipline_hiit
        Discipline.COMBAT -> Res.string.discipline_combat
        Discipline.YOGA -> Res.string.discipline_yoga
        Discipline.PILATES -> Res.string.discipline_pilates
        Discipline.MOBILITY -> Res.string.discipline_mobility
        Discipline.BREATHING -> Res.string.discipline_breathing
        Discipline.CLIMBING -> Res.string.discipline_climbing
        Discipline.CARDIO -> Res.string.discipline_cardio
        Discipline.REHAB -> Res.string.discipline_rehab
        Discipline.SPORT -> Res.string.discipline_sport
    },
)

@Composable
fun MovementPattern.editorLabel(): String = stringResource(
    when (this) {
        MovementPattern.HORIZONTAL_PUSH -> Res.string.movementpattern_horizontal_push
        MovementPattern.VERTICAL_PUSH -> Res.string.movementpattern_vertical_push
        MovementPattern.HORIZONTAL_PULL -> Res.string.movementpattern_horizontal_pull
        MovementPattern.VERTICAL_PULL -> Res.string.movementpattern_vertical_pull
        MovementPattern.SQUAT -> Res.string.movementpattern_squat
        MovementPattern.HINGE -> Res.string.movementpattern_hinge
        MovementPattern.LUNGE -> Res.string.movementpattern_lunge
        MovementPattern.HIP_EXTENSION -> Res.string.movementpattern_hip_extension
        MovementPattern.HIP_ABDUCTION -> Res.string.movementpattern_hip_abduction
        MovementPattern.CHEST_FLY -> Res.string.movementpattern_chest_fly
        MovementPattern.SHOULDER_ISOLATION -> Res.string.movementpattern_shoulder_isolation
        MovementPattern.SHRUG -> Res.string.movementpattern_shrug
        MovementPattern.ELBOW_FLEXION -> Res.string.movementpattern_elbow_flexion
        MovementPattern.ELBOW_EXTENSION -> Res.string.movementpattern_elbow_extension
        MovementPattern.KNEE_EXTENSION -> Res.string.movementpattern_knee_extension
        MovementPattern.KNEE_FLEXION -> Res.string.movementpattern_knee_flexion
        MovementPattern.CALF_RAISE -> Res.string.movementpattern_calf_raise
        MovementPattern.CORE -> Res.string.movementpattern_core
        MovementPattern.CARRY -> Res.string.movementpattern_carry
        MovementPattern.CARDIO -> Res.string.movementpattern_cardio
        MovementPattern.OTHER -> Res.string.movementpattern_other
        MovementPattern.ROTATION -> Res.string.movementpattern_rotation
        MovementPattern.ANTI_ROTATION -> Res.string.movementpattern_anti_rotation
        MovementPattern.JUMP -> Res.string.movementpattern_jump
        MovementPattern.OLYMPIC_LIFT -> Res.string.movementpattern_olympic_lift
        MovementPattern.LOCOMOTION -> Res.string.movementpattern_locomotion
        MovementPattern.INVERSION -> Res.string.movementpattern_inversion
        MovementPattern.POSE -> Res.string.movementpattern_pose
        MovementPattern.BREATHING -> Res.string.movementpattern_breathing
    },
)

@Composable
fun TrackingType.editorLabel(): String = stringResource(
    when (this) {
        TrackingType.WEIGHT_REPS -> Res.string.trackingtype_weight_reps
        TrackingType.REPS -> Res.string.trackingtype_reps
        TrackingType.WEIGHTED_BODYWEIGHT -> Res.string.trackingtype_weighted_bodyweight
        TrackingType.ASSISTED_BODYWEIGHT -> Res.string.trackingtype_assisted_bodyweight
        TrackingType.DURATION -> Res.string.trackingtype_duration
        TrackingType.DISTANCE_DURATION -> Res.string.trackingtype_distance_duration
        TrackingType.WEIGHT_DISTANCE -> Res.string.trackingtype_weight_distance
        TrackingType.CALORIES -> Res.string.trackingtype_calories
        TrackingType.DISTANCE -> Res.string.trackingtype_distance
        TrackingType.HEIGHT_REPS -> Res.string.trackingtype_height_reps
        TrackingType.BREATHS -> Res.string.trackingtype_breaths
        TrackingType.WATTS_DURATION -> Res.string.trackingtype_watts_duration
    },
)
