package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.common.IdGenerator

/** Rutina nueva con un bloque de series vacío, lista para agregarle ejercicios. */
class CreateRoutine(private val repository: RoutineRepository, private val ids: IdGenerator) {

    suspend operator fun invoke(name: String, folderId: FolderId? = null): RoutineDetail {
        val detail = RoutineDetail(
            routine = Routine(id = RoutineId(ids.newId()), folderId = folderId, name = name.trim()),
            blocks = listOf(WorkoutBlock(id = BlockId(ids.newId()), sortOrder = 0, format = BlockFormat.SETS)),
            exercises = emptyList(),
            setTemplates = emptyList(),
        )
        repository.saveRoutine(detail)
        return detail
    }
}

/**
 * Agrega un ejercicio al final de un bloque con [sets] series de trabajo iguales. El rango de
 * reps por defecto es el de hipertrofia de HANDOFF 10.2 (8 a 12, RIR 2), pendiente de Juan.
 */
fun RoutineDetail.addExercise(
    ids: IdGenerator,
    blockId: BlockId,
    exerciseId: ExerciseId,
    sets: Int = 3,
    repsMin: Int? = 8,
    repsMax: Int? = 12,
    targetRir: Int? = 2,
): RoutineDetail {
    require(blocks.any { it.id == blockId }) { "El bloque no existe" }
    require(sets > 0) { "Hace falta al menos una serie" }
    val exercise = RoutineExercise(
        id = RoutineExerciseId(ids.newId()),
        blockId = blockId,
        exerciseId = exerciseId,
        sortOrder = exercisesOf(blockId).size,
    )
    val templates = (0 until sets).map { index ->
        SetTemplate(
            id = SetTemplateId(ids.newId()),
            routineExerciseId = exercise.id,
            sortOrder = index,
            repsMin = repsMin,
            repsMax = repsMax,
            targetRir = targetRir,
        )
    }
    return copy(exercises = exercises + exercise, setTemplates = setTemplates + templates)
}

/** Mueve un ejercicio dentro de su bloque a la posición [toIndex] y reescribe el orden. */
fun RoutineDetail.moveExercise(id: RoutineExerciseId, toIndex: Int): RoutineDetail {
    val moving = exercises.first { it.id == id }
    val inBlock = exercisesOf(moving.blockId).toMutableList()
    inBlock.remove(moving)
    inBlock.add(toIndex.coerceIn(0, inBlock.size), moving)
    val reordered = inBlock.withSortOrder { exercise, order -> exercise.copy(sortOrder = order) }.associateBy { it.id }
    return copy(exercises = exercises.map { reordered[it.id] ?: it })
}

/** Quita un ejercicio con sus series y deja el orden del bloque sin huecos. */
fun RoutineDetail.removeExercise(id: RoutineExerciseId): RoutineDetail {
    val removed = exercises.first { it.id == id }
    val remaining = exercisesOf(removed.blockId).filter { it.id != id }
        .withSortOrder { exercise, order -> exercise.copy(sortOrder = order) }
        .associateBy { it.id }
    return copy(
        exercises = exercises.filter { it.id != id }.map { remaining[it.id] ?: it },
        setTemplates = setTemplates.filter { it.routineExerciseId != id },
    )
}

/**
 * Une ejercicios seguidos de un bloque en una superserie. Toma el número de grupo más bajo
 * libre del bloque.
 */
fun RoutineDetail.groupAsSuperset(ids: List<RoutineExerciseId>): RoutineDetail {
    require(ids.size >= 2) { "Una superserie necesita al menos dos ejercicios" }
    val members = exercises.filter { it.id in ids }
    require(members.size == ids.size) { "Hay ejercicios que no están en la rutina" }
    val blockId = members.first().blockId
    require(members.all { it.blockId == blockId }) { "Una superserie va dentro de un solo bloque" }
    val orders = members.map { it.sortOrder }.sorted()
    require(orders.zipWithNext().all { (a, b) -> b == a + 1 }) { "Los ejercicios de una superserie van seguidos" }
    val used = exercisesOf(blockId).mapNotNull { it.supersetGroup }.toSet()
    val group = generateSequence(1) { it + 1 }.first { it !in used }
    return copy(exercises = exercises.map { if (it.id in ids) it.copy(supersetGroup = group) else it })
}
