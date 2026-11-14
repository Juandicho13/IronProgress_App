package com.olarte.ironprogress.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

// Seguimiento del cuerpo (F2-09, versión 5). Llevan user_id y fechas para la sincronización de F3;
// borrar marca deleted_at. Las fotos guardan solo el nombre del archivo privado.

@Entity(tableName = "body_weights", indices = [Index("measured_at")])
data class BodyWeightEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(tableName = "body_measurements", indices = [Index("measured_at")])
data class BodyMeasurementEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "value_cm") val valueCm: Double,
    @ColumnInfo(name = "custom_name") val customName: String?,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(tableName = "progress_photos", indices = [Index("taken_at")])
data class ProgressPhotoEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_weights WHERE deleted_at IS NULL ORDER BY measured_at ASC")
    fun observeWeights(): Flow<List<BodyWeightEntity>>

    @Upsert
    suspend fun upsertWeight(weight: BodyWeightEntity)

    @Query("UPDATE body_weights SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun markWeightDeleted(id: String, now: Long)

    @Query("SELECT * FROM body_measurements WHERE deleted_at IS NULL ORDER BY measured_at ASC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Upsert
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity)

    @Query("UPDATE body_measurements SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun markMeasurementDeleted(id: String, now: Long)

    @Query("SELECT * FROM progress_photos WHERE deleted_at IS NULL ORDER BY taken_at ASC")
    fun observePhotos(): Flow<List<ProgressPhotoEntity>>

    @Upsert
    suspend fun upsertPhoto(photo: ProgressPhotoEntity)

    @Query("UPDATE progress_photos SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun markPhotoDeleted(id: String, now: Long)
}
