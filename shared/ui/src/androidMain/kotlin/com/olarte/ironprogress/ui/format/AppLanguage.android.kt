package com.olarte.ironprogress.ui.format

import java.util.Locale

actual fun systemLanguageTag(): String = Locale.getDefault().toLanguageTag()

actual fun systemRegion(): String? = Locale.getDefault().country.ifBlank { null }
