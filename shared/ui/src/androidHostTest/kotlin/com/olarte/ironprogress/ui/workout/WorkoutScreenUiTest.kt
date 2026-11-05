package com.olarte.ironprogress.ui.workout

import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import com.olarte.ironprogress.ui.resources.Res
import com.olarte.ironprogress.ui.resources.workout_complete
import com.olarte.ironprogress.ui.resources.workout_done
import com.olarte.ironprogress.ui.resources.workout_next
import com.olarte.ironprogress.ui.theme.ForjaTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de interfaz de Entreno en la JVM con Robolectric (F1-27):
 * registrar una serie con el teclado propio y recuperar la sesión con un ViewModel nuevo.
 */
@RunWith(RobolectricTestRunner::class)
// Pantalla del tamaño de un teléfono real; la de Robolectric por defecto deja el teclado fuera.
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class WorkoutScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = ViewModelStore()

    @After
    fun tearDown() = store.clear()

    private val shown = mutableStateOf<WorkoutViewModel?>(null)

    /** Monta Entreno una sola vez; cambiar [shown] simula abrir la app con otro ViewModel. */
    private fun show(model: WorkoutViewModel) {
        if (shown.value != null) {
            shown.value = model
            return
        }
        shown.value = model
        compose.setContent {
            val model = shown.value ?: return@setContent
            val state by model.state.collectAsState()
            ForjaTheme(darkTheme = true, reduceMotion = true) {
                WorkoutScreen(state, 0, model::onIntent, onMinimize = {})
            }
        }
    }

    private fun startWithBench(sessions: TestSessions): WorkoutViewModel {
        val model = newWorkoutViewModel(sessions).also { store.put("model-${store.keys().size}", it) }
        show(model)
        // Primero termina de leer la base; antes de eso Empezar se ignora.
        until { !model.state.value.loading }
        model.onIntent(WorkoutIntent.StartFree)
        until { model.state.value.session != null }
        model.onIntent(WorkoutIntent.AddExercise(testBench.id))
        until { model.state.value.session?.sets?.isNotEmpty() == true }
        return model
    }

    /** Espera a [condition] dejando correr la cola principal, donde trabajan los ViewModels. */
    private fun until(condition: () -> Boolean) {
        try {
            compose.waitUntil(5_000) {
                shadowOf(Looper.getMainLooper()).idle()
                condition()
            }
        } catch (e: Throwable) {
            val st = shown.value?.state?.value
            throw AssertionError("Entreno no llegó al estado esperado: p=${st?.pendingWrites} k=${st?.keyboard} s=${st?.session?.sets}", e)
        }
    }

    private fun key(digit: String) = compose.onAllNodesWithText(digit).onFirst().performClick()

    /** Cada tecla guarda la serie; Siguiente y Completar esperan a que no quede nada por escribir. */
    private fun writesDone(model: WorkoutViewModel?) {
        until { model?.state?.value?.pendingWrites == 0 }
        compose.waitForIdle()
    }

    @Test
    fun registersASetWithTheKeypad() {
        val sessions = TestSessions()
        startWithBench(sessions)

        compose.onNodeWithText("Press").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Set 1 ·", substring = true).onFirst().performClick()
        key("6")
        key("0")
        writesDone(shown.value)
        compose.onNodeWithText(runBlocking { getString(Res.string.workout_next) }).assertIsEnabled().performClick()
        until { shown.value?.state?.value?.keyboard?.field == WorkoutField.REPS }
        key("8")
        writesDone(shown.value)
        // El teclado y la barra de abajo ofrecen Completar; se usa el del teclado.
        compose.onAllNodesWithText(runBlocking { getString(Res.string.workout_complete) }).onLast().performClick()

        until { sessions.data.value.single().sets.single().isCompleted }
        val saved = sessions.data.value.single().sets.single()
        assertEquals(60.0, saved.weightKg)
        assertEquals(8, saved.reps)
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.workout_done) }).assertExists()
    }

    @Test
    fun recoversTheSessionAfterTheViewModelIsRecreated() {
        val sessions = TestSessions()
        val first = startWithBench(sessions)
        val id = sessions.data.value.single().sets.single().id
        first.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.WEIGHT))
        "60".forEach { first.onIntent(WorkoutIntent.Key(it.toString())) }
        writesDone(first)
        first.onIntent(WorkoutIntent.OpenKeyboard(id, WorkoutField.REPS))
        first.onIntent(WorkoutIntent.Key("8"))
        until { first.state.value.pendingWrites == 0 && first.state.value.session?.sets?.singleOrNull()?.reps == 8 }
        first.onIntent(WorkoutIntent.Complete(id))
        until { sessions.data.value.single().sets.single().isCompleted }

        // La app se cierra: el ViewModel muere y uno nuevo lee la sesión guardada.
        val second = newWorkoutViewModel(sessions)
        store.clear()
        store.put("again", second)
        show(second)

        until { second.state.value.session != null }
        compose.onNodeWithText("Press").assertIsDisplayed()
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.workout_done) }).assertExists()
        assertTrue(compose.onAllNodesWithText("60", substring = true).fetchSemanticsNodes().isNotEmpty())
    }
}
