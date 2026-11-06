package com.olarte.ironprogress.domain.routine

import com.olarte.ironprogress.domain.catalog.ExerciseId
import com.olarte.ironprogress.domain.catalog.ExerciseRepository
import com.olarte.ironprogress.domain.common.IdGenerator
import com.olarte.ironprogress.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/** Un ejercicio de una rutina demo: series, rango de reps y cómo se hace. */
data class DemoExercise(
    val exerciseId: String,
    val sets: Int,
    val repsMin: Int?,
    val repsMax: Int?,
    val mode: ExerciseMode = ExerciseMode.REST,
    val restSeconds: Int? = null,
    val timedSeconds: Int? = null,
)

data class DemoRoutine(val name: String, val exercises: List<DemoExercise>)

/**
 * Rutinas de ejemplo para que la app no arranque vacía. Van en la carpeta "Demo" y con el
 * nombre empezando por "Demo" para que se note que se pueden borrar o copiar.
 */
object DemoRoutines {
    const val FOLDER_NAME = "Demo"

    val all: List<DemoRoutine> = listOf(
        DemoRoutine(
            "Demo · Empuje",
            listOf(
                DemoExercise("barbell-bench-press", 3, 6, 10, restSeconds = 150),
                DemoExercise("incline-dumbbell-bench-press", 3, 8, 12),
                DemoExercise("seated-dumbbell-shoulder-press", 3, 8, 12),
                DemoExercise("dumbbell-lateral-raise", 3, 12, 15, restSeconds = 60),
                DemoExercise("cable-tricep-pushdown", 3, 10, 15, restSeconds = 60),
            ),
        ),
        DemoRoutine(
            "Demo · Tirón",
            listOf(
                DemoExercise("lat-pulldown", 3, 8, 12),
                DemoExercise("seated-cable-row", 3, 8, 12),
                DemoExercise("face-pull", 3, 12, 15, restSeconds = 60),
                DemoExercise("dumbbell-bicep-curl", 3, 10, 12, restSeconds = 60),
                DemoExercise("hammer-curl", 2, 10, 12, restSeconds = 60),
            ),
        ),
        DemoRoutine(
            "Demo · Pierna",
            listOf(
                DemoExercise("barbell-back-squat", 3, 6, 10, restSeconds = 180),
                DemoExercise("romanian-deadlift", 3, 8, 10, restSeconds = 150),
                DemoExercise("leg-press", 3, 10, 12),
                DemoExercise("seated-leg-curl", 3, 10, 15, restSeconds = 60),
                DemoExercise("standing-calf-raise", 3, 12, 15, mode = ExerciseMode.NO_REST),
            ),
        ),
        DemoRoutine(
            "Demo · Cuerpo completo",
            listOf(
                DemoExercise("leg-press", 3, 10, 12),
                DemoExercise("dumbbell-bench-press", 3, 8, 12),
                DemoExercise("one-arm-dumbbell-row", 3, 8, 12),
                DemoExercise("glute-bridge", 3, 12, 15, restSeconds = 60),
                DemoExercise("plank", 3, null, null, mode = ExerciseMode.TIMED, timedSeconds = 30),
            ),
        ),
    )
}

/**
 * Siembra las rutinas demo una sola vez. Si ya hay rutinas, solo marca que no hace falta: así
 * borrar las demo no las vuelve a traer y quien ya tenía las suyas no recibe nada.
 */
class SeedDemoRoutines(
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(): Boolean {
        if (settings.settings.first().demoRoutinesSeeded) return false
        val seeded = routines.observeRoutines().first().isEmpty() && seed()
        settings.update { it.copy(demoRoutinesSeeded = true) }
        return seeded
    }

    private suspend fun seed(): Boolean {
        val folders = routines.observeFolders().first()
        val folder = folders.firstOrNull { it.name == DemoRoutines.FOLDER_NAME }
            ?: Folder(FolderId(ids.newId()), DemoRoutines.FOLDER_NAME, folders.size).also { routines.saveFolder(it) }
        var saved = 0
        DemoRoutines.all.forEachIndexed { index, demo ->
            val detail = build(demo, folder.id, index) ?: return@forEachIndexed
            routines.saveRoutine(detail)
            saved++
        }
        return saved > 0
    }

    private suspend fun build(demo: DemoRoutine, folderId: FolderId, order: Int): RoutineDetail? {
        val block = WorkoutBlock(id = BlockId(ids.newId()), sortOrder = 0, format = BlockFormat.SETS)
        var detail = RoutineDetail(
            routine = Routine(id = RoutineId(ids.newId()), folderId = folderId, name = demo.name, sortOrder = order),
            blocks = listOf(block),
            exercises = emptyList(),
            setTemplates = emptyList(),
        )
        for (item in demo.exercises) {
            if (item.sets <= 0 || exercises.findExercise(ExerciseId(item.exerciseId)) == null) continue
            detail = detail.addExercise(ids, block.id, ExerciseId(item.exerciseId), item.sets, item.repsMin, item.repsMax, targetRir = 2)
            val added = detail.exercises.last()
            val withMode = when (item.mode) {
                ExerciseMode.NO_REST -> added.copy(restSeconds = 0)
                ExerciseMode.TIMED -> added.copy(timedSeconds = item.timedSeconds ?: ExerciseMode.DEFAULT_TIMED_SECONDS)
                else -> added.copy(restSeconds = item.restSeconds)
            }
            detail = detail.copy(exercises = detail.exercises.dropLast(1) + withMode)
        }
        return detail.takeIf { it.exercises.isNotEmpty() }
    }
}
