package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val uriString: String,
    val fileName: String,
    val fileSizeFormatted: String,
    val fileType: String,
    val lastOpenedTimestamp: Long = System.currentTimeMillis()
)
