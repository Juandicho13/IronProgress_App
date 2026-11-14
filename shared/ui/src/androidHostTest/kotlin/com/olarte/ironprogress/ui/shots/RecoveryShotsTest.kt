package com.olarte.ironprogress.ui.shots

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Muscle
import com.olarte.ironprogress.domain.feedback.FeedbackId
import com.olarte.ironprogress.domain.feedback.Joint
import com.olarte.ironprogress.domain.feedback.PerceivedLoad
import com.olarte.ironprogress.domain.feedback.SessionFeedback
import com.olarte.ironprogress.domain.feedback.SetChange
import com.olarte.ironprogress.domain.progress.MuscleRecovery
import com.olarte.ironprogress.domain.progress.Recovery
import com.olarte.ironprogress.domain.progress.RecoveryLevel
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.SessionStatus
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.session.summarize
import com.olarte.ironprogress.domain.settings.AppSettings
import com.olarte.ironprogress.ui.progress.RecoveryCard
import com.olarte.ironprogress.ui.settings.LocalAppSettings
import com.olarte.ironprogress.ui.summary.FeedbackActions
import com.olarte.ironprogress.ui.summary.FeedbackForm
import com.olarte.ironprogress.ui.summary.SummaryScreen
import com.olarte.ironprogress.ui.summary.SummaryState
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
import com.olarte.ironprogress.ui.workout.RecoveryWarmupNote
import com.olarte.ironprogress.ui.workout.WorkoutScreen
import com.olarte.ironprogress.ui.workout.WorkoutState
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Capturas de la ronda 2 de F2: el feedback del Resumen, las agujetas al empezar, el calentamiento
 * según la recuperación y el mapa del cuerpo. Solo corre con -PshotsDir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class RecoveryShotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir: String? = System.getProperty("shots.dir")
    private val dark = mutableStateOf(true)
    private val screen = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val now = Instant.parse("2026-11-14T12:00:00Z")

    private fun shoot(name: String, pro: Boolean = false, content: @Composable () -> Unit) {
        assumeTrue(!dir.isNullOrBlank())
        if (screen.value == null) {
            compose.setContent {
                val current = screen.value ?: return@setContent
                CompositionLocalProvider(LocalAppSettings provides AppSettings(proMode = pro)) {
                    ForjaTheme(darkTheme = dark.value, reduceMotion = true) { current() }
                }
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

    private val exerciseId = SessionExerciseId("press")

    private fun press(finished: Boolean, done: Int): SessionDetail {
        val session = if (finished) {
            WorkoutSession(SessionId("s"), null, Discipline.STRENGTH, now - 1.hours, now, SessionStatus.FINISHED)
        } else {
            WorkoutSession(SessionId("s"), null, Discipline.STRENGTH, now)
        }
        val block = SessionBlock(SessionBlockId("b"), session.id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(exerciseId, session.id, block.id, testBench.id, 0)
        val sets = (0..3).map {
            WorkoutSet(WorkoutSetId("w$it"), exercise.id, it, weightKg = 62.5, reps = 8, completedAt = if (it < done) now else null)
        }
        return SessionDetail(session, listOf(block), listOf(exercise), sets)
    }

    private val actions = FeedbackActions({ _, _ -> }, { _, _ -> }, {}, {}, {})

    private fun summaryState(form: FeedbackForm) = SummaryState(
        loading = false,
        summary = summarize(press(finished = true, done = 4), emptyList()),
        routineName = "Demo · Empuje",
        exercises = mapOf(testBench.id to testBench),
        feedback = form,
    )

    @Test
    fun feedbackBasic() {
        val form = FeedbackForm(
            muscles = listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.SHOULDERS),
            loads = mapOf(Muscle.CHEST to PerceivedLoad.LIGHT),
            weeklySets = mapOf(Muscle.CHEST to 8.0),
        )
        shoot("feedback-basico") { SummaryScreen(summaryState(form), "es", {}, {}, actions) }
    }

    @Test
    fun feedbackProWithPain() {
        val form = FeedbackForm(
            muscles = listOf(Muscle.CHEST, Muscle.TRICEPS),
            loads = mapOf(Muscle.TRICEPS to PerceivedLoad.HARD),
            pumps = mapOf(Muscle.CHEST to 2),
            joints = setOf(Joint.SHOULDER),
            strongPain = true,
        )
        shoot("feedback-pro-molestias", pro = true) { SummaryScreen(summaryState(form), "es", {}, {}, actions) }
    }

    private fun workoutState(extra: WorkoutState.() -> WorkoutState) = WorkoutState(
        loading = false,
        session = press(finished = false, done = 0),
        catalog = mapOf(testBench.id to testBench),
        selectedExerciseId = exerciseId,
        routineName = "Demo · Empuje",
    ).extra()

    @Test
    fun sorenessPrompt() {
        val question = SessionFeedback(FeedbackId("f"), SessionId("old"), Muscle.CHEST, load = 1, createdAt = now - 96.hours)
        val triceps = question.copy(id = FeedbackId("t"), muscle = Muscle.TRICEPS)
        shoot("agujetas") {
            WorkoutScreen(
                workoutState {
                    copy(soreness = listOf(question, triceps), sorenessAnswers = mapOf(Muscle.CHEST to 0))
                },
                12,
                {},
                onMinimize = {},
            )
        }
    }

    @Test
    fun warmupAndResult() {
        shoot("calentamiento") {
            WorkoutScreen(
                workoutState {
                    copy(
                        sorenessResult = mapOf(Muscle.CHEST to SetChange.ADD),
                        warmups = mapOf(exerciseId to RecoveryWarmupNote(Muscle.CHEST, RecoveryLevel.RECOVERING, 5)),
                    )
                },
                95,
                {},
                onMinimize = {},
            )
        }
    }

    @Test
    fun painSwap() {
        shoot("molestia-cambiar") {
            WorkoutScreen(workoutState { copy(painSwap = setOf(exerciseId)) }, 40, {}, onMinimize = {})
        }
    }

    /** Pierna ayer y empuje hace dos días: cuádriceps en rojo, glúteos y pecho en ámbar. */
    private val recovery: Map<Muscle, MuscleRecovery> = run {
        val fatigue = mapOf(
            Muscle.QUADS to 7.2,
            Muscle.GLUTES to 4.8,
            Muscle.HAMSTRINGS to 3.3,
            Muscle.CHEST to 3.6,
            Muscle.TRICEPS to 1.4,
            Muscle.SHOULDERS to 1.1,
            Muscle.LOWER_BACK to 1.8,
        )
        Muscle.entries.associateWith { muscle ->
            val value = fatigue[muscle] ?: 0.0
            MuscleRecovery(
                muscle = muscle,
                fatigue = value,
                level = Recovery.level(value),
                heat = Recovery.heat(value),
                readyAt = Recovery.readyAt(value, muscle.size.recoveryHalfLifeHours, now),
                lastTrainedAt = if (value > 0) now - 20.hours else null,
            )
        }
    }

    @Composable
    private fun MapScreen(selected: Muscle?) {
        Column(
            Modifier.fillMaxSize().background(ForjaTheme.colors.background).statusBarsPadding()
                .verticalScroll(rememberScrollState()).padding(ForjaSpacing.esp5),
        ) {
            RecoveryCard(recovery, now.toEpochMilliseconds(), selected)
        }
    }

    @Test
    fun recoveryMap() {
        shoot("mapa-recuperacion") { MapScreen(null) }
    }

    @Test
    fun recoveryMapSelected() {
        shoot("mapa-recuperacion-cuadriceps", pro = true) { MapScreen(Muscle.QUADS) }
    }
}
