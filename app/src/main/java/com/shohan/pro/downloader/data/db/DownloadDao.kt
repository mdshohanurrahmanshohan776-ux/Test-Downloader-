package com.shohan.pro.downloader.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shohan.pro.downloader.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY queueOrder ASC, createdAt ASC")
    fun getQueueDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getHistoryDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY queueOrder ASC, createdAt ASC LIMIT 1")
    suspend fun getNextQueuedItem(): DownloadItem?

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' LIMIT 1")
    suspend fun getCurrentDownloadingItem(): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url AND status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') LIMIT 1")
    suspend fun getActiveDownloadByUrl(url: String): DownloadItem?

    @Query("SELECT COALESCE(MAX(queueOrder), 0) FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    suspend fun getMaxQueueOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(downloadItem: DownloadItem): Long

    @Update
    suspend fun updateDownload(downloadItem: DownloadItem)

    @Query("UPDATE downloads SET queueOrder = :order WHERE id = :id")
    suspend fun updateQueueOrder(id: Long, order: Int)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, status = :status, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, status: String, speed: String)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, totalBytes = :totalBytes, downloadedBytes = :totalBytes, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, filePath: String, totalBytes: Long, completedAt: Long = System.currentTimeMillis(), status: String = "COMPLETED")

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: Long, errorMessage: String, status: String = "FAILED")

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun deleteDownload(downloadItem: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun clearHistory()

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
