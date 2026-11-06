package com.olarte.ironprogress.data.local

import java.io.File

internal actual fun fileExists(path: String): Boolean = File(path).exists()

internal actual fun copyFile(from: String, to: String) {
    File(from).copyTo(File(to), overwrite = true)
}
