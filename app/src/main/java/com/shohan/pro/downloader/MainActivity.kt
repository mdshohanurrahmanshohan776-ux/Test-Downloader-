package com.shohan.pro.downloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.service.ClipboardMonitorService
import com.shohan.pro.downloader.ui.MainViewModel
import com.shohan.pro.downloader.ui.components.androidDrawableBackground
import com.shohan.pro.downloader.ui.dialogs.DownloadDetailsDialog
import com.shohan.pro.downloader.ui.dialogs.DownloadResolutionDialog
import com.shohan.pro.downloader.ui.screens.HistoryScreen
import com.shohan.pro.downloader.ui.screens.HomeScreen
import com.shohan.pro.downloader.ui.screens.QueueScreen
import com.shohan.pro.downloader.ui.theme.BrandNavyDark
import com.shohan.pro.downloader.ui.theme.LinkDownloaderTheme
import com.shohan.pro.downloader.ui.theme.NeonCoral
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite
import com.shohan.pro.downloader.util.ClipboardHelper
import com.shohan.pro.downloader.util.FileOpener

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as LinkDownloaderApp
        MainViewModel.Factory(app.repository)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIncomingIntent(intent)

        // Start background clipboard monitor service so copied links anywhere pop up dialog immediately
        try {
            ClipboardMonitorService.start(this)
        } catch (_: Exception) {}

        setContent {
            LinkDownloaderTheme {
                MainAppContent(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically check clipboard for copied links as soon as user opens or resumes the app!
        viewModel.checkClipboard(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val extractedUrl = ClipboardHelper.extractUrl(sharedText) ?: sharedText.trim()
                viewModel.analyzeUrl(extractedUrl)
            }
        }
    }
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Download),
    QUEUE("Queue", Icons.Default.Queue),
    HISTORY("History", Icons.Default.History)
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analyzedMedia by viewModel.analyzedMedia.collectAsStateWithLifecycle()
    val showResolutionDialog by viewModel.showResolutionDialog.collectAsStateWithLifecycle()
    val selectedOption by viewModel.selectedOption.collectAsStateWithLifecycle()
    val customFileName by viewModel.customFileName.collectAsStateWithLifecycle()
    val detectedClipboardUrl by viewModel.detectedClipboardUrl.collectAsStateWithLifecycle()

    val queueItems by viewModel.queueItems.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historyCategoryFilter by viewModel.historyCategoryFilter.collectAsStateWithLifecycle()

    var selectedHistoryItemForDetails by remember { mutableStateOf<DownloadItem?>(null) }

    // Entire app root background directly styled with drawable papi_king_bg
    Box(
        modifier = Modifier
            .fillMaxSize()
            .androidDrawableBackground(R.drawable.papi_king_bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_app_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Screen Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            detectedClipboardUrl = detectedClipboardUrl,
                            isAnalyzing = isAnalyzing,
                            queueCount = queueItems.size,
                            historyCount = historyItems.size,
                            onAnalyzeUrl = { url -> viewModel.analyzeUrl(url) },
                            onNavigateToQueue = { currentTab = NavigationTab.QUEUE },
                            onNavigateToHistory = { currentTab = NavigationTab.HISTORY },
                            onDismissClipboardBanner = { viewModel.dismissClipboardBanner() }
                        )
                    }

                    NavigationTab.QUEUE -> {
                        QueueScreen(
                            queueItems = queueItems,
                            onMoveUp = { index -> viewModel.moveQueueUp(index) },
                            onMoveDown = { index -> viewModel.moveQueueDown(index) },
                            onPauseQueue = { viewModel.pauseQueue(context) },
                            onResumeQueue = { viewModel.resumeQueue(context) },
                            onCancelItem = { id -> viewModel.cancelQueueItem(context, id) },
                            onRemoveItem = { id -> viewModel.removeQueueItem(id) },
                            onAddNewLink = { currentTab = NavigationTab.HOME }
                        )
                    }

                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            historyItems = historyItems,
                            searchQuery = historySearchQuery,
                            selectedCategory = historyCategoryFilter,
                            onSearchChange = { viewModel.historySearchQuery.value = it },
                            onCategorySelect = { viewModel.historyCategoryFilter.value = it },
                            onOpenFile = { path -> FileOpener.openFile(context, path) },
                            onShareFile = { path -> FileOpener.shareFile(context, path) },
                            onViewDetails = { item -> selectedHistoryItemForDetails = item },
                            onDeleteItem = { item -> viewModel.deleteHistoryItem(item, true) },
                            onClearAll = { viewModel.clearAllHistory(false) }
                        )
                    }
                }
            }

            // Bottom Navigation Bar with live badges
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1D4D7C), RoundedCornerShape(12.dp)),
                color = BrandNavyDark.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        val tint = if (isSelected) NeonCyan else TextMuted

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF163E66) else Color.Transparent)
                                .clickable { currentTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = tint,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    // Badge for Queue & History
                                    val badgeCount = when (tab) {
                                        NavigationTab.QUEUE -> queueItems.size
                                        NavigationTab.HISTORY -> historyItems.size
                                        else -> 0
                                    }

                                    if (badgeCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (tab == NavigationTab.QUEUE) NeonCoral else NeonCyan),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (badgeCount > 99) "99+" else "$badgeCount",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = tab.title,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Automatic Download Resolution Dialog (appears when link copied/analyzed)
        if (showResolutionDialog && analyzedMedia != null) {
            DownloadResolutionDialog(
                mediaInfo = analyzedMedia!!,
                selectedOption = selectedOption,
                fileName = customFileName,
                onFileNameChange = { viewModel.updateCustomFileName(it) },
                onOptionSelected = { viewModel.selectResolutionOption(it) },
                onDownloadNow = { viewModel.startDownloadNow(context) },
                onAddToQueue = { viewModel.addToQueue(context) },
                onDismiss = { viewModel.dismissResolutionDialog() }
            )
        }

        // Download Details Dialog for History Screen
        if (selectedHistoryItemForDetails != null) {
            DownloadDetailsDialog(
                item = selectedHistoryItemForDetails!!,
                onOpen = {
                    selectedHistoryItemForDetails?.filePath?.let { path ->
                        FileOpener.openFile(context, path)
                    }
                },
                onShare = {
                    selectedHistoryItemForDetails?.filePath?.let { path ->
                        FileOpener.shareFile(context, path)
                    }
                },
                onRedownload = {
                    val item = selectedHistoryItemForDetails
                    if (item != null) {
                        viewModel.redownload(context, item)
                        selectedHistoryItemForDetails = null
                        currentTab = NavigationTab.QUEUE
                    }
                },
                onDelete = { deleteFromDisk ->
                    val item = selectedHistoryItemForDetails
                    if (item != null) {
                        viewModel.deleteHistoryItem(item, deleteFromDisk)
                        selectedHistoryItemForDetails = null
                    }
                },
                onDismiss = { selectedHistoryItemForDetails = null }
            )
        }
    }
}
