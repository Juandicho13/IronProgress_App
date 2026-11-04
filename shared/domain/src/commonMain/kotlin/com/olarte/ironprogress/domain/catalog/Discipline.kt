package com.olarte.ironprogress.domain.catalog

/**
 * Disciplina de un ejercicio o de una sesión: define qué se anota y con qué tipo se guarda
 * en Salud y en Health Connect (tabla de docs/PLAN_V3.md, sección 6). MEDITATION llega en la
 * Etapa 3 y se agrega al final, como todo valor nuevo.
 */
enum class Discipline {
    STRENGTH,
    WEIGHTLIFTING,
    CALISTHENICS,
    FUNCTIONAL,
    HIIT,
    COMBAT,
    YOGA,
    PILATES,
    MOBILITY,
    BREATHING,
    CLIMBING,
    CARDIO,
    REHAB,
    SPORT,
}
