package com.olarte.ironprogress.data.local

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.domain.catalog.Discipline
import com.olarte.ironprogress.domain.catalog.Muscle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Protege a quien ya tiene la app instalada: una base creada con un esquema exportado debe abrir
 * con el código actual sin perder datos. La 1 a 2 la genera Room (AutoMigration); cuando llegue
 * otra versión, agrega una prueba que parta del esquema anterior.
 */
class CatalogMigrationTest {

    private val schemasDirectory = File(
        System.getProperty("ironprogress.roomSchemas") ?: "schemas",
        "com.olarte.ironprogress.data.local.IronProgressDatabase",
    )

    @Test
    fun theCurrentVersionHasAnExportedSchema() {
        val schema = readSchema(IronProgressDatabase.SCHEMA_VERSION)

        assertEquals(IronProgressDatabase.SCHEMA_VERSION, schema.getValue("version").jsonPrimitive.int)
    }

    @Test
    fun aVersion1DatabaseOpensWithTheCurrentCodeAndKeepsItsData() = runTest {
        val file = File.createTempFile("ironprogress-v1", ".db").apply { delete() }
        try {
            createDatabaseFromSchema(file, version = 1) { connection ->
                connection.execSQL(
                    "INSERT INTO exercises (id, name_es, name_en, tracking_type, equipment, " +
                        "movement_pattern, is_unilateral, preferred_unit, is_custom, is_archived) " +
                        "VALUES ('mi-press-landmine', 'Press landmine', 'Landmine press', " +
                        "'WEIGHT_REPS', 'BARBELL', 'VERTICAL_PUSH', 1, 'LB', 1, 0)",
                )
                connection.execSQL(
                    "INSERT INTO exercises (id, name_es, name_en, tracking_type, equipment, " +
                        "movement_pattern, is_unilateral, preferred_unit, is_custom, is_archived) " +
                        "VALUES ('barbell-bench-press', 'Press banca con barra', 'Barbell Bench Press', " +
                        "'WEIGHT_REPS', 'BARBELL', 'HORIZONTAL_PUSH', 0, NULL, 0, 1)",
                )
                connection.execSQL(
                    "INSERT INTO exercise_muscles (exercise_id, muscle_id, role, sort_order) " +
                        "VALUES ('barbell-bench-press', 'chest', 'PRIMARY', 0)",
                )
                connection.execSQL(
                    "INSERT INTO exercise_muscles (exercise_id, muscle_id, role, sort_order) " +
                        "VALUES ('mi-press-landmine', 'shoulders', 'PRIMARY', 0)",
                )
            }

            val backup = backupBeforeMigration(file.absolutePath)
            assertEquals(file.absolutePath + ".v1.bak", backup)
            assertTrue(File(backup).length() > 0)

            // Room migra a la versión actual y compara el esquema con el del código: si no
            // cuadran, falla aquí.
            val database = Room.databaseBuilder<IronProgressDatabase>(
                name = file.absolutePath,
                factory = { IronProgressDatabaseConstructor.initialize() },
            ).buildIronProgressDatabase()
            try {
                val exercise = database.catalogDao()
                    .findExerciseWithMuscles("mi-press-landmine")
                    ?.toDomain()

                assertNotNull(exercise)
                assertEquals("Press landmine", exercise.name.es)
                assertEquals(Muscle.SHOULDERS, exercise.muscles.single().muscle)
                assertTrue(exercise.isCustom)
                // Las columnas nuevas llegan con su valor por defecto.
                assertEquals(Discipline.STRENGTH, exercise.discipline)
                assertEquals(emptyList(), exercise.aliases)

                // La base de Juan con ejercicios archivados sobrevive, también al re-sembrar.
                val catalog = RoomExerciseRepository(database.catalogDao()).observeExercises().first()
                assertTrue(catalog.single { it.id.value == "barbell-bench-press" }.isArchived)
                assertTrue(catalog.any { it.id.value == "mi-press-landmine" })

                // Las tablas nuevas existen y aceptan filas.
                database.routineDao().upsertFolder(FolderEntity("f1", "Fuerza", 0, createdAt = 1, updatedAt = 1))
                assertEquals(listOf("Fuerza"), database.routineDao().observeFolders().first().map { it.name })
            } finally {
                database.close()
            }
            // Ya en la versión actual no se vuelve a copiar.
            assertNull(backupBeforeMigration(file.absolutePath))
        } finally {
            listOf("", "-wal", "-shm", "-journal", ".v1.bak").forEach { File(file.path + it).delete() }
        }
    }

