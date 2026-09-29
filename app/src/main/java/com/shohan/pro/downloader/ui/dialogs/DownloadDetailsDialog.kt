package com.shohan.pro.downloader.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.ui.components.PapiKingButton
import com.shohan.pro.downloader.ui.components.PapiKingCard
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.NeonGold
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite

@Composable
fun DownloadDetailsDialog(
    item: DownloadItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: (deleteFromDisk: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PapiKingCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("download_details_dialog"),
            contentPadding = 18.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0C2136)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.mediaCategory.iconEmoji, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fileName,
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.mediaCategory.displayName,
                            color = NeonGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Items
                DetailRow(label = "Resolution / Format", value = "${item.resolution} (${item.format})")
                DetailRow(label = "File Size", value = item.formattedSize)
                DetailRow(label = "Date Downloaded", value = item.formattedDate)
                DetailRow(label = "Status", value = item.status)
                DetailRow(label = "Source URL", value = item.url, isLink = true)
                if (item.filePath != null) {
                    DetailRow(label = "Storage Path", value = item.filePath)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons using PapiKingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PapiKingButton(
                        text = "Open",
                        onClick = onOpen,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.OpenInNew,
                        testTag = "details_open_button"
                    )

                    PapiKingButton(
                        text = "Share",
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        testTag = "details_share_button"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PapiKingButton(
                        text = "Redownload",
                        onClick = onRedownload,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh,
                        testTag = "details_redownload_button"
                    )

                    PapiKingButton(
                        text = "Delete",
                        onClick = { onDelete(true) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Delete,
                        testTag = "details_delete_button"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PapiKingButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "details_close_button"
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isLink: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFFFFB58D),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = if (isLink) NeonCyan else TextWhite,
            fontSize = 13.sp,
            maxLines = if (isLink) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
