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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun inspectUrl(url: String): AnalyzedMediaInfo = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        var mimeType: String? = null
        var contentLength: Long? = null
        var headerFileName: String? = null

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    mimeType = response.header("Content-Type")?.lowercase()
                    contentLength = response.header("Content-Length")?.toLongOrNull()
                    val disposition = response.header("Content-Disposition")
                    if (disposition != null && disposition.contains("filename=")) {
                        headerFileName = disposition.substringAfter("filename=")
                            .trim('"', ' ', ';', '\'')
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback to pattern matching
        }

        val category = detectCategory(cleanUrl, mimeType)
        val fileName = headerFileName ?: extractFileNameFromUrl(cleanUrl, category)
        val options = generateResolutionOptions(category, contentLength)
        val defaultOption = options.firstOrNull { it.isRecommended } ?: options.first()

        AnalyzedMediaInfo(
            originalUrl = cleanUrl,
            suggestedFileName = fileName,
            category = category,
            contentLength = contentLength,
            mimeType = mimeType,
            resolutionOptions = options,
            defaultOption = defaultOption
        )
    }

    private fun detectCategory(url: String, mimeType: String?): MediaCategory {
        val lowerUrl = url.lowercase(Locale.ROOT)

        if (mimeType != null) {
            when {
                mimeType.startsWith("video/") -> return MediaCategory.VIDEO
                mimeType.startsWith("image/") -> return MediaCategory.IMAGE
                mimeType.startsWith("audio/") -> return MediaCategory.AUDIO
                mimeType.contains("pdf") || mimeType.contains("msword") ||
                        mimeType.contains("officedocument") || mimeType.contains("text/") -> return MediaCategory.DOCUMENT
                mimeType.contains("zip") || mimeType.contains("compressed") ||
                        mimeType.contains("tar") || mimeType.contains("rar") -> return MediaCategory.ARCHIVE
            }
        }

        // Known Video Platforms & Extensions
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") ||
            lowerUrl.contains("tiktok.com") || lowerUrl.contains("facebook.com") ||
            lowerUrl.contains("fb.watch") || lowerUrl.contains("instagram.com/reel") ||
            lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") ||
            lowerUrl.contains("vimeo.com") || lowerUrl.contains("dailymotion.com") ||
            lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".webm") ||
            lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".m4v") ||
            lowerUrl.endsWith(".flv") || lowerUrl.contains(".mp4?") || lowerUrl.contains(".m3u8")
        ) {
            return MediaCategory.VIDEO
        }

        // Images
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") ||
            lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".svg") ||
            lowerUrl.endsWith(".bmp") || lowerUrl.contains("unsplash.com") || lowerUrl.contains("imgur.com") ||
            lowerUrl.contains("pinterest.com") || lowerUrl.contains("instagram.com/p/")
        ) {
            return MediaCategory.IMAGE
        }

        // Audio
        if (lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".m4a") ||
            lowerUrl.endsWith(".aac") || lowerUrl.endsWith(".flac") || lowerUrl.endsWith(".ogg") ||
            lowerUrl.contains("soundcloud.com") || lowerUrl.contains("spotify.com")
        ) {
            return MediaCategory.AUDIO
        }

        // Documents
        if (lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".docx") || lowerUrl.endsWith(".doc") ||
            lowerUrl.endsWith(".xlsx") || lowerUrl.endsWith(".xls") || lowerUrl.endsWith(".pptx") ||
            lowerUrl.endsWith(".ppt") || lowerUrl.endsWith(".txt") || lowerUrl.endsWith(".csv")
        ) {
            return MediaCategory.DOCUMENT
        }

        // Archives
        if (lowerUrl.endsWith(".zip") || lowerUrl.endsWith(".rar") || lowerUrl.endsWith(".7z") ||
            lowerUrl.endsWith(".apk") || lowerUrl.endsWith(".tar.gz")
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
                decodedName
            } else {
                val host = uri.host?.replace("www.", "")?.substringBefore('.') ?: "media"
                val timestamp = System.currentTimeMillis() % 100000
                val ext = when (category) {
                    MediaCategory.VIDEO -> "mp4"
                    MediaCategory.IMAGE -> "jpg"
                    MediaCategory.DOCUMENT -> "pdf"
                    MediaCategory.AUDIO -> "mp3"
                    MediaCategory.ARCHIVE -> "zip"
                    MediaCategory.OTHER -> "dat"
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

    private fun generateResolutionOptions(category: MediaCategory, totalBytes: Long?): List<ResolutionOption> {
        val baseSize = totalBytes ?: (35L * 1024L * 1024L)
        val sizeFormatted = formatBytes(baseSize)

        return when (category) {
            MediaCategory.VIDEO -> listOf(
                ResolutionOption(
                    id = "res_4k",
                    label = "4K Ultra HD",
                    resolution = "3840x2160 (2160p)",
                    estimatedSize = formatBytes((baseSize * 3.8).toLong()),
                    format = "MP4 • 60 FPS",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "res_2k",
                    label = "2K Quad HD",
                    resolution = "2560x1440 (1440p)",
                    estimatedSize = formatBytes((baseSize * 2.2).toLong()),
                    format = "MP4 • High Bitrate",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "res_1080p",
                    label = "Full HD (1080p)",
                    resolution = "1920x1080 (1080p)",
                    estimatedSize = sizeFormatted,
                    format = "MP4 • Recommended",
                    isRecommended = true
                ),
                ResolutionOption(
                    id = "res_720p",
                    label = "HD Ready (720p)",
                    resolution = "1280x720 (720p)",
                    estimatedSize = formatBytes((baseSize * 0.55).toLong()),
                    format = "MP4 • Balanced",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "res_480p",
                    label = "SD Quality (480p)",
                    resolution = "854x480 (480p)",
                    estimatedSize = formatBytes((baseSize * 0.3).toLong()),
                    format = "MP4 • Fast",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "res_360p",
                    label = "Data Saver (360p)",
                    resolution = "640x360 (360p)",
                    estimatedSize = formatBytes((baseSize * 0.18).toLong()),
                    format = "MP4 • Low Data",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "res_audio_mp3",
                    label = "Extract Audio Only (MP3)",
                    resolution = "320 kbps Stereo",
                    estimatedSize = formatBytes((baseSize * 0.12).coerceAtLeast(3.0 * 1024.0 * 1024.0).toLong()),
                    format = "MP3 Audio",
                    isRecommended = false,
                    isAudioOnly = true
                )
            )

            MediaCategory.IMAGE -> listOf(
                ResolutionOption(
                    id = "img_original",
                    label = "Original Quality (Best)",
                    resolution = "Source Resolution",
                    estimatedSize = if (totalBytes != null) formatBytes(totalBytes) else "Full Size",
                    format = "Source Format",
                    isRecommended = true
                ),
                ResolutionOption(
                    id = "img_1080p",
                    label = "High Resolution (1080p)",
                    resolution = "1920x1080",
                    estimatedSize = "~2.5 MB",
                    format = "JPG / PNG",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "img_720p",
                    label = "Medium Quality (720p)",
                    resolution = "1280x720",
                    estimatedSize = "~1.1 MB",
                    format = "WebP / JPG",
                    isRecommended = false
                ),
                ResolutionOption(
                    id = "img_thumb",
                    label = "Compact Thumbnail",
                    resolution = "480x480",
                    estimatedSize = "~350 KB",
                    format = "WebP",
                    isRecommended = false
                )
            )

            MediaCategory.DOCUMENT -> listOf(
                ResolutionOption(
                    id = "doc_original",
                    label = "Original Document",
                    resolution = "Full Resolution",
                    estimatedSize = if (totalBytes != null) formatBytes(totalBytes) else "Source Size",
                    format = "Source Format",
                    isRecommended = true
                ),
                ResolutionOption(
                    id = "doc_stream",
                    label = "Fast Stream Download",
                    resolution = "Multi-threaded Chunked",
                    estimatedSize = if (totalBytes != null) formatBytes(totalBytes) else "Optimized",
                    format = "Direct Stream",
                    isRecommended = false
                )
            )

            MediaCategory.AUDIO -> listOf(
                ResolutionOption(
                    id = "audio_320",
                    label = "Ultra High Quality (320 kbps)",
                    resolution = "Crystal Clear Audio",
                    estimatedSize = if (totalBytes != null) formatBytes(totalBytes) else "~8.5 MB",
                    format = "MP3 • 320 kbps",
                    isRecommended = true,
                    isAudioOnly = true
                ),
                ResolutionOption(
                    id = "audio_192",
                    label = "Standard Quality (192 kbps)",
                    resolution = "CD Quality Sound",
                    estimatedSize = "~5.2 MB",
                    format = "MP3 • 192 kbps",
                    isRecommended = false,
                    isAudioOnly = true
                ),
                ResolutionOption(
                    id = "audio_128",
                    label = "Data Saver (128 kbps)",
                    resolution = "Voice & Compact",
                    estimatedSize = "~3.4 MB",
                    format = "MP3 • 128 kbps",
                    isRecommended = false,
                    isAudioOnly = true
                )
            )

            else -> listOf(
                ResolutionOption(
                    id = "file_original",
                    label = "Original File",
                    resolution = "Standard",
                    estimatedSize = if (totalBytes != null) formatBytes(totalBytes) else "Unknown",
                    format = "Direct File",
                    isRecommended = true
                )
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
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
