package com.shohan.pro.downloader.data.network

import com.shohan.pro.downloader.data.model.MediaCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedMedia(
    val directUrl: String,
    val title: String,
    val category: MediaCategory,
    val extension: String,
    val thumbnailUrl: String? = null
)

object SocialMediaExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    fun isSocialMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") || lower.contains("fb.watch") ||
                lower.contains("instagram.com") || lower.contains("tiktok.com") ||
                lower.contains("twitter.com") || lower.contains("x.com") ||
                lower.contains("youtube.com") || lower.contains("youtu.be") ||
                lower.contains("pinterest.com")
    }

    suspend fun extractMedia(rawUrl: String): ExtractedMedia? = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        val lower = cleanUrl.lowercase()

        when {
            lower.contains("facebook.com") || lower.contains("fb.watch") -> {
                extractFacebook(cleanUrl)
            }
            lower.contains("instagram.com") -> {
                extractInstagram(cleanUrl)
            }
            lower.contains("tiktok.com") -> {
                extractTikTok(cleanUrl)
            }
            lower.contains("twitter.com") || lower.contains("x.com") -> {
                extractGenericMeta(cleanUrl, "Twitter_Video", MediaCategory.VIDEO, "mp4")
            }
            else -> {
                extractGenericMeta(cleanUrl, "Media", MediaCategory.VIDEO, "mp4")
            }
        }
    }

    private fun extractFacebook(url: String): ExtractedMedia? {
        try {
            val html = fetchHtml(url, DESKTOP_UA) ?: fetchHtml(url, MOBILE_UA)
            if (html != null) {
                // 1. Try finding HD or SD direct video URLs in JavaScript / JSON payload
                val hdMatch = findFirstRegex(html, listOf(
                    "\"browser_native_hd_url\":\"(https:[^\"]+)\"",
                    "\"playable_url_quality_hd\":\"(https:[^\"]+)\"",
                    "\"hd_src\":\"(https:[^\"]+)\"",
                    "\"hd_src_no_ratelimit\":\"(https:[^\"]+)\""
                ))
                val sdMatch = findFirstRegex(html, listOf(
                    "\"browser_native_sd_url\":\"(https:[^\"]+)\"",
                    "\"playable_url\":\"(https:[^\"]+)\"",
                    "\"sd_src\":\"(https:[^\"]+)\"",
                    "\"sd_src_no_ratelimit\":\"(https:[^\"]+)\""
                ))

                val videoUrl = unescapeJson(hdMatch ?: sdMatch)
                val metaVideo = extractMetaProperty(html, "og:video") ?: extractMetaProperty(html, "og:video:secure_url")
                val finalVideoUrl = videoUrl ?: metaVideo ?: url

                val title = extractTitle(html, "Facebook_Reel_${extractIdFromUrl(url)}")
                val thumb = extractMetaProperty(html, "og:image")

                return ExtractedMedia(
                    directUrl = finalVideoUrl,
                    title = title,
                    category = MediaCategory.VIDEO,
                    extension = "mp4",
                    thumbnailUrl = thumb
                )
            }
        } catch (_: Exception) {}

        return ExtractedMedia(
            directUrl = url,
            title = "Facebook_Video_${extractIdFromUrl(url)}",
            category = MediaCategory.VIDEO,
            extension = "mp4"
        )
    }

    private fun extractInstagram(url: String): ExtractedMedia? {
        try {
            val html = fetchHtml(url, DESKTOP_UA) ?: fetchHtml(url, MOBILE_UA)
            if (html != null) {
                val videoUrl = findFirstRegex(html, listOf(
                    "\"video_url\":\"(https:[^\"]+)\"",
                    "\"video_versions\":\\[\\{\"url\":\"(https:[^\"]+)\"",
                    "<meta property=\"og:video\" content=\"([^\"]+)\"",
                    "<meta property=\"og:video:secure_url\" content=\"([^\"]+)\""
                ))?.let { unescapeJson(it) }

                val isImage = html.contains("\"__typename\":\"GraphImage\"") || !html.contains("og:video")
                val imageUrl = if (isImage) {
                    findFirstRegex(html, listOf(
                        "\"display_url\":\"(https:[^\"]+)\"",
                        "<meta property=\"og:image\" content=\"([^\"]+)\""
                    ))?.let { unescapeJson(it) }
                } else null

                val finalUrl = videoUrl ?: imageUrl ?: url
                val category = if (isImage && videoUrl == null) MediaCategory.IMAGE else MediaCategory.VIDEO
                val ext = if (category == MediaCategory.IMAGE) "jpg" else "mp4"
                val title = extractTitle(html, "Instagram_Reel_${extractIdFromUrl(url)}")

                return ExtractedMedia(
                    directUrl = finalUrl,
                    title = title,
                    category = category,
                    extension = ext,
                    thumbnailUrl = imageUrl
                )
            }
        } catch (_: Exception) {}

        return ExtractedMedia(
            directUrl = url,
            title = "Instagram_Reel_${extractIdFromUrl(url)}",
            category = MediaCategory.VIDEO,
            extension = "mp4"
        )
    }

    private fun extractTikTok(url: String): ExtractedMedia? {
        try {
            val html = fetchHtml(url, MOBILE_UA)
            if (html != null) {
                val videoUrl = findFirstRegex(html, listOf(
                    "\"playAddr\":\"(https:[^\"]+)\"",
                    "<meta property=\"og:video\" content=\"([^\"]+)\"",
                    "<video[^>]*src=\"([^\"]+)\""
                ))?.let { unescapeJson(it) } ?: url

                val title = extractTitle(html, "TikTok_Video_${extractIdFromUrl(url)}")
                return ExtractedMedia(
                    directUrl = videoUrl,
                    title = title,
                    category = MediaCategory.VIDEO,
                    extension = "mp4"
                )
            }
        } catch (_: Exception) {}

        return ExtractedMedia(
            directUrl = url,
            title = "TikTok_Video_${extractIdFromUrl(url)}",
            category = MediaCategory.VIDEO,
            extension = "mp4"
        )
    }

    private fun extractGenericMeta(
        url: String,
        prefix: String,
        defaultCategory: MediaCategory,
        defaultExt: String
    ): ExtractedMedia? {
        try {
            val html = fetchHtml(url, DESKTOP_UA)
            if (html != null) {
                val metaVideo = extractMetaProperty(html, "og:video") ?: extractMetaProperty(html, "og:video:secure_url")
                val metaImage = extractMetaProperty(html, "og:image")
                val title = extractTitle(html, "${prefix}_${extractIdFromUrl(url)}")

                if (metaVideo != null) {
                    return ExtractedMedia(
                        directUrl = metaVideo,
                        title = title,
                        category = MediaCategory.VIDEO,
                        extension = "mp4",
                        thumbnailUrl = metaImage
                    )
                } else if (metaImage != null) {
                    return ExtractedMedia(
                        directUrl = metaImage,
                        title = title,
                        category = MediaCategory.IMAGE,
                        extension = "jpg",
                        thumbnailUrl = metaImage
                    )
                }
            }
        } catch (_: Exception) {}

        return ExtractedMedia(
            directUrl = url,
            title = "${prefix}_${extractIdFromUrl(url)}",
            category = defaultCategory,
            extension = defaultExt
        )
    }

    private fun fetchHtml(url: String, userAgent: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractMetaProperty(html: String, property: String): String? {
        val pattern = Pattern.compile("<meta\\s+property=\"$property\"\\s+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return unescapeJson(matcher.group(1))
        }
        val altPattern = Pattern.compile("<meta\\s+content=\"([^\"]+)\"\\s+property=\"$property\"", Pattern.CASE_INSENSITIVE)
        val altMatcher = altPattern.matcher(html)
        if (altMatcher.find()) {
            return unescapeJson(altMatcher.group(1))
        }
        return null
    }

    private fun extractTitle(html: String, fallback: String): String {
        val ogTitle = extractMetaProperty(html, "og:title")
        if (!ogTitle.isNullOrBlank()) {
            return cleanTitle(ogTitle)
        }
        val pattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val title = matcher.group(1)?.trim()
            if (!title.isNullOrBlank()) {
                return cleanTitle(title)
            }
        }
        return fallback
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9 _-]"), "")
            .trim()
            .take(40)
            .ifBlank { "Social_Media_Download" }
    }

    private fun extractIdFromUrl(url: String): String {
        return try {
            val path = URI(url).path ?: ""
            val last = path.trimEnd('/').substringAfterLast('/')
            if (last.length in 4..30) last else (System.currentTimeMillis() % 100000).toString()
        } catch (_: Exception) {
            (System.currentTimeMillis() % 100000).toString()
        }
    }

    private fun findFirstRegex(text: String, patterns: List<String>): String? {
        for (p in patterns) {
            val pattern = Pattern.compile(p)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun unescapeJson(str: String?): String? {
        if (str == null) return null
        return str.replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
    }
}
