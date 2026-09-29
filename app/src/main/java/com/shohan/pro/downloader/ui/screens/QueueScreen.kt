package com.shohan.pro.downloader.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import com.shohan.pro.downloader.ui.components.PapiKingButton
import com.shohan.pro.downloader.ui.components.PapiKingCard
import com.shohan.pro.downloader.ui.theme.BrandNavyDark
import com.shohan.pro.downloader.ui.theme.ErrorRed
import com.shohan.pro.downloader.ui.theme.NeonCoral
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.NeonGold
import com.shohan.pro.downloader.ui.theme.SuccessGreen
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite

@Composable
fun QueueScreen(
    queueItems: List<DownloadItem>,
    onMoveUp: (index: Int) -> Unit,
    onMoveDown: (index: Int) -> Unit,
    onPauseQueue: () -> Unit,
    onResumeQueue: () -> Unit,
    onCancelItem: (id: Long) -> Unit,
    onRemoveItem: (id: Long) -> Unit,
    onAddNewLink: () -> Unit
) {
    val activeItem = queueItems.firstOrNull { it.currentStatus == DownloadStatus.DOWNLOADING }
    val waitingItems = queueItems.filter { it.currentStatus != DownloadStatus.DOWNLOADING }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Queue Top Control Header
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("queue_header_card"),
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Download Queue",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${queueItems.size} tasks in queue • Downloads one-by-one",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (activeItem != null) {
                        PapiKingButton(
                            text = "Pause",
                            onClick = onPauseQueue,
                            icon = Icons.Default.Pause,
                            testTag = "queue_pause_button"
                        )
                    } else if (waitingItems.isNotEmpty()) {
                        PapiKingButton(
                            text = "Start",
                            onClick = onResumeQueue,
                            icon = Icons.Default.PlayArrow,
                            testTag = "queue_start_button"
                        )
                    }
                }
            }
        }

        // Active Downloading Item Card (if any)
        if (activeItem != null) {
            item {
                PapiKingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_download_card"),
                    contentPadding = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0C2136)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = activeItem.mediaCategory.iconEmoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeItem.fileName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeItem.resolution,
                                    color = NeonCoral,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { onCancelItem(activeItem.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { activeItem.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = Color(0xFF0C2136)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats: percent, speed, size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = NeonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeItem.downloadSpeed,
                                    color = NeonGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "${activeItem.progressPercent}%",
                                color = NeonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${formatSize(activeItem.downloadedBytes)} / ${formatSize(activeItem.totalBytes)}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Waiting in Queue Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Waiting in Queue (${waitingItems.size})",
                    color = NeonGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Use ⬆️ ⬇️ to control order",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (waitingItems.isEmpty() && activeItem == null) {
            item {
                PapiKingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_queue_card"),
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📥", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Download Queue is Empty",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy any link or add tasks to begin sequential downloads",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PapiKingButton(
                            text = "Add Link to Queue",
                            onClick = onAddNewLink,
                            testTag = "empty_queue_add_button"
                        )
                    }
                }
            }
        }

        // Waiting Queue Items with Move Up/Down Controls
        itemsIndexed(waitingItems, key = { _, item -> item.id }) { index, item ->
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("queue_item_${item.id}"),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C2136))
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${index + 1}",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(text = item.mediaCategory.iconEmoji, fontSize = 20.sp)

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileName,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.resolution,
                                color = NeonCoral,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•  ${item.formattedSize}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (item.currentStatus == DownloadStatus.PAUSED) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "[PAUSED]",
                                    color = NeonGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Reorder Controls (Move Up, Move Down)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (index > 0) NeonCyan else Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < waitingItems.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < waitingItems.size - 1) NeonCyan else Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRemoveItem(item.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        else -> String.format(java.util.Locale.US, "%.0f KB", kb)
    }
}
