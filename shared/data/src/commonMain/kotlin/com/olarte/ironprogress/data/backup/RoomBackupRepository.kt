package com.olarte.ironprogress.data.backup

import androidx.room3.PooledConnection
import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
import com.olarte.ironprogress.data.catalog.RoomExerciseRepository
import com.olarte.ironprogress.data.local.IronProgressDatabase
import com.olarte.ironprogress.domain.backup.BackupRepository
import com.olarte.ironprogress.domain.backup.RestoreResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * Copia de seguridad sobre las tablas de Room (F1-24). Lee cada tabla con sus columnas reales
 * (PRAGMA table_info), así una columna nueva entra sola en la copia y una copia vieja se
 * restaura con las columnas que tenga. Del catálogo solo van los ejercicios propios y los
 * archivados; la semilla la trae la app.
 */
class RoomBackupRepository(
    private val database: IronProgressDatabase,
    private val clock: Clock = Clock.System,
    /** Siembra el catálogo antes de restaurar, para que el archivado de la semilla se aplique. */
    private val catalog: RoomExerciseRepository = RoomExerciseRepository(database.catalogDao()),
) : BackupRepository {

    override suspend fun createBackup(): String = database.useReaderConnection { connection ->
        val tables = buildJsonObject {
            USER_TABLES.forEach { table -> put(table, JsonArray(connection.rows("SELECT * FROM $table"))) }
            CATALOG_TABLES.forEach { (table, filter) -> put(table, JsonArray(connection.rows("SELECT * FROM $table WHERE $filter"))) }
        }
        val backup = buildJsonObject {
            put("format", JsonPrimitive(FORMAT))
            put("version", JsonPrimitive(VERSION))
            put("schemaVersion", JsonPrimitive(IronProgressDatabase.SCHEMA_VERSION))
            put("createdAt", JsonPrimitive(clock.now().toEpochMilliseconds()))
            put("tables", tables)
        }
        json.encodeToString(JsonObject.serializer(), backup)
    }

    override suspend fun restore(json: String): RestoreResult {
        val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return RestoreResult.NotABackup
        if (root["format"]?.jsonPrimitive?.contentOrNull != FORMAT) return RestoreResult.NotABackup
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: return RestoreResult.NotABackup
        if (schema > IronProgressDatabase.SCHEMA_VERSION || (root["version"]?.jsonPrimitive?.intOrNull ?: 0) > VERSION) {
            return RestoreResult.TooNew
        }
        val tables = runCatching { root["tables"]!!.jsonObject }.getOrNull() ?: return RestoreResult.NotABackup
        val rowsByTable = runCatching {
            (USER_TABLES + CATALOG_TABLES.map { it.first }).associateWith { table ->
                tables[table]?.jsonArray?.map { it.jsonObject }.orEmpty()
            }
        }.getOrNull() ?: return RestoreResult.NotABackup

        catalog.ensureSeeded()
        return runCatching {
            database.withWriteTransaction {
                executeSQL("PRAGMA defer_foreign_keys = ON")
                USER_TABLES.reversed().forEach { executeSQL("DELETE FROM $it") }
                CATALOG_TABLES.drop(1).reversed().forEach { (table, filter) -> executeSQL("DELETE FROM $table WHERE $filter") }
                // Solo se borran los ejercicios propios; los de la semilla se quedan y solo cambia su archivado.
                executeSQL("DELETE FROM exercises WHERE is_custom = 1")
                executeSQL("UPDATE exercises SET is_archived = 0")
                var count = 0
                (CATALOG_TABLES.map { it.first } + USER_TABLES).forEach { table ->
                    val columns = columnsOf(table)
                    rowsByTable.getValue(table).forEach { row ->
                        if (table == "exercises" && row["is_custom"]?.jsonPrimitive?.longOrNull == 0L) {
                            archiveSeed(row)
                        } else {
                            insert(table, columns, row)
                        }
                        count++
                    }
                }
                RestoreResult.Restored(count)
            }
        }.getOrElse { RestoreResult.NotABackup }
    }

    override suspend fun exportSetsCsv(languageCode: String): String = database.useReaderConnection { connection ->
        val name = if (languageCode.startsWith("es")) "e.name_es" else "e.name_en"
        val sql = """
            SELECT s.started_at, COALESCE(r.name, ''), $name, ws.sort_order, ws.set_type, ws.weight_kg, ws.reps,
                ws.duration_seconds, ws.distance_meters, ws.calories, ws.rir, ws.rpe, ws.completed_at
            FROM workout_sets ws
            JOIN session_exercises se ON se.id = ws.session_exercise_id
            JOIN workout_sessions s ON s.id = se.session_id
            LEFT JOIN routines r ON r.id = s.routine_id
            LEFT JOIN exercises e ON e.id = se.exercise_id
            WHERE ws.completed_at IS NOT NULL AND ws.deleted_at IS NULL AND s.deleted_at IS NULL
            ORDER BY s.started_at, se.sort_order, ws.sort_order
        """.trimIndent()
        val lines = mutableListOf(CSV_HEADER)
        connection.usePrepared(sql) { statement ->
            while (statement.step()) {
                lines += (0 until statement.getColumnCount()).joinToString(",") { csvCell(statement, it) }
            }
        }
        lines.joinToString("\n", postfix = "\n")
    }

    private suspend fun PooledConnection.rows(sql: String): List<JsonObject> = usePrepared(sql) { statement ->
        val names = statement.getColumnNames()
        buildList {
            while (statement.step()) {
                add(JsonObject(names.indices.associate { names[it] to statement.valueAt(it) }))
            }
        }
    }

    private suspend fun PooledConnection.columnsOf(table: String): Set<String> = usePrepared("PRAGMA table_info($table)") { statement ->
        val nameIndex = statement.getColumnNames().indexOf("name")
        buildSet { while (statement.step()) add(statement.getText(nameIndex)) }
    }

    private suspend fun PooledConnection.insert(table: String, columns: Set<String>, row: JsonObject) {
        val values = row.filterKeys { it in columns }.entries.toList()
        if (values.isEmpty()) return
        val sql = "INSERT OR REPLACE INTO $table (${values.joinToString { it.key }}) VALUES (${values.joinToString { "?" }})"
        usePrepared(sql) { statement ->
            values.forEachIndexed { index, (_, value) -> statement.bind(index + 1, value) }
            statement.step()
        }
    }

    private suspend fun PooledConnection.archiveSeed(row: JsonObject) {
        val id = row["id"]?.jsonPrimitive?.contentOrNull ?: return
        usePrepared("UPDATE exercises SET is_archived = 1 WHERE id = ?") { statement ->
            statement.bindText(1, id)
            statement.step()
        }
    }

    private fun SQLiteStatement.valueAt(index: Int): JsonElement = when (getColumnType(index)) {
        SQLITE_DATA_NULL -> JsonNull
        SQLITE_DATA_INTEGER -> JsonPrimitive(getLong(index))
        SQLITE_DATA_FLOAT -> JsonPrimitive(getDouble(index))
        else -> JsonPrimitive(getText(index))
    }

    private fun SQLiteStatement.bind(index: Int, value: JsonElement) {
        val primitive = value as? JsonPrimitive
        when {
            primitive == null || primitive is JsonNull -> bindNull(index)
            primitive.isString -> bindText(index, primitive.content)
            primitive.longOrNull != null -> bindLong(index, primitive.longOrNull!!)
            primitive.doubleOrNull != null -> bindDouble(index, primitive.doubleOrNull!!)
            else -> bindText(index, primitive.content)
        }
    }

    private fun csvCell(statement: SQLiteStatement, index: Int): String {
        if (statement.isNull(index)) return ""
        return when (index) {
            0, 12 -> kotlin.time.Instant.fromEpochMilliseconds(statement.getLong(index)).toString()
            else -> statement.getText(index)
        }.let { text -> if (text.any { it == ',' || it == '"' || it == '\n' }) "\"" + text.replace("\"", "\"\"") + "\"" else text }
    }

    companion object {
        const val FORMAT = "ironprogress-backup"
        const val VERSION = 1

        /** Tablas del usuario, de padres a hijos (así se insertan; se borran al revés). */
        val USER_TABLES = listOf(
            "folders",
            "routines",
            "workout_sessions",
            "workout_blocks",
            "routine_exercises",
            "set_templates",
            "session_exercises",
            "workout_sets",
            "interval_templates",
            "breathing_patterns",
            "screening_answers",
            "guardian_consents",
        )

        private const val OWN = "exercise_id IN (SELECT id FROM exercises WHERE is_custom = 1)"

        /** Del catálogo: los ejercicios propios con sus datos y los de la semilla archivados. */
        val CATALOG_TABLES = listOf(
            "exercises" to "is_custom = 1 OR is_archived = 1",
            "exercise_muscles" to OWN,
            "exercise_demands" to OWN,
            "exercise_sources" to OWN,
            "exercise_progressions" to OWN,
        )

        private const val CSV_HEADER =
            "session_started_at,routine,exercise,set_order,set_type,weight_kg,reps,duration_seconds,distance_meters," +
                "calories,rir,rpe,completed_at"

        private val json = Json { prettyPrint = false }
    }
}
