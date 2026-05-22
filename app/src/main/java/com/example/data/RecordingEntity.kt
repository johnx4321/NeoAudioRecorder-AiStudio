package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val format: String,
    val timestamp: Long,
    val isFavorite: Boolean = false
)
