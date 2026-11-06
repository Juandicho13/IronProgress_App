package com.olarte.ironprogress.domain.session

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator

/**
 * Cambios a la sesión en curso (F1-12): reordenar, saltar, sustituir y agrupar. Cada función
 * devuelve solo las filas que cambian, para guardarlas una por una sin tocar el resto.
 */

private fun SessionDetail.ordered(): List<SessionExercise> = exercises.sortedBy { it.sortOrder }

private fun List<SessionExercise>.renumbered(original: SessionDetail): List<SessionExercise> =
    mapIndexed { index, exercise -> exercise.copy(sortOrder = index) }
        .filter { changed -> original.exercises.first { it.id == changed.id }.sortOrder != changed.sortOrder }

/** Mueve un ejercicio [delta] puestos (negativo sube). Devuelve los que cambiaron de orden. */
fun SessionDetail.moveExercise(id: SessionExerciseId, delta: Int): List<SessionExercise> {
    val list = ordered().toMutableList()
    val from = list.indexOfFirst { it.id == id }
    if (from < 0) return emptyList()
    val to = (from + delta).coerceIn(0, list.lastIndex)
    if (to == from) return emptyList()
    list.add(to, list.removeAt(from))
    return list.renumbered(this)
}

/** "Saltar por ahora": lo manda al final para hacerlo después. Sale de su superserie. */
fun SessionDetail.skipExercise(id: SessionExerciseId): List<SessionExercise> {
    val list = ordered().toMutableList()
    val index = list.indexOfFirst { it.id == id }
    if (index < 0) return emptyList()
    val skipped = list.removeAt(index).copy(supersetGroup = null)
    list += skipped
    val renumbered = list.mapIndexed { i, exercise -> exercise.copy(sortOrder = i) }
    return renumbered.filter { changed -> exercises.first { it.id == changed.id } != changed }
}

/**
 * Une un ejercicio con el siguiente en una superserie; con tres o más es un circuito. Si alguno
 * ya está en un grupo, el otro entra a ese grupo.
 */
fun SessionDetail.groupWithNext(id: SessionExerciseId): List<SessionExercise> {
    val list = ordered()
    val index = list.indexOfFirst { it.id == id }
    if (index < 0 || index == list.lastIndex) return emptyList()
    val current = list[index]
    val next = list[index + 1]
    val group = current.supersetGroup ?: next.supersetGroup ?: ((exercises.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
    return listOf(current, next).filter { it.supersetGroup != group }.map { it.copy(supersetGroup = group) }
}

/** Saca un ejercicio de su grupo. Si el grupo queda con uno solo, ese también queda suelto. */
fun SessionDetail.ungroup(id: SessionExerciseId): List<SessionExercise> {
    val exercise = exercises.firstOrNull { it.id == id } ?: return emptyList()
    val group = exercise.supersetGroup ?: return emptyList()
    val rest = exercises.filter { it.supersetGroup == group && it.id != id }
    val changed = mutableListOf(exercise.copy(supersetGroup = null))
    if (rest.size == 1) changed += rest.single().copy(supersetGroup = null)
    return changed
}

/** Los ejercicios de su grupo, en orden; solo él si no está en uno. */
fun SessionDetail.groupOf(id: SessionExerciseId): List<SessionExercise> {
    val exercise = exercises.firstOrNull { it.id == id } ?: return emptyList()
    val group = exercise.supersetGroup ?: return listOf(exercise)
    return ordered().filter { it.supersetGroup == group }
}

/**
 * Después de completar una serie de [id] en una superserie o circuito: el siguiente del grupo con
 * series pendientes, sin descanso en medio. Null si es el último del grupo en esta vuelta (toca
 * descansar) o si no está en un grupo.
 */
fun SessionDetail.nextInGroupWithoutRest(id: SessionExerciseId): SessionExercise? {
    val group = groupOf(id)
    if (group.size < 2) return null
    val index = group.indexOfFirst { it.id == id }
    return group.drop(index + 1).firstOrNull { member -> setsOf(member.id).any { !it.isCompleted } }
}

/** El primero del grupo con series pendientes, para empezar la vuelta siguiente después del descanso. */
fun SessionDetail.firstPendingInGroup(id: SessionExerciseId): SessionExercise? =
    groupOf(id).firstOrNull { member -> setsOf(member.id).any { !it.isCompleted } }

/** Resultado de sustituir: los ejercicios y las series que hay que guardar. */
data class Substitution(val exercises: List<SessionExercise>, val sets: List<WorkoutSet>, val selected: SessionExerciseId)

/**
 * Cambia un ejercicio por otro. Si no tiene series hechas, solo cambia el ejercicio. Si ya tiene,
 * lo hecho se queda y las pendientes pasan a un ejercicio nuevo justo después, sin medidas.
 */
fun SessionDetail.substitute(id: SessionExerciseId, newExercise: ExerciseId, ids: IdGenerator): Substitution? {
    val exercise = exercises.firstOrNull { it.id == id } ?: return null
    if (exercise.exerciseId == newExercise) return null
    val sets = setsOf(id)
    val pending = sets.filter { !it.isCompleted }
    fun blank(set: WorkoutSet) = set.copy(
        weightKg = null,
        reps = null,
        durationSeconds = null,
        distanceMeters = null,
        calories = null,
        heightCm = null,
        breaths = null,
        watts = null,
        rir = null,
        rpe = null,
        suggestion = null,
    )
    if (sets.none { it.isCompleted }) {
        return Substitution(listOf(exercise.copy(exerciseId = newExercise)), pending.map(::blank), id)
    }
    val replacement = SessionExercise(
        id = SessionExerciseId(ids.newId()),
        sessionId = exercise.sessionId,
        blockId = exercise.blockId,
        exerciseId = newExercise,
        sortOrder = exercise.sortOrder + 1,
        supersetGroup = exercise.supersetGroup,
        restSeconds = exercise.restSeconds,
        timedSeconds = exercise.timedSeconds,
    )
    val shifted = ordered().filter { it.sortOrder > exercise.sortOrder }.map { it.copy(sortOrder = it.sortOrder + 1) }
    val moved = pending.mapIndexed { index, set -> blank(set).copy(sessionExerciseId = replacement.id, sortOrder = index) }
    return Substitution(shifted + replacement, moved, replacement.id)
}
