package com.shohan.pro.downloader.data.model

enum class MediaCategory(val displayName: String, val iconEmoji: String) {
    VIDEO("Video", "🎬"),
    IMAGE("Image", "🖼️"),
    DOCUMENT("Document", "📄"),
    AUDIO("Audio", "🎵"),
    ARCHIVE("Archive", "📦"),
    OTHER("File", "📁")
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ResolutionOption(
    val id: String,
    val label: String,
    val resolution: String,
    val estimatedSize: String,
    val format: String,
    val isRecommended: Boolean = false,
    val isAudioOnly: Boolean = false
)
