package com.shohan.pro.downloader.util

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import java.util.regex.Pattern

object ClipboardHelper {
    private val URL_PATTERN = Pattern.compile(
        "\\b(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
        Pattern.CASE_INSENSITIVE
    )

    fun getClipboardUrl(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null

        if (!clipboard.hasPrimaryClip()) return null

        val description = clipboard.primaryClipDescription
        if (description != null && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                    description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))
        ) {
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim() ?: item?.uri?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                val extracted = extractUrl(text)
                if (extracted != null) {
                    return extracted
                }
            }
        }
        return null
    }

    fun extractUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(0)
        }
        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed.substringBefore(' ').substringBefore('\n')
        }
        if (trimmed.startsWith("www.", ignoreCase = true)) {
            return "https://$trimmed".substringBefore(' ').substringBefore('\n')
        }
        return null
    }
}
