package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedColorDao {
    @Query("SELECT * FROM captured_colors ORDER BY capturedAt DESC LIMIT 100")
    fun observeAll(): Flow<List<CapturedColorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CapturedColorEntity): Long

    @Query("DELETE FROM captured_colors WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captured_colors")
    suspend fun clearAll()
}
