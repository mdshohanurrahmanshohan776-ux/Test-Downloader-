package com.shohan.pro.downloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.shohan.pro.downloader.data.db.AppDatabase
import com.shohan.pro.downloader.data.repository.DownloadRepository
import com.shohan.pro.downloader.service.DownloadForegroundService

class LinkDownloaderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: DownloadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = DownloadRepository(database.downloadDao())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                DownloadForegroundService.CHANNEL_ID,
                getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_desc)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
