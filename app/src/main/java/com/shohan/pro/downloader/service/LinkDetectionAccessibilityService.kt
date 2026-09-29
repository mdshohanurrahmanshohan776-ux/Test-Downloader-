package com.shohan.pro.downloader.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.shohan.pro.downloader.ui.FloatingDownloadActivity
import com.shohan.pro.downloader.util.ClipboardHelper

class LinkDetectionAccessibilityService : AccessibilityService() {

    private var lastProcessedUrl: String? = null
    private var clipboardManager: ClipboardManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check clipboard directly as accessibility service has full permission to read clipboard on Android 10-14
        try {
            val clipUrl = ClipboardHelper.getClipboardUrl(this)
            if (!clipUrl.isNullOrBlank() && clipUrl != lastProcessedUrl) {
                lastProcessedUrl = clipUrl
                FloatingDownloadActivity.start(this, clipUrl)
                return
            }
        } catch (_: Exception) {}

        // Also inspect text selection / change events
        val textList = event.text
        if (textList != null && textList.isNotEmpty()) {
            for (charSeq in textList) {
                val str = charSeq?.toString() ?: continue
                val extracted = ClipboardHelper.extractUrl(str)
                if (extracted != null && extracted != lastProcessedUrl) {
                    lastProcessedUrl = extracted
                    FloatingDownloadActivity.start(this, extracted)
                    break
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required method
    }
}
