package com.olarte.ironprogress.domain.catalog

/**
 * Catálogo fijo de grupos musculares: son las zonas del mapa de recuperación y las filas
 * de series por músculo. Los nombres visibles viven en los textos de la app (ES/EN).
 */
enum class Muscle(val size: MuscleSize, val bodyView: BodyView) {
    CHEST(MuscleSize.LARGE, BodyView.FRONT),
    SHOULDERS(MuscleSize.SMALL, BodyView.BOTH),
    BICEPS(MuscleSize.SMALL, BodyView.FRONT),
    TRICEPS(MuscleSize.SMALL, BodyView.BACK),
    FOREARMS(MuscleSize.SMALL, BodyView.BOTH),
    ABS(MuscleSize.SMALL, BodyView.FRONT),
    LATS(MuscleSize.LARGE, BodyView.BACK),
    TRAPS(MuscleSize.SMALL, BodyView.BACK),
    LOWER_BACK(MuscleSize.LARGE, BodyView.BACK),
    GLUTES(MuscleSize.LARGE, BodyView.BACK),
    QUADS(MuscleSize.LARGE, BodyView.FRONT),
    HAMSTRINGS(MuscleSize.LARGE, BodyView.BACK),
    CALVES(MuscleSize.SMALL, BodyView.BOTH),
    ;

    /** Identificador que se guarda en la base. No renombres un valor del enum: cambiaría su id. */
    val id: String get() = name.lowercase()

    companion object {
        fun fromId(id: String): Muscle? = entries.firstOrNull { it.id == id }
    }
}

/** La fatiga de un músculo baja a la mitad cada [recoveryHalfLifeHours] horas: 24 en los pequeños y 48 en los grandes. */
enum class MuscleSize(val recoveryHalfLifeHours: Int) {
    SMALL(24),
    LARGE(48),
}

/** En qué vista del mapa corporal aparece el músculo. */
enum class BodyView { FRONT, BACK, BOTH }