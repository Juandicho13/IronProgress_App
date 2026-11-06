package com.olarte.ironprogress.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Destinos de la app. Hoy, Plan y Progreso son raíces de pestaña; Catálogo y Entreno van encima. */
@Serializable
sealed interface Route : NavKey {
    @Serializable data object Today : Route

    @Serializable data object Plan : Route

    @Serializable data object Progress : Route

    @Serializable data object Catalog : Route

    @Serializable data class Workout(val sessionId: String? = null) : Route

    @Serializable data class RoutineEditor(val id: String? = null) : Route

    @Serializable data class ExerciseEditor(val id: String? = null) : Route

    @Serializable data class ExerciseDetail(val id: String) : Route

    @Serializable data class IntervalEditor(val id: String? = null) : Route

    @Serializable data class IntervalRun(val id: String) : Route

    @Serializable data object Wod : Route

    @Serializable data class Summary(val sessionId: String) : Route

    @Serializable data object History : Route

    @Serializable data class HistorySession(val sessionId: String) : Route

    @Serializable data object Settings : Route

    @Serializable data object Backup : Route

    @Serializable data object Disciplines : Route

    @Serializable data class Flow(val id: String) : Route

    @Serializable data object Breathing : Route
}

/**
 * Registra cada ruta para guardar la pila. Sin esta configuración la pila usa reflexión,
 * que no existe en iOS, y falla al guardarse.
 */
val navConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Today::class, Route.Today.serializer())
            subclass(Route.Plan::class, Route.Plan.serializer())
            subclass(Route.Progress::class, Route.Progress.serializer())
            subclass(Route.Catalog::class, Route.Catalog.serializer())
            subclass(Route.Workout::class, Route.Workout.serializer())
            subclass(Route.RoutineEditor::class, Route.RoutineEditor.serializer())
            subclass(Route.ExerciseEditor::class, Route.ExerciseEditor.serializer())
            subclass(Route.ExerciseDetail::class, Route.ExerciseDetail.serializer())
            subclass(Route.IntervalEditor::class, Route.IntervalEditor.serializer())
            subclass(Route.IntervalRun::class, Route.IntervalRun.serializer())
            subclass(Route.Wod::class, Route.Wod.serializer())
            subclass(Route.Summary::class, Route.Summary.serializer())
            subclass(Route.History::class, Route.History.serializer())
            subclass(Route.HistorySession::class, Route.HistorySession.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.Backup::class, Route.Backup.serializer())
            subclass(Route.Disciplines::class, Route.Disciplines.serializer())
            subclass(Route.Flow::class, Route.Flow.serializer())
            subclass(Route.Breathing::class, Route.Breathing.serializer())
        }
    }
}
