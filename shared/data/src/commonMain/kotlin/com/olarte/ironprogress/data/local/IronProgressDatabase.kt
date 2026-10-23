package com.olarte.ironprogress.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        CatalogMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(IronProgressDatabaseConstructor::class)
abstract class IronProgressDatabase : RoomDatabase() {

    abstract fun catalogDao(): CatalogDao

    companion object {
        const val SCHEMA_VERSION = 1
        const val DATABASE_NAME = "ironprogress.db"
    }
}

/**
 * Constructor generado por KSP para Room en Kotlin Multiplatform.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object IronProgressDatabaseConstructor : RoomDatabaseConstructor<IronProgressDatabase> {
    override fun initialize(): IronProgressDatabase
}

/**
 * Configura el builder de Room con el driver SQLite empaquetado y el hilo de disco.
 */
fun RoomDatabase.Builder<IronProgressDatabase>.configureForjaDatabase(
    driver: SQLiteDriver = BundledSQLiteDriver(),
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
): IronProgressDatabase =
    this
        .setDriver(driver)
        .setQueryCoroutineContext(queryDispatcher)
        .build()