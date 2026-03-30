package com.massmediasaver.util

import com.massmediasaver.data.MediaItem
import com.massmediasaver.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import java.net.URLDecoder

object HtmlRewriter {

    private const val MEDIA_FOLDER = "media"

    suspend fun rewriteHtmlForOffline(
        html: String,
        baseUrl: String,
        mediaItems: List<MediaItem>,
        mediaFolder: File
    ): String = withContext(Dispatchers.Default) {
        try {
            val document: Document = Jsoup.parse(html, baseUrl)
            
            rewriteImgTags(document, mediaItems)
            rewriteVideoTags(document, mediaItems)
            rewriteBackgroundImages(document, mediaItems)
            
            addLocalStyles(document)
            
            document.html()
        } catch (e: Exception) {
            e.printStackTrace()
            html
        }
    }

    private fun rewriteImgTags(doc: Document, mediaItems: List<MediaItem>) {
        val images: Elements = doc.select("img[src], img[data-src], img[data-original], img[data-lazy-src]")
        
        for (img in images) {
            var originalUrl = img.attr("data-original")
            if (originalUrl.isEmpty()) originalUrl = img.attr("data-src")
            if (originalUrl.isEmpty()) originalUrl = img.attr("data-lazy-src")
            if (originalUrl.isEmpty()) originalUrl = img.attr("src")
            
            if (originalUrl.isNotEmpty() && originalUrl.startsWith("http")) {
                val localFile = findLocalFileForUrl(originalUrl, mediaItems, MediaType.IMAGE)
                if (localFile != null) {
                    img.attr("src", localFile)
                    img.removeAttr("data-src")
                    img.removeAttr("data-original")
                    img.removeAttr("data-lazy-src")
                    img.removeAttr("srcset")
                }
            }
        }
    }

    private fun rewriteVideoTags(doc: Document, mediaItems: List<MediaItem>) {
        val videos: Elements = doc.select("video[src], video source[src], video source[data-src]")
        
        for (video in videos) {
            if (video.tagName() == "source") {
                var videoUrl = video.attr("data-src")
                if (videoUrl.isEmpty()) videoUrl = video.attr("src")
                
                if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                    val localFile = findLocalFileForUrl(videoUrl, mediaItems, MediaType.VIDEO)
                    if (localFile != null) {
                        video.attr("src", localFile)
                        video.removeAttr("data-src")
                    }
                }
            } else {
                var videoUrl = video.attr("data-src")
                if (videoUrl.isEmpty()) videoUrl = video.attr("src")
                
                if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                    val localFile = findLocalFileForUrl(videoUrl, mediaItems, MediaType.VIDEO)
                    if (localFile != null) {
                        video.attr("src", localFile)
                        video.attr("data-src", "")
                    }
                }
            }
        }
        
        val iframes: Elements = doc.select("iframe[src]")
        for (iframe in iframes) {
            val src = iframe.attr("src")
            if (src.contains("youtube") || src.contains("vimeo") || src.contains("dailymotion")) {
                iframe.removeAttr("src")
                iframe.attr("data-offline", "removed")
            }
        }
    }

    private fun rewriteBackgroundImages(doc: Document, mediaItems: List<MediaItem>) {
        val elementsWithBg: Elements = doc.select("[style*=background]")
        
        for (element in elementsWithBg) {
            val style = element.attr("style")
            var newStyle = style
            
            val urlPattern = Regex("url\\(['\"]?(http[^'\"\\)]+)['\"]?\\)")
            val matches = urlPattern.findAll(style)
            
            for (match in matches) {
                val bgUrl = match.groupValues[1]
                val localFile = findLocalFileForUrl(bgUrl, mediaItems, MediaType.IMAGE)
                if (localFile != null) {
                    newStyle = newStyle.replace(match.value, "url('$localFile')")
                }
            }
            
            if (newStyle != style) {
                element.attr("style", newStyle)
            }
        }
    }

    private fun findLocalFileForUrl(originalUrl: String, mediaItems: List<MediaItem>, type: com.massmediasaver.data.MediaType): String? {
        val cleanUrl = cleanUrl(originalUrl)
        
        for (item in mediaItems) {
            if (item.type == type) {
                val itemUrl = cleanUrl(item.url)
                if (itemUrl == cleanUrl || itemUrl.contains(cleanUrl.substringAfter("://").substringBefore("?"))) {
                    val fileName = item.url.substringAfterLast("/")
                    return "$MEDIA_FOLDER/$fileName"
                }
            }
        }
        
        val fileName = originalUrl.substringAfterLast("/").substringBefore("?")
        return "$MEDIA_FOLDER/$fileName"
    }

    private fun cleanUrl(url: String): String {
        return try {
            URLDecoder.decode(url, "UTF-8")
                .substringBefore("?")
                .substringBefore("#")
        } catch (e: Exception) {
            url
        }
    }

    private fun addLocalStyles(doc: Document) {
        val style = doc.createElement("style")
        style.text("""
            body { padding: 10px; }
            img { max-width: 100%; height: auto; }
            video { max-width: 100%; height: auto; }
            iframe { display: none; }
        """.trimIndent())
        
        doc.head().appendChild(style)
    }
}
