package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

/**
 * Toca fuera de un campo y el teclado se cierra. Solo actúa si nadie usó el toque: un botón, un
 * campo o un desplazamiento lo consumen y siguen funcionando igual.
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return this.pointerInput(focus, keyboard) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val up = waitForUpOrCancellation(PointerEventPass.Final) ?: return@awaitEachGesture
            if (!down.isConsumed && !up.isConsumed) {
                focus.clearFocus()
                keyboard?.hide()
            }
        }
    }
}

/** Una línea termina en Listo y cierra el teclado; si el campo pide Siguiente, pasa al de abajo. */
fun KeyboardOptions.withDefaultImeAction(singleLine: Boolean): KeyboardOptions =
    if (imeAction == ImeAction.Unspecified || imeAction == ImeAction.Default) {
        copy(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default)
    } else {
        this
    }

@Composable
fun dismissingKeyboardActions(): KeyboardActions {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(
        onDone = {
            focus.clearFocus()
            keyboard?.hide()
        },
        onNext = {
            if (!focus.moveFocus(FocusDirection.Down)) {
                focus.clearFocus()
                keyboard?.hide()
            }
        },
        onSearch = {
            focus.clearFocus()
            keyboard?.hide()
        },
        onGo = {
            focus.clearFocus()
            keyboard?.hide()
        },
    )
}
