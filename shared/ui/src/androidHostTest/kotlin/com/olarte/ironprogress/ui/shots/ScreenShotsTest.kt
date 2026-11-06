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
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.routine.BlockId
import com.olarte.ironprogress.domain.routine.Folder
import com.olarte.ironprogress.domain.routine.FolderId
import com.olarte.ironprogress.domain.routine.Routine
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineExercise
import com.olarte.ironprogress.domain.routine.RoutineExerciseId
import com.olarte.ironprogress.domain.routine.RoutineId
import com.olarte.ironprogress.domain.routine.SetTemplate
import com.olarte.ironprogress.domain.routine.SetTemplateId
import com.olarte.ironprogress.domain.routine.WorkoutBlock
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.domain.time.stateAt
import com.olarte.ironprogress.ui.catalog.CatalogState
import com.olarte.ironprogress.ui.plan.PlanScreen
import com.olarte.ironprogress.ui.plan.PlanState
import com.olarte.ironprogress.ui.plan.RoutineEditorScreen
import com.olarte.ironprogress.ui.plan.RoutineEditorState
import com.olarte.ironprogress.ui.plan.SetDraft
import com.olarte.ironprogress.ui.progress.ProgressScreen
import com.olarte.ironprogress.ui.progress.ProgressState
import com.olarte.ironprogress.ui.settings.SettingsScreen
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.TestSessions
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

