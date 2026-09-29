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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.shohan.pro.downloader.data.model.MediaCategory
import com.shohan.pro.downloader.ui.components.PapiKingButton
import com.shohan.pro.downloader.ui.components.PapiKingCard
import com.shohan.pro.downloader.ui.theme.BrandNavyDark
import com.shohan.pro.downloader.ui.theme.ErrorRed
import com.shohan.pro.downloader.ui.theme.NeonCoral
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.NeonGold
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite

@Composable
fun HistoryScreen(
    historyItems: List<DownloadItem>,
    searchQuery: String,
    selectedCategory: MediaCategory?,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (MediaCategory?) -> Unit,
    onOpenFile: (filePath: String) -> Unit,
    onShareFile: (filePath: String) -> Unit,
    onViewDetails: (DownloadItem) -> Unit,
    onDeleteItem: (DownloadItem) -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Header Card with Search & Clear All
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_header_card"),
                contentPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Download History",
                                color = NeonCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${historyItems.size} completed files",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        if (historyItems.isNotEmpty()) {
                            PapiKingButton(
                                text = "Clear",
                                onClick = onClearAll,
                                icon = Icons.Default.Delete,
                                testTag = "history_clear_all_button"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = {
                            Text("Search by file name or URL...", color = TextMuted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1D4D7C),
                            focusedContainerColor = BrandNavyDark,
                            unfocusedContainerColor = BrandNavyDark,
                            cursorColor = NeonCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                label = "All",
                                isSelected = selectedCategory == null,
                                onClick = { onCategorySelect(null) }
                            )
                        }
                        items(MediaCategory.values()) { cat ->
                            FilterChip(
                                label = "${cat.iconEmoji} ${cat.displayName}",
                                isSelected = selectedCategory == cat,
                                onClick = { onCategorySelect(cat) }
                            )
                        }
                    }
                }
            }
        }

        // Empty state
        if (historyItems.isEmpty()) {
            item {
                PapiKingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_empty_card"),
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📁", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Download History",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files match your search" else "Completed files will appear here",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // History items
        items(historyItems, key = { it.id }) { item ->
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_item_${item.id}"),
                contentPadding = 14.dp
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
                            Text(text = item.mediaCategory.iconEmoji, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.url,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date & Size Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.formattedDate,
                            color = NeonCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0C2136))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.resolution.substringBefore(" ("),
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = item.formattedSize,
                                color = NeonGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.filePath != null) {
                            PapiKingButton(
                                text = "Open",
                                onClick = { onOpenFile(item.filePath) },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.OpenInNew,
                                testTag = "history_open_button_${item.id}"
                            )

                            PapiKingButton(
                                text = "Share",
                                onClick = { onShareFile(item.filePath) },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Share,
                                testTag = "history_share_button_${item.id}"
                            )
                        }

                        PapiKingButton(
                            text = "Details",
                            onClick = { onViewDetails(item) },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Info,
                            testTag = "history_details_button_${item.id}"
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF163E66) else Color(0xFF0C2136)
    val border = if (isSelected) NeonCyan else Color(0xFF1D4D7C)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
