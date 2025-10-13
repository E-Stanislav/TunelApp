package com.tunelapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for VLESS servers
 */
@Database(
    entities = [VlessServer::class],
    version = 1,
    exportSchema = false
)
abstract class VlessDatabase : RoomDatabase() {
    
    abstract fun vlessServerDao(): VlessServerDao
    
    companion object {
        @Volatile
        private var INSTANCE: VlessDatabase? = null
        
        fun getDatabase(context: Context): VlessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VlessDatabase::class.java,
                    "vless_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}



