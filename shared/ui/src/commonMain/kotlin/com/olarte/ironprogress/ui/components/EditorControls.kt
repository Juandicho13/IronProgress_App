package com.olarte.ironprogress.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.olarte.ironprogress.ui.theme.ForjaMeasures
import com.olarte.ironprogress.ui.theme.ForjaRadius
import com.olarte.ironprogress.ui.theme.ForjaTheme

@Composable
fun EditorAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = ForjaMeasures.minTouch),
        shape = ForjaRadius.shapeFull,
    ) {
        Text(text, style = ForjaTheme.typography.label)
    }
}

@Composable
fun EditorField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = ForjaTheme.typography.label) },
        textStyle = ForjaTheme.typography.body,
        shape = ForjaRadius.shapeM,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun <T> EditorChoice(label: String, selected: T, options: List<T>, name: @Composable (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    EditorAction(label + ": " + name(selected)) { expanded = true }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(label, style = ForjaTheme.typography.title3) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { value ->
                        TextButton(onClick = {
                            onSelect(value)
                            expanded = false
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = ForjaMeasures.minTouch)) {
                            Text(name(value), style = ForjaTheme.typography.body)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}
