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
 * la 3, agrega una prueba igual que parta del esquema 2.
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
