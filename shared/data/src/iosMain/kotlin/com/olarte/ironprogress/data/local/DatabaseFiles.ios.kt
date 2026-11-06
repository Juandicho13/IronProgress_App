package com.olarte.ironprogress.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

internal actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
internal actual fun copyFile(from: String, to: String) {
    val manager = NSFileManager.defaultManager
    if (manager.fileExistsAtPath(to)) manager.removeItemAtPath(to, error = null)
    check(manager.copyItemAtPath(from, toPath = to, error = null)) { "No se pudo copiar la base a $to" }
}
