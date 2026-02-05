package com.example.ironprogress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RoutineEntity::class, RoutineExerciseEntity::class, ExerciseEntity::class],
    version = 5, // 🚀 VERSION 5 OBLIGATORIA
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}