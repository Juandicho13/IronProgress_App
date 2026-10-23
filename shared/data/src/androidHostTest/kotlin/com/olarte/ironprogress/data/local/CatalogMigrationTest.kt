package com.olarte.ironprogress.data.local

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.olarte.ironprogress.domain.catalog.Muscle
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
import kotlin.test.assertTrue

/**
 * Protege a quien ya tiene la app instalada: una base creada con un esquema exportado debe abrir
 * con el código actual sin perder datos. Hoy solo existe la versión 1. Cuando llegue la 2,
 * agrega MIGRATION_1_2 al builder y una prueba igual que parta del esquema 1.
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
                    "INSERT INTO exercise_muscles (exercise_id, muscle_id, role, sort_order) " +
                        "VALUES ('mi-press-landmine', 'shoulders', 'PRIMARY', 0)",
                )
            }

            // Room compara el identityHash guardado con el del código: si no cuadran, falla aquí.
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
            } finally {
                database.close()
            }
        } finally {
            listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
        }
    }

    private fun readSchema(version: Int): JsonObject {
        val file = File(schemasDirectory, "$version.json")
        assertTrue(file.exists(), "Falta el esquema exportado ${file.path}")
        return Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
    }

    /** Crea la base tal como la dejaba la versión [version], igual que MigrationTestHelper. */
    private fun createDatabaseFromSchema(
        file: File,
        version: Int,
        insertData: (SQLiteConnection) -> Unit,
    ) {
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
