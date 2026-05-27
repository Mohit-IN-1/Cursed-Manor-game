package com.example.data

import kotlinx.coroutines.flow.Flow

class RecordRepository(private val recordDao: RecordDao) {
    val allRecords: Flow<List<SurvivorRecord>> = recordDao.getAllRecords()

    suspend fun insert(record: SurvivorRecord) {
        recordDao.insertRecord(record)
    }

    suspend fun clearAll() {
        recordDao.clearAllRecords()
    }
}