/**
 * Capturas de las pantallas principales en claro y oscuro para revisarlas a ojo. Solo corre si se
 * pasa -PshotsDir; en el CI se salta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class ScreenShotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir: String? = System.getProperty("shots.dir")
    private val dark = mutableStateOf(true)
    private val screen = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val store = ViewModelStore()

    private fun shoot(name: String, content: @Composable () -> Unit) {
        assumeTrue(!dir.isNullOrBlank())
        if (screen.value == null) {
            compose.setContent {
                val current = screen.value ?: return@setContent
                ForjaTheme(darkTheme = dark.value, reduceMotion = true) { current() }
            }
        }
        listOf(true, false).forEach { isDark ->
            dark.value = isDark
            screen.value = content
            repeat(5) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.waitForIdle()
            }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(dir!!).mkdirs()
            File(dir, "$name-${if (isDark) "oscuro" else "claro"}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun plan() {
        val folder = Folder(FolderId("f"), "Gimnasio", 0)
        val routines = listOf(
            Routine(RoutineId("a"), folder.id, "Empuje A", sortOrder = 0),
            Routine(RoutineId("b"), folder.id, "Tirón A", sortOrder = 1),
            Routine(RoutineId("c"), null, "Pierna libre", sortOrder = 2),
        )
        shoot("plan") {
            PlanScreen(
                PlanState(routines, listOf(folder), routines.associate { it.id to 5 }, loading = false),
                {},
                {},
            )
        }
    }

    @Test
    fun planEmpty() {
        shoot("plan-vacio") { PlanScreen(PlanState(loading = false), {}, {}) }
    }

    @Test
    fun routineEditor() {
        val block = WorkoutBlock(BlockId("b1"), 0, BlockFormat.SETS)
        val exercise = RoutineExercise(RoutineExerciseId("e1"), block.id, testBench.id, 0)
        val sets = (0..2).map { SetTemplate(SetTemplateId("s$it"), exercise.id, it, repsMin = 8, repsMax = 12, targetRir = 2) }
        val detail = RoutineDetail(Routine(RoutineId("a"), null, "Empuje A"), listOf(block), listOf(exercise), sets)
        shoot("editar-rutina") {
            RoutineEditorScreen(
                RoutineEditorState(
                    detail = detail,
                    name = "Empuje A",
                    notes = "Pecho y tríceps",
                    sets = sets.associate { it.id to SetDraft("8", "12", "2") },
                    catalog = listOf(testBench),
                    loading = false,
                ),
                {},
            )
        }
    }

    @Test
    fun settings() {
        shoot("ajustes") { SettingsScreen(AppSettings(), {}, {}) }
    }

    @Test
    fun progressEmpty() {
        shoot("progreso-vacio") {
            ProgressScreen(ProgressState(loading = false), rememberLazyListState(), CatalogState(), "es", {}, rememberLazyListState(), {})
        }
    }

    @Test
    fun progressWithSets() {
        val state = ProgressState(
            loading = false,
            hasSessions = true,
            sets = listOf(Muscle.CHEST to 12.0, Muscle.TRICEPS to 6.0, Muscle.SHOULDERS to 4.5),
            fromEpochMs = 1_793_750_400_000,
            toEpochMs = 1_794_268_800_000,
        )
        shoot("progreso") { ProgressScreen(state, rememberLazyListState(), CatalogState(), "es", {}, rememberLazyListState(), {}) }
    }

    @Test
    fun freeWorkout() {
        val model = newWorkoutViewModel(TestSessions()).also { store.put("m", it) }
        shoot("entreno-libre") {
            val state by model.state.collectAsState()
            if (!state.loading && state.session == null) model.onIntent(WorkoutIntent.StartFree)
            WorkoutScreen(state, 0, model::onIntent, onMinimize = {})
        }
        store.clear()
    }

    private fun idle() {
        repeat(5) { shadowOf(Looper.getMainLooper()).idle() }
    }

    /** Sesión libre con Press: 3 series, la primera hecha. */
    private fun workoutWithOneSetDone(
        mode: com.olarte.ironprogress.domain.routine.ExerciseMode?,
    ): com.olarte.ironprogress.ui.workout.WorkoutViewModel {
        val model = newWorkoutViewModel(TestSessions()).also { store.put("w${store.keys().size}", it) }
        idle()
        model.onIntent(WorkoutIntent.StartFree)
        idle()
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        idle()
        val exercise = model.state.value.session!!.exercises.single().id
        if (mode != null) model.onIntent(WorkoutIntent.SetMode(exercise, mode))
        model.onIntent(WorkoutIntent.AddSet(exercise))
        model.onIntent(WorkoutIntent.AddSet(exercise))
        idle()
        val first = model.state.value.session!!.setsOf(exercise).first().id
        model.onIntent(WorkoutIntent.OpenKeyboard(first, com.olarte.ironprogress.ui.workout.WorkoutField.WEIGHT))
        "60".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
        model.onIntent(WorkoutIntent.NextField)
        model.onIntent(WorkoutIntent.Key("8"))
        idle()
        model.onIntent(WorkoutIntent.CloseKeyboard)
        model.onIntent(WorkoutIntent.Complete(first))
        idle()
        return model
    }

    @Test
    fun workoutOneSetAtATime() {
        val model = workoutWithOneSetDone(com.olarte.ironprogress.domain.routine.ExerciseMode.NO_REST)
        shoot("entreno-serie") {
            val state by model.state.collectAsState()
            WorkoutScreen(state.copy(undoSetId = null), 754, model::onIntent, onMinimize = {})
        }
        store.clear()
    }

    @Test
    fun rest() {
        val model = workoutWithOneSetDone(null)
        shoot("descanso") {
            val state by model.state.collectAsState()
            WorkoutScreen(state, 754, model::onIntent, onMinimize = {})
        }
        store.clear()
    }

    @Test
    fun breathing() {
        shoot("respiracion") {
            com.olarte.ironprogress.ui.discipline.BreathingScreen(
                com.olarte.ironprogress.ui.discipline.GuidedRunState(),
                com.olarte.ironprogress.ui.discipline.BreathingChoice(),
                {},
                {},
                {},
                {},
            )
        }
    }

    @Test
    fun breathingRunning() {
        val plan = com.olarte.ironprogress.domain.discipline.BreathingPattern.BOX.toPlan(5)
        val timer = plan.stateAt(6_000, 0)
        shoot("respiracion-corriendo") {
            com.olarte.ironprogress.ui.discipline.BreathingScreen(
                com.olarte.ironprogress.ui.discipline.GuidedRunState(started = true, timer = timer),
                com.olarte.ironprogress.ui.discipline.BreathingChoice(),
                {},
                {},
                {},
                {},
            )
        }
    }

    @Test
    fun newInterval() {
        val template = com.olarte.ironprogress.domain.interval.IntervalTemplate(
            com.olarte.ironprogress.domain.interval.IntervalTemplateId("t"),
            "Mis intervalos",
            40,
            20,
            10,
        )
        shoot("nuevo-intervalo") {
            com.olarte.ironprogress.ui.interval.IntervalEditorScreen(
                com.olarte.ironprogress.ui.interval.IntervalEditorState(template),
                "es",
                false,
                {},
                {},
            )
        }
    }

    @Test
    fun planWithWeek() {
        val routines = listOf(
            Routine(RoutineId("a"), null, "Pierna", sortOrder = 0),
            Routine(RoutineId("b"), null, "Empuje", sortOrder = 1),
        )
        val week = com.olarte.ironprogress.domain.routine.WeekProgress(
            (0..6).map { com.olarte.ironprogress.domain.routine.WeekDayStatus(it, it == 0 || it == 2, it == 3) },
            2,
            1,
        )
        shoot("plan-semana") {
            PlanScreen(
                PlanState(
                    routines,
                    emptyList(),
                    routines.associate {
                        it.id to 4
                    },
                    loading = false,
                    week = week,
                    nextRoutineId = RoutineId("b"),
                ),
                {},
                {},
            )
        }
    }

    @Test
    fun summary() {
        val sessions = TestSessions()
        val model = newWorkoutViewModel(sessions).also { store.put("s", it) }
        idle()
        model.onIntent(WorkoutIntent.StartFree)
        idle()
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        idle()
        val id = model.state.value.session!!.sets.single().id
        model.onIntent(WorkoutIntent.OpenKeyboard(id, com.olarte.ironprogress.ui.workout.WorkoutField.WEIGHT))
        "60".forEach { model.onIntent(WorkoutIntent.Key(it.toString())) }
        model.onIntent(WorkoutIntent.NextField)
        model.onIntent(WorkoutIntent.Key("8"))
        idle()
        model.onIntent(WorkoutIntent.Complete(id))
        idle()
        val detail = sessions.data.value.single()
        val ended = detail.copy(
            session = detail.session.copy(
                status = com.olarte.ironprogress.domain.session.SessionStatus.FINISHED,
                endedAt = detail.session.startedAt + kotlin.time.Duration.parse("18m"),
            ),
        )
        val summary = com.olarte.ironprogress.domain.session.summarize(ended, listOf(ended))
        shoot("resumen") {
            com.olarte.ironprogress.ui.summary.SummaryScreen(
                com.olarte.ironprogress.ui.summary.SummaryState(
                    loading = false,
                    summary = summary,
                    exercises = mapOf(
                        testBench.id to testBench,
                    ),
                ),
                "es",
                {},
                {},
            )
        }
        store.clear()
    }
}
