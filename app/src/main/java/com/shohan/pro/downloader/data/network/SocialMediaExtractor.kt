package com.shohan.pro.downloader.data.network

import com.shohan.pro.downloader.data.model.MediaCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLEncoder
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
    private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0"

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
                extractTwitter(cleanUrl)
            }
            else -> {
                extractGenericMeta(cleanUrl, "Media", MediaCategory.VIDEO, "mp4")
            }
        }
    }

    private fun extractFacebook(url: String): ExtractedMedia? {
        try {
            val resolvedUrl = followRedirects(url)
            val id = extractFacebookId(resolvedUrl) ?: extractFacebookId(url) ?: (System.currentTimeMillis() % 10000).toString()

            // 1. Try public video plugin embed endpoint (yields direct fbcdn MP4 stream with audio!)
            val encoded = URLEncoder.encode(resolvedUrl, "UTF-8")
            val pluginUrl = "https://www.facebook.com/plugins/video.php?href=$encoded&_fb_noscript=1"
            val pluginHtml = fetchHtml(pluginUrl, DESKTOP_UA)
            if (pluginHtml != null) {
                val unescaped = pluginHtml.replace("\\/", "/")
                    .replace("\\u00253D", "=")
                    .replace("&amp;", "&")
                    .replace("\\u0026", "&")

                val videoUrl = findFirstRegex(unescaped, listOf(
                    "(https://video[^\"'\\s<>]+\\.mp4[^\"'\\s<>]*)",
                    "\"playable_url\":\"(https:[^\"]+)\"",
                    "\"browser_native_hd_url\":\"(https:[^\"]+)\"",
                    "\"browser_native_sd_url\":\"(https:[^\"]+)\"",
                    "\"hd_src\":\"(https:[^\"]+)\"",
                    "\"sd_src\":\"(https:[^\"]+)\""
                ))

                if (!videoUrl.isNullOrBlank() && videoUrl.contains(".mp4")) {
                    val cleanUrl = unescapeJson(videoUrl) ?: videoUrl
                    val title = extractTitle(pluginHtml, "Facebook_Reel_$id")
                    return ExtractedMedia(
                        directUrl = cleanUrl,
                        title = title,
                        category = MediaCategory.VIDEO,
                        extension = "mp4"
                    )
                }
            }

            // 2. Direct page scraping fallback
            val html = fetchHtml(resolvedUrl, DESKTOP_UA) ?: fetchHtml(resolvedUrl, MOBILE_UA)
            if (html != null) {
                val unescaped = html.replace("\\/", "/")
                    .replace("\\u00253D", "=")
                    .replace("&amp;", "&")
                    .replace("\\u0026", "&")

                val videoUrl = findFirstRegex(unescaped, listOf(
                    "(https://video[^\"'\\s<>]+\\.mp4[^\"'\\s<>]*)",
                    "\"browser_native_hd_url\":\"(https:[^\"]+)\"",
                    "\"browser_native_sd_url\":\"(https:[^\"]+)\"",
                    "\"playable_url_quality_hd\":\"(https:[^\"]+)\"",
                    "\"playable_url\":\"(https:[^\"]+)\"",
                    "\"hd_src\":\"(https:[^\"]+)\"",
                    "\"sd_src\":\"(https:[^\"]+)\"",
                    "<meta property=\"og:video\" content=\"([^\"]+)\"",
                    "<meta property=\"og:video:secure_url\" content=\"([^\"]+)\""
                ))

                if (!videoUrl.isNullOrBlank() && videoUrl.contains(".mp4")) {
                    val cleanUrl = unescapeJson(videoUrl) ?: videoUrl
                    val title = extractTitle(html, "Facebook_Video_$id")
                    val thumb = extractMetaProperty(html, "og:image")
                    return ExtractedMedia(
                        directUrl = cleanUrl,
                        title = title,
                        category = MediaCategory.VIDEO,
                        extension = "mp4",
                        thumbnailUrl = thumb
                    )
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun extractInstagram(url: String): ExtractedMedia? {
        val shortcode = extractInstagramShortcode(url)

        if (shortcode != null) {
            val embedUrl = "https://www.instagram.com/reel/$shortcode/embed/captioned/"
            val embedHtml = fetchHtml(embedUrl, DESKTOP_UA) ?: fetchHtml(embedUrl, MOBILE_UA)
            if (embedHtml != null) {
                val unescaped = embedHtml.replace("\\/", "/")
                    .replace("\\u00253D", "=")
                    .replace("&amp;", "&")
                    .replace("\\u0026", "&")

                val videoUrl = findFirstRegex(unescaped, listOf(
                    "video_url\":\"(https:[^\"]+)\"",
                    "\"video_url\":\"(https:[^\"]+)\"",
                    "(https://[^\"]*cdninstagram\\.com[^\"\\s<>]+\\.mp4[^\"\\s<>]*)",
                    "<video[^>]*src=\"([^\"]+)\"",
                    "\"src\":\"(https:[^\"\\\\]*\\.mp4[^\"]*)\""
                ))

                if (!videoUrl.isNullOrBlank() && !videoUrl.contains("instagram.com")) {
                    return ExtractedMedia(
                        directUrl = unescapeJson(videoUrl) ?: videoUrl,
                        title = "Instagram_Reel_$shortcode",
                        category = MediaCategory.VIDEO,
                        extension = "mp4"
                    )
                }

                if (!url.contains("/reel/")) {
                    val photoUrl = findFirstRegex(unescaped, listOf(
                        "\"display_url\":\"(https:[^\"]+)\"",
                        "<img[^>]*class=\"EmbeddedMediaImage\"[^>]*src=\"([^\"]+)\"",
                        "<meta property=\"og:image\" content=\"([^\"]+)\""
                    ))

                    if (!photoUrl.isNullOrBlank() && !photoUrl.contains("instagram.com")) {
                        return ExtractedMedia(
                            directUrl = unescapeJson(photoUrl) ?: photoUrl,
                            title = "Instagram_Photo_$shortcode",
                            category = MediaCategory.IMAGE,
                            extension = "jpg"
                        )
                    }
                }
            }
        }

        try {
            val html = fetchHtml(url, DESKTOP_UA) ?: fetchHtml(url, MOBILE_UA)
            if (html != null) {
                val unescaped = html.replace("\\/", "/").replace("&amp;", "&")
                val videoUrl = findFirstRegex(unescaped, listOf(
                    "\"video_url\":\"(https:[^\"]+)\"",
                    "(https://[^\"]*cdninstagram\\.com[^\"\\s<>]+\\.mp4[^\"\\s<>]*)",
                    "<meta property=\"og:video\" content=\"([^\"]+)\"",
                    "<meta property=\"og:video:secure_url\" content=\"([^\"]+)\""
                ))

                if (!videoUrl.isNullOrBlank() && !videoUrl.contains("instagram.com")) {
                    return ExtractedMedia(
                        directUrl = unescapeJson(videoUrl) ?: videoUrl,
                        title = "Instagram_Reel_${shortcode ?: extractIdFromUrl(url)}",
                        category = MediaCategory.VIDEO,
                        extension = "mp4"
                    )
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun extractTikTok(url: String): ExtractedMedia? {
        try {
            val html = fetchHtml(url, MOBILE_UA)
            if (html != null) {
                val unescaped = html.replace("\\/", "/").replace("&amp;", "&")
                val videoUrl = findFirstRegex(unescaped, listOf(
                    "\"playAddr\":\"(https:[^\"]+)\"",
                    "<meta property=\"og:video\" content=\"([^\"]+)\"",
                    "<video[^>]*src=\"([^\"]+)\""
                ))

                if (!videoUrl.isNullOrBlank() && !videoUrl.contains("tiktok.com")) {
                    val title = extractTitle(html, "TikTok_Video_${extractIdFromUrl(url)}")
                    return ExtractedMedia(
                        directUrl = unescapeJson(videoUrl) ?: videoUrl,
                        title = title,
                        category = MediaCategory.VIDEO,
                        extension = "mp4"
                    )
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun extractTwitter(url: String): ExtractedMedia? {
        return extractGenericMeta(url, "Twitter_Video", MediaCategory.VIDEO, "mp4")
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
                if (!metaVideo.isNullOrBlank() && (metaVideo.startsWith("http://") || metaVideo.startsWith("https://"))) {
                    val metaImage = extractMetaProperty(html, "og:image")
                    val title = extractTitle(html, "${prefix}_${extractIdFromUrl(url)}")
                    return ExtractedMedia(
                        directUrl = metaVideo,
                        title = title,
                        category = MediaCategory.VIDEO,
                        extension = "mp4",
                        thumbnailUrl = metaImage
                    )
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun followRedirects(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", DESKTOP_UA)
                .build()
            client.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        } catch (_: Exception) {
            url
        }
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

    private fun extractInstagramShortcode(url: String): String? {
        return try {
            val pattern = Pattern.compile("(?:reel|p|tv)/([A-Za-z0-9_-]+)")
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                matcher.group(1)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFacebookId(url: String): String? {
        return try {
            val pattern = Pattern.compile("(?:reel|videos|watch[?&]v=)/?([0-9]+)")
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                matcher.group(1)
            } else null
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
            .ifBlank { "Media_Download" }
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
            .replace("\\\"", "\"")
    }
}
