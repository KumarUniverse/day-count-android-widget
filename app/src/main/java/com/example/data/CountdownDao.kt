package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {

  @Query("SELECT * FROM countdown_timers ORDER BY createdAt DESC")
  fun getAllTimers(): Flow<List<CountdownTimer>>

  @Query("SELECT * FROM countdown_timers WHERE id = :id LIMIT 1")
  suspend fun getTimerByIdDirect(id: Long): CountdownTimer?

  @Query("SELECT * FROM countdown_timers ORDER BY createdAt DESC LIMIT 1")
  suspend fun getFirstTimerDirect(): CountdownTimer?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTimer(timer: CountdownTimer): Long

  @Update
  suspend fun updateTimer(timer: CountdownTimer)

  @Delete
  suspend fun deleteTimer(timer: CountdownTimer)

  @Query("SELECT COUNT(*) FROM countdown_timers")
  suspend fun getCount(): Int
}
