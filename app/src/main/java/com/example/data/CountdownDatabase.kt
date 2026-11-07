package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CountdownTimer::class], version = 5, exportSchema = false)
abstract class CountdownDatabase : RoomDatabase() {

  abstract fun countdownDao(): CountdownDao

  companion object {
    @Volatile
    private var INSTANCE: CountdownDatabase? = null

    fun getDatabase(context: Context): CountdownDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          CountdownDatabase::class.java,
          "countdown_timer_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
