package com.example.ironprogress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ⚠️ IMPORTANTE: Asegúrate de que las 3 entidades estén aquí dentro de los corchetes []
@Database(
    entities = [
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        ExerciseEntity::class
    ],
    version = 6, // Subimos la versión por si acaso
    exportSchema = false
)
abstract class IronDatabase : RoomDatabase() {
    abstract fun ironDao(): IronDao

    companion object {
        @Volatile
        private var INSTANCE: IronDatabase? = null

        fun getDatabase(context: Context): IronDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IronDatabase::class.java,
                    "iron_database"
                )
                    .fallbackToDestructiveMigration() // Esto permite borrar la BD vieja si cambia la versión
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}