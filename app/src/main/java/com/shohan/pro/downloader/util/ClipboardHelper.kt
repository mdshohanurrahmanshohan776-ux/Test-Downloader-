package com.shohan.pro.downloader.util

import android.content.ClipboardManager
import android.content.Context
import java.util.regex.Pattern

object ClipboardHelper {
    private val URL_PATTERN = Pattern.compile(
        "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
        Pattern.CASE_INSENSITIVE
    )

    fun getClipboardUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null

        try {
            if (!clipboard.hasPrimaryClip()) return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount > 0) {
                val item = clip.getItemAt(0) ?: return null
                val text = item.coerceToText(context)?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    return extractUrl(text)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun extractUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(0)?.trim()
        }
        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed.substringBefore(' ').substringBefore('\n').trim()
        }
        if (trimmed.startsWith("www.", ignoreCase = true)) {
            return "https://${trimmed.substringBefore(' ').substringBefore('\n').trim()}"
        }
        return null
    }
}
