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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import com.shohan.pro.downloader.service.ClipboardMonitorService
import androidx.compose.material.icons.filled.Speed
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
    detectedClipboardUrl: String?,
    isAnalyzing: Boolean,
    queueCount: Int,
    historyCount: Int,
    onAnalyzeUrl: (String) -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onDismissClipboardBanner: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
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
                                text = "Auto clipboard detection & custom resolution",
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

        // Instant Auto-Popup Overlay Permission Card (shown ONLY if permission not yet granted)
        if (!canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item {
                PapiKingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("everywhere_popup_card"),
                    contentPadding = 14.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0C2136)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Auto-Popup",
                                    color = NeonCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pops up instantly when copying links in ANY app",
                                    color = Color(0xFFFFB58D),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "অন্য যেকোনো অ্যাপ (ইউটিউব, ফেসবুক, ক্রোম ইত্যাদি) থেকে লিংক কপি করার সাথে সাথেই ডাউনলোড ডায়ালগ শো করার জন্য এই পারমিশনটি অন করুন।",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        PapiKingButton(
                            text = "Grant 'Appear On Top' Permission",
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Settings,
                            testTag = "enable_overlay_button"
                        )
                    }
                }
            }
        }

        // Live Clipboard Detection Banner (if a link was copied)
        if (!detectedClipboardUrl.isNullOrBlank()) {
            item {
                PapiKingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clipboard_detected_banner"),
                    contentPadding = 12.dp
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Copied Link Detected!",
                                color = NeonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = onDismissClipboardBanner,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Dismiss",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = detectedClipboardUrl,
                            color = TextWhite,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PapiKingButton(
                            text = "Download With Resolution Dialog",
                            onClick = { onAnalyzeUrl(detectedClipboardUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isAnalyzing,
                            testTag = "clipboard_banner_download_button"
                        )
                    }
                }
            }
        }

        // URL Input Card
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
                        text = "Supports Video, Image, Audio, Document, Archive, etc.",
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
                            Text("https://... (video, photo, doc link)", color = TextMuted, fontSize = 13.sp)
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
                        Triple("Images", "🖼️ Original HD, 1080p, 720p, Thumb", "JPG / PNG / WebP"),
                        Triple("Documents", "📄 PDF, DOCX, XLSX, TXT, PPT", "Full & Stream"),
                        Triple("Audio", "🎵 320 kbps, 192 kbps, 128 kbps", "MP3 Extract"),
                        Triple("Archives", "📦 ZIP, RAR, 7Z, APK, TAR.GZ", "High Speed")
                    )

                    categories.forEach { (title, desc, format) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = desc.substringBefore(' '),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = desc.substringAfter(' '),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0C2136))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = format,
                                    color = NeonGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Features Highlight Card
        item {
            PapiKingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("features_card"),
                contentPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background Download Engine",
                            color = NeonGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Sequential Queue: downloads files one-by-one with custom priority reordering.\n• Background Processing: continues downloading when screen is locked or in other apps.\n• Smart Auto-Detection: copy any link and open the app for instant resolution dialog.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
