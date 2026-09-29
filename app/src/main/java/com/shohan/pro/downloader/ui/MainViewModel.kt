package com.shohan.pro.downloader.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import com.shohan.pro.downloader.data.model.MediaCategory
import com.shohan.pro.downloader.data.model.ResolutionOption
import com.shohan.pro.downloader.data.network.AnalyzedMediaInfo
import com.shohan.pro.downloader.data.network.MediaUrlInspector
import com.shohan.pro.downloader.data.repository.DownloadRepository
import com.shohan.pro.downloader.service.DownloadForegroundService
import com.shohan.pro.downloader.util.ClipboardHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val repository: DownloadRepository,
    private val inspector: MediaUrlInspector = MediaUrlInspector()
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzedMedia = MutableStateFlow<AnalyzedMediaInfo?>(null)
    val analyzedMedia: StateFlow<AnalyzedMediaInfo?> = _analyzedMedia.asStateFlow()

    private val _showResolutionDialog = MutableStateFlow(false)
    val showResolutionDialog: StateFlow<Boolean> = _showResolutionDialog.asStateFlow()

    private val _selectedOption = MutableStateFlow<ResolutionOption?>(null)
    val selectedOption: StateFlow<ResolutionOption?> = _selectedOption.asStateFlow()

    private val _customFileName = MutableStateFlow("")
    val customFileName: StateFlow<String> = _customFileName.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private var lastHandledClipboardUrl: String? = null

    // Queue list
    val queueItems: StateFlow<List<DownloadItem>> = repository.queueDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History filter & search
    val historySearchQuery = MutableStateFlow("")
    val historyCategoryFilter = MutableStateFlow<MediaCategory?>(null)

    val historyItems: StateFlow<List<DownloadItem>> = combine(
        repository.historyDownloads,
        historySearchQuery,
        historyCategoryFilter
    ) { items, query, cat ->
        items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.fileName.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)
            val matchesCat = cat == null || item.category == cat.name
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkClipboard(context: Context, force: Boolean = false) {
        val url = ClipboardHelper.getClipboardUrl(context)
        if (url != null) {
            _detectedClipboardUrl.value = url
            if (force || url != lastHandledClipboardUrl) {
                lastHandledClipboardUrl = url
                analyzeUrl(url)
            }
        }
    }

    fun dismissClipboardBanner() {
        _detectedClipboardUrl.value = null
    }

    fun analyzeUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val mediaInfo = inspector.inspectUrl(url)
                _analyzedMedia.value = mediaInfo
                _selectedOption.value = mediaInfo.defaultOption
                _customFileName.value = mediaInfo.suggestedFileName
                _showResolutionDialog.value = true
            } catch (e: Exception) {
                // In case of error still show dialog with sensible defaults
                val fallbackCategory = MediaCategory.OTHER
                val fallbackOption = ResolutionOption(
                    id = "standard",
                    label = "Standard Download",
                    resolution = "Default",
                    estimatedSize = "Direct Stream",
                    format = "File",
                    isRecommended = true
                )
                val fallbackInfo = AnalyzedMediaInfo(
                    originalUrl = url,
                    suggestedFileName = "download_${System.currentTimeMillis() % 10000}",
                    category = fallbackCategory,
                    contentLength = null,
                    mimeType = null,
                    resolutionOptions = listOf(fallbackOption),
                    defaultOption = fallbackOption
                )
                _analyzedMedia.value = fallbackInfo
                _selectedOption.value = fallbackOption
                _customFileName.value = fallbackInfo.suggestedFileName
                _showResolutionDialog.value = true
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun selectResolutionOption(option: ResolutionOption) {
        _selectedOption.value = option
    }

    fun updateCustomFileName(name: String) {
        _customFileName.value = name
    }

    fun dismissResolutionDialog() {
        _showResolutionDialog.value = false
        _analyzedMedia.value = null
    }

    fun startDownloadNow(context: Context) {
        val info = _analyzedMedia.value ?: return
        val option = _selectedOption.value ?: info.defaultOption
        val name = _customFileName.value.ifBlank { info.suggestedFileName }

        viewModelScope.launch {
            val item = DownloadItem(
                url = info.originalUrl,
                fileName = name,
                category = info.category.name,
                resolution = "${option.label} (${option.resolution})",
                format = option.format,
                totalBytes = info.contentLength ?: 0L,
                status = DownloadStatus.QUEUED.name
            )
            repository.insertDownload(item)
            dismissResolutionDialog()
            DownloadForegroundService.startQueue(context)
        }
    }

    fun addToQueue(context: Context) {
        val info = _analyzedMedia.value ?: return
        val option = _selectedOption.value ?: info.defaultOption
        val name = _customFileName.value.ifBlank { info.suggestedFileName }

        viewModelScope.launch {
            val item = DownloadItem(
                url = info.originalUrl,
                fileName = name,
                category = info.category.name,
                resolution = "${option.label} (${option.resolution})",
                format = option.format,
                totalBytes = info.contentLength ?: 0L,
                status = DownloadStatus.QUEUED.name
            )
            repository.insertDownload(item)
            dismissResolutionDialog()
            DownloadForegroundService.startQueue(context)
        }
    }

    fun moveQueueUp(index: Int) {
        viewModelScope.launch {
            repository.moveQueueItemUp(queueItems.value, index)
        }
    }

    fun moveQueueDown(index: Int) {
        viewModelScope.launch {
            repository.moveQueueItemDown(queueItems.value, index)
        }
    }

    fun pauseQueue(context: Context) {
        DownloadForegroundService.pauseCurrent(context)
    }

    fun resumeQueue(context: Context) {
        DownloadForegroundService.startQueue(context)
    }

    fun cancelQueueItem(context: Context, id: Long) {
        DownloadForegroundService.cancelDownload(context, id)
    }

    fun removeQueueItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteHistoryItem(item: DownloadItem, deleteFromDisk: Boolean) {
        viewModelScope.launch {
            if (deleteFromDisk && item.filePath != null) {
                val f = File(item.filePath)
                if (f.exists()) f.delete()
            }
            repository.deleteDownload(item)
        }
    }

    fun clearAllHistory(deleteFromDisk: Boolean) {
        viewModelScope.launch {
            if (deleteFromDisk) {
                historyItems.value.forEach { item ->
                    item.filePath?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                }
            }
            repository.clearHistory()
        }
    }

    fun redownload(context: Context, item: DownloadItem) {
        viewModelScope.launch {
            val newItem = item.copy(
                id = 0,
                status = DownloadStatus.QUEUED.name,
                downloadedBytes = 0L,
                downloadSpeed = "0 KB/s",
                errorMessage = null,
                createdAt = System.currentTimeMillis(),
                completedAt = null
            )
            repository.insertDownload(newItem)
            DownloadForegroundService.startQueue(context)
        }
    }

    class Factory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
