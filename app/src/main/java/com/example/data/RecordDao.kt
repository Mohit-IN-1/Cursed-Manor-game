package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM survivor_records ORDER BY dateTimestamp DESC")
    fun getAllRecords(): Flow<List<SurvivorRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SurvivorRecord)

    @Query("DELETE FROM survivor_records")
    suspend fun clearAllRecords()
}
