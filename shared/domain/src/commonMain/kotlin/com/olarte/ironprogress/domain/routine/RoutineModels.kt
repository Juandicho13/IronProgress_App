package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.WeightUnit
import kotlin.jvm.JvmInline

@JvmInline
value class FolderId(val value: String)

@JvmInline
value class RoutineId(val value: String)

@JvmInline
value class BlockId(val value: String)

@JvmInline
value class RoutineExerciseId(val value: String)

@JvmInline
value class SetTemplateId(val value: String)

/** Carpeta de rutinas, con orden por arrastre. */
data class Folder(val id: FolderId, val name: String, val sortOrder: Int)

/** Quién creó una rutina. Los profesionales llegan en la Etapa 4; el campo existe desde F1. */
sealed interface CreatedBy {
    data object User : CreatedBy

    data class Professional(val professionalId: String) : CreatedBy
}

data class Routine(
    val id: RoutineId,
    val folderId: FolderId?,
    val name: String,
    val notes: String = "",
    /** Descanso por defecto entre series de la rutina, en segundos. */
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val sortOrder: Int = 0,
    val createdBy: CreatedBy = CreatedBy.User,
    /** Programa publicado del que viene; null hasta la Etapa 4. */
    val sourceProgramVersionId: String? = null,
) {
    init {
        require(name.isNotBlank()) { "La rutina necesita un nombre" }
        require(restSeconds >= 0) { "El descanso no puede ser negativo" }
    }

    companion object {
        const val DEFAULT_REST_SECONDS = 120
    }
}

/**
 * Cómo corre el reloj y cómo se puntúa un bloque (HANDOFF 10.5 y 11.9). SETS cubre series,
 * superseries y circuitos; el cardio por intervalos usa INTERVALS.
 */
enum class BlockFormat {
    SETS,
    EMOM,
    AMRAP,
    FOR_TIME,
    INTERVALS,
    ROUNDS,
    FLOW,
    BREATHING,
}

/**
 * Un entreno es una lista de bloques. Los campos de tiempo dependen del formato: EMOM usa
 * rounds (minutos); AMRAP, timeCapSeconds; intervalos, workSeconds, restSeconds y rounds.
 */
data class WorkoutBlock(
    val id: BlockId,
    val sortOrder: Int,
    val format: BlockFormat,
    val rounds: Int? = null,
    val workSeconds: Int? = null,
    val restSeconds: Int? = null,
    val timeCapSeconds: Int? = null,
) {
    init {
        when (format) {
            BlockFormat.EMOM -> require((rounds ?: 0) > 0) { "Un EMOM necesita minutos (rounds)" }
            BlockFormat.AMRAP -> require((timeCapSeconds ?: 0) > 0) { "Un AMRAP necesita su tiempo total" }
            BlockFormat.INTERVALS -> require((workSeconds ?: 0) > 0 && (rounds ?: 0) > 0) {
                "Los intervalos necesitan tiempo de trabajo y rondas"
            }
            else -> Unit
        }
        require(listOfNotNull(rounds, workSeconds, restSeconds, timeCapSeconds).all { it >= 0 }) {
            "Los tiempos y las rondas no pueden ser negativos"
        }
    }
}

/** Un ejercicio dentro de un bloque de la rutina. Mismo supersetGroup = se alternan. */
data class RoutineExercise(
    val id: RoutineExerciseId,
    val blockId: BlockId,
    val exerciseId: ExerciseId,
    val sortOrder: Int,
    val supersetGroup: Int? = null,
    /** null usa el descanso de la rutina. */
    val restSeconds: Int? = null,
    /** null usa la unidad del ejercicio o la general. */
    val unit: WeightUnit? = null,
    val notes: String = "",
    /** Segundos de cada serie cuando el ejercicio va por tiempo; null en series normales. */
    val timedSeconds: Int? = null,
) {
    val mode: ExerciseMode get() = ExerciseMode.of(supersetGroup, restSeconds, timedSeconds)
}

/** Plantilla de cada serie objetivo. */
data class SetTemplate(
    val id: SetTemplateId,
    val routineExerciseId: RoutineExerciseId,
    val sortOrder: Int,
    val setType: SetType = SetType.WORKING,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val targetRir: Int? = null,
) {
    init {
        require(repsMin == null || repsMin > 0) { "El mínimo de reps tiene que ser mayor que 0" }
        require(repsMin == null || repsMax == null || repsMin <= repsMax) { "El rango de reps está al revés" }
        require(targetRir == null || targetRir in 0..5) { "El RIR objetivo va de 0 a 5" }
        require(setType != SetType.FAILURE || targetRir == null || targetRir == 0) {
            "Una serie al fallo no tiene RIR objetivo mayor que 0"
        }
    }
}

/**
 * Tipos de serie (HANDOFF 10.2, valores propuestos). Todos menos WARMUP cuentan como series
 * de trabajo. Los nuevos van al final.
 */
enum class SetType(val countsAsWorking: Boolean) {
    WARMUP(false),
    WORKING(true),
    DROP(true),
    FAILURE(true),
    REST_PAUSE(true),
    MYO_REPS(true),
    CLUSTER(true),
}

/** Rutina con sus bloques, ejercicios y plantillas, en orden. */
data class RoutineDetail(
    val routine: Routine,
    val blocks: List<WorkoutBlock>,
    val exercises: List<RoutineExercise>,
    val setTemplates: List<SetTemplate>,
) {
    init {
        val blockIds = blocks.map { it.id }.toSet()
        require(exercises.all { it.blockId in blockIds }) { "Hay un ejercicio en un bloque que no existe" }
        val exerciseIds = exercises.map { it.id }.toSet()
        require(setTemplates.all { it.routineExerciseId in exerciseIds }) { "Hay una serie de un ejercicio que no existe" }
    }

    fun exercisesOf(blockId: BlockId): List<RoutineExercise> = exercises.filter { it.blockId == blockId }.sortedBy { it.sortOrder }

    fun setsOf(exerciseId: RoutineExerciseId): List<SetTemplate> =
        setTemplates.filter { it.routineExerciseId == exerciseId }.sortedBy { it.sortOrder }

    /**
     * Grupos que se alternan dentro de un bloque: ejercicios seguidos con el mismo
     * supersetGroup forman un grupo; uno sin grupo va solo.
     */
    fun supersetsOf(blockId: BlockId): List<List<RoutineExercise>> {
        val groups = mutableListOf<MutableList<RoutineExercise>>()
        for (exercise in exercisesOf(blockId)) {
            val last = groups.lastOrNull()
            val group = exercise.supersetGroup
            if (group != null && last != null && last.first().supersetGroup == group) {
                last += exercise
            } else {
                groups += mutableListOf(exercise)
            }
        }
        return groups
    }
}

/** Reordena una lista dejando sortOrder 0, 1, 2... en el orden dado. */
fun <T> List<T>.withSortOrder(copy: (T, Int) -> T): List<T> = mapIndexed { index, item -> copy(item, index) }
