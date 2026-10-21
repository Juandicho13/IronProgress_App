package com.olarte.ironprogress.domain.health

/** Si se pueden leer datos de salud en este teléfono. */
enum class HealthAvailability {
    /** Hay permiso y la fuente responde. */
    AVAILABLE,

    /** La fuente existe pero falta el permiso de la persona. */
    NEEDS_PERMISSION,

    /** La app de la fuente no está instalada o necesita actualizarse (Health Connect en Android). */
    NOT_INSTALLED,

    /** Este dispositivo no tiene una fuente de datos de salud. */
    NOT_SUPPORTED,
}

/**
 * Datos de salud del reloj y del teléfono. Domain la define y el módulo health la implementa:
 * Health Connect en Android y HealthKit en iOS, en la fase 4.
 */
interface HealthRepository {
    suspend fun availability(): HealthAvailability
}