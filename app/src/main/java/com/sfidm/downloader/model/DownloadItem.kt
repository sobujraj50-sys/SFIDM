package com.sfidm.downloader.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadItem(
    val id: String,
    val fileName: String,
    val url: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val threads: Int = 8,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filePath: String = "",
    val errorMessage: String = ""
) {
    val progress: Float
        get() = when {
            status == DownloadStatus.COMPLETED -> 1f
            totalBytes > 0L -> (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

    val etaSeconds: Long
        get() = if (speedBytesPerSec > 0L && totalBytes > downloadedBytes) {
            (totalBytes - downloadedBytes) / speedBytesPerSec
        } else {
            -1L
        }
}
