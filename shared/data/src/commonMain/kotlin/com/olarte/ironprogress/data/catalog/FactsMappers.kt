package com.olarte.ironprogress.data.catalog

import com.olarte.ironprogress.data.local.ExerciseDemandEntity
import com.olarte.ironprogress.data.local.ExerciseProgressionEntity
import com.olarte.ironprogress.data.local.ExerciseSourceEntity
import com.olarte.ironprogress.data.local.RoomJson
import com.olarte.ironprogress.domain.catalog.ExerciseDemand
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseProgression
import com.olarte.ironprogress.domain.catalog.ExerciseSource
import com.olarte.ironprogress.domain.catalog.Joint
import com.olarte.ironprogress.domain.catalog.JointImpact

// joint_impact se guarda como JSON {"KNEE":"HIGH",...} con solo las articulaciones medio o alto.

internal fun ExerciseDemand.toEntity(exerciseId: String) = ExerciseDemandEntity(
    exerciseId = exerciseId,
    overall = overall,
    technique = technique,
    spine = spine,
    jointImpact = RoomJson.encodeToString<Map<Joint, JointImpact>>(jointImpact),
    met = met,
)

internal fun ExerciseDemandEntity.toDomain() = ExerciseDemand(
    overall = overall,
    technique = technique,
    spine = spine,
    jointImpact = RoomJson.decodeFromString<Map<Joint, JointImpact>>(jointImpact),
    met = met,
)

internal fun ExerciseProgression.toEntity(exerciseId: String) =
    ExerciseProgressionEntity(exerciseId = exerciseId, easierId = easier?.value, harderId = harder?.value)

internal fun ExerciseProgressionEntity.toDomain() = ExerciseProgression(easierId?.let(::ExerciseId), harderId?.let(::ExerciseId))

internal fun ExerciseSourceEntity.toDomain() = ExerciseSource(source, license, author)
