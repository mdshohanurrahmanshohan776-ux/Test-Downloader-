package com.shohan.pro.downloader.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shohan.pro.downloader.data.model.ResolutionOption
import com.shohan.pro.downloader.data.network.AnalyzedMediaInfo
import com.shohan.pro.downloader.ui.components.PapiKingButton
import com.shohan.pro.downloader.ui.components.PapiKingCard
import com.shohan.pro.downloader.ui.theme.BrandNavyDark
import com.shohan.pro.downloader.ui.theme.NeonCoral
import com.shohan.pro.downloader.ui.theme.NeonCyan
import com.shohan.pro.downloader.ui.theme.NeonGold
import com.shohan.pro.downloader.ui.theme.TextMuted
import com.shohan.pro.downloader.ui.theme.TextWhite

@Composable
fun DownloadResolutionDialog(
    mediaInfo: AnalyzedMediaInfo,
    selectedOption: ResolutionOption?,
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onOptionSelected: (ResolutionOption) -> Unit,
    onDownloadNow: () -> Unit,
    onAddToQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PapiKingCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("download_resolution_dialog"),
            contentPadding = 18.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header: Category Icon + Title
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
                        Text(
                            text = mediaInfo.category.iconEmoji,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download ${mediaInfo.category.displayName}",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mediaInfo.originalUrl,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Editable File Name
                Text(
                    text = "File Name",
                    color = Color(0xFFFFB58D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_filename_input"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextWhite,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF1D4D7C),
                        focusedContainerColor = BrandNavyDark,
                        unfocusedContainerColor = BrandNavyDark,
                        cursorColor = NeonCyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Resolution Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Quality / Resolution",
                        color = Color(0xFFFFB58D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${mediaInfo.resolutionOptions.size} options available",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Resolutions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaInfo.resolutionOptions) { option ->
                        val isSelected = selectedOption?.id == option.id
                        val borderColor = if (isSelected) NeonCyan else Color(0xFF1D4D7C)
                        val bg = if (isSelected) Color(0xFF163E66) else Color(0xFF0C2136)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resolution_option_${option.id}")
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { onOptionSelected(option) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.label,
                                            color = if (isSelected) Color.White else TextMuted,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )

                                        if (option.isRecommended) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NeonCoral)
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "BEST",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${option.resolution} • ${option.format}",
                                        color = Color(0xFFB0C4DE),
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = option.estimatedSize,
                                    color = if (isSelected) NeonGold else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons using PapiKingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PapiKingButton(
                        text = "Add Queue",
                        onClick = onAddToQueue,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Queue,
                        testTag = "dialog_add_queue_button"
                    )

                    PapiKingButton(
                        text = "Download",
                        onClick = onDownloadNow,
                        modifier = Modifier.weight(1.2f),
                        icon = Icons.Default.Download,
                        testTag = "dialog_download_now_button"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Cancel Link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
