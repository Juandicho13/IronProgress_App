package com.olarte.ironprogress.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import com.olarte.ironprogress.domain.catalog.Equipment
import com.olarte.ironprogress.domain.catalog.Exercise
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseMuscle
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.catalog.MovementPattern
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.catalog.MuscleRole
import com.olarte.ironprogress.domain.catalog.TrackingType
import com.olarte.ironprogress.domain.catalog.WeightUnit

/**
 * Tabla de ejercicios del catalogo (tanto semilla como personalizados por el usuario).
 * Todo peso se guarda internamente en kg; preferredUnit es nullable y solo cambia como se muestra.
 */
@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["is_custom"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name_es")
    val nameEs: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "tracking_type")
    val trackingType: String,
    @ColumnInfo(name = "equipment")
    val equipment: String,
    @ColumnInfo(name = "movement_pattern")
    val movementPattern: String,
    @ColumnInfo(name = "is_unilateral")
    val isUnilateral: Boolean,
    @ColumnInfo(name = "preferred_unit")
    val preferredUnit: String?,
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
)

/**
 * Relacion entre ejercicio y musculo con su rol (PRIMARY = 1.0, SECONDARY = 0.5).
 */
@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exercise_id", "muscle_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exercise_id"]),
        Index(value = ["muscle_id"]),
    ],
)
data class ExerciseMuscleEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "muscle_id")
    val muscleId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

/**
 * Guarda metadatos locales como la version instalada del SeedCatalog.
 */
@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "int_value")
    val intValue: Int,
) {
    companion object {
        const val KEY_SEED_CATALOG_VERSION = "seed_catalog_version"
    }
}

/**
 * Proyeccion relacional de Room para leer un ejercicio junto con todos sus musculos.
 */
data class ExerciseWithMusclesRelation(
    @Embedded
    val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exercise_id",
    )
    val muscles: List<ExerciseMuscleEntity>,
)

/**
 * Convierte la proyeccion de Room al modelo de dominio [Exercise].
 */
internal fun ExerciseWithMusclesRelation.toDomain(): Exercise =
    Exercise(
        id = ExerciseId(exercise.id),
        name = LocalizedText(
            es = exercise.nameEs,
            en = exercise.nameEn,
        ),
        muscles = muscles
            .sortedBy { it.sortOrder }
            .map { muscleEntity ->
                ExerciseMuscle(
                    muscle = Muscle.valueOf(muscleEntity.muscleId),
                    role = MuscleRole.valueOf(muscleEntity.role),
                )
            },
        trackingType = TrackingType.valueOf(exercise.trackingType),
        equipment = Equipment.valueOf(exercise.equipment),
        pattern = MovementPattern.valueOf(exercise.movementPattern),
        unilateral = exercise.isUnilateral,
        preferredUnit = exercise.preferredUnit?.let { WeightUnit.valueOf(it) },
        isCustom = exercise.isCustom,
        isArchived = exercise.isArchived,
    )

/**
 * Convierte un [Exercise] de dominio a su fila principal y sus filas de musculos.
 */
internal fun Exercise.toEntities(): Pair<ExerciseEntity, List<ExerciseMuscleEntity>> {
    val exerciseEntity = ExerciseEntity(
        id = id.value,
        nameEs = name.es,
        nameEn = name.en,
        trackingType = trackingType.name,
        equipment = equipment.name,
        movementPattern = pattern.name,
        isUnilateral = unilateral,
        preferredUnit = preferredUnit?.name,
        isCustom = isCustom,
        isArchived = isArchived,
    )
    val muscleEntities = muscles.mapIndexed { index, item ->
        ExerciseMuscleEntity(
            exerciseId = id.value,
            muscleId = item.muscle.name,
            role = item.role.name,
            sortOrder = index,
        )
    }
    return exerciseEntity to muscleEntities
}