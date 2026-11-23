package com.sfidm.downloader.ui.strings

enum class AppLanguage(val code: String) {
    BN("bn"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.find { it.code == code } ?: BN
    }
}

data class SfidmStrings(
    val activeCountSuffix: String,
    val tabAll: String,
    val tabActive: String,
    val tabPaused: String,
    val tabDone: String,
    val statusQueued: String,
    val statusDownloading: String,
    val statusPaused: String,
    val statusCompleted: String,
    val statusFailed: String,
    val emptyListTitle: String,
    val emptyListSub: String,
    val addDownloadTitle: String,
    val urlLabel: String,
    val urlPlaceholder: String,
    val fileNameLabel: String,
    val fileNamePlaceholder: String,
    val threadsLabel: String,
    val pasteClipboard: String,
    val startDownload: String,
    val cancel: String,
    val chooseTheme: String,
    val close: String,
    val savedToDownloads: String,
    val openFileError: String,
    val invalidUrl: String,
    val leftSuffix: String,
    val langToggleLabel: String
)

fun stringsFor(lang: AppLanguage): SfidmStrings = when (lang) {
    AppLanguage.BN -> SfidmStrings(
        activeCountSuffix = "টি ডাউনলোড চলছে",
        tabAll = "সব",
        tabActive = "চলমান",
        tabPaused = "বিরতি",
        tabDone = "সম্পন্ন",
        statusQueued = "অপেক্ষমাণ",
        statusDownloading = "চলছে",
        statusPaused = "বিরতি (Paused)",
        statusCompleted = "সম্পন্ন হয়েছে",
        statusFailed = "ব্যর্থ হয়েছে",
        emptyListTitle = "কোনো ডাউনলোড নেই",
        emptyListSub = "নিচের '+' বাটনে ট্যাপ করে নতুন লিংক যোগ করুন",
        addDownloadTitle = "নতুন ডাউনলোড যোগ করুন",
        urlLabel = "ডাউনলোড লিংক (URL)",
        urlPlaceholder = "https://example.com/file.mp4",
        fileNameLabel = "ফাইলের নাম (ঐচ্ছিক)",
        fileNamePlaceholder = "স্বয়ংক্রিয়ভাবে নির্ধারিত হবে",
        threadsLabel = "মাল্টি-থ্রেড সংখ্যা",
        pasteClipboard = "পেস্ট",
        startDownload = "ডাউনলোড শুরু",
        cancel = "বাতিল",
        chooseTheme = "ডার্ক থিম নির্বাচন করুন",
        close = "বন্ধ",
        savedToDownloads = "Downloads/SFIDM ফোল্ডারে সেভ হয়েছে",
        openFileError = "ফাইলটি ওপেন করার অ্যাপ পাওয়া যায়নি",
        invalidUrl = "সঠিক http:// বা https:// লিংক দিন",
        leftSuffix = "বাকি",
        langToggleLabel = "EN"
    )
    AppLanguage.EN -> SfidmStrings(
        activeCountSuffix = "active downloads",
        tabAll = "All",
        tabActive = "Active",
        tabPaused = "Paused",
        tabDone = "Done",
        statusQueued = "Queued",
        statusDownloading = "Downloading",
        statusPaused = "Paused",
        statusCompleted = "Completed",
        statusFailed = "Failed",
        emptyListTitle = "No downloads yet",
        emptyListSub = "Tap the '+' button below to add a download link",
        addDownloadTitle = "Add New Download",
        urlLabel = "Download URL",
        urlPlaceholder = "https://example.com/file.mp4",
        fileNameLabel = "File Name (Optional)",
        fileNamePlaceholder = "Auto-detected from URL",
        threadsLabel = "Multi-Thread Parts",
        pasteClipboard = "Paste",
        startDownload = "Start Download",
        cancel = "Cancel",
        chooseTheme = "Choose Dark Theme",
        close = "Close",
        savedToDownloads = "Saved to Downloads/SFIDM",
        openFileError = "No app found to open this file",
        invalidUrl = "Please enter a valid http:// or https:// URL",
        leftSuffix = "left",
        langToggleLabel = "বাং"
    )
}
