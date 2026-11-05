package com.olarte.ironprogress.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Idioma elegido en Ajustes sin reiniciar (CFG-02). null vuelve al del teléfono. Quien lo provee
 * envuelve el contenido en key(idioma) para que los textos se vuelvan a leer.
 */
@Composable
expect fun appLocaleProvider(languageTag: String?): ProvidedValue<*>
