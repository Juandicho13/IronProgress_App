package com.olarte.ironprogress.ui.shots

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.progress.ExerciseHistoryEntry
import com.olarte.ironprogress.domain.progress.ExerciseProgress
import com.olarte.ironprogress.domain.progress.RecordHit
import com.olarte.ironprogress.domain.progress.RecordKind
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.BlockResult
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.time.stateAt
import com.olarte.ironprogress.domain.wod.WodMovement
import com.olarte.ironprogress.domain.wod.WodScore
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.progress.ExerciseProgressScreen
import com.olarte.ironprogress.ui.progress.ExerciseProgressState
import com.olarte.ironprogress.ui.progress.ProgressScreen
import com.olarte.ironprogress.ui.progress.ProgressState
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.wod.WodPhase
import com.olarte.ironprogress.ui.wod.WodScreen
import com.olarte.ironprogress.ui.wod.WodState
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

/** Capturas de récords y progreso de F2 (aviso, gráfica, series por músculo y bloque funcional). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class RecordShotsTest {
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

    @Test
    fun recordBanner() {
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
        shoot("record-aviso") {
            val state by model.state.collectAsState()
            WorkoutScreen(state.copy(record = hit), 754, model::onIntent, onMinimize = {})
        }
        store.clear()
    }

    private fun history(): List<ExerciseHistoryEntry> {
        var n = 0
        val sessions = listOf(
            70 to listOf(60.0 to 8, 60.0 to 8, 60.0 to 7),
            63 to listOf(60.0 to 9, 60.0 to 8, 60.0 to 8),
            56 to listOf(62.5 to 8, 62.5 to 7, 60.0 to 9),
            49 to listOf(62.5 to 8, 62.5 to 8, 62.5 to 7),
            42 to listOf(62.5 to 9, 62.5 to 9, 62.5 to 8),
            35 to listOf(65.0 to 6, 62.5 to 9, 62.5 to 8),
            28 to listOf(65.0 to 7, 65.0 to 6, 62.5 to 9),
            21 to listOf(65.0 to 8, 65.0 to 7, 65.0 to 6),
            14 to listOf(67.5 to 6, 65.0 to 8, 65.0 to 7),
            7 to listOf(67.5 to 7, 67.5 to 6, 65.0 to 8),
            2 to listOf(70.0 to 6, 67.5 to 7, 67.5 to 6),
        )
        return sessions.flatMap { (daysAgo, sets) ->
            val at = now - daysAgo.days
            sets.map { (kg, reps) ->
                n++
                ExerciseHistoryEntry(
                    SessionId("s$daysAgo"),
                    at,
                    WorkoutSet(WorkoutSetId("x$n"), SessionExerciseId("e$daysAgo"), n, weightKg = kg, reps = reps, completedAt = at),
                )
            }
        }
    }

    @Test
    fun exerciseProgress() {
        val state = ExerciseProgressState(
            loading = false,
            exercise = testBench,
            progress = ExerciseProgress.of(history()),
            nowEpochMs = now.toEpochMilliseconds(),
        )
        shoot("ejercicio-progreso") { ExerciseProgressScreen(state, "es", {}, {}, {}) }
    }

    @Test
    fun muscleSetsTrafficLight() {
        val state = ProgressState(
            loading = false,
            hasSessions = true,
            sets = listOf(
                Muscle.CHEST to 22.5,
                Muscle.QUADS to 16.0,
                Muscle.LATS to 12.0,
                Muscle.TRICEPS to 8.6,
                Muscle.SHOULDERS to 5.5,
                Muscle.BICEPS to 3.0,
            ),
            fromEpochMs = (now - 6.days).toEpochMilliseconds(),
            toEpochMs = now.toEpochMilliseconds(),
        )
        shoot("progreso-musculos-semaforo") {
            ProgressScreen(
                state,
                androidx.compose.foundation.lazy.rememberLazyListState(),
                CatalogState(),
                "es",
                {},
                androidx.compose.foundation.lazy.rememberLazyListState(),
                {},
            )
        }
    }

    @Test
    fun wodRecord() {
        val movements = listOf(WodMovement(testBench.id, 10), WodMovement(testBench.id, 15))
        val wod = com.olarte.ironprogress.domain.wod.Wod("Cindy", BlockFormat.AMRAP, 12, movements)
        val timer = wod.toPlan().stateAt(12 * 60_000L + 500, 0)
        val state = WodState(
            phase = WodPhase.FINISHED,
            name = "Cindy",
            format = BlockFormat.AMRAP,
            minutes = 12,
            movements = movements,
            exercises = listOf(testBench),
            timer = timer,
            score = WodScore(9, 1),
            best = BlockResult.RoundsAndReps(8, 15),
        )
        shoot("wod-record") { WodScreen(state, "es", {}, {}) }
    }
}
