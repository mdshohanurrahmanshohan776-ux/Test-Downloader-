package com.shohan.pro.downloader.data.repository

import com.shohan.pro.downloader.data.db.DownloadDao
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val queueDownloads: Flow<List<DownloadItem>> = downloadDao.getQueueDownloads()
    val historyDownloads: Flow<List<DownloadItem>> = downloadDao.getHistoryDownloads()

    suspend fun insertDownload(item: DownloadItem): Long {
        val nextOrder = downloadDao.getMaxQueueOrder() + 1
        return downloadDao.insertDownload(item.copy(queueOrder = nextOrder))
    }

    suspend fun getDownloadById(id: Long): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun getNextQueuedItem(): DownloadItem? {
        return downloadDao.getNextQueuedItem()
    }

    suspend fun getCurrentDownloadingItem(): DownloadItem? {
        return downloadDao.getCurrentDownloadingItem()
    }

    suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownload(item)
    }

    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, status: String, speed: String) {
        downloadDao.updateProgress(id, downloaded, total, status, speed)
    }

    suspend fun markCompleted(id: Long, filePath: String, totalBytes: Long) {
        downloadDao.markCompleted(id, filePath, totalBytes, System.currentTimeMillis())
    }

    suspend fun markFailed(id: Long, errorMessage: String) {
        downloadDao.markFailed(id, errorMessage)
    }

    suspend fun updateStatus(id: Long, status: DownloadStatus) {
        downloadDao.updateStatus(id, status.name)
    }

    suspend fun moveQueueItemUp(currentList: List<DownloadItem>, index: Int) {
        if (index <= 0 || index >= currentList.size) return
        val currentItem = currentList[index]
        val prevItem = currentList[index - 1]

        val currentOrder = currentItem.queueOrder
        val prevOrder = prevItem.queueOrder

        downloadDao.updateQueueOrder(currentItem.id, prevOrder)
        downloadDao.updateQueueOrder(prevItem.id, currentOrder)
    }

    suspend fun moveQueueItemDown(currentList: List<DownloadItem>, index: Int) {
        if (index < 0 || index >= currentList.size - 1) return
        val currentItem = currentList[index]
        val nextItem = currentList[index + 1]

        val currentOrder = currentItem.queueOrder
        val nextOrder = nextItem.queueOrder

        downloadDao.updateQueueOrder(currentItem.id, nextOrder)
        downloadDao.updateQueueOrder(nextItem.id, currentOrder)
    }

    suspend fun deleteDownload(item: DownloadItem) {
        downloadDao.deleteDownload(item)
    }

    suspend fun deleteById(id: Long) {
        downloadDao.deleteById(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearHistory()
    }

    suspend fun clearAll() {
        downloadDao.clearAll()
    }
}
