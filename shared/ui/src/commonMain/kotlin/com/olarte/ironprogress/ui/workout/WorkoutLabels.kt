package com.olarte.ironprogress.ui.workout

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.SetType
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.workout_block_amrap
import com.olarte.ironprogress.ui.resources.workout_block_breathing
import com.olarte.ironprogress.ui.resources.workout_block_emom
import com.olarte.ironprogress.ui.resources.workout_block_flow
import com.olarte.ironprogress.ui.resources.workout_block_intervals
import com.olarte.ironprogress.ui.resources.workout_block_rounds
import com.olarte.ironprogress.ui.resources.workout_block_time
import com.olarte.ironprogress.ui.resources.workout_breaths
import com.olarte.ironprogress.ui.resources.workout_calories
import com.olarte.ironprogress.ui.resources.workout_cluster
import com.olarte.ironprogress.ui.resources.workout_complete
import com.olarte.ironprogress.ui.resources.workout_distance
import com.olarte.ironprogress.ui.resources.workout_drop
import com.olarte.ironprogress.ui.resources.workout_duration
import com.olarte.ironprogress.ui.resources.workout_failure
import com.olarte.ironprogress.ui.resources.workout_height
import com.olarte.ironprogress.ui.resources.workout_myo
import com.olarte.ironprogress.ui.resources.workout_reps
import com.olarte.ironprogress.ui.resources.workout_rest_pause
import com.olarte.ironprogress.ui.resources.workout_rir
import com.olarte.ironprogress.ui.resources.workout_rpe
import com.olarte.ironprogress.ui.resources.workout_warmup
import com.olarte.ironprogress.ui.resources.workout_watts
import com.olarte.ironprogress.ui.resources.workout_weight
import com.olarte.ironprogress.ui.resources.workout_working
import org.jetbrains.compose.resources.stringResource

@Composable
fun WorkoutField.label(): String = stringResource(
    when (this) {
        WorkoutField.WEIGHT -> Res.string.workout_weight
        WorkoutField.REPS -> Res.string.workout_reps
        WorkoutField.DURATION -> Res.string.workout_duration
        WorkoutField.DISTANCE -> Res.string.workout_distance
        WorkoutField.CALORIES -> Res.string.workout_calories
        WorkoutField.HEIGHT -> Res.string.workout_height
        WorkoutField.BREATHS -> Res.string.workout_breaths
        WorkoutField.WATTS -> Res.string.workout_watts
        WorkoutField.RIR -> Res.string.workout_rir
        WorkoutField.RPE -> Res.string.workout_rpe
    },
)

@Composable
fun SetType.workoutLabel(): String = stringResource(
    when (this) {
        SetType.WARMUP -> Res.string.workout_warmup
        SetType.WORKING -> Res.string.workout_working
        SetType.DROP -> Res.string.workout_drop
        SetType.FAILURE -> Res.string.workout_failure
        SetType.REST_PAUSE -> Res.string.workout_rest_pause
        SetType.MYO_REPS -> Res.string.workout_myo
        SetType.CLUSTER -> Res.string.workout_cluster
    },
)

@Composable
fun BlockFormat.actionLabel(): String = stringResource(
    when (this) {
        BlockFormat.SETS -> Res.string.workout_complete
        BlockFormat.EMOM -> Res.string.workout_block_emom
        BlockFormat.AMRAP -> Res.string.workout_block_amrap
        BlockFormat.FOR_TIME -> Res.string.workout_block_time
        BlockFormat.INTERVALS -> Res.string.workout_block_intervals
        BlockFormat.ROUNDS -> Res.string.workout_block_rounds
        BlockFormat.FLOW -> Res.string.workout_block_flow
        BlockFormat.BREATHING -> Res.string.workout_block_breathing
    },
)
