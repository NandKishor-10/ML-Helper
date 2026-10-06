package com.example.data

import kotlinx.coroutines.flow.Flow

class RecentFilesRepository(private val dao: RecentFilesDao) {
    val recentFiles: Flow<List<RecentFileEntity>> = dao.getAllRecentFiles()

    suspend fun recordFileOpened(
        uriString: String,
        fileName: String,
        fileSizeFormatted: String,
        fileType: String
    ) {
        dao.insertOrUpdate(
            RecentFileEntity(
                uriString = uriString,
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted,
                fileType = fileType,
                lastOpenedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeRecentFile(uriString: String) {
        dao.deleteByUri(uriString)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
