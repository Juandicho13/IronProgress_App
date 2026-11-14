package com.olarte.ironprogress.data.body

import com.olarte.ironprogress.data.local.BodyDao
import com.olarte.ironprogress.data.local.BodyMeasurementEntity
import com.olarte.ironprogress.data.local.BodyWeightEntity
import com.olarte.ironprogress.data.local.ProgressPhotoEntity
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyMeasurement
import com.olarte.ironprogress.domain.body.BodyRepository
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.ProgressPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

/** Peso, medidas y fotos de progreso en Room (F2-09). */
class RoomBodyRepository(private val dao: BodyDao, private val clock: Clock) : BodyRepository {

    override fun observeWeights(): Flow<List<BodyWeight>> = dao.observeWeights().map { rows ->
        rows.map { BodyWeight(BodyEntryId(it.id), Instant.fromEpochMilliseconds(it.measuredAt), it.weightKg) }
    }

    override suspend fun saveWeight(weight: BodyWeight) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsertWeight(
            BodyWeightEntity(
                id = weight.id.value,
                measuredAt = weight.measuredAt.toEpochMilliseconds(),
                weightKg = weight.weightKg,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteWeight(id: BodyEntryId) = dao.markWeightDeleted(id.value, clock.now().toEpochMilliseconds())

    override fun observeMeasurements(): Flow<List<BodyMeasurement>> = dao.observeMeasurements().map { rows ->
        rows.mapNotNull { row ->
            val kind = BodyMeasure.entries.firstOrNull { it.name == row.kind } ?: return@mapNotNull null
            BodyMeasurement(BodyEntryId(row.id), Instant.fromEpochMilliseconds(row.measuredAt), kind, row.valueCm, row.customName)
        }
    }

    override suspend fun saveMeasurement(measurement: BodyMeasurement) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsertMeasurement(
            BodyMeasurementEntity(
                id = measurement.id.value,
                measuredAt = measurement.measuredAt.toEpochMilliseconds(),
                kind = measurement.kind.name,
                valueCm = measurement.valueCm,
                customName = measurement.customName,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteMeasurement(id: BodyEntryId) = dao.markMeasurementDeleted(id.value, clock.now().toEpochMilliseconds())

    override fun observePhotos(): Flow<List<ProgressPhoto>> = dao.observePhotos().map { rows ->
        rows.map { ProgressPhoto(BodyEntryId(it.id), Instant.fromEpochMilliseconds(it.takenAt), it.fileName) }
    }

    override suspend fun savePhoto(photo: ProgressPhoto) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsertPhoto(
            ProgressPhotoEntity(
                id = photo.id.value,
                takenAt = photo.takenAt.toEpochMilliseconds(),
                fileName = photo.fileName,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deletePhoto(id: BodyEntryId) = dao.markPhotoDeleted(id.value, clock.now().toEpochMilliseconds())
}
