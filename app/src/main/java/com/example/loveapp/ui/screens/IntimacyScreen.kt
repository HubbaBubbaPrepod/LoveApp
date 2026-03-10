package com.example.loveapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.IntimacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntimacyScreen(
    onNavigateBack: () -> Unit,
    viewModel: IntimacyViewModel = hiltViewModel()
) {
    val score by viewModel.score.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { IOSTopAppBar(title = "Близость", onBackClick = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading && score == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B9D))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Score Card ──
                item {
                    score?.let { data ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFFFF6B9D), Color(0xFFFF8E8E))
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Level badge
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${data.level}",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Level name
                                    Text(
                                        data.name.ifBlank { "Уровень ${data.level}" },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    // Total score
                                    Text(
                                        "${data.score} очков",
                                        fontSize = 16.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )

                                    // Progress to next level
                                    data.nextLevel?.let { next ->
                                        Spacer(Modifier.height(16.dp))

                                        val currentLevelMin = data.levels
                                            .lastOrNull { it.minScore <= data.score }?.minScore ?: 0
                                        val range = next.pointsNeeded + (data.score - currentLevelMin)
                                        val progress = if (range > 0) (data.score - currentLevelMin).toFloat() / range else 0f
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = progress.coerceIn(0f, 1f),
                                            animationSpec = tween(1000),
                                            label = "progress"
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            LinearProgressIndicator(
                                                progress = { animatedProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = Color.White,
                                                trackColor = Color.White.copy(alpha = 0.3f)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "До «${next.name}»: ${next.pointsNeeded} очков",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Level Roadmap ──
                item {
                    score?.let { data ->
                        if (data.levels.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Уровни близости", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                                    data.levels.forEach { level ->
                                        val reached = data.score >= level.minScore
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                if (reached) Icons.Filled.Favorite else Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = if (reached) Color(0xFFFF6B9D)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "${level.level}. ${level.name}",
                                                fontWeight = if (level.level == data.level) FontWeight.Bold else FontWeight.Normal,
                                                color = if (reached) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                "${level.minScore}+",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Activity History ──
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            "Последняя активность",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(history) { log ->
                        val actionLabel = when (log.actionType) {
                            "daily_qa" -> "❓ Вопрос дня"
                            "qa_both_answered" -> "🎉 Оба ответили"
                            "chat_message" -> "💬 Сообщение"
                            "miss_you" -> "💕 Скучаю"
                            "love_touch" -> "💞 Love Touch"
                            "task_complete" -> "✅ Задача"
                            "spark_log" -> "🔥 Искра"
                            else -> "💫 ${log.actionType}"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(actionLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        log.displayName ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "+${log.points}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B9D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