    @Test
    fun version2KeepsActiveSessionAndAddsPersistentSetNotes() = runTest {
        val file = File.createTempFile("ironprogress-v2", ".db").apply { delete() }
        try {
            createDatabaseFromSchema(file, version = 2) { connection ->
                connection.execSQL(
                    "INSERT INTO workout_sessions (id, discipline, started_at, status, notes, adjustment_applied, created_at, updated_at) VALUES ('session', 'STRENGTH', 1000, 'IN_PROGRESS', 'nota previa', 0, 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO workout_blocks (id, session_id, sort_order, format, created_at, updated_at) VALUES ('block', 'session', 0, 'SETS', 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO session_exercises (id, session_id, block_id, exercise_id, sort_order, notes, created_at, updated_at) VALUES ('exercise', 'session', 'block', 'bench', 0, '', 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO workout_sets (id, session_exercise_id, sort_order, set_type, weight_kg, reps, rir, created_at, updated_at) VALUES ('set', 'exercise', 0, 'WORKING', 62.5, 10, 2, 1, 1)",
                )
            }
            assertEquals(file.absolutePath + ".v2.bak", backupBeforeMigration(file.absolutePath))
            fun open() = Room.databaseBuilder<IronProgressDatabase>(
                name = file.absolutePath,
                factory = { IronProgressDatabaseConstructor.initialize() },
            ).buildIronProgressDatabase()
            val database = open()
            try {
                val repository = com.olarte.ironprogress.data.workout.RoomSessionRepository(database.sessionDao(), kotlin.time.Clock.System)
                val active = assertNotNull(repository.observeActiveSession().first())
                assertEquals("nota previa", active.session.notes)
                val set = active.sets.single()
                assertEquals(62.5, set.weightKg)
                assertEquals(10, set.reps)
                assertEquals(2, set.rir)
                assertEquals("", set.notes)
                val timings = (1..20).map { index ->
                    val mark = kotlin.time.TimeSource.Monotonic.markNow()
                    repository.saveSet(
                        set.copy(notes = "Nota $index", reps = 11, completedAt = kotlin.time.Instant.fromEpochMilliseconds(2000)),
                    )
                    mark.elapsedNow().inWholeMicroseconds / 1000.0
                }.sorted()
                println("F1-10 guardado Room en disco del host: p95=${timings[18]} ms; máximo=${timings.last()} ms")
            } finally {
                database.close()
            }
            val reopened = open()
            try {
                val repository = com.olarte.ironprogress.data.workout.RoomSessionRepository(reopened.sessionDao(), kotlin.time.Clock.System)
                val active = assertNotNull(repository.observeActiveSession().first())
                assertEquals("Nota 20", active.sets.single().notes)
                assertEquals(11, active.sets.single().reps)
                assertTrue(active.sets.single().isCompleted)
            } finally {
                reopened.close()
            }
        } finally {
            listOf("", "-wal", "-shm", "-journal", ".v2.bak").forEach { File(file.path + it).delete() }
        }
    }

    @Test
    fun version3KeepsTheSessionAndAddsTheModeOfEachExercise() = runTest {
        val file = File.createTempFile("ironprogress-v3", ".db").apply { delete() }
        try {
            createDatabaseFromSchema(file, version = 3) { connection ->
                connection.execSQL(
                    "INSERT INTO workout_sessions (id, discipline, started_at, status, notes, adjustment_applied, created_at, updated_at) VALUES ('session', 'STRENGTH', 1000, 'IN_PROGRESS', '', 0, 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO workout_blocks (id, session_id, sort_order, format, created_at, updated_at) VALUES ('block', 'session', 0, 'SETS', 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO session_exercises (id, session_id, block_id, exercise_id, sort_order, notes, created_at, updated_at) VALUES ('exercise', 'session', 'block', 'bench', 0, '', 1, 1)",
                )
            }
            val database = Room.databaseBuilder<IronProgressDatabase>(
                name = file.absolutePath,
                factory = { IronProgressDatabaseConstructor.initialize() },
            ).buildIronProgressDatabase()
            try {
                val repository = com.olarte.ironprogress.data.workout.RoomSessionRepository(database.sessionDao(), kotlin.time.Clock.System)
                val active = assertNotNull(repository.observeActiveSession().first())
                val exercise = active.exercises.single()
                assertNull(exercise.restSeconds)
                assertNull(exercise.timedSeconds)
                assertEquals(com.olarte.ironprogress.domain.routine.ExerciseMode.REST, exercise.mode)
                repository.saveExercise(exercise.copy(timedSeconds = 45, restSeconds = 0))
                val saved = assertNotNull(repository.observeActiveSession().first()).exercises.single()
                assertEquals(45, saved.timedSeconds)
                assertEquals(com.olarte.ironprogress.domain.routine.ExerciseMode.TIMED, saved.mode)
            } finally {
                database.close()
            }
        } finally {
            listOf("", "-wal", "-shm", "-journal", ".v3.bak").forEach { File(file.path + it).delete() }
        }
    }

    @Test
    fun noBackupForANewInstall() {
        val file = File.createTempFile("ironprogress-new", ".db").apply { delete() }
        assertNull(backupBeforeMigration(file.absolutePath))
    }

    private fun readSchema(version: Int): JsonObject {
        val file = File(schemasDirectory, "$version.json")
        assertTrue(file.exists(), "Falta el esquema exportado ${file.path}")
        return Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
    }

    /** Crea la base tal como la dejaba la versión [version], igual que MigrationTestHelper. */
    private fun createDatabaseFromSchema(file: File, version: Int, insertData: (SQLiteConnection) -> Unit) {
        val schema = readSchema(version)
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            schema.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val tableName = entity.getValue("tableName").jsonPrimitive.content
                connection.execSQL(entity.sql("createSql").replace(TABLE_NAME, tableName))
                entity["indices"]?.jsonArray?.forEach { index ->
                    connection.execSQL(index.jsonObject.sql("createSql").replace(TABLE_NAME, tableName))
                }
            }
            schema["views"]?.jsonArray?.forEach { element ->
                val view = element.jsonObject
                val viewName = view.getValue("viewName").jsonPrimitive.content
                connection.execSQL(view.sql("createSql").replace(VIEW_NAME, viewName))
            }
            schema.getValue("setupQueries").jsonArray.forEach { query ->
                connection.execSQL(query.jsonPrimitive.content)
            }
            connection.execSQL("PRAGMA user_version = $version")
            insertData(connection)
        } finally {
            connection.close()
        }
    }

    private fun JsonObject.sql(key: String): String = getValue(key).jsonPrimitive.content

    private companion object {
        const val TABLE_NAME = "\${TABLE_NAME}"
        const val VIEW_NAME = "\${VIEW_NAME}"
    }
}
