package com.shohan.pro.downloader.data.network

import com.shohan.pro.downloader.data.model.MediaCategory
import com.shohan.pro.downloader.data.model.ResolutionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AnalyzedMediaInfo(
    val originalUrl: String,
    val suggestedFileName: String,
    val category: MediaCategory,
    val contentLength: Long? = null,
    val mimeType: String? = null,
    val resolutionOptions: List<ResolutionOption>,
    val defaultOption: ResolutionOption
)

class MediaUrlInspector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun inspectUrl(url: String): AnalyzedMediaInfo = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // 1. Social media URL extraction (Facebook, Instagram, TikTok, etc.)
        if (SocialMediaExtractor.isSocialMediaUrl(cleanUrl)) {
            val extracted = SocialMediaExtractor.extractMedia(cleanUrl)
            if (extracted != null && extracted.streams.isNotEmpty()) {
                val category = extracted.category
                val fileName = "${extracted.title}.${extracted.extension}"
                val primaryStream = extracted.streams.first()
                val contentLength = primaryStream.bytes

                val options = extracted.streams.mapIndexed { index, stream ->
                    val sizeFormatted = if (stream.bytes != null && stream.bytes > 0) {
                        formatBytes(stream.bytes)
                    } else {
                        "Direct Stream"
                    }
                    ResolutionOption(
                        id = "stream_$index",
                        label = stream.label,
                        resolution = stream.resolution,
                        estimatedSize = sizeFormatted,
                        format = extracted.extension.uppercase(),
                        isRecommended = stream.isRecommended,
                        directStreamUrl = stream.url,
                        exactBytes = stream.bytes
                    )
                }.toMutableList()

                // Add Audio option for video media
                if (category == MediaCategory.VIDEO) {
                    val audioBytes = if (contentLength != null && contentLength > 0) (contentLength * 0.15).toLong() else null
                    val audioSize = if (audioBytes != null) formatBytes(audioBytes) else "Audio Track"
                    options.add(
                        ResolutionOption(
                            id = "audio_only",
                            label = "Audio Only",
                            resolution = "Original Audio",
                            estimatedSize = audioSize,
                            format = "MP3",
                            isRecommended = false,
                            isAudioOnly = true,
                            directStreamUrl = primaryStream.url,
                            exactBytes = audioBytes
                        )
                    )
                }

                val defaultOption = options.firstOrNull { it.isRecommended } ?: options.first()

                return@withContext AnalyzedMediaInfo(
                    originalUrl = primaryStream.url,
                    suggestedFileName = fileName,
                    category = category,
                    contentLength = contentLength,
                    mimeType = if (category == MediaCategory.IMAGE) "image/jpeg" else "video/mp4",
                    resolutionOptions = options,
                    defaultOption = defaultOption
                )
            }
        }

        // 2. Direct server verification via HEAD request
        var mimeType: String? = null
        var contentLength: Long? = null
        var headerFileName: String? = null

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    mimeType = response.header("Content-Type")?.lowercase(Locale.ROOT)
                    contentLength = response.header("Content-Length")?.toLongOrNull()
                    val disposition = response.header("Content-Disposition")
                    if (disposition != null && disposition.contains("filename=")) {
                        headerFileName = disposition.substringAfter("filename=")
                            .trim('"', ' ', ';', '\'')
                    }
                }
            }
        } catch (_: Exception) {}

        val category = detectCategory(cleanUrl, mimeType)
        val fileName = headerFileName ?: extractFileNameFromUrl(cleanUrl, category)
        val ext = fileName.substringAfterLast('.', "bin").uppercase()

        val finalContentLength = contentLength
        val verifiedSizeStr = if (finalContentLength != null && finalContentLength > 0) {
            formatBytes(finalContentLength)
        } else {
            "Direct Stream"
        }

        val options = mutableListOf<ResolutionOption>()
        options.add(
            ResolutionOption(
                id = "original_file",
                label = "Original File",
                resolution = if (finalContentLength != null && finalContentLength > 0) "Server Verified" else "Direct Stream",
                estimatedSize = verifiedSizeStr,
                format = ext,
                isRecommended = true,
                directStreamUrl = cleanUrl,
                exactBytes = finalContentLength
            )
        )

        if (category == MediaCategory.VIDEO) {
            val audioBytes = if (finalContentLength != null && finalContentLength > 0) (finalContentLength * 0.15).toLong() else null
            val audioSize = if (audioBytes != null) formatBytes(audioBytes) else "Audio Track"
            options.add(
                ResolutionOption(
                    id = "audio_only",
                    label = "Audio Only",
                    resolution = "Original Audio",
                    estimatedSize = audioSize,
                    format = "MP3",
                    isRecommended = false,
                    isAudioOnly = true,
                    directStreamUrl = cleanUrl,
                    exactBytes = audioBytes
                )
            )
        }

        val defaultOption = options.first()

        AnalyzedMediaInfo(
            originalUrl = cleanUrl,
            suggestedFileName = fileName,
            category = category,
            contentLength = finalContentLength,
            mimeType = mimeType,
            resolutionOptions = options,
            defaultOption = defaultOption
        )
    }

    private fun detectCategory(url: String, mimeType: String?): MediaCategory {
        val lowerUrl = url.lowercase(Locale.ROOT)

        if (mimeType != null) {
            if (mimeType.startsWith("video/")) return MediaCategory.VIDEO
            if (mimeType.startsWith("image/")) return MediaCategory.IMAGE
            if (mimeType.startsWith("audio/")) return MediaCategory.AUDIO
            if (mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("text/")) return MediaCategory.DOCUMENT
            if (mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("rar") || mimeType.contains("octet-stream")) {
                if (lowerUrl.endsWith(".apk")) return MediaCategory.OTHER
                return MediaCategory.ARCHIVE
            }
        }

        // URL extension detection
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".mkv") || lowerUrl.contains(".webm") ||
            lowerUrl.contains(".mov") || lowerUrl.contains(".avi") || lowerUrl.contains(".flv") ||
            lowerUrl.contains(".m3u8") || lowerUrl.contains("video")
        ) {
            return MediaCategory.VIDEO
        }

        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".png") ||
            lowerUrl.contains(".webp") || lowerUrl.contains(".gif") || lowerUrl.contains(".svg")
        ) {
            return MediaCategory.IMAGE
        }

        if (lowerUrl.contains(".mp3") || lowerUrl.contains(".m4a") || lowerUrl.contains(".wav") ||
            lowerUrl.contains(".aac") || lowerUrl.contains(".flac") || lowerUrl.contains(".ogg")
        ) {
            return MediaCategory.AUDIO
        }

        if (lowerUrl.contains(".pdf") || lowerUrl.contains(".doc") || lowerUrl.contains(".docx") ||
            lowerUrl.contains(".txt") || lowerUrl.contains(".xlsx") || lowerUrl.contains(".pptx")
        ) {
            return MediaCategory.DOCUMENT
        }

        if (lowerUrl.contains(".zip") || lowerUrl.contains(".rar") || lowerUrl.contains(".7z") ||
            lowerUrl.contains(".apk") || lowerUrl.contains(".tar")
        ) {
            return MediaCategory.ARCHIVE
        }

        return MediaCategory.OTHER
    }

    private fun extractFileNameFromUrl(url: String, category: MediaCategory): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: ""
            val rawName = path.substringAfterLast('/', "")
            val decodedName = if (rawName.isNotEmpty()) {
                URLDecoder.decode(rawName, "UTF-8")
            } else ""

            if (decodedName.contains('.') && decodedName.length > 3) {
                decodedName.substringBefore('?').substringBefore('&')
            } else {
                val host = uri.host?.replace("www.", "")?.substringBefore('.') ?: "media"
                val timestamp = System.currentTimeMillis() % 100000
                val ext = when (category) {
                    MediaCategory.VIDEO -> "mp4"
                    MediaCategory.IMAGE -> "jpg"
                    MediaCategory.DOCUMENT -> "pdf"
                    MediaCategory.AUDIO -> "mp3"
                    MediaCategory.ARCHIVE -> "zip"
                    MediaCategory.OTHER -> "bin"
                }
                "${host}_download_$timestamp.$ext"
            }
        } catch (_: Exception) {
            "download_${System.currentTimeMillis() % 100000}.${when(category) {
                MediaCategory.VIDEO -> "mp4"
                MediaCategory.IMAGE -> "jpg"
                MediaCategory.DOCUMENT -> "pdf"
                MediaCategory.AUDIO -> "mp3"
                else -> "bin"
            }}"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
