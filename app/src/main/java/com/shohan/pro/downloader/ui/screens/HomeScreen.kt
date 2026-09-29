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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohan.pro.downloader.ui.CopiedLinkItem
import com.shohan.pro.downloader.ui.components.PapiKingButton
import com.shohan.pro.downloader.ui.components.PapiKingCard
import com.shohan.pro.downloader.ui.theme.BrandNavyDark
import com.shohan.pro.downloader.ui.theme.NeonCoral
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.NeonGold
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite
import com.shohan.pro.downloader.util.ClipboardHelper

@Composable
fun HomeScreen(
    copiedLinks: List<CopiedLinkItem>,
    isAnalyzing: Boolean,
    queueCount: Int,
    historyCount: Int,
    onAnalyzeUrl: (String) -> Unit,
    onRemoveCopiedLink: (String) -> Unit,
    onClearAllCopiedLinks: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var inputUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // App Brand Header Card
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_brand_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0C2136))
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Pro Link Downloader",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fast media & file downloader with custom quality",
                                color = NeonCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0C2136))
                                .clickable { onNavigateToQueue() }
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(text = "Download Queue", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "$queueCount Active / Waiting",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0C2136))
                                .clickable { onNavigateToHistory() }
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(text = "Download History", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "$historyCount Completed",
                                    color = NeonGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Serial Copied Links Shelf (Always shown ABOVE the empty input box)
        item {
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copied_links_card"),
                contentPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (copiedLinks.isNotEmpty()) "কপি করা লিংকসমূহ (${copiedLinks.size})" else "কপি করা লিংক",
                            color = NeonCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (copiedLinks.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { onClearAllCopiedLinks() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (copiedLinks.isEmpty()) {
                        Text(
                            text = "অন্য যেকোনো অ্যাপ থেকে ভিডিও, অডিও বা ফাইলের লিংক কপি করলে তা স্বয়ংক্রিয়ভাবে এখানে সিরিয়াল করে জমা হবে।",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        copiedLinks.forEachIndexed { index, item ->
                            CopiedLinkRowItem(
                                index = index + 1,
                                item = item,
                                onDownload = { onAnalyzeUrl(item.url) },
                                onDismiss = { onRemoveCopiedLink(item.id) }
                            )
                            if (index < copiedLinks.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Empty URL Input Card
        item {
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_input_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Paste or Enter Any Link",
                        color = Color(0xFFFFB58D),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supports Facebook, Instagram, TikTok, Video, Audio, Docs",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_url_input"),
                        placeholder = {
                            Text("https://... (video, audio, or file link)", color = TextMuted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(onClick = { inputUrl = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text
                                        if (!clipText.isNullOrBlank()) {
                                            val url = ClipboardHelper.extractUrl(clipText) ?: clipText.trim()
                                            inputUrl = url
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF1D4D7C),
                            focusedContainerColor = BrandNavyDark,
                            unfocusedContainerColor = BrandNavyDark,
                            cursorColor = NeonCyan
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (inputUrl.isNotBlank()) onAnalyzeUrl(inputUrl.trim())
                        })
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PapiKingButton(
                        text = "Fetch Link & Choose Quality",
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                onAnalyzeUrl(inputUrl.trim())
                            } else {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    val url = ClipboardHelper.extractUrl(clipText) ?: clipText.trim()
                                    inputUrl = url
                                    onAnalyzeUrl(url)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Download,
                        isLoading = isAnalyzing,
                        testTag = "home_fetch_button"
                    )
                }
            }
        }

        // Quick Category Suggestions / Formats
        item {
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categories_card"),
                contentPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Supported Media Categories",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = listOf(
                        Triple("Videos", "🎬 4K, 2K, 1080p, 720p, 480p, 360p", "MP4 / MKV"),
                        Triple("Audios", "🎵 320kbps, 256kbps, 192kbps, 128kbps", "MP3 / M4A"),
                        Triple("Photos", "🖼️ Full HD & Original Quality", "JPG / PNG / WEBP"),
                        Triple("Files & Docs", "📦 PDF, ZIP, APK, Docs, Archives", "Any Format")
                    )

                    categories.forEach { (title, desc, formats) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = desc,
                                color = TextWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0C2136))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formats,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

@Composable
fun CopiedLinkRowItem(
    index: Int,
    item: CopiedLinkItem,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val platformColor = when (item.platformName) {
        "Facebook" -> Color(0xFF1877F2)
        "Instagram" -> Color(0xFFE1306C)
        "YouTube" -> Color(0xFFFF3333)
        "TikTok" -> Color(0xFF00F2FE)
        "Twitter / X" -> Color(0xFF1DA1F2)
        "Pinterest" -> Color(0xFFBD081C)
        "Audio File" -> Color(0xFF00E676)
        "Video File" -> Color(0xFFFF6D00)
        "Document" -> Color(0xFFFFD600)
        else -> NeonCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A1B2D))
            .border(1.dp, Color(0xFF163E63), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Platform tag pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(platformColor.copy(alpha = 0.2f))
                        .border(1.dp, platformColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.platformName,
                        color = platformColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.url,
                    color = TextWhite,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Prominent Download button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF5722))
                    .clickable { onDownload() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ডাউনলোড",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Remove button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
