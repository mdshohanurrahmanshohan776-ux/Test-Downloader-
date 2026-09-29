package com.shohan.pro.downloader.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.shohan.pro.downloader.LinkDownloaderApp
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import com.shohan.pro.downloader.data.model.ResolutionOption
import com.shohan.pro.downloader.data.network.MediaUrlInspector
import com.shohan.pro.downloader.service.DownloadForegroundService
import com.shohan.pro.downloader.ui.dialogs.DownloadResolutionDialog
import com.shohan.pro.downloader.ui.theme.LinkDownloaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages true system-level floating download dialog directly using WindowManager.
 * Pops up over ANY app (YouTube, Facebook, Instagram, Chrome, TikTok, etc.)
 * without relying on background activity launches.
 */
object FloatingOverlayManager {

    private var activeOverlayView: ComposeView? = null
    private var windowManager: WindowManager? = null
    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastShownUrl: String? = null
    private var lastShownTime: Long = 0

    fun showFloatingDialog(context: Context, url: String) {
        val cleanUrl = url.trim()
        val now = System.currentTimeMillis()
        if (cleanUrl == lastShownUrl && now - lastShownTime < 3000) {
            return // Prevent duplicate triggers within 3 seconds
        }
        lastShownUrl = cleanUrl
        lastShownTime = now

        Handler(Looper.getMainLooper()).post {
            // Check if overlay permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                try {
                    showDirectWindowManagerDialog(context, cleanUrl)
                    return@post
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback: Launch FloatingDownloadActivity
            try {
                FloatingDownloadActivity.start(context, cleanUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDirectWindowManagerDialog(context: Context, url: String) {
        dismissCurrentOverlay()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER
        }

        // Custom lifecycle owner for ComposeView attached to WindowManager
        val overlayLifecycleOwner = OverlayLifecycleOwner()
        overlayLifecycleOwner.performRestore(null)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val inspector = MediaUrlInspector()
        val isAnalyzingFlow = MutableStateFlow(true)
        val analyzedMediaFlow = MutableStateFlow<com.shohan.pro.downloader.data.network.AnalyzedMediaInfo?>(null)
        val selectedOptionFlow = MutableStateFlow<ResolutionOption?>(null)
        val customFileNameFlow = MutableStateFlow("")

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)

            setContent {
                LinkDownloaderTheme {
                    val isAnalyzing by isAnalyzingFlow.collectAsState()
                    val analyzedMedia by analyzedMediaFlow.collectAsState()
                    val selectedOption by selectedOptionFlow.collectAsState()
                    val customFileName by customFileNameFlow.collectAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        if (analyzedMedia != null) {
                            DownloadResolutionDialog(
                                mediaInfo = analyzedMedia!!,
                                selectedOption = selectedOption,
                                fileName = customFileName,
                                onFileNameChange = { customFileNameFlow.value = it },
                                onOptionSelected = { selectedOptionFlow.value = it },
                                onDownloadNow = {
                                    val item = analyzedMedia!!
                                    val option = selectedOption ?: item.defaultOption
                                    val fileName = customFileName.ifBlank { item.suggestedFileName }
                                    startDownload(context, item.originalUrl, fileName, option)
                                    Toast.makeText(context, "Download Started 🚀", Toast.LENGTH_SHORT).show()
                                    dismissCurrentOverlay()
                                },
                                onAddToQueue = {
                                    val item = analyzedMedia!!
                                    val option = selectedOption ?: item.defaultOption
                                    val fileName = customFileName.ifBlank { item.suggestedFileName }
                                    addToQueue(context, item.originalUrl, fileName, option)
                                    Toast.makeText(context, "Added to Queue 📥", Toast.LENGTH_SHORT).show()
                                    dismissCurrentOverlay()
                                },
                                onDismiss = {
                                    dismissCurrentOverlay()
                                }
                            )
                        }
                    }
                }
            }
        }

        activeOverlayView = composeView
        wm.addView(composeView, layoutParams)

        // Inspect URL in background
        overlayScope.launch(Dispatchers.IO) {
            val result = inspector.inspectUrl(url)
            withContext(Dispatchers.Main) {
                analyzedMediaFlow.value = result
                selectedOptionFlow.value = result.defaultOption
                customFileNameFlow.value = result.suggestedFileName
                isAnalyzingFlow.value = false
            }
        }
    }

    private fun startDownload(context: Context, url: String, fileName: String, option: ResolutionOption) {
        val app = context.applicationContext as LinkDownloaderApp
        overlayScope.launch(Dispatchers.IO) {
            val item = DownloadItem(
                url = url,
                fileName = fileName,
                category = "VIDEO",
                resolution = "${option.label} (${option.resolution})",
                format = option.format,
                totalBytes = 0L,
                status = DownloadStatus.QUEUED.name
            )
            app.repository.insertDownload(item)
            withContext(Dispatchers.Main) {
                DownloadForegroundService.startQueue(context)
            }
        }
    }

    private fun addToQueue(context: Context, url: String, fileName: String, option: ResolutionOption) {
        val app = context.applicationContext as LinkDownloaderApp
        overlayScope.launch(Dispatchers.IO) {
            val item = DownloadItem(
                url = url,
                fileName = fileName,
                category = "VIDEO",
                resolution = "${option.label} (${option.resolution})",
                format = option.format,
                totalBytes = 0L,
                status = DownloadStatus.QUEUED.name
            )
            app.repository.insertDownload(item)
        }
    }

    fun dismissCurrentOverlay() {
        val view = activeOverlayView
        val wm = windowManager
        if (view != null && wm != null) {
            try {
                wm.removeView(view)
            } catch (_: Exception) {}
            activeOverlayView = null
        }
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val myViewModelStore = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val viewModelStore: ViewModelStore = myViewModelStore
        override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

        fun performRestore(savedState: android.os.Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
