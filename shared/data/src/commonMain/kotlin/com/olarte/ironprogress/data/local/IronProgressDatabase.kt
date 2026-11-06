package com.olarte.ironprogress.data.local

import androidx.room3.AutoMigration
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
 *
 * Versión 3 (F1-10): agrega notes a workout_sets con valor vacío por defecto.
 * Versión 2 (F1-03): rutinas, sesiones, bloques y las tablas de la Etapa 1, más discipline,
 * level y aliases en exercises y work_share en exercise_muscles. La 1 a 2 solo agrega tablas y
 * columnas con valor por defecto, así que Room la genera (AutoMigration).
 */
@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        CatalogMetadataEntity::class,
        FolderEntity::class,
        RoutineEntity::class,
        WorkoutSessionEntity::class,
        WorkoutBlockEntity::class,
        RoutineExerciseEntity::class,
        SetTemplateEntity::class,
        SessionExerciseEntity::class,
        WorkoutSetEntity::class,
        IntervalTemplateEntity::class,
        BreathingPatternEntity::class,
        ExerciseProgressionEntity::class,
        ExerciseSourceEntity::class,
        ExerciseDemandEntity::class,
        ScreeningAnswerEntity::class,
        GuardianConsentEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@ConstructedBy(IronProgressDatabaseConstructor::class)
abstract class IronProgressDatabase : RoomDatabase() {

    abstract fun catalogDao(): CatalogDao

    abstract fun routineDao(): RoutineDao

    abstract fun sessionDao(): SessionDao

    abstract fun stageOneDao(): StageOneDao

    companion object {
        const val SCHEMA_VERSION = 4
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
): IronProgressDatabase = this
    .setDriver(driver)
    .setQueryCoroutineContext(queryDispatcher)
    .build()
