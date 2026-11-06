package com.olarte.ironprogress.ui.today

/** Día civil del calendario local del dispositivo, expresado como días desde 1970-01-01. */
expect fun localEpochDay(epochMs: Long): Long
