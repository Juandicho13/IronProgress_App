package com.olarte.ironprogress.data.local

import android.content.Context
import androidx.room3.Room

/**
 * Abre la base de IronProgress en la carpeta de bases de datos de la app.
 * Llámala una sola vez (con Koin será un single) y comparte la instancia.
 */
fun createIronProgressDatabase(context: Context): IronProgressDatabase {
    val appContext = context.applicationContext
    val databaseFile = appContext.getDatabasePath(IronProgressDatabase.DATABASE_NAME)
    return Room.databaseBuilder<IronProgressDatabase>(
        context = appContext,
        name = databaseFile.absolutePath,
        factory = { IronProgressDatabaseConstructor.initialize() },
    ).buildIronProgressDatabase()
}
