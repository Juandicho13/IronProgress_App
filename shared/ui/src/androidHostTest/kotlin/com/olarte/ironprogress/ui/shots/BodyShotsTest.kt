package com.olarte.ironprogress.ui.shots

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.body.BodyEntryId
import com.olarte.ironprogress.domain.body.BodyMeasure
import com.olarte.ironprogress.domain.body.BodyMeasurement
import com.olarte.ironprogress.domain.body.BodyTrends
import com.olarte.ironprogress.domain.body.BodyWeight
import com.olarte.ironprogress.domain.body.ProgressPhoto
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.FavouriteExercise
import com.olarte.ironprogress.domain.progress.Period
import com.olarte.ironprogress.domain.progress.PeriodKind
import com.olarte.ironprogress.domain.progress.PeriodStats
import com.olarte.ironprogress.domain.progress.PeriodSummary
import com.olarte.ironprogress.domain.progress.RecordHit
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.body.BodyState
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.progress.PeriodSummaryScreen
import com.olarte.ironprogress.ui.progress.PeriodUiState
import com.olarte.ironprogress.ui.progress.ProgressScreen
import com.olarte.ironprogress.ui.progress.ProgressState
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.TestSessions
import com.olarte.ironprogress.ui.workout.WorkoutField
import com.olarte.ironprogress.ui.workout.WorkoutIntent
import com.olarte.ironprogress.ui.workout.WorkoutScreen
import com.olarte.ironprogress.ui.workout.newWorkoutViewModel
import com.olarte.ironprogress.ui.workout.testBench
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Capturas de F2-09, F2-10 y la franja del récord ya sin tapar la barra del descanso. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class BodyShotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir: String? = System.getProperty("shots.dir")
    private val dark = mutableStateOf(true)
    private val screen = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val store = ViewModelStore()
    private val now = Instant.parse("2026-11-12T18:00:00Z")

    private fun shoot(name: String, content: @Composable () -> Unit) {
        assumeTrue(!dir.isNullOrBlank())
        if (screen.value == null) {
            compose.setContent {
                val current = screen.value ?: return@setContent
                ForjaTheme(darkTheme = dark.value, reduceMotion = true) { current() }
            }
        }
        compose.mainClock.autoAdvance = false
        listOf(true, false).forEach { isDark ->
            dark.value = isDark
            screen.value = content
            repeat(5) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeBy(1_000)
            }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(dir!!).mkdirs()
            File(dir, "$name-${if (isDark) "oscuro" else "claro"}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun idle() = repeat(5) { shadowOf(Looper.getMainLooper()).idle() }

    private fun bodyState(hidden: Boolean): BodyState {
        val weights = listOf(84.2 to 70, 83.6 to 56, 82.9 to 42, 82.1 to 28, 81.4 to 14, 80.8 to 7, 80.5 to 1).mapIndexed {
                i,
                (kg, daysAgo),
            ->
            BodyWeight(BodyEntryId("w$i"), now - daysAgo.days, kg)
        }
        val measures = listOf(
            BodyMeasurement(BodyEntryId("m1"), now - 30.days, BodyMeasure.WAIST, 88.0),
            BodyMeasurement(BodyEntryId("m2"), now - 2.days, BodyMeasure.WAIST, 85.5),
            BodyMeasurement(BodyEntryId("m3"), now - 2.days, BodyMeasure.ARM, 37.5),
            BodyMeasurement(BodyEntryId("m4"), now - 2.days, BodyMeasure.CUSTOM, 38.0, "Pantorrilla"),
        )
        val photos = (0 until 4).map { ProgressPhoto(BodyEntryId("p$it"), now - (60 - it * 20).days, "p$it.jpg") }
        return BodyState(
            loading = false,
            weights = weights,
            weightChangeKg = BodyTrends.weightChange(weights, now),
            measures = BodyTrends.latestMeasures(measures),
            photos = photos,
            hidden = hidden,
        )
    }

    @Composable
    private fun body(state: BodyState) {
        ProgressScreen(
            ProgressState(loading = false),
            rememberLazyListState(),
            CatalogState(),
            "es",
            {},
            rememberLazyListState(),
            {},
            initialSegment = 2,
            bodyState = state,
            withPhotoPicker = false,
        )
    }

    @Test
    fun bodyWithData() = shoot("cuerpo-datos") { body(bodyState(hidden = false)) }

    @Test
    fun bodyHidden() = shoot("cuerpo-oculto") { body(bodyState(hidden = true)) }

    @Test
    fun photoCompare() = shoot("cuerpo-comparar") {
        com.olarte.ironprogress.ui.body.PhotoCompareScreen(bodyState(hidden = false).photos, store = null, onBack = {})
    }

    @Test
    fun bodyEmpty() = shoot("cuerpo-vacio") { body(BodyState(loading = false)) }

    @Test
    fun monthSummary() {
        val hit = RecordHit(testBench.id, RecordKind.HEAVIEST_WEIGHT, 70.0, 67.5, 70.0, 6, WorkoutSetId("x"), SessionId("s"), now - 2.days)
        val oneRm = hit.copy(kind = RecordKind.ESTIMATED_ONE_REP_MAX, value = 84.0, previous = 81.0)
        val summary = PeriodSummary(
            period = Period(PeriodKind.MONTH, 2026, 11),
            stats = PeriodStats(sessions = 9, totalSeconds = 9 * 3_600L + 25 * 60, volumeKg = 48_620.0, trainedDays = 9),
            previous = PeriodStats(sessions = 7, totalSeconds = 7 * 3_600L, volumeKg = 41_100.0, trainedDays = 7),
            records = listOf(oneRm, hit),
            topMuscles = listOf(Muscle.CHEST to 31.0, Muscle.QUADS to 24.0, Muscle.LATS to 19.5),
            favourite = FavouriteExercise(testBench.id, 27),
            bestStreakWeeks = 2,
        )
        val state = PeriodUiState(
            loading = false,
            period = summary.period,
            summary = summary,
            exercises = mapOf(testBench.id to testBench),
            hasAnySession = true,
            canGoBack = true,
            canGoForward = false,
        )
        shoot("resumen-mes") { PeriodSummaryScreen(state, onBack = {}) }
        shoot("progreso-tarjeta-mes") {
            ProgressScreen(
                ProgressState(loading = false, hasSessions = true, sets = listOf(Muscle.CHEST to 12.0, Muscle.QUADS to 9.0)),
                rememberLazyListState(),
                CatalogState(),
                "es",
                {},
                rememberLazyListState(),
                {},
                periodState = state,
            )
        }
    }

    @Test
    fun restWithRecordBanner() {
        val model = newWorkoutViewModel(TestSessions()).also { store.put("w", it) }
        idle()
        model.onIntent(WorkoutIntent.StartFree)
        idle()
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        idle()
        val first = model.state.value.session!!.sets.first().id
        model.onIntent(WorkoutIntent.OpenKeyboard(first, WorkoutField.WEIGHT))
        "85".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
        model.onIntent(WorkoutIntent.NextField)
        model.onIntent(WorkoutIntent.Key("5"))
        idle()
        model.onIntent(WorkoutIntent.CloseKeyboard)
        model.onIntent(WorkoutIntent.Complete(first))
        idle()
        val hit = RecordHit(testBench.id, RecordKind.ESTIMATED_ONE_REP_MAX, 99.2, 95.0, 85.0, 5, first, SessionId("s"), now)
        shoot("descanso-record") {
            val state by model.state.collectAsState()
            WorkoutScreen(state.copy(record = hit), 754, model::onIntent, onMinimize = {})
        }
        shoot("entreno-record") {
            val state by model.state.collectAsState()
            WorkoutScreen(state.copy(record = hit, restExpanded = false), 754, model::onIntent, onMinimize = {})
        }
        store.clear()
    }
}
