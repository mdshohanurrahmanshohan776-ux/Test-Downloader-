package com.shohan.pro.downloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shohan.pro.downloader.MainActivity
import com.shohan.pro.downloader.R
import com.shohan.pro.downloader.ui.FloatingDownloadActivity
import com.shohan.pro.downloader.util.ClipboardHelper

class ClipboardMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "clipboard_monitor_channel"
        const val POPUP_NOTIFICATION_CHANNEL = "instant_download_popup_channel"
        private const val MONITOR_NOTIFICATION_ID = 3001
        private const val POPUP_NOTIFICATION_ID = 3002

        fun start(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private var clipboardManager: ClipboardManager? = null
    private var lastCopiedUrl: String? = null
    private lateinit var notificationManager: NotificationManager

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboardForLink()
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)

        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createMonitorNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }

        checkClipboardForLink()
        return START_STICKY
    }

    private fun checkClipboardForLink() {
        val url = ClipboardHelper.getClipboardUrl(this)
        if (!url.isNullOrBlank() && url != lastCopiedUrl) {
            lastCopiedUrl = url
            onLinkDetectedEverywhere(url)
        }
    }

    private fun onLinkDetectedEverywhere(url: String) {
        // 1. Launch the Floating Dialog Activity immediately on top of the screen!
        try {
            FloatingDownloadActivity.start(this, url)
        } catch (_: Exception) {}

        // 2. Also trigger a High Priority Heads-up notification for instant 1-tap download
        showInstantDownloadNotification(url)
    }

    private fun showInstantDownloadNotification(url: String) {
        val dialogIntent = Intent(this, FloatingDownloadActivity::class.java).apply {
            putExtra(FloatingDownloadActivity.EXTRA_URL, url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            url.hashCode(),
            dialogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, POPUP_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎬 Download Link Detected!")
            .setContentText("Tap to choose quality (4K, 1080p, 720p, MP3) and download")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.stat_sys_download, "Download Now", pendingIntent)
            .build()

        try {
            notificationManager.notify(POPUP_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    private fun createMonitorNotification(): android.app.Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Auto Link Detector Active")
            .setContentText("Copy any video/photo/doc link to immediately pop up download dialog")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.clipboard_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.clipboard_service_channel_desc)
            }
            notificationManager.createNotificationChannel(monitorChannel)

            val popupChannel = NotificationChannel(
                POPUP_NOTIFICATION_CHANNEL,
                "Instant Download Popups",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority popups when any media link is copied"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(popupChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
