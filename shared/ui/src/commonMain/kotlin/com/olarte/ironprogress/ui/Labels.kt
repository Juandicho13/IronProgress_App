package com.olarte.ironprogress.ui

import androidx.compose.runtime.Composable
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.equipment_air_bike
import com.olarte.ironprogress.ui.resources.equipment_band
import com.olarte.ironprogress.ui.resources.equipment_barbell
import com.olarte.ironprogress.ui.resources.equipment_bike
import com.olarte.ironprogress.ui.resources.equipment_bodyweight
import com.olarte.ironprogress.ui.resources.equipment_cable
import com.olarte.ironprogress.ui.resources.equipment_dumbbell
import com.olarte.ironprogress.ui.resources.equipment_ez_bar
import com.olarte.ironprogress.ui.resources.equipment_foam_roller
import com.olarte.ironprogress.ui.resources.equipment_hangboard
import com.olarte.ironprogress.ui.resources.equipment_heavy_bag
import com.olarte.ironprogress.ui.resources.equipment_jump_rope
import com.olarte.ironprogress.ui.resources.equipment_kettlebell
import com.olarte.ironprogress.ui.resources.equipment_machine
import com.olarte.ironprogress.ui.resources.equipment_mat
import com.olarte.ironprogress.ui.resources.equipment_medicine_ball
import com.olarte.ironprogress.ui.resources.equipment_other
import com.olarte.ironprogress.ui.resources.equipment_parallel_bars
import com.olarte.ironprogress.ui.resources.equipment_plyo_box
import com.olarte.ironprogress.ui.resources.equipment_pull_up_bar
import com.olarte.ironprogress.ui.resources.equipment_reformer
import com.olarte.ironprogress.ui.resources.equipment_rings
import com.olarte.ironprogress.ui.resources.equipment_rower
import com.olarte.ironprogress.ui.resources.equipment_sandbag
import com.olarte.ironprogress.ui.resources.equipment_ski_erg
import com.olarte.ironprogress.ui.resources.equipment_sled
import com.olarte.ironprogress.ui.resources.equipment_smith_machine
import com.olarte.ironprogress.ui.resources.equipment_suspension_trainer
import com.olarte.ironprogress.ui.resources.equipment_trap_bar
import com.olarte.ironprogress.ui.resources.equipment_treadmill
import com.olarte.ironprogress.ui.resources.equipment_yoga_block
import com.olarte.ironprogress.ui.resources.equipment_yoga_strap
import com.olarte.ironprogress.ui.resources.muscle_abs
import com.olarte.ironprogress.ui.resources.muscle_biceps
import com.olarte.ironprogress.ui.resources.muscle_calves
import com.olarte.ironprogress.ui.resources.muscle_chest
import com.olarte.ironprogress.ui.resources.muscle_forearms
import com.olarte.ironprogress.ui.resources.muscle_glutes
import com.olarte.ironprogress.ui.resources.muscle_hamstrings
import com.olarte.ironprogress.ui.resources.muscle_lats
import com.olarte.ironprogress.ui.resources.muscle_lower_back
import com.olarte.ironprogress.ui.resources.muscle_quads
import com.olarte.ironprogress.ui.resources.muscle_shoulders
import com.olarte.ironprogress.ui.resources.muscle_traps
import com.olarte.ironprogress.ui.resources.muscle_triceps
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Recurso de texto asociado a cada grupo muscular. */
fun Muscle.labelRes(): StringResource = when (this) {
    Muscle.CHEST -> Res.string.muscle_chest
    Muscle.SHOULDERS -> Res.string.muscle_shoulders
    Muscle.BICEPS -> Res.string.muscle_biceps
    Muscle.TRICEPS -> Res.string.muscle_triceps
    Muscle.FOREARMS -> Res.string.muscle_forearms
    Muscle.ABS -> Res.string.muscle_abs
    Muscle.LATS -> Res.string.muscle_lats
    Muscle.TRAPS -> Res.string.muscle_traps
    Muscle.LOWER_BACK -> Res.string.muscle_lower_back
    Muscle.GLUTES -> Res.string.muscle_glutes
    Muscle.QUADS -> Res.string.muscle_quads
    Muscle.HAMSTRINGS -> Res.string.muscle_hamstrings
    Muscle.CALVES -> Res.string.muscle_calves
}

/** Recurso de texto asociado a cada tipo de equipo. */
fun Equipment.labelRes(): StringResource = when (this) {
    Equipment.BARBELL -> Res.string.equipment_barbell
    Equipment.DUMBBELL -> Res.string.equipment_dumbbell
    Equipment.MACHINE -> Res.string.equipment_machine
    Equipment.CABLE -> Res.string.equipment_cable
    Equipment.SMITH_MACHINE -> Res.string.equipment_smith_machine
    Equipment.EZ_BAR -> Res.string.equipment_ez_bar
    Equipment.KETTLEBELL -> Res.string.equipment_kettlebell
    Equipment.BAND -> Res.string.equipment_band
    Equipment.BODYWEIGHT -> Res.string.equipment_bodyweight
    Equipment.OTHER -> Res.string.equipment_other
    Equipment.TRAP_BAR -> Res.string.equipment_trap_bar
    Equipment.MEDICINE_BALL -> Res.string.equipment_medicine_ball
    Equipment.SANDBAG -> Res.string.equipment_sandbag
    Equipment.SLED -> Res.string.equipment_sled
    Equipment.PLYO_BOX -> Res.string.equipment_plyo_box
    Equipment.JUMP_ROPE -> Res.string.equipment_jump_rope
    Equipment.RINGS -> Res.string.equipment_rings
    Equipment.PULL_UP_BAR -> Res.string.equipment_pull_up_bar
    Equipment.PARALLEL_BARS -> Res.string.equipment_parallel_bars
    Equipment.SUSPENSION_TRAINER -> Res.string.equipment_suspension_trainer
    Equipment.FOAM_ROLLER -> Res.string.equipment_foam_roller
    Equipment.MAT -> Res.string.equipment_mat
    Equipment.YOGA_BLOCK -> Res.string.equipment_yoga_block
    Equipment.YOGA_STRAP -> Res.string.equipment_yoga_strap
    Equipment.REFORMER -> Res.string.equipment_reformer
    Equipment.ROWER -> Res.string.equipment_rower
    Equipment.BIKE -> Res.string.equipment_bike
    Equipment.TREADMILL -> Res.string.equipment_treadmill
    Equipment.SKI_ERG -> Res.string.equipment_ski_erg
    Equipment.AIR_BIKE -> Res.string.equipment_air_bike
    Equipment.HEAVY_BAG -> Res.string.equipment_heavy_bag
    Equipment.HANGBOARD -> Res.string.equipment_hangboard
}

@Composable
fun Muscle.label(): String = stringResource(labelRes())

@Composable
fun Equipment.label(): String = stringResource(labelRes())
