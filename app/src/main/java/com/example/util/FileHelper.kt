package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.model.FileType
import com.example.model.OpenFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.DecimalFormat

object FileHelper {

    fun tryTakePersistableUriPermission(context: Context, uri: Uri) {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
    }

    suspend fun readFile(context: Context, uri: Uri): Result<Pair<OpenFileInfo, String>> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val fileInfo = getFileInfo(context, uri)

                var inputStream: InputStream? = null
                var parcelFd: ParcelFileDescriptor? = null

                try {
                    inputStream = resolver.openInputStream(uri)
                } catch (se: SecurityException) {
                    try {
                        parcelFd = resolver.openFileDescriptor(uri, "r")
                        if (parcelFd != null) {
                            inputStream = FileInputStream(parcelFd.fileDescriptor)
                        }
                    } catch (_: Exception) {
                        if (uri.scheme == "file" && uri.path != null) {
                            val file = File(uri.path!!)
                            if (file.exists() && file.canRead()) {
                                inputStream = FileInputStream(file)
                            }
                        }
                        if (inputStream == null) throw se
                    }
                } catch (e: Exception) {
                    try {
                        parcelFd = resolver.openFileDescriptor(uri, "r")
                        if (parcelFd != null) {
                            inputStream = FileInputStream(parcelFd.fileDescriptor)
                        }
                    } catch (_: Exception) {
                        if (uri.scheme == "file" && uri.path != null) {
                            val file = File(uri.path!!)
                            if (file.exists() && file.canRead()) {
                                inputStream = FileInputStream(file)
                            }
                        }
                    }
                }

                if (inputStream == null) {
                    return@withContext Result.failure(
                        Exception("Could not open file stream for $uri. Please verify permissions or open using the file picker.")
                    )
                }

                try {
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        val stringBuilder = StringBuilder()
                        val buffer = CharArray(16384)
                        var charsRead: Int
                        while (reader.read(buffer).also { charsRead = it } != -1) {
                            stringBuilder.append(buffer, 0, charsRead)
                        }
                        Result.success(Pair(fileInfo, stringBuilder.toString()))
                    }
                } finally {
                    try {
                        parcelFd?.close()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun saveFile(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var outputStream = resolver.openOutputStream(uri, "wt")
                if (outputStream == null && uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    outputStream = FileOutputStream(file)
                }

                outputStream?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                        writer.flush()
                    }
                    Result.success(Unit)
                } ?: Result.failure(Exception("Cannot open stream for writing"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getFileInfo(context: Context, uri: Uri): OpenFileInfo {
        val resolver = context.contentResolver
        var displayName: String? = null
        var size: Long = -1

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                            displayName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment
            if (displayName.isNullOrBlank()) {
                displayName = "document"
            }
        }

        val type = when {
            displayName.endsWith(".xml", ignoreCase = true) -> FileType.XML
            displayName.endsWith(".html", ignoreCase = true) ||
            displayName.endsWith(".htm", ignoreCase = true) ||
            displayName.endsWith(".xhtml", ignoreCase = true) -> FileType.HTML
            else -> FileType.UNKNOWN
        }

        val formattedSize = if (size >= 0) formatFileSize(size) else "Document"
        return OpenFileInfo(
            uri = uri,
            fileName = displayName,
            fileSizeFormatted = formattedSize,
            fileType = type
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
