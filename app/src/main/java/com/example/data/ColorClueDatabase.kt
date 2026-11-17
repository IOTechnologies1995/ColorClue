package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CapturedColorEntity::class], version = 1, exportSchema = false)
abstract class ColorClueDatabase : RoomDatabase() {
    abstract fun capturedColorDao(): CapturedColorDao

    companion object {
        @Volatile
        private var INSTANCE: ColorClueDatabase? = null

        fun getInstance(context: Context): ColorClueDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ColorClueDatabase::class.java,
                    "colorclue_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
