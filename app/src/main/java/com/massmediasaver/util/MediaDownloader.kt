package com.massmediasaver.util

import android.content.Context
import com.massmediasaver.data.DownloadItem
import com.massmediasaver.data.DownloadProgress
import com.massmediasaver.data.MediaItem
import com.massmediasaver.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class MediaDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun downloadMedia(
        mediaItems: List<MediaItem>,
        onProgress: (DownloadProgress) -> Unit
    ): Flow<DownloadProgress> = flow {
        val total = mediaItems.size
        val downloadsDir = FileStorageUtil.getDownloadsDirectory(context)
        
        mediaItems.forEachIndexed { index, item ->
            try {
                val fileName = generateFileName(item)
                val file = File(downloadsDir, fileName)
                
                val request = Request.Builder()
                    .url(item.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.let { body ->
                            FileOutputStream(file).use { output ->
                                body.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        
                        emit(DownloadProgress(
                            current = index + 1,
                            total = total,
                            currentFile = fileName,
                            isComplete = index == total - 1
                        ))
                    } else {
                        emit(DownloadProgress(
                            current = index + 1,
                            total = total,
                            currentFile = fileName,
                            error = "HTTP ${response.code}"
                        ))
                    }
                }
            } catch (e: Exception) {
                emit(DownloadProgress(
                    current = index + 1,
                    total = total,
                    currentFile = item.url.substringAfterLast("/"),
                    error = e.message
                ))
            }
        }
    }

    suspend fun downloadSingleMedia(item: MediaItem): DownloadItem? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = FileStorageUtil.getDownloadsDirectory(context)
            val fileName = generateFileName(item)
            val file = File(downloadsDir, fileName)
            
            val request = Request.Builder()
                .url(item.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.let { body ->
                        FileOutputStream(file).use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    DownloadItem(
                        id = UUID.randomUUID().toString(),
                        fileName = fileName,
                        filePath = file.absolutePath,
                        url = item.url,
                        type = item.type,
                        timestamp = System.currentTimeMillis(),
                        size = file.length()
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateFileName(item: MediaItem): String {
        val urlPart = item.url.substringAfterLast("/")
            .substringBefore("?")
            .substringBefore("#")
        
        val extension = if (item.type == MediaType.IMAGE) {
            when {
                urlPart.endsWith(".jpg") || urlPart.endsWith(".jpeg") -> ".jpg"
                urlPart.endsWith(".png") -> ".png"
                urlPart.endsWith(".gif") -> ".gif"
                urlPart.endsWith(".webp") -> ".webp"
                urlPart.endsWith(".bmp") -> ".bmp"
                else -> ".jpg"
            }
        } else {
            when {
                urlPart.endsWith(".mp4") -> ".mp4"
                urlPart.endsWith(".webm") -> ".webm"
                urlPart.endsWith(".mov") -> ".mov"
                urlPart.endsWith(".avi") -> ".avi"
                urlPart.endsWith(".mkv") -> ".mkv"
                else -> ".mp4"
            }
        }
        
        val baseName = if (urlPart.contains(".")) {
            urlPart.substringBeforeLast(".")
        } else {
            UUID.randomUUID().toString().substring(0, 8)
        }
        
        val sanitizedBase = FileStorageUtil.sanitizeFileName(baseName)
        
        return FileStorageUtil.generateUniqueFileName(
            FileStorageUtil.getDownloadsDirectory(context),
            sanitizedBase,
            extension
        )
    }
}
