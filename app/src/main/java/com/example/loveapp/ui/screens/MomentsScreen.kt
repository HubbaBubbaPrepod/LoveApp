package com.example.loveapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.loveapp.data.api.models.MomentResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.MomentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val moments by viewModel.moments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val momentShared by viewModel.momentShared.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(momentShared) {
        if (momentShared) {
            snackbarHostState.showSnackbar("Момент отправлен! 💕")
            viewModel.clearMessages()
            showShareDialog = false
        }
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(title = "Моменты", onBackClick = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showShareDialog = true },
                containerColor = Color(0xFFFF6B9D),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Поделиться моментом")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && moments.isEmpty()) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6B9D),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (moments.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📸", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Пока нет моментов",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Поделитесь первым моментом\nс вашей половинкой!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(moments, key = { it.id }) { moment ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            MomentCard(
                                moment = moment,
                                onDelete = { viewModel.deleteMoment(moment.id) }
                            )
                        }
                    }
                    item {
                        if (moments.size >= 20) {
                            TextButton(
                                onClick = { viewModel.loadMore() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Загрузить ещё", color = Color(0xFFFF6B9D))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        ShareMomentDialog(
            isSharing = isSharing,
            onDismiss = { showShareDialog = false },
            onShare = { content, mood, imageUri ->
                if (imageUri != null) {
                    viewModel.shareMomentWithImage(uri = imageUri, content = content, mood = mood)
                } else {
                    viewModel.shareMoment(content = content, mood = mood)
                }
            }
        )
    }
}

@Composable
private fun MomentCard(moment: MomentResponse, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image at top of card if present
            if (!moment.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = "Фото момента",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!moment.profileImage.isNullOrBlank()) {
                        AsyncImage(
                            model = moment.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFF6B9D), Color(0xFFFF8E8E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = moment.displayName?.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moment.displayName ?: "Партнёр",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = moment.createdAt?.take(10) ?: "",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (moment.mood != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF6B9D).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = moment.mood,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B9D)
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = moment.content,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                if (moment.locationName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "📍 ${moment.locationName}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareMomentDialog(
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onShare: (content: String, mood: String?, imageUri: Uri?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val moods = listOf("😊 Счастье", "🥰 Любовь", "😌 Спокойствие", "🤗 Тепло", "🎉 Радость", "😢 Скучаю")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { selectedImageUri = it } }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Поделиться моментом", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Photo attachment area
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Выбранное фото",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Убрать фото",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить фото")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Что у вас на душе?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        cursorColor = Color(0xFFFF6B9D)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text("Настроение:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moods.forEach { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            onClick = { selectedMood = if (isSelected) null else mood },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFFF6B9D) else Color(0xFFF5F5F5),
                            contentColor = if (isSelected) Color.White else Color.DarkGray
                        ) {
                            Text(
                                text = mood,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(content, selectedMood, selectedImageUri) },
                enabled = content.isNotBlank() && !isSharing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузка...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.Gray)
            }
        }
    )
}
