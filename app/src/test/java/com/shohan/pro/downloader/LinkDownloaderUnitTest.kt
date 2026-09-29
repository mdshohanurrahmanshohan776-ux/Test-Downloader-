package com.shohan.pro.downloader

import com.shohan.pro.downloader.data.model.DownloadItem
import com.shohan.pro.downloader.data.model.DownloadStatus
import com.shohan.pro.downloader.data.model.MediaCategory
import com.shohan.pro.downloader.util.ClipboardHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDownloaderUnitTest {

    @Test
    fun testClipboardUrlExtraction() {
        val rawText = "Check this amazing video: https://www.example.com/video.mp4 don't miss it!"
        val extracted = ClipboardHelper.extractUrl(rawText)
        assertNotNull(extracted)
        assertEquals("https://www.example.com/video.mp4", extracted)
    }

    @Test
    fun testDownloadItemProgressCalculation() {
        val item = DownloadItem(
            id = 1,
            url = "https://example.com/file.zip",
            fileName = "file.zip",
            category = MediaCategory.ARCHIVE.name,
            resolution = "Standard",
            format = "ZIP",
            totalBytes = 1000L,
            downloadedBytes = 500L,
            status = DownloadStatus.DOWNLOADING.name
        )

        assertEquals(0.5f, item.progress, 0.001f)
        assertEquals(50, item.progressPercent)
        assertEquals(DownloadStatus.DOWNLOADING, item.currentStatus)
    }

    @Test
    fun testMediaCategoryFallback() {
        val item = DownloadItem(
            id = 2,
            url = "https://example.com/photo.jpg",
            fileName = "photo.jpg",
            category = "IMAGE",
            resolution = "1080p",
            format = "JPG"
        )
        assertEquals(MediaCategory.IMAGE, item.mediaCategory)
    }
}
