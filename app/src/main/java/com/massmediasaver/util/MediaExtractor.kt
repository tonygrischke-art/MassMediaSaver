package com.massmediasaver.util

import com.massmediasaver.data.MediaItem
import com.massmediasaver.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL
import java.util.regex.Pattern

object MediaExtractor {

    suspend fun extractMediaFromHtml(html: String, baseUrl: String): List<MediaItem> = withContext(Dispatchers.Default) {
        val mediaItems = mutableSetOf<MediaItem>()
        
        try {
            val document: Document = Jsoup.parse(html, baseUrl)
            
            extractImages(document, baseUrl, mediaItems)
            extractVideos(document, baseUrl, mediaItems)
            extractBackgroundImages(document, baseUrl, mediaItems)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mediaItems.toList()
    }

    private fun extractImages(doc: Document, baseUrl: String, mediaItems: MutableSet<MediaItem>) {
        val images: Elements = doc.select("img[src], img[data-src], img[data-original], img[data-lazy-src], img[srcset]")
        
        for (img in images) {
            var highestResUrl: String? = null
            
            val srcset = img.attr("srcset")
            if (srcset.isNotEmpty()) {
                highestResUrl = getHighestResFromSrcset(srcset)
            }
            
            if (highestResUrl.isNullOrEmpty()) {
                highestResUrl = img.attr("data-original")
            }
            if (highestResUrl.isNullOrEmpty()) {
                highestResUrl = img.attr("data-src")
            }
            if (highestResUrl.isNullOrEmpty()) {
                highestResUrl = img.attr("data-lazy-src")
            }
            if (highestResUrl.isNullOrEmpty()) {
                highestResUrl = img.attr("src")
            }
            
            if (!highestResUrl.isNullOrEmpty()) {
                val absoluteUrl = resolveUrl(baseUrl, highestResUrl)
                if (isValidMediaUrl(absoluteUrl)) {
                    mediaItems.add(MediaItem(
                        url = absoluteUrl,
                        type = MediaType.IMAGE,
                        originalUrl = absoluteUrl
                    ))
                }
            }
        }
    }

    private fun extractVideos(doc: Document, baseUrl: String, mediaItems: MutableSet<MediaItem>) {
        val videos: Elements = doc.select("video[src], video source[src], video source[data-src], video[data-src]")
        
        for (video in videos) {
            var videoUrl: String? = null
            
            if (video.tagName() == "source") {
                videoUrl = video.attr("data-src")
                if (videoUrl.isNullOrEmpty()) {
                    videoUrl = video.attr("src")
                }
                val parent = video.parent()
                if (parent != null && parent.tagName() == "video") {
                    if (videoUrl.isNullOrEmpty()) {
                        videoUrl = parent.attr("data-src")
                    }
                    if (videoUrl.isNullOrEmpty()) {
                        videoUrl = parent.attr("src")
                    }
                }
            } else {
                videoUrl = video.attr("data-src")
                if (videoUrl.isNullOrEmpty()) {
                    videoUrl = video.attr("src")
                }
            }
            
            if (!videoUrl.isNullOrEmpty()) {
                val absoluteUrl = resolveUrl(baseUrl, videoUrl)
                if (isValidMediaUrl(absoluteUrl)) {
                    mediaItems.add(MediaItem(
                        url = absoluteUrl,
                        type = MediaType.VIDEO,
                        originalUrl = absoluteUrl
                    ))
                }
            }
        }

        val iframes: Elements = doc.select("iframe[src]")
        for (iframe in iframes) {
            val src = iframe.attr("src")
            if (src.contains("youtube") || src.contains("vimeo") || src.contains("dailymotion")) {
                val absoluteUrl = resolveUrl(baseUrl, src)
                mediaItems.add(MediaItem(
                    url = absoluteUrl,
                    type = MediaType.VIDEO,
                    originalUrl = absoluteUrl
                ))
            }
        }
    }

    private fun extractBackgroundImages(doc: Document, baseUrl: String, mediaItems: MutableSet<MediaItem>) {
        val elementsWithBg: Elements = doc.select("[style*=background]")
        
        for (element in elementsWithBg) {
            val style = element.attr("style")
            val urlPattern = Pattern.compile("url\\(['\"]?([^'\"\\)]+)['\"]?\\)")
            val matcher = urlPattern.matcher(style)
            
            while (matcher.find()) {
                val bgUrl = matcher.group(1)
                if (bgUrl.isNotEmpty() && !bgUrl.startsWith("data:")) {
                    val absoluteUrl = resolveUrl(baseUrl, bgUrl)
                    if (isValidImageUrl(absoluteUrl)) {
                        mediaItems.add(MediaItem(
                            url = absoluteUrl,
                            type = MediaType.IMAGE,
                            originalUrl = absoluteUrl
                        ))
                    }
                }
            }
        }
    }

    private fun getHighestResFromSrcset(srcset: String): String? {
        try {
            val sources = srcset.split(",").map { it.trim() }
            if (sources.isEmpty()) return null
            
            var highestResUrl: String? = null
            var highestWidth = 0
            
            for (source in sources) {
                val parts = source.split(Regex("\\s+"))
                if (parts.isNotEmpty()) {
                    val url = parts[0]
                    var width = 0
                    if (parts.size >= 2) {
                        val descriptor = parts[parts.size - 1]
                        if (descriptor.endsWith("w")) {
                            width = descriptor.dropLast(1).toIntOrNull() ?: 0
                        } else if (descriptor.endsWith("x")) {
                            width = (descriptor.dropLast(1).toDoubleOrNull() ?: 1.0 * 1000).toInt()
                        }
                    }
                    
                    if (width >= highestWidth) {
                        highestWidth = width
                        highestResUrl = url
                    }
                }
            }
            
            return highestResUrl
        } catch (e: Exception) {
            return null
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else if (relativeUrl.startsWith("//")) {
                "https:$relativeUrl"
            } else {
                val base = URL(baseUrl)
                if (relativeUrl.startsWith("/")) {
                    "${base.protocol}://${base.host}${relativeUrl}"
                } else {
                    "${base.protocol}://${base.host}/${relativeUrl}"
                }
            }
        } catch (e: Exception) {
            relativeUrl
        }
    }

    private fun isValidMediaUrl(url: String): Boolean {
        if (!url.startsWith("http")) return false
        val lower = url.lowercase()
        return lower.contains("image") || lower.contains("photo") ||
               lower.contains("img") || lower.contains("pic") ||
               lower.contains("media") || lower.contains("cdn") ||
               lower.contains("video") || lower.contains("stream") ||
               lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp") ||
               lower.endsWith(".mp4") || lower.endsWith(".webm") ||
               lower.endsWith(".mov") || lower.endsWith(".avi") ||
               lower.endsWith(".mkv")
    }

    private fun isValidImageUrl(url: String): Boolean {
        if (!url.startsWith("http")) return false
        val lower = url.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp") ||
               lower.contains("image") || lower.contains("photo") ||
               lower.contains("img") || lower.contains("pic")
    }
}
