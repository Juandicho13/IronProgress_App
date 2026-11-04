package com.olarte.ironprogress.ui.today

import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.LocalizedText
import com.olarte.ironprogress.domain.routine.RoutineDetail
import com.olarte.ironprogress.domain.routine.RoutineExerciseId
import com.olarte.ironprogress.domain.routine.WeekProgress

/** Fila de un ejercicio dentro de la tarjeta Siguiente en la pantalla Hoy. */
data class TodayExerciseRow(
    val id: RoutineExerciseId,
    val exerciseId: ExerciseId,
    val name: LocalizedText?,
    val fallbackName: String,
    val workingSetsCount: Int,
    val repsMin: Int?,
    val repsMax: Int?,
    val targetRir: Int?,
)

/** Resumen de la rutina propuesta por rotación para pintar la tarjeta Siguiente. */
data class TodayRoutineCard(
    val detail: RoutineDetail,
    val folderName: String?,
    val positionInRotation: Int,
    val rotationSize: Int,
    val isActiveSession: Boolean,
    val estimatedMinutes: Int,
    val primaryDiscipline: Discipline,
    val exercises: List<TodayExerciseRow>,
)

/** Estado inmutable de la pantalla Hoy (MVI). */
data class TodayState(
    val loading: Boolean = true,
    val starting: Boolean = false,
    val failed: Boolean = false,
    val todayEpochMs: Long = 0L,
    val weekProgress: WeekProgress = WeekProgress(emptyList(), 0, 0),
    val nextRoutine: TodayRoutineCard? = null,
    val activeSessionId: String? = null,
)

/** Intenciones de la pantalla Hoy. */
sealed interface TodayIntent {
    data object StartWorkout : TodayIntent

    data object Retry : TodayIntent

    data object Refresh : TodayIntent
}

/** Efectos de una sola vez de la pantalla Hoy. */
sealed interface TodayEffect {
    data class OpenWorkout(val sessionId: String) : TodayEffect
}
