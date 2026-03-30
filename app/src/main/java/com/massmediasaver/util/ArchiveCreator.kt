package com.massmediasaver.util

import android.content.Context
import com.massmediasaver.data.ArchiveItem
import com.massmediasaver.data.DownloadItem
import com.massmediasaver.data.MediaItem
import com.massmediasaver.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ArchiveCreator(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun createFullArchive(
        url: String,
        html: String,
        onProgress: (String) -> Unit
    ): Flow<ArchiveItem?> = flow {
        try {
            val siteName = extractSiteName(url)
            val sanitizedName = FileStorageUtil.sanitizeFolderName(siteName)
            
            val archivesDir = FileStorageUtil.getArchivesDirectory(context)
            val archiveFolder = File(archivesDir, sanitizedName)
            
            var counter = 1
            var finalFolder = archiveFolder
            while (finalFolder.exists()) {
                finalFolder = File(archivesDir, "${sanitizedName}_$counter")
                counter++
            }
            finalFolder.mkdirs()
            
            val mediaDir = File(finalFolder, "media")
            mediaDir.mkdirs()
            
            onProgress("Extracting media URLs...")
            val mediaItems = MediaExtractor.extractMediaFromHtml(html, url)
            
            onProgress("Downloading ${mediaItems.size} media files...")
            val downloadedFiles = mutableListOf<DownloadItem>()
            
            mediaItems.forEachIndexed { index, item ->
                try {
                    val request = Request.Builder()
                        .url(item.url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .build()
                    
                    val fileName = generateMediaFileName(item)
                    val mediaFile = File(mediaDir, fileName)
                    
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.let { body ->
                                FileOutputStream(mediaFile).use { output ->
                                    body.byteStream().use { input ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            
                            downloadedFiles.add(DownloadItem(
                                id = UUID.randomUUID().toString(),
                                fileName = fileName,
                                filePath = mediaFile.absolutePath,
                                url = item.url,
                                type = item.type,
                                timestamp = System.currentTimeMillis(),
                                size = mediaFile.length()
                            ))
                        }
                    }
                    
                    onProgress("Downloaded ${index + 1}/${mediaItems.size}")
                } catch (e: Exception) {
                    onProgress("Error downloading: ${item.url}")
                }
            }
            
            onProgress("Rewriting HTML for offline...")
            val rewrittenHtml = HtmlRewriter.rewriteHtmlForOffline(
                html,
                url,
                downloadedFiles,
                mediaDir
            )
            
            onProgress("Saving index.html...")
            val indexFile = File(finalFolder, "index.html")
            indexFile.writeText(rewrittenHtml)
            
            val archiveItem = ArchiveItem(
                id = UUID.randomUUID().toString(),
                name = finalFolder.name,
                folderPath = finalFolder.absolutePath,
                url = url,
                timestamp = System.currentTimeMillis(),
                mediaCount = downloadedFiles.size
            )
            
            emit(archiveItem)
            
        } catch (e: Exception) {
            e.printStackTrace()
            emit(null)
        }
    }

    private fun extractSiteName(url: String): String {
        return try {
            val host = url.substringAfter("://").substringBefore("/")
            host.replace("www.", "")
                .substringBefore(".")
                .ifEmpty { "archive" }
        } catch (e: Exception) {
            "archive"
        }
    }

    private fun generateMediaFileName(item: MediaItem): String {
        val urlPart = item.url.substringAfterLast("/")
            .substringBefore("?")
        
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
        
        return FileStorageUtil.generateUniqueFileName(
            FileStorageUtil.getMediaDirectory(File("")),
            FileStorageUtil.sanitizeFileName(baseName),
            extension
        )
    }
}
