package com.example.model

import android.net.Uri

enum class FileType(val label: String, val extension: String) {
    XML("XML", ".xml"),
    HTML("HTML", ".html"),
    UNKNOWN("TEXT", ".txt")
}

data class OpenFileInfo(
    val uri: Uri?,
    val fileName: String,
    val fileSizeFormatted: String,
    val fileType: FileType,
    val isReadOnly: Boolean = false
)

enum class ViewMode {
    HOME,
    READER,
    EDITOR
}
