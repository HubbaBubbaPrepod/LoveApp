package com.example.loveapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.MissYouViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissYouScreen(
    onNavigateBack: () -> Unit,
    viewModel: MissYouViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Скучаю",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(24.dp))

                // Send miss-you card
                MissYouSendCard(onSend = { emoji, message ->
                    viewModel.sendMissYou(emoji, message)
                })

                Spacer(Modifier.height(20.dp))
            }

            // Today stats
            if (todayStats.count > 0 || todayStats.sentByMe > 0 || todayStats.sentByPartner > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(label = "Отправлено мной", value = todayStats.sentByMe.toString())
                            StatItem(label = "От партнёра", value = todayStats.sentByPartner.toString())
                            StatItem(label = "Всего", value = todayStats.count.toString())
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // History header
            if (events.isNotEmpty()) {
                item {
                    Text(
                        "История",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(events) { event ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(event.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (event.message.isNotBlank()) {
                                    Text(event.message, fontWeight = FontWeight.Medium)
                                } else {
                                    Text("Скучаю по тебе…", fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.timestamp)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B9D))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MissYouSendCard(onSend: (String, String) -> Unit) {
    var selectedEmoji by remember { mutableStateOf("❤️") }
    var message by remember { mutableStateOf("") }
    var showSent by remember { mutableStateOf(false) }
    val emojis = listOf("❤️", "😘", "🥺", "💕", "💗", "🫶")

    val scale = remember { Animatable(1f) }
    LaunchedEffect(showSent) {
        if (showSent) {
            scale.animateTo(1.3f, animationSpec = tween(200))
            scale.animateTo(1f, animationSpec = tween(200))
            showSent = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji picker
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojis.forEach { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Surface(
                        onClick = { selectedEmoji = emoji },
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFFFFD6E8) else Color.Transparent
                    ) {
                        Text(
                            emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Добавьте сообщение (необязательно)") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onSend(selectedEmoji, message)
                    message = ""
                    showSent = true
                },
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
            ) {
                Text(selectedEmoji, fontSize = 36.sp)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Нажмите, чтобы отправить",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
