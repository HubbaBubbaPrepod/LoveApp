package com.example.loveapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.LoveTouchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveTouchScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoveTouchViewModel = hiltViewModel()
) {
    val isActive by viewModel.isActive.collectAsState()
    val partnerJoined by viewModel.partnerJoined.collectAsState()
    val heartsCount by viewModel.heartsCount.collectAsState()
    val history by viewModel.history.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Love Touch",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            if (!isActive) {
                // Start session card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💕", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Коснитесь экрана вместе",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Пригласите партнёра коснуться экрана одновременно — появятся сердечки!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.startSession() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                        ) {
                            Text("Начать сессию", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }
            } else {
                // Active session
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!partnerJoined) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6B9D),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Ожидаем партнёра…", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Сердечки: $heartsCount", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Touch area
                if (partnerJoined) {
                    TouchArea(
                        onTouch = { viewModel.onTouch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.endSession() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Завершить")
                }
            }

            // History section
            if (history.isNotEmpty() && !isActive) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "История",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { session ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💕", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${session.heartsCount} сердец", fontWeight = FontWeight.Medium)
                                    Text(
                                        session.createdAt.take(10),
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
}

@Composable
private fun TouchArea(onTouch: () -> Unit, modifier: Modifier = Modifier) {
    val hearts = remember { mutableStateListOf<HeartState>() }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTouch()
                        hearts.add(HeartState(it.x, it.y))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Коснитесь здесь!",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        hearts.forEach { heart ->
            key(heart.id) {
                FloatingHeart(heart) {
                    hearts.remove(heart)
                }
            }
        }
    }
}

private data class HeartState(
    val x: Float,
    val y: Float,
    val id: Long = System.nanoTime()
)

@Composable
private fun FloatingHeart(state: HeartState, onEnd: () -> Unit) {
    val anim = remember { Animatable(1f) }
    LaunchedEffect(state.id) {
        anim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
        onEnd()
    }
    val scale = 1f + (1f - anim.value) * 0.5f
    val alpha = anim.value

    Text(
        text = "❤️",
        fontSize = 32.sp,
        modifier = Modifier
            .offset(x = (state.x / 3).dp, y = ((state.y / 3) - (1f - anim.value) * 100).dp)
            .scale(scale)
            .alpha(alpha)
    )
}
