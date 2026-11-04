package com.olarte.ironprogress.ui.today

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual fun localEpochDay(epochMs: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMs / 1_000.0)
    val offset = NSTimeZone.localTimeZone.secondsFromGMTForDate(date)
    return (epochMs + offset * 1_000L).floorDiv(86_400_000L)
}
