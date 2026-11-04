package com.olarte.ironprogress.ui.today

import java.time.Instant
import java.time.ZoneId

actual fun localEpochDay(epochMs: Long): Long = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
