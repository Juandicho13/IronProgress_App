package com.olarte.ironprogress.domain.body

import com.olarte.ironprogress.domain.common.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BodyTest {
    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private class MemoryBody : BodyRepository {
        val weights = MutableStateFlow<List<BodyWeight>>(emptyList())
        val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())
        val photos = MutableStateFlow<List<ProgressPhoto>>(emptyList())
        override fun observeWeights(): Flow<List<BodyWeight>> = weights
        override suspend fun saveWeight(weight: BodyWeight) {
            weights.value = weights.value + weight
        }
        override suspend fun deleteWeight(id: BodyEntryId) {
            weights.value = weights.value.filterNot { it.id == id }
        }
        override fun observeMeasurements(): Flow<List<BodyMeasurement>> = measurements
        override suspend fun saveMeasurement(measurement: BodyMeasurement) {
            measurements.value = measurements.value + measurement
        }
        override suspend fun deleteMeasurement(id: BodyEntryId) = Unit
        override fun observePhotos(): Flow<List<ProgressPhoto>> = photos
        override suspend fun savePhoto(photo: ProgressPhoto) {
            photos.value = photos.value + photo
        }
        override suspend fun deletePhoto(id: BodyEntryId) = Unit
    }

    private val start = Instant.fromEpochMilliseconds(1_760_000_000_000)
    private val clock = FakeClock(start)
    private var next = 0
    private val ids = IdGenerator { "b${next++}" }

    @Test
    fun weightIsSavedRoundedAndOutOfRangeIsRejected() = runTest {
        val repository = MemoryBody()
        val log = LogBodyWeight(repository, clock, ids)

        assertTrue(log(78.456))
        assertFalse(log(9.0))
        assertFalse(log(780.0))
        assertFalse(log(Double.NaN))

        assertEquals(listOf(78.46), repository.weights.value.map { it.weightKg })
        assertEquals(start, repository.weights.value.single().measuredAt)
    }

    @Test
    fun customMeasureNeedsANameAndFixedOnesIgnoreIt() = runTest {
        val repository = MemoryBody()
        val log = LogBodyMeasurement(repository, clock, ids)

        assertFalse(log(BodyMeasure.CUSTOM, 35.0, "  "))
        assertTrue(log(BodyMeasure.CUSTOM, 35.04, " Pantorrilla "))
        assertTrue(log(BodyMeasure.WAIST, 82.0, "ignorado"))
        assertFalse(log(BodyMeasure.ARM, 5.0))

        val saved = repository.measurements.value
        assertEquals("Pantorrilla", saved[0].customName)
        assertEquals(35.0, saved[0].valueCm)
        assertNull(saved[1].customName)
    }

    @Test
    fun weightChangeComparesWithTheEntryFrom30DaysAgo() {
        fun w(id: String, daysAgo: Int, kg: Double) = BodyWeight(BodyEntryId(id), start - daysAgo.days, kg)
        val weights = listOf(w("a", 60, 82.0), w("b", 35, 80.0), w("c", 10, 79.0), w("d", 0, 78.4))

        assertEquals(-1.6, BodyTrends.weightChange(weights, start))
        // Sin un registro tan viejo, compara con el primero.
        assertEquals(-0.6, BodyTrends.weightChange(weights.drop(2), start))
        assertNull(BodyTrends.weightChange(weights.take(1), start))
    }

    @Test
    fun latestMeasuresKeepOneRowPerKindWithItsChange() {
        fun m(id: String, daysAgo: Int, kind: BodyMeasure, cm: Double, name: String? = null) =
            BodyMeasurement(BodyEntryId(id), start - daysAgo.days, kind, cm, name)
        val rows = BodyTrends.latestMeasures(
            listOf(
                m("1", 20, BodyMeasure.WAIST, 84.0),
                m("2", 1, BodyMeasure.WAIST, 82.5),
                m("3", 5, BodyMeasure.ARM, 36.0),
                m("4", 3, BodyMeasure.CUSTOM, 38.0, "Pantorrilla"),
            ),
        )

        assertEquals(listOf(BodyMeasure.WAIST, BodyMeasure.ARM, BodyMeasure.CUSTOM), rows.map { it.key.kind })
        assertEquals(82.5, rows[0].latest.valueCm)
        assertEquals(-1.5, rows[0].changeCm)
        assertNull(rows[1].changeCm)
        assertEquals("Pantorrilla", rows[2].key.customName)
    }

    @Test
    fun aPhotoNeedsAFile() = runTest {
        val repository = MemoryBody()
        val add = AddProgressPhoto(repository, clock, ids)

        assertNull(add(" "))
        assertEquals("p.jpg", add("p.jpg")?.fileName)
        assertEquals(1, repository.photos.value.size)
    }
}
