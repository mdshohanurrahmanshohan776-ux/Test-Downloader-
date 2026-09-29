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
import com.shohan.pro.downloader.util.CopiedLinksStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

data class CopiedLinkItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val platformName: String,
    val category: MediaCategory,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(
    private val appContext: Context,
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

    // Persisted copied links list - loaded from SharedPreferences so it never vanishes
    private val _copiedLinks = MutableStateFlow<List<CopiedLinkItem>>(
        CopiedLinksStorage.loadLinks(appContext)
    )
    val copiedLinks: StateFlow<List<CopiedLinkItem>> = _copiedLinks.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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

    fun checkClipboard(context: Context) {
        val url = ClipboardHelper.getClipboardUrl(context)
        if (!url.isNullOrBlank()) {
            addCopiedLink(url)
        }
    }

    fun addCopiedLink(rawUrl: String) {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return
        val current = _copiedLinks.value
        // If already exists, move to top
        val filtered = current.filterNot { it.url.equals(clean, ignoreCase = true) }

        val (platform, category) = detectPlatformAndCategory(clean)
        val newItem = CopiedLinkItem(
            url = clean,
            platformName = platform,
            category = category
        )
        val updated = listOf(newItem) + filtered.take(19)
        _copiedLinks.value = updated
        CopiedLinksStorage.saveLinks(appContext, updated)
    }

    fun removeCopiedLink(id: String) {
        val updated = _copiedLinks.value.filterNot { it.id == id }
        _copiedLinks.value = updated
        CopiedLinksStorage.saveLinks(appContext, updated)
    }

    fun clearAllCopiedLinks() {
        _copiedLinks.value = emptyList()
        CopiedLinksStorage.saveLinks(appContext, emptyList())
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun analyzeUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        // Always add to persistent copied links shelf
        addCopiedLink(cleanUrl)

        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            try {
                val mediaInfo = inspector.inspectUrl(cleanUrl)
                _analyzedMedia.value = mediaInfo
                _selectedOption.value = mediaInfo.defaultOption
                _customFileName.value = mediaInfo.suggestedFileName
                _showResolutionDialog.value = true
            } catch (e: Exception) {
                // If anything fails, still display standard resolution dialog with direct link!
                val (platform, category) = detectPlatformAndCategory(cleanUrl)
                val fallbackOption = ResolutionOption(
                    id = "standard",
                    label = "Standard Download",
                    resolution = "Default",
                    estimatedSize = "Direct Stream",
                    format = if (category == MediaCategory.VIDEO) "MP4" else "File",
                    isRecommended = true
                )
                val fallbackInfo = AnalyzedMediaInfo(
                    originalUrl = cleanUrl,
                    suggestedFileName = "${platform}_${System.currentTimeMillis() % 10000}.${if (category == MediaCategory.VIDEO) "mp4" else "bin"}",
                    category = category,
                    contentLength = null,
                    mimeType = if (category == MediaCategory.VIDEO) "video/mp4" else null,
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

    private fun detectPlatformAndCategory(url: String): Pair<String, MediaCategory> {
        val lower = url.lowercase(Locale.ROOT)
        return when {
            lower.contains("facebook.com") || lower.contains("fb.watch") -> "Facebook" to MediaCategory.VIDEO
            lower.contains("instagram.com") -> "Instagram" to MediaCategory.VIDEO
            lower.contains("tiktok.com") -> "TikTok" to MediaCategory.VIDEO
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube" to MediaCategory.VIDEO
            lower.contains("twitter.com") || lower.contains("x.com") -> "Twitter / X" to MediaCategory.VIDEO
            lower.contains("pinterest.com") -> "Pinterest" to MediaCategory.IMAGE
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov") -> "Video File" to MediaCategory.VIDEO
            lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".aac") -> "Audio File" to MediaCategory.AUDIO
            lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") -> "Document" to MediaCategory.DOCUMENT
            lower.endsWith(".apk") || lower.endsWith(".zip") || lower.endsWith(".rar") -> "Archive / App" to MediaCategory.OTHER
            else -> "Web File / Link" to MediaCategory.OTHER
        }
    }

    class Factory(
        private val context: Context,
        private val repository: DownloadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context.applicationContext, repository) as T
        }
    }
}
