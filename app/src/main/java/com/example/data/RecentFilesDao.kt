package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFilesDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedTimestamp DESC")
    fun getAllRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(file: RecentFileEntity)

    @Delete
    suspend fun delete(file: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}
