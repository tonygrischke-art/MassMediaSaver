package com.massmediasaver.viewmodel

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.massmediasaver.data.DownloadProgress
import com.massmediasaver.data.DownloadItem
import com.massmediasaver.data.MediaItem
import com.massmediasaver.util.ArchiveCreator
import com.massmediasaver.util.MediaDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _htmlContent = MutableStateFlow("")
    val htmlContent: StateFlow<String> = _htmlContent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _downloadComplete = MutableStateFlow<DownloadItem?>(null)
    val downloadComplete: StateFlow<DownloadItem?> = _downloadComplete.asStateFlow()

    private val _archiveProgress = MutableStateFlow<String?>(null)
    val archiveProgress: StateFlow<String?> = _archiveProgress.asStateFlow()

    private val _archiveComplete = MutableStateFlow(false)
    val archiveComplete: StateFlow<Boolean> = _archiveComplete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val mediaDownloader = MediaDownloader(application)
    private val archiveCreator = ArchiveCreator(application)

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun updateTitle(title: String) {
        _pageTitle.value = title
    }

    fun updateHtml(html: String) {
        _htmlContent.value = html
    }

    fun updateLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(message: String?) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun extractMediaFromPage(html: String, baseUrl: String) {
        viewModelScope.launch {
            try {
                val items = com.massmediasaver.util.MediaExtractor.extractMediaFromHtml(html, baseUrl)
                _mediaItems.value = items
            } catch (e: Exception) {
                _error.value = "Failed to extract media: ${e.message}"
            }
        }
    }

    fun downloadAllMedia() {
        val items = _mediaItems.value
        if (items.isEmpty()) {
            _error.value = "No media found on this page"
            return
        }

        viewModelScope.launch {
            _downloadProgress.value = DownloadProgress(0, items.size, "", false)
            
            try {
                mediaDownloader.downloadMedia(items) { progress ->
                    _downloadProgress.value = progress
                    if (progress.isComplete) {
                        _downloadComplete.value = DownloadItem(
                            id = System.currentTimeMillis().toString(),
                            fileName = "Batch download complete",
                            filePath = "",
                            url = _currentUrl.value,
                            type = com.massmediasaver.data.MediaType.IMAGE,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
            }
        }
    }

    fun downloadSingleMedia(item: MediaItem) {
        viewModelScope.launch {
            try {
                val result = mediaDownloader.downloadSingleMedia(item)
                if (result != null) {
                    _downloadComplete.value = result
                } else {
                    _error.value = "Failed to download media"
                }
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
            }
        }
    }

    fun savePageComplete() {
        val html = _htmlContent.value
        val url = _currentUrl.value

        if (html.isEmpty() || url.isEmpty()) {
            _error.value = "No page content to save"
            return
        }

        viewModelScope.launch {
            _archiveProgress.value = "Starting archive creation..."
            
            try {
                archiveCreator.createFullArchive(url, html) { progress ->
                    _archiveProgress.value = progress
                }.collect { archiveItem ->
                    if (archiveItem != null) {
                        _archiveProgress.value = "Archive saved: ${archiveItem.name}"
                        _archiveComplete.value = true
                    } else {
                        _error.value = "Failed to create archive"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Archive failed: ${e.message}"
            }
        }
    }

    fun clearDownloadProgress() {
        _downloadProgress.value = null
        _downloadComplete.value = null
    }

    fun clearArchiveProgress() {
        _archiveProgress.value = null
        _archiveComplete.value = false
    }

    fun clearMediaItems() {
        _mediaItems.value = emptyList()
    }
}
