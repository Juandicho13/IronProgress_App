package com.olarte.ironprogress.ui.body

import com.olarte.ironprogress.domain.body.AddProgressPhoto
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyMeasurement
import com.olarte.ironprogress.domain.body.BodyRepository
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.LogBodyMeasurement
import com.olarte.ironprogress.domain.body.LogBodyWeight
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.domain.catalog.WeightUnit
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.settings.SettingsRepository
import com.olarte.ironprogress.domain.settings.TrainingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModelTest {
    private class MemoryBody : BodyRepository {
        val weights = MutableStateFlow<List<BodyWeight>>(emptyList())
        val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())
        val photos = MutableStateFlow<List<ProgressPhoto>>(emptyList())
        override fun observeWeights(): Flow<List<BodyWeight>> = weights
        override suspend fun saveWeight(weight: BodyWeight) = weights.update { it + weight }
        override suspend fun deleteWeight(id: BodyEntryId) = weights.update { list -> list.filterNot { it.id == id } }
        override fun observeMeasurements(): Flow<List<BodyMeasurement>> = measurements
        override suspend fun saveMeasurement(measurement: BodyMeasurement) = measurements.update { it + measurement }
        override suspend fun deleteMeasurement(id: BodyEntryId) = measurements.update { list -> list.filterNot { it.id == id } }
        override fun observePhotos(): Flow<List<ProgressPhoto>> = photos
        override suspend fun savePhoto(photo: ProgressPhoto) = photos.update { it + photo }
        override suspend fun deletePhoto(id: BodyEntryId) = photos.update { list -> list.filterNot { it.id == id } }
    }

    private class MemorySettings(initial: AppSettings) : SettingsRepository {
        val flow = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = flow
        override suspend fun update(transform: (AppSettings) -> AppSettings) = flow.update(transform)
    }

    private val dispatcher = StandardTestDispatcher()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_760_000_000_000)
    }
    private var next = 0
    private val ids = IdGenerator { "id${next++}" }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun model(body: BodyRepository, settings: SettingsRepository) = BodyViewModel(
        body,
        settings,
        LogBodyWeight(body, clock, ids),
        LogBodyMeasurement(body, clock, ids),
        AddProgressPhoto(body, clock, ids),
        clock,
    )

    @Test
    fun poundsAndInchesAreConvertedBeforeSaving() = runTest(dispatcher) {
        val body = MemoryBody()
        val profile = TrainingProfile(unit = com.olarte.ironprogress.domain.settings.WeightUnit.LB)
        val model = model(body, MemorySettings(AppSettings(profile = profile)))
        backgroundScope.launch { model.state.collect {} }
        runCurrent()
        assertEquals(WeightUnit.LB, model.state.value.unit)

        var saved = false
        model.logWeight(176.4) { saved = it }
        model.logMeasure(BodyMeasure.WAIST, 33.5, null)
        runCurrent()

        assertTrue(saved)
        assertEquals(80.01, body.weights.value.single().weightKg)
        assertEquals(85.1, body.measurements.value.single().valueCm)
        assertEquals(1, model.state.value.weights.size)
    }

    @Test
    fun hiddenAndYoungModeComeFromSettings() = runTest(dispatcher) {
        val settings = MemorySettings(AppSettings(hideBodyMetrics = true, youngMode = true))
        val model = model(MemoryBody(), settings)
        backgroundScope.launch { model.state.collect {} }
        runCurrent()

        assertTrue(model.state.value.hidden)
        assertFalse(model.state.value.photosAllowed)

        settings.flow.update { it.copy(hideBodyMetrics = false, youngMode = false) }
        runCurrent()
        assertFalse(model.state.value.hidden)
        assertTrue(model.state.value.photosAllowed)
    }

    @Test
    fun anOutOfRangeWeightIsReportedAndNotSaved() = runTest(dispatcher) {
        val body = MemoryBody()
        val model = model(body, MemorySettings(AppSettings()))
        backgroundScope.launch { model.state.collect {} }
        runCurrent()

        var saved = true
        model.logWeight(900.0) { saved = it }
        runCurrent()

        assertFalse(saved)
        assertTrue(body.weights.value.isEmpty())
    }

    @Test
    fun photosAreAddedAndDeleted() = runTest(dispatcher) {
        val body = MemoryBody()
        val model = model(body, MemorySettings(AppSettings()))
        backgroundScope.launch { model.state.collect {} }
        model.addPhoto("a.jpg")
        runCurrent()
        val photo = model.state.value.photos.single()

        model.deletePhoto(photo.id)
        runCurrent()

        assertTrue(model.state.value.photos.isEmpty())
    }
}
