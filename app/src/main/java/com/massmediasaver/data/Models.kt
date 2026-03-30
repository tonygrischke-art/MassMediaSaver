package com.massmediasaver.data

data class MediaItem(
    val url: String,
    val type: MediaType,
    val originalUrl: String = url
)

enum class MediaType {
    IMAGE, VIDEO
}

data class DownloadItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val url: String,
    val type: MediaType,
    val timestamp: Long,
    val size: Long = 0
)

data class ArchiveItem(
    val id: String,
    val name: String,
    val folderPath: String,
    val url: String,
    val timestamp: Long,
    val mediaCount: Int = 0
)

data class DownloadProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
    val isComplete: Boolean = false,
    val error: String? = null
)
