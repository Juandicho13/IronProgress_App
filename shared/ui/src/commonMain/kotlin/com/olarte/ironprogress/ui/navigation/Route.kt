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

    @Serializable data object Workout : Route

    @Serializable data class RoutineEditor(val id: String? = null) : Route

    @Serializable data class ExerciseEditor(val id: String? = null) : Route
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
        }
    }
}
