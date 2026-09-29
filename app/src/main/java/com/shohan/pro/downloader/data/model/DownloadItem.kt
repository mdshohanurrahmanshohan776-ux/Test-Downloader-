package com.shohan.pro.downloader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val category: String, // String representation of MediaCategory
    val resolution: String,
    val format: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = DownloadStatus.QUEUED.name,
    val queueOrder: Int = 0,
    val filePath: String? = null,
    val mimeType: String? = null,
    val downloadSpeed: String = "0 KB/s",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val currentStatus: DownloadStatus
        get() = try {
            DownloadStatus.valueOf(status)
        } catch (_: Exception) {
            DownloadStatus.QUEUED
        }

    val mediaCategory: MediaCategory
        get() = try {
            MediaCategory.valueOf(category)
        } catch (_: Exception) {
            MediaCategory.OTHER
        }

    val formattedDate: String
        get() {
            val date = Date(if (completedAt != null && completedAt > 0) completedAt else createdAt)
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(date)
        }

    val formattedSize: String
        get() {
            val bytes = if (totalBytes > 0) totalBytes else downloadedBytes
            if (bytes <= 0) return "Unknown size"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
