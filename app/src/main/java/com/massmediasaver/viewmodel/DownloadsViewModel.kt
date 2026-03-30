package com.massmediasaver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.massmediasaver.data.ArchiveItem
import com.massmediasaver.data.DownloadItem
import com.massmediasaver.data.MediaType
import com.massmediasaver.util.FileStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _archives = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val archives: StateFlow<List<ArchiveItem>> = _archives.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadDownloads()
        loadArchives()
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun loadDownloads() {
        viewModelScope.launch {
            val downloadsList = withContext(Dispatchers.IO) {
                val dir = FileStorageUtil.getDownloadsDirectory(getApplication())
                val files = dir.listFiles() ?: return@withContext emptyList()
                
                files.filter { it.isFile && !it.isDirectory }
                    .sortedByDescending { it.lastModified() }
                    .map { file ->
                        val isVideo = file.extension.lowercase() in 
                            listOf("mp4", "webm", "mov", "avi", "mkv")
                        DownloadItem(
                            id = file.name,
                            fileName = file.name,
                            filePath = file.absolutePath,
                            url = "",
                            type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                            timestamp = file.lastModified(),
                            size = file.length()
                        )
                    }
            }
            _downloads.value = downloadsList
        }
    }

    fun loadArchives() {
        viewModelScope.launch {
            val archivesList = withContext(Dispatchers.IO) {
                val dir = FileStorageUtil.getArchivesDirectory(getApplication())
                val folders = dir.listFiles() ?: return@withContext emptyList()
                
                folders.filter { it.isDirectory }
                    .sortedByDescending { it.lastModified() }
                    .map { folder ->
                        val indexFile = File(folder, "index.html")
                        val mediaDir = File(folder, "media")
                        val mediaCount = mediaDir.listFiles()?.size ?: 0
                        
                        ArchiveItem(
                            id = folder.name,
                            name = folder.name,
                            folderPath = folder.absolutePath,
                            url = "",
                            timestamp = folder.lastModified(),
                            mediaCount = mediaCount
                        )
                    }
            }
            _archives.value = archivesList
        }
    }

    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            loadDownloads()
        }
    }

    fun deleteArchive(item: ArchiveItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val folder = File(item.folderPath)
                if (folder.exists()) {
                    folder.deleteRecursively()
                }
            }
            loadArchives()
        }
    }

    fun refresh() {
        loadDownloads()
        loadArchives()
    }
}
