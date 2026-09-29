package com.shohan.pro.downloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.shohan.pro.downloader.MainActivity
import com.shohan.pro.downloader.R
import com.shohan.pro.downloader.ui.FloatingOverlayManager
import com.shohan.pro.downloader.util.ClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClipboardMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "clipboard_monitor_channel_silent"
        const val POPUP_NOTIFICATION_CHANNEL = "instant_download_popup_channel"
        private const val MONITOR_NOTIFICATION_ID = 3001
        private const val POPUP_NOTIFICATION_ID = 3002

        fun start(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    private var clipboardManager: ClipboardManager? = null
    private var lastCopiedUrl: String? = null
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var dummyOverlayView: View? = null
    private var windowManager: WindowManager? = null

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
        val notification = createSilentMonitorNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }

        // Hide foreground notification from the top status bar as requested:
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}

        // Ensure 1x1 overlay window for background clipboard access on Android 10-15
        ensureWindowFocusOverlay()

        startClipboardPolling()
        checkClipboardForLink()
        return START_STICKY
    }

    private fun ensureWindowFocusOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            if (dummyOverlayView == null) {
                try {
                    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val view = View(this)
                    val params = WindowManager.LayoutParams(
                        1, 1,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        } else {
                            @Suppress("DEPRECATION")
                            WindowManager.LayoutParams.TYPE_PHONE
                        },
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                        x = 0
                        y = 0
                    }
                    windowManager?.addView(view, params)
                    dummyOverlayView = view
                } catch (_: Exception) {}
            }
        }
    }

    private fun removeWindowFocusOverlay() {
        dummyOverlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
            dummyOverlayView = null
        }
    }

    private fun startClipboardPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    checkClipboardForLink()
                }
                delay(800)
            }
        }
    }

    private fun checkClipboardForLink() {
        try {
            val url = ClipboardHelper.getClipboardUrl(this)
            if (!url.isNullOrBlank() && url != lastCopiedUrl) {
                lastCopiedUrl = url
                onLinkDetectedEverywhere(url)
            }
        } catch (_: Exception) {}
    }

    private fun onLinkDetectedEverywhere(url: String) {
        // Pop up the Floating Download Dialog directly over whatever app the user is on!
        FloatingOverlayManager.showFloatingDialog(this, url)

        // Also trigger high priority heads-up notification in case overlays are waiting for click
        showInstantDownloadNotification(url)
    }

    private fun showInstantDownloadNotification(url: String) {
        val dialogIntent = Intent(this, com.shohan.pro.downloader.ui.FloatingDownloadActivity::class.java).apply {
            putExtra(com.shohan.pro.downloader.ui.FloatingDownloadActivity.EXTRA_URL, url)
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

    private fun createSilentMonitorNotification(): android.app.Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Link Downloader")
            .setContentText("Active")
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val silentChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Link Detection",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Silent background link detector"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(silentChannel)

            val popupChannel = NotificationChannel(
                POPUP_NOTIFICATION_CHANNEL,
                "Instant Download Popups",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Popups when any media link is copied"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(popupChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        removeWindowFocusOverlay()
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
