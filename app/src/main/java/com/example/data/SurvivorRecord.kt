package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survivor_records")
data class SurvivorRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stageReached: Int,
    val artifactsCollected: Int,
    val timeSurvivedSeconds: Long,
    val isEscape: Boolean,
    val deathReason: String,
    val dateTimestamp: Long = System.currentTimeMillis()
)
