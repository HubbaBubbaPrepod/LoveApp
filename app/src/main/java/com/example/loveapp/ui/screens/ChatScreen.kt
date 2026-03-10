package com.example.loveapp.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.loveapp.data.entity.ChatMessage
import com.example.loveapp.ui.components.GifPickerSheet
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.ChatViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDrawing: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val partnerName by viewModel.partnerName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val isPartnerOnline by viewModel.isPartnerOnline.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val wallpaperUrl by viewModel.wallpaperUrl.collectAsState()
    val bubbleColor by viewModel.bubbleColor.collectAsState()
    val bubbleShape by viewModel.bubbleShape.collectAsState()
    val gifResults by viewModel.gifResults.collectAsState()
    val stickerPacks by viewModel.stickerPacks.collectAsState()
    val currentStickers by viewModel.currentStickers.collectAsState()
    val popupSticker by viewModel.popupSticker.collectAsState()
    val missYouCounter by viewModel.missYouCounter.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // ── Panel state ──
    var showGifPicker by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }

    // ── Voice recording state ──
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingStartTime by remember { mutableStateOf(0L) }

    // ── Context menu state ──
    var contextMenuMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // ── Miss You emoji rain state ──
    var showMissYouRain by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.let {
                try { it.stop() } catch (_: Exception) {}
                try { it.release() } catch (_: Exception) {}
            }
            mediaRecorder = null
        }
    }

    // ── Image picker ──
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendImageMessage(it) } }

    // ── Video picker ──
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendVideoMessage(it) } }

    // ── Audio permission ──
    var hasAudioPermission by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    // ── Snackbar ──
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            kotlinx.coroutines.delay(300)
            viewModel.sendTypingIndicator()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
        viewModel.markAllRead()
    }

    // ── Miss You rain auto-dismiss ──
    LaunchedEffect(showMissYouRain) {
        if (showMissYouRain) {
            kotlinx.coroutines.delay(2000)
            showMissYouRain = false
        }
    }

    val subtitle = when {
        isPartnerTyping -> "печатает..."
        isPartnerOnline -> "в сети"
        else -> null
    }

    val resolvedBubbleColor = when (bubbleColor) {
        "rose" -> Color(0xFFFF6B9D)
        "sky" -> Color(0xFF64B5F6)
        "lavender" -> Color(0xFFCE93D8)
        "mint" -> Color(0xFF81C784)
        "sunset" -> Color(0xFFFF8A65)
        "ocean" -> Color(0xFF4DD0E1)
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = partnerName ?: "Чат",
                onBackClick = onNavigateBack,
                subtitle = subtitle
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Message List ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (!wallpaperUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = wallpaperUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (isLoading && messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Напишите первое сообщение 💌", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatBubble(
                                    message = msg,
                                    isMine = viewModel.isMyMessage(msg),
                                    bubbleColor = resolvedBubbleColor,
                                    bubbleShapeKey = bubbleShape ?: "rounded",
                                    onLongClick = { contextMenuMessage = msg },
                                    onStickerClick = { sticker ->
                                        viewModel.showPopupSticker(sticker)
                                    }
                                )
                            }
                        }
                    }

                    // Upload indicator
                    if (isUploading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Отправка...", fontSize = 13.sp)
                            }
                        }
                    }

                    // ── Miss You emoji rain overlay ──
                    if (showMissYouRain) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("❤️", fontSize = 72.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Скучаю! x${missYouCounter.myCount}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── Input Bar ──
                Surface(tonalElevation = 2.dp) {
                    Column {
                        // Attach menu
                        AnimatedVisibility(visible = showAttachMenu) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                AttachButton(icon = Icons.Default.Photo, label = "Фото") {
                                    showAttachMenu = false
                                    galleryLauncher.launch("image/*")
                                }
                                AttachButton(icon = Icons.Default.Videocam, label = "Видео") {
                                    showAttachMenu = false
                                    videoLauncher.launch("video/*")
                                }
                                AttachButton(icon = Icons.Default.Gif, label = "GIF") {
                                    showAttachMenu = false
                                    showStickerPicker = false
                                    showGifPicker = !showGifPicker
                                    if (showGifPicker) viewModel.loadTrendingGifs()
                                }
                                AttachButton(icon = Icons.Default.LocationOn, label = "Место") {
                                    showAttachMenu = false
                                    // Send current location stub (in real app, open location picker)
                                    viewModel.sendLocationMessage(0.0, 0.0, "Моё местоположение")
                                }
                                AttachButton(icon = Icons.Default.Brush, label = "Рисунок") {
                                    showAttachMenu = false
                                    onNavigateToDrawing?.invoke()
                                }
                            }
                        }

                        // Text input + buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attach button
                            IconButton(onClick = {
                                showGifPicker = false
                                showStickerPicker = false
                                showAttachMenu = !showAttachMenu
                            }) {
                                Icon(
                                    if (showAttachMenu) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = "Вложения",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sticker button
                            IconButton(onClick = {
                                showAttachMenu = false
                                showGifPicker = false
                                showStickerPicker = !showStickerPicker
                            }) {
                                Icon(
                                    Icons.Default.SentimentSatisfied,
                                    contentDescription = "Стикеры",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Text field
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Сообщение...") },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4
                            )
                            Spacer(Modifier.width(4.dp))

                            // Miss You / Voice / Send
                            if (inputText.isBlank()) {
                                // I Miss U long-press heart
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    viewModel.sendMissYou("❤️")
                                                    showMissYouRain = true
                                                },
                                                onLongPress = {
                                                    viewModel.sendMissYou("💕")
                                                    showMissYouRain = true
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Скучаю",
                                        tint = Color(0xFFFF6B9D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Voice
                                IconButton(
                                    onClick = {
                                        if (isRecording) {
                                            try {
                                                mediaRecorder?.stop()
                                                mediaRecorder?.release()
                                            } catch (_: Exception) {}
                                            mediaRecorder = null
                                            isRecording = false
                                            val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                                            recordingFile?.let { file ->
                                                if (duration >= 1) {
                                                    viewModel.sendMessage(
                                                        content = "",
                                                        messageType = "voice",
                                                        audioUrl = file.absolutePath,
                                                        audioDurationSeconds = duration
                                                    )
                                                } else {
                                                    file.delete()
                                                }
                                            }
                                            recordingFile = null
                                        } else {
                                            if (!hasAudioPermission) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@IconButton
                                            }
                                            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                            recordingFile = file
                                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                MediaRecorder(context)
                                            } else {
                                                @Suppress("DEPRECATION")
                                                MediaRecorder()
                                            }
                                            try {
                                                recorder.apply {
                                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                    setAudioSamplingRate(44100)
                                                    setAudioEncodingBitRate(128000)
                                                    setOutputFile(file.absolutePath)
                                                    prepare()
                                                    start()
                                                }
                                                mediaRecorder = recorder
                                                recordingStartTime = System.currentTimeMillis()
                                                isRecording = true
                                            } catch (_: Exception) {
                                                try { recorder.release() } catch (_: Exception) {}
                                                file.delete()
                                                recordingFile = null
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (isRecording) "Стоп" else "Голосовое",
                                        tint = if (isRecording) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Отправить",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // GIF picker
                        AnimatedVisibility(visible = showGifPicker) {
                            GifPickerSheet(
                                gifs = gifResults,
                                onSearch = { query -> viewModel.searchGifs(query) },
                                onGifSelected = { gif ->
                                    viewModel.sendGifMessage(gif)
                                    showGifPicker = false
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // ── Sticker Picker Panel ──
                        AnimatedVisibility(visible = showStickerPicker) {
                            StickerPickerPanel(
                                packs = stickerPacks,
                                stickers = currentStickers,
                                onPackSelected = { viewModel.loadStickersForPack(it.id) },
                                onStickerSelected = { sticker ->
                                    viewModel.sendStickerMessage(sticker)
                                    showStickerPicker = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Popup Sticker Fullscreen Overlay ──
            val popup = popupSticker
            if (popup != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { viewModel.dismissPopupSticker() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = popup.url,
                        contentDescription = popup.name,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // ── Context Menu Dialog ──
    if (contextMenuMessage != null) {
        val msg = contextMenuMessage!!
        val isMine = viewModel.isMyMessage(msg)
        AlertDialog(
            onDismissRequest = { contextMenuMessage = null },
            title = { Text("Сообщение", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (msg.messageType == "text" && msg.content.isNotBlank()) {
                        TextButton(onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("message", msg.content))
                            } catch (_: Exception) {}
                            contextMenuMessage = null
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Копировать")
                            }
                        }
                    }
                    if (isMine) {
                        TextButton(onClick = {
                            viewModel.deleteMessage(msg.id)
                            contextMenuMessage = null
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuMessage = null }) { Text("Закрыть") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Sticker Picker Panel
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun StickerPickerPanel(
    packs: List<com.example.loveapp.data.api.models.StickerPackResponse>,
    stickers: List<com.example.loveapp.data.api.models.StickerResponse>,
    onPackSelected: (com.example.loveapp.data.api.models.StickerPackResponse) -> Unit,
    onStickerSelected: (com.example.loveapp.data.api.models.StickerResponse) -> Unit
) {
    var selectedPackId by remember { mutableIntStateOf(packs.firstOrNull()?.id ?: 0) }

    LaunchedEffect(packs) {
        packs.firstOrNull()?.let {
            selectedPackId = it.id
            onPackSelected(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Pack tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(packs) { pack ->
                FilterChip(
                    selected = pack.id == selectedPackId,
                    onClick = {
                        selectedPackId = pack.id
                        onPackSelected(pack)
                    },
                    label = {
                        Text(
                            pack.name,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // Sticker grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stickers) { sticker ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStickerSelected(sticker) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = sticker.url,
                        contentDescription = sticker.name,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                    if (sticker.isPopup) {
                        Text(
                            "POP",
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color(0xFFFF6B9D), RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Attach Button
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AttachButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Chat Bubble (supports all message types + 6 shapes)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean,
    bubbleColor: Color,
    bubbleShapeKey: String,
    onLongClick: () -> Unit,
    onStickerClick: (com.example.loveapp.data.api.models.StickerResponse) -> Unit = {}
) {
    if (message.isRevoked) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isMine) "Вы отозвали сообщение" else "Сообщение отозвано",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )
        }
        return
    }

    // Sticker messages render without bubble background
    if (message.messageType == "sticker") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (message.isPopupSticker) {
                            onStickerClick(
                                com.example.loveapp.data.api.models.StickerResponse(
                                    id = 0,
                                    packId = 0,
                                    code = message.stickerId ?: "",
                                    name = message.content,
                                    url = message.stickerUrl ?: "",
                                    isPopup = true
                                )
                            )
                        }
                    },
                    onLongClick = onLongClick
                ),
            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                AsyncImage(
                    model = message.stickerUrl,
                    contentDescription = "Стикер",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
                if (message.isPopupSticker) {
                    Text(
                        "🎆 Нажмите для просмотра",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = formatTime(message.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    // Emoji (Miss You) messages — large centered emoji
    if (message.messageType == "emoji") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongClick),
            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                Text(
                    text = message.emojiType ?: message.content,
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formatTime(message.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    val bgColor = if (isMine) bubbleColor else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart

    // 6 bubble shapes
    val shape = when (bubbleShapeKey) {
        "classic" -> RoundedCornerShape(12.dp)
        "sharp" -> RoundedCornerShape(4.dp)
        "tail" -> if (isMine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                  else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        "cloud" -> RoundedCornerShape(20.dp)
        "square" -> RoundedCornerShape(2.dp)
        else -> RoundedCornerShape(16.dp) // "rounded"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (message.messageType) {
                "voice" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = textColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("🎤 ${message.audioDurationSeconds}с", color = textColor, fontSize = 15.sp)
                    }
                }
                "image" -> {
                    val url = message.imageUrl
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Фото",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Photo, null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Фото", color = textColor, fontSize = 15.sp)
                        }
                    }
                }
                "video" -> {
                    val url = message.videoThumbnailUrl ?: message.videoUrl
                    if (!url.isNullOrBlank()) {
                        Box {
                            AsyncImage(
                                model = url,
                                contentDescription = "Видео",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Видео", color = textColor, fontSize = 15.sp)
                        }
                    }
                }
                "location" -> {
                    val name = message.locationName ?: message.content.split("|").firstOrNull()?.ifBlank { "Местоположение" } ?: "Местоположение"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = textColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(name, color = textColor, fontSize = 15.sp)
                    }
                }
                "drawing" -> {
                    val url = message.drawingUrl
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Рисунок",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Brush, null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Рисунок", color = textColor, fontSize = 15.sp)
                        }
                    }
                }
                "custom" -> {
                    val label = when (message.timCustomType) {
                        "spark" -> "⚡ Искра!"
                        "intimacy" -> "💕 Близость"
                        "gift" -> "🎁 Подарок"
                        "miss_you" -> "💭 Скучаю"
                        "love_touch" -> "💗 Прикосновение"
                        "system" -> "ℹ️ ${message.content}"
                        else -> "📨 ${message.content}"
                    }
                    Text(label, color = textColor, fontSize = 15.sp)
                }
                else -> {
                    Text(message.content, color = textColor, fontSize = 15.sp)
                }
            }

            Text(
                text = formatTime(message.timestamp),
                color = textColor.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            )
        }
    }
}

private fun formatTime(ts: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
