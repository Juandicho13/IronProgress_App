package com.olarte.ironprogress.data.body

import androidx.room3.Room
import com.olarte.ironprogress.data.backup.RoomBackupRepository
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.data.local.IronProgressDatabaseConstructor
import com.olarte.ironprogress.data.local.buildIronProgressDatabase
import com.olarte.ironprogress.domain.backup.RestoreResult
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyMeasurement
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.ProgressPhoto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class RoomBodyRepositoryTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_760_000_000_000)
    }
    private val opened = mutableListOf<IronProgressDatabase>()

    private fun newDatabase(): IronProgressDatabase = Room.inMemoryDatabaseBuilder<IronProgressDatabase>(
        factory = { IronProgressDatabaseConstructor.initialize() },
    ).buildIronProgressDatabase().also { opened += it }

    @AfterTest
    fun close() = opened.forEach { it.close() }

    private val at = Instant.fromEpochMilliseconds(1_759_000_000_000)

    @Test
    fun savesListsInOrderAndDeletesSoftly() = runTest {
        val repository = RoomBodyRepository(newDatabase().bodyDao(), clock)
        repository.saveWeight(BodyWeight(BodyEntryId("w2"), at, 79.0))
        repository.saveWeight(BodyWeight(BodyEntryId("w1"), Instant.fromEpochMilliseconds(1_758_000_000_000), 80.0))
        repository.saveMeasurement(BodyMeasurement(BodyEntryId("m1"), at, BodyMeasure.CUSTOM, 38.0, "Pantorrilla"))
        repository.savePhoto(ProgressPhoto(BodyEntryId("p1"), at, "p1.jpg"))

        assertEquals(listOf(80.0, 79.0), repository.observeWeights().first().map { it.weightKg })
        assertEquals("Pantorrilla", repository.observeMeasurements().first().single().customName)
        assertEquals("p1.jpg", repository.observePhotos().first().single().fileName)

        repository.deleteWeight(BodyEntryId("w1"))
        repository.deletePhoto(BodyEntryId("p1"))
        assertEquals(listOf(79.0), repository.observeWeights().first().map { it.weightKg })
        assertTrue(repository.observePhotos().first().isEmpty())
    }

    @Test
    fun theBackupCarriesWeightAndMeasuresButNotPhotos() = runTest {
        val first = newDatabase().also { RoomExerciseRepository(it.catalogDao()).ensureSeeded() }
        val body = RoomBodyRepository(first.bodyDao(), clock)
        body.saveWeight(BodyWeight(BodyEntryId("w1"), at, 79.5))
        body.saveMeasurement(BodyMeasurement(BodyEntryId("m1"), at, BodyMeasure.WAIST, 82.0))
        body.savePhoto(ProgressPhoto(BodyEntryId("p1"), at, "p1.jpg"))

        val backup = RoomBackupRepository(first, clock).createBackup()
        assertFalse(backup.contains("progress_photos"))

        val second = newDatabase()
        assertIs<RestoreResult.Restored>(RoomBackupRepository(second, clock).restore(backup))
        val restored = RoomBodyRepository(second.bodyDao(), clock)
        assertEquals(listOf(79.5), restored.observeWeights().first().map { it.weightKg })
        assertEquals(listOf(BodyMeasure.WAIST), restored.observeMeasurements().first().map { it.kind })
        assertTrue(restored.observePhotos().first().isEmpty())
    }
}
