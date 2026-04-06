package com.gastop.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gastop.app.data.model.Categoria
import com.gastop.app.data.model.Transaccion
import com.gastop.app.data.model.Usuario

@Database(entities = [Usuario::class, Categoria::class, Transaccion::class], version = 2, exportSchema = false)
abstract class GastopDatabase : RoomDatabase() {
    abstract fun gastopDao(): GastopDao

    companion object {
        @Volatile
        private var INSTANCE: GastopDatabase? = null

        fun getDatabase(context: Context): GastopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastopDatabase::class.java,
                    "gastop_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
