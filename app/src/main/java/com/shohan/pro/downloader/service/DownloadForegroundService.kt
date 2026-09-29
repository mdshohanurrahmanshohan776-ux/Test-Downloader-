package com.shohan.pro.downloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shohan.pro.downloader.MainActivity
import com.shohan.pro.downloader.R
import com.shohan.pro.downloader.data.db.AppDatabase
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import com.shohan.pro.downloader.data.network.SocialMediaExtractor
import com.shohan.pro.downloader.data.repository.DownloadRepository
import com.shohan.pro.downloader.util.FileOpener
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.TimeUnit

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var currentDownloadJob: Job? = null
    private var currentItem: DownloadItem? = null
    private var isQueuePaused = false

    private lateinit var repository: DownloadRepository
    private lateinit var notificationManager: NotificationManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CHANNEL_ID = "link_downloader_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val COMPLETE_NOTIFICATION_ID_BASE = 5000

        const val ACTION_START_QUEUE = "com.shohan.pro.downloader.START_QUEUE"
        const val ACTION_PAUSE_CURRENT = "com.shohan.pro.downloader.PAUSE_CURRENT"
        const val ACTION_RESUME_QUEUE = "com.shohan.pro.downloader.RESUME_QUEUE"
        const val ACTION_CANCEL_DOWNLOAD = "com.shohan.pro.downloader.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startQueue(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START_QUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseCurrent(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE_CURRENT
            }
            context.startService(intent)
        }

        fun cancelDownload(context: Context, id: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, id)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getDatabase(this).downloadDao()
        repository = DownloadRepository(dao)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_QUEUE

        when (action) {
            ACTION_START_QUEUE, ACTION_RESUME_QUEUE -> {
                isQueuePaused = false
                startForegroundWithInitialNotification()
                processNextInQueue()
            }
            ACTION_PAUSE_CURRENT -> {
                isQueuePaused = true
                currentDownloadJob?.cancel()
                currentItem?.let { item ->
                    serviceScope.launch {
                        repository.updateStatus(item.id, DownloadStatus.PAUSED)
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val cancelId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (cancelId != -1L) {
                    if (currentItem?.id == cancelId) {
                        currentDownloadJob?.cancel()
                    }
                    serviceScope.launch {
                        val item = repository.getDownloadById(cancelId)
                        repository.updateStatus(cancelId, DownloadStatus.CANCELLED)
                        if (item?.filePath != null) {
                            val f = File(item.filePath)
                            if (f.exists()) f.delete()
                        }
                        processNextInQueue()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithInitialNotification() {
        val notification = createNotification("Starting Download Manager...", 0, true, "Preparing queue...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun processNextInQueue() {
        if (isQueuePaused) return
        if (currentDownloadJob?.isActive == true) return

        currentDownloadJob = serviceScope.launch {
            // Find next queued item
            val nextItem = repository.getNextQueuedItem()
            if (nextItem == null) {
                // Queue is empty or complete!
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            currentItem = nextItem
            downloadSingleItem(nextItem)

            // Loop to the next item in the queue
            processNextInQueue()
        }
    }

    private suspend fun downloadSingleItem(item: DownloadItem) {
        val id = item.id
        var downloadedBytes = item.downloadedBytes
        var targetFile: File? = null

        try {
            repository.updateProgress(id, downloadedBytes, item.totalBytes, DownloadStatus.DOWNLOADING.name, "Connecting...")
            updateNotification(item.fileName, 0, true, "Connecting to host...")

            val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(filesDir, "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val isSocial = SocialMediaExtractor.isSocialMediaUrl(item.url)
            val effectiveUrl = if (isSocial) {
                try {
                    SocialMediaExtractor.extractMedia(item.url)?.directUrl ?: item.url
                } catch (_: Exception) {
                    item.url
                }
            } else {
                item.url
            }

            val adjustedName = if (isSocial && item.fileName.endsWith(".pdf", ignoreCase = true)) {
                item.fileName.substringBeforeLast('.') + ".mp4"
            } else {
                item.fileName
            }

            val safeName = adjustedName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            targetFile = File(downloadDir, safeName)

            val requestBuilder = Request.Builder()
                .url(effectiveUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")

            if (targetFile.exists() && targetFile.length() > 0 && downloadedBytes > 0) {
                downloadedBytes = targetFile.length()
                requestBuilder.header("Range", "bytes=$downloadedBytes-")
            } else if (!targetFile.exists()) {
                downloadedBytes = 0L
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body
            if (!response.isSuccessful || responseBody == null) {
                repository.markFailed(id, "HTTP Error: ${response.code}")
                return
            }

            val isPartial = response.code == 206
            val contentLength = responseBody.contentLength()
            val totalLength = if (isPartial) downloadedBytes + contentLength else contentLength
            val isResuming = isPartial && downloadedBytes > 0

            val raf = RandomAccessFile(targetFile, "rw")
            if (isResuming) {
                raf.seek(downloadedBytes)
            } else {
                raf.setLength(0)
                downloadedBytes = 0
            }

            val buffer = ByteArray(8192)
            val inputStream: InputStream = responseBody.byteStream()

            var lastSpeedTime = System.currentTimeMillis()
            var bytesSinceSpeedCheck = 0L
            var currentSpeed = "Starting..."
            var lastDbUpdateTime = 0L

            while (coroutineContext.isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                raf.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesSinceSpeedCheck += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastSpeedTime >= 1000) {
                    val durationSec = (now - lastSpeedTime) / 1000.0
                    val speed = (bytesSinceSpeedCheck / durationSec).toLong()
                    currentSpeed = formatSpeed(speed)
                    bytesSinceSpeedCheck = 0L
                    lastSpeedTime = now
                }

                if (now - lastDbUpdateTime >= 500) {
                    repository.updateProgress(id, downloadedBytes, totalLength, DownloadStatus.DOWNLOADING.name, currentSpeed)
                    val percent = if (totalLength > 0) ((downloadedBytes * 100) / totalLength).toInt() else 0
                    val details = "${formatBytes(downloadedBytes)} / ${formatBytes(totalLength)} • $currentSpeed"
                    updateNotification(item.fileName, percent, totalLength <= 0, details)
                    lastDbUpdateTime = now
                }
            }

            raf.close()
            inputStream.close()

            if (coroutineContext.isActive) {
                repository.markCompleted(id, targetFile.absolutePath, downloadedBytes)
                showCompletionNotification(id, item.fileName, targetFile)
            }

        } catch (e: CancellationException) {
            repository.updateProgress(id, downloadedBytes, item.totalBytes, DownloadStatus.PAUSED.name, "Paused")
        } catch (e: Exception) {
            repository.markFailed(id, e.localizedMessage ?: "Network error")
        } finally {
            currentItem = null
        }
    }

    private fun createNotification(
        title: String,
        progress: Int,
        indeterminate: Boolean,
        statusText: String
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_PAUSE_CURRENT
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, progress: Int, indeterminate: Boolean, statusText: String) {
        val notification = createNotification(title, progress, indeterminate, statusText)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(id: Long, fileName: String, file: File) {
        val openIntent = FileOpener.createOpenFileIntent(this, file)
        val pendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                this,
                id.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val mainIntent = Intent(this, MainActivity::class.java)
            PendingIntent.getActivity(
                this,
                id.toInt(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Download Complete! ✅")
            .setContentText("$fileName is ready to open")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify((COMPLETE_NOTIFICATION_ID_BASE + id).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.download_notification_channel_name)
            val descriptionText = getString(R.string.download_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        currentDownloadJob?.cancel()
    }
}
