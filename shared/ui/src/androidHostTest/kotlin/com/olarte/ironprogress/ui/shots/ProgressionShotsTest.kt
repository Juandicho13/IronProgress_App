package com.olarte.ironprogress.ui.shots

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.progression.PreviousSummary
import com.olarte.ironprogress.domain.progression.RepRange
import com.olarte.ironprogress.domain.progression.Suggestion
import com.olarte.ironprogress.domain.progression.SuggestionReason
import com.olarte.ironprogress.domain.progression.recordFor
import com.olarte.ironprogress.domain.routine.BlockFormat
import com.olarte.ironprogress.domain.session.SessionBlock
import com.olarte.ironprogress.domain.session.SessionBlockId
import com.olarte.ironprogress.domain.session.SessionDetail
import com.olarte.ironprogress.domain.session.SessionExercise
import com.olarte.ironprogress.domain.session.SessionExerciseId
import com.olarte.ironprogress.domain.session.SessionId
import com.olarte.ironprogress.domain.session.WorkoutSession
import com.olarte.ironprogress.domain.session.WorkoutSet
import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaSpacing
import com.olarte.ironprogress.ui.theme.ForjaTheme
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
import kotlin.time.Instant

/**
 * Capturas de la progresión en Entreno (F2): la sugerencia en la serie actual, el estancamiento y la
 * escalera. Solo corre con -PshotsDir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rCO-w411dp-h891dp")
class ProgressionShotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir: String? = System.getProperty("shots.dir")
    private val dark = mutableStateOf(true)
    private val screen = mutableStateOf<(@Composable () -> Unit)?>(null)

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

    private val now = Instant.parse("2026-11-14T12:00:00Z")
    private val exerciseId = SessionExerciseId("press")

    /** Press con la primera serie hecha y la segunda con la sugerencia del motor. */
    internal fun pressSession(suggestion: Suggestion, changed: Boolean = false): SessionDetail {
        val session = WorkoutSession(SessionId("s"), null, Discipline.STRENGTH, now)
        val block = SessionBlock(SessionBlockId("b"), session.id, 0, BlockFormat.SETS)
        val exercise = SessionExercise(exerciseId, session.id, block.id, testBench.id, 0)
        val sets = (0..2).map { index ->
            val record = suggestion.recordFor(index)
            WorkoutSet(
                WorkoutSetId("w$index"),
                exercise.id,
                index,
                weightKg = if (changed && index == 1) 60.0 else record.weightKg,
                reps = record.reps,
                suggestion = record.encode(),
                completedAt = if (index == 0) now else null,
            )
        }
        return SessionDetail(session, listOf(block), listOf(exercise), sets)
    }

    internal fun state(detail: SessionDetail, pro: Boolean) = WorkoutState(
        loading = false,
        session = detail,
        catalog = mapOf(testBench.id to testBench),
        selectedExerciseId = exerciseId,
        proMode = pro,
    )

    private val increase = Suggestion(
        SuggestionReason.INCREASE,
        62.5,
        listOf(8, 8, 8),
        RepRange(8, 12, 2),
        PreviousSummary(3, 12, 12, 60.0, 2),
    )

    @Test
    fun suggestionBasic() {
        shoot("sugerencia-basico") { WorkoutScreen(state(pressSession(increase), pro = false), 754, {}, onMinimize = {}) }
    }

    @Test
    fun suggestionProChanged() {
        val detail = pressSession(increase, changed = true)
        shoot("sugerencia-pro-cambiada") { WorkoutScreen(state(detail, pro = true), 754, {}, onMinimize = {}) }
    }

    private val plateau = com.olarte.ironprogress.domain.progression.Stagnation(
        com.olarte.ironprogress.domain.progression.StagnationKind.PLATEAU,
        4,
        listOf(
            com.olarte.ironprogress.domain.progression.StagnationProposal.DELOAD,
            com.olarte.ironprogress.domain.progression.StagnationProposal.CHANGE_RANGE,
            com.olarte.ironprogress.domain.progression.StagnationProposal.SUBSTITUTE,
        ),
        RepRange(5, 8, 2),
        8,
    )

    @Test
    fun stagnationRow() {
        val repeat = Suggestion(SuggestionReason.ADD_REP, 60.0, listOf(11, 11, 11), RepRange(8, 12, 2), PreviousSummary(3, 10, 10, 60.0, 2))
        val state = state(pressSession(repeat), pro = false).copy(stagnation = mapOf(exerciseId to plateau))
        shoot("estancamiento") { WorkoutScreen(state, 754, {}, onMinimize = {}) }
    }

    @Test
    fun stagnationOptions() {
        shoot("estancamiento-opciones") {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.fillMaxSize().background(ForjaTheme.colors.background),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
            ) {
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier.background(ForjaTheme.colors.surface1, ForjaRadius.shapeXlTop)
                        .padding(top = ForjaSpacing.esp5),
                ) {
                    androidx.compose.material3.Text(
                        "Para volver a progresar",
                        androidx.compose.ui.Modifier.padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp2),
                        style = ForjaTheme.typography.title3,
                        color = ForjaTheme.colors.text1,
                    )
                    com.olarte.ironprogress.ui.workout.StagnationOptions(plateau, {}, {})
                }
            }
        }
    }

    private val pushUp = testBench.copy(
        id = com.olarte.ironprogress.domain.catalog.ExerciseId("push-up"),
        name = com.olarte.ironprogress.domain.catalog.LocalizedText("Flexiones", "Push-ups"),
        trackingType = com.olarte.ironprogress.domain.catalog.TrackingType.REPS,
        equipment = com.olarte.ironprogress.domain.catalog.Equipment.BODYWEIGHT,
    )
    private val decline = pushUp.copy(
        id = com.olarte.ironprogress.domain.catalog.ExerciseId("decline-push-up"),
        name = com.olarte.ironprogress.domain.catalog.LocalizedText("Flexiones declinadas", "Decline push-ups"),
    )

    private fun pushUpState(suggestion: Suggestion): WorkoutState {
        val detail = pressSession(suggestion).let { detail ->
            detail.copy(
                session = detail.session.copy(routineId = com.olarte.ironprogress.domain.routine.RoutineId("r")),
                exercises = detail.exercises.map { it.copy(exerciseId = pushUp.id) },
                sets = detail.sets.map { it.copy(weightKg = null) },
            )
        }
        return state(detail, pro = false).copy(catalog = mapOf(pushUp.id to pushUp, decline.id to decline))
    }

    @Test
    fun ladderHarder() {
        val ladder = Suggestion(SuggestionReason.LADDER_HARDER, null, listOf(15, 15, 15), RepRange(8, 15, 2), ladderTarget = decline.id)
        val state = pushUpState(ladder)
        shoot("escalera") { WorkoutScreen(state, 754, {}, onMinimize = {}) }
    }

    @Test
    fun ladderOneMore() {
        val ladder = Suggestion(SuggestionReason.LADDER_ONE_MORE, null, listOf(15, 15, 15), RepRange(8, 15, 2), ladderTarget = decline.id)
        val state = pushUpState(ladder)
        shoot("escalera-una-mas") { WorkoutScreen(state, 754, {}, onMinimize = {}) }
    }

    @Test
    fun ladderOptions() {
        shoot("escalera-opciones") {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.fillMaxSize().background(ForjaTheme.colors.background),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
            ) {
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier.background(ForjaTheme.colors.surface1, ForjaRadius.shapeXlTop)
                        .padding(top = ForjaSpacing.esp5),
                ) {
                    androidx.compose.material3.Text(
                        "Cambiar a Flexiones declinadas",
                        androidx.compose.ui.Modifier.padding(horizontal = ForjaSpacing.esp5, vertical = ForjaSpacing.esp2),
                        style = ForjaTheme.typography.title3,
                        color = ForjaTheme.colors.text1,
                    )
                    com.olarte.ironprogress.ui.workout.LadderOptions(8, inRoutine = true, {}, {})
                }
            }
        }
    }
}
