package com.massmediasaver.util

import android.content.Context
import android.os.Environment
import java.io.File

object FileStorageUtil {
    
    private const val APP_FOLDER = "MassMediaSaver"
    private const val DOWNLOADS_FOLDER = "Downloads"
    private const val ARCHIVES_FOLDER = "Archives"
    private const val MEDIA_FOLDER = "media"

    fun getAppDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), APP_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDownloadsDirectory(context: Context): File {
        val dir = File(getAppDirectory(context), DOWNLOADS_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getArchivesDirectory(context: Context): File {
        val dir = File(getAppDirectory(context), ARCHIVES_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getMediaDirectory(archiveFolder: File): File {
        val dir = File(archiveFolder, MEDIA_FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeFileName(name: String): String {
        val invalidChars = Regex("[\\\\/:*?\"<>|]")
        var sanitized = name.replace(invalidChars, "_")
        if (sanitized.length > 100) {
            sanitized = sanitized.substring(0, 100)
        }
        return sanitized
    }

    fun sanitizeFolderName(name: String): String {
        val invalidChars = Regex("[\\\\/:*?\"<>|]")
        var sanitized = name.replace(invalidChars, "_")
        sanitized = sanitized.replace(".", "_")
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }
        return sanitized
    }

    fun getFileExtension(url: String): String {
        return try {
            val path = url.substringBefore("?")
            val ext = path.substringAfterLast(".").lowercase()
            when {
                ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> ".$ext"
                ext in listOf("mp4", "webm", "mov", "avi", "mkv") -> ".$ext"
                ext == "jpeg" -> ".jpg"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun generateUniqueFileName(directory: File, baseName: String, extension: String): String {
        var fileName = "$baseName$extension"
        var counter = 1
        while (File(directory, fileName).exists()) {
            fileName = "${baseName}_$counter$extension"
            counter++
        }
        return fileName
    }

    fun isValidMediaUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("http") && (
            lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
            lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
            lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".bmp") ||
            lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
            lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".avi") ||
            lowerUrl.endsWith(".mkv") ||
            "image" in lowerUrl || "video" in lowerUrl ||
            "media" in lowerUrl || "cdn" in lowerUrl ||
            "img" in lowerUrl || "pic" in lowerUrl
        )
    }
}
