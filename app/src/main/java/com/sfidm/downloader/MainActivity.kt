package com.sfidm.downloader

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sfidm.downloader.data.PrefsManager
import com.sfidm.downloader.engine.DownloadManager
import com.sfidm.downloader.model.DownloadItem
import com.sfidm.downloader.model.DownloadStatus
import com.sfidm.downloader.ui.strings.AppLanguage
import com.sfidm.downloader.ui.strings.SfidmStrings
import com.sfidm.downloader.ui.strings.stringsFor
import com.sfidm.downloader.ui.theme.AppTheme
import com.sfidm.downloader.ui.theme.SfidmTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var sharedUrlState = mutableStateOf<String?>(null)

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadManager.init(applicationContext)
        val prefs = PrefsManager(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleShareIntent(intent)

        setContent {
            var currentTheme by remember { mutableStateOf(prefs.getTheme()) }
            var currentLang by remember { mutableStateOf(prefs.getLanguage()) }

            SfidmTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SfidmHomeScreen(
                        currentTheme = currentTheme,
                        currentLang = currentLang,
                        incomingSharedUrl = sharedUrlState.value,
                        onConsumeSharedUrl = { sharedUrlState.value = null },
                        onThemeChange = {
                            currentTheme = it
                            prefs.saveTheme(it)
                        },
                        onToggleLanguage = {
                            val next = if (currentLang == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
                            currentLang = next
                            prefs.saveLanguage(next)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val url = Regex("https?://\\S+").find(sharedText)?.value ?: sharedText
                sharedUrlState.value = url
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SfidmHomeScreen(
    currentTheme: AppTheme,
    currentLang: AppLanguage,
    incomingSharedUrl: String?,
    onConsumeSharedUrl: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(currentLang) { stringsFor(currentLang) }
    val downloads by DownloadManager.downloads.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillUrl by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(incomingSharedUrl) {
        if (!incomingSharedUrl.isNullOrBlank()) {
            prefillUrl = incomingSharedUrl
            showAddDialog = true
            onConsumeSharedUrl()
        }
    }

    val activeDownloads = downloads.filter { it.status == DownloadStatus.DOWNLOADING }
    val totalSpeed = activeDownloads.sumOf { it.speedBytesPerSec }

    val tabs = listOf(strings.tabAll, strings.tabActive, strings.tabPaused, strings.tabDone)
    val filteredList = when (selectedTab) {
        1 -> downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
        2 -> downloads.filter { it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED }
        3 -> downloads.filter { it.status == DownloadStatus.COMPLETED }
        else -> downloads
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SFIDM",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${activeDownloads.size} ${strings.activeCountSuffix}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (totalSpeed > 0L) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "⚡ ${DownloadManager.formatSpeed(totalSpeed)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = onToggleLanguage,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = strings.langToggleLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = strings.chooseTheme,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    prefillUrl = readClipboardUrl(context)
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addDownloadTitle)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.indices.toList()) { idx ->
                    val selected = selectedTab == idx
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTab = idx },
                        label = {
                            Text(
                                text = tabs[idx],
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.emptyListTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.emptyListSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        DownloadCard(
                            item = item,
                            strings = strings,
                            onPause = { DownloadManager.pauseDownload(item.id) },
                            onResume = { DownloadManager.startDownload(item.id) },
                            onDelete = { DownloadManager.deleteDownload(item.id) },
                            onOpen = { openDownloadedFile(context, item, strings.openFileError) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDownloadDialog(
            initialUrl = prefillUrl,
            strings = strings,
            onDismiss = { showAddDialog = false },
            onStart = { url, name, threads ->
                if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                    Toast.makeText(context, strings.invalidUrl, Toast.LENGTH_SHORT).show()
                } else {
                    DownloadManager.addDownload(url, name, threads)
                    showAddDialog = false
                }
            }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = currentTheme,
            currentLang = currentLang,
            strings = strings,
            onSelect = {
                onThemeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    strings: SfidmStrings,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val statusColor = when (item.status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> accent
        DownloadStatus.PAUSED -> Color(0xFFF59E0B)
        DownloadStatus.COMPLETED -> Color(0xFF10B981)
        DownloadStatus.FAILED -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.status == DownloadStatus.COMPLETED) { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(statusColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIconFor(item.fileName),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val statusLabel = when (item.status) {
                        DownloadStatus.QUEUED -> strings.statusQueued
                        DownloadStatus.DOWNLOADING -> "${DownloadManager.formatSpeed(item.speedBytesPerSec)} • ${strings.statusDownloading}"
                        DownloadStatus.PAUSED -> strings.statusPaused
                        DownloadStatus.COMPLETED -> strings.statusCompleted
                        DownloadStatus.FAILED -> "${strings.statusFailed}: ${item.errorMessage}"
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = accent)
                        }
                    }
                    DownloadStatus.PAUSED, DownloadStatus.QUEUED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = accent)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = statusColor)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onOpen) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = statusColor)
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeText = if (item.totalBytes > 0L) {
                    "${DownloadManager.formatBytes(item.downloadedBytes)} / ${DownloadManager.formatBytes(item.totalBytes)}"
                } else {
                    DownloadManager.formatBytes(item.downloadedBytes)
                }
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.status == DownloadStatus.DOWNLOADING && item.etaSeconds > 0L) {
                        Text(
                            text = "${formatEta(item.etaSeconds)} ${strings.leftSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.threads}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AddDownloadDialog(
    initialUrl: String,
    strings: SfidmStrings,
    onDismiss: () -> Unit,
    onStart: (url: String, fileName: String, threads: Int) -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(initialUrl) }
    var fileName by remember {
        mutableStateOf(
            if (initialUrl.contains("/")) initialUrl.substringBefore("?").substringAfterLast("/") else ""
        )
    }
    var selectedThreads by remember { mutableIntStateOf(8) }
    val threadOptions = listOf(4, 8, 16)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(strings.addDownloadTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (fileName.isBlank() && it.contains("/")) {
                            fileName = it.substringBefore("?").substringAfterLast("/")
                        }
                    },
                    label = { Text(strings.urlLabel) },
                    placeholder = { Text(strings.urlPlaceholder) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val pasted = readClipboardUrl(context)
                            if (pasted.isNotBlank()) {
                                url = pasted
                                if (fileName.isBlank() && pasted.contains("/")) {
                                    fileName = pasted.substringBefore("?").substringAfterLast("/")
                                }
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = strings.pasteClipboard)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(strings.fileNameLabel) },
                    placeholder = { Text(strings.fileNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = strings.threadsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    threadOptions.forEach { count ->
                        val selected = selectedThreads == count
                        FilterChip(
                            selected = selected,
                            onClick = { selectedThreads = count },
                            label = { Text("${count}x", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(url.trim(), fileName.trim(), selectedThreads) },
                enabled = url.isNotBlank()
            ) {
                Text(strings.startDownload, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun ThemePickerDialog(
    currentTheme: AppTheme,
    currentLang: AppLanguage,
    strings: SfidmStrings,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(strings.chooseTheme, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    val selected = theme == currentTheme
                    Surface(
                        onClick = { onSelect(theme) },
                        shape = RoundedCornerShape(12.dp),
                        color = theme.bg,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) theme.accent else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(theme.accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.BN) theme.labelBn else theme.labelEn,
                                color = Color(0xFFF1F5F9),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

private fun fileIconFor(fileName: String) = when {
    fileName.endsWith(".mp4", true) || fileName.endsWith(".mkv", true) || fileName.endsWith(".webm", true) -> Icons.Default.Movie
    fileName.endsWith(".mp3", true) || fileName.endsWith(".m4a", true) || fileName.endsWith(".wav", true) -> Icons.Default.AudioFile
    fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".webp", true) -> Icons.Default.Image
    fileName.endsWith(".zip", true) || fileName.endsWith(".rar", true) || fileName.endsWith(".7z", true) -> Icons.Default.FolderZip
    fileName.endsWith(".apk", true) -> Icons.Default.Android
    fileName.endsWith(".pdf", true) || fileName.endsWith(".docx", true) -> Icons.Default.Description
    else -> Icons.Default.InsertDriveFile
}

private fun readClipboardUrl(context: Context): String {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
        if (text.startsWith("http://", true) || text.startsWith("https://", true)) text else ""
    } catch (_: Exception) {
        ""
    }
}

private fun formatEta(seconds: Long): String {
    return when {
        seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun openDownloadedFile(context: Context, item: DownloadItem, errorMsg: String) {
    try {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, item.fileName))
    } catch (_: Exception) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }
}
