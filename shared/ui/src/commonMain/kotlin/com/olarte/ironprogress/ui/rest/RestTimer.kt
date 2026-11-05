package com.olarte.ironprogress.ui.rest

import com.olarte.ironprogress.domain.session.WorkoutSetId
import com.olarte.ironprogress.domain.time.TimerEdit
import com.olarte.ironprogress.domain.time.TimerPlans
import com.olarte.ironprogress.domain.time.eventsBetween
import com.olarte.ironprogress.domain.time.stateAt

/** Un descanso conserva instantes y ediciones. Nunca descuenta segundos acumulados. */
data class RestTimer(val setId: WorkoutSetId, val startedAtMs: Long, val seconds: Int, val edits: List<TimerEdit> = emptyList()) {
    val plan get() = TimerPlans.rest(seconds)
    fun at(nowMs: Long) = plan.stateAt(nowMs, startedAtMs, edits)
    fun events(fromMs: Long, toMs: Long) = plan.eventsBetween(fromMs, toMs, startedAtMs, edits)
    fun adjust(nowMs: Long, deltaSeconds: Int) = copy(edits = edits + TimerEdit.Adjust(nowMs, deltaSeconds * 1000L))
}
