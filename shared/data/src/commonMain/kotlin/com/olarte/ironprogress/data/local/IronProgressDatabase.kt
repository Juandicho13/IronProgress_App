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

/**
 * Base local de IronProgress. Si cambias una entidad, sube [SCHEMA_VERSION] y version a la vez,
 * agrega la migración y amplía CatalogMigrationTest con el esquema anterior.
 */
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
 * KSP genera el actual de este objeto en cada plataforma (Android, iosArm64 e iosSimulatorArm64).
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object IronProgressDatabaseConstructor : RoomDatabaseConstructor<IronProgressDatabase> {
    override fun initialize(): IronProgressDatabase
}

/**
 * Termina de armar la base con el SQLite empaquetado (el mismo en Android e iOS)
 * y las consultas fuera del hilo principal.
 */
fun RoomDatabase.Builder<IronProgressDatabase>.buildIronProgressDatabase(
    driver: SQLiteDriver = BundledSQLiteDriver(),
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
): IronProgressDatabase =
    this
        .setDriver(driver)
        .setQueryCoroutineContext(queryDispatcher)
        .build()