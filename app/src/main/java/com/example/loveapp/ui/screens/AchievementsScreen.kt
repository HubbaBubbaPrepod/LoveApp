package com.example.loveapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import com.example.loveapp.data.api.models.AchievementResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.viewmodels.AchievementsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val achievements by viewModel.achievements.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val newlyUnlocked by viewModel.newlyUnlocked.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Show celebration dialog for newly unlocked achievements
    if (newlyUnlocked.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearNewlyUnlocked() },
            title = { Text("🎉 Новые достижения!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    newlyUnlocked.forEach { ach ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ach.icon, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(ach.title, fontWeight = FontWeight.Bold)
                                Text(ach.description, fontSize = 13.sp, color = Color.Gray)
                                Text("+${ach.xpReward} XP", fontSize = 12.sp, color = Color(0xFFFF6B9D))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearNewlyUnlocked() }) {
                    Text("Отлично!")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F0F5)).padding(padding)) {
        IOSTopAppBar(
            title = "Достижения",
            onBackClick = onNavigateBack
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B9D))
            }
            return@Column
        }

        val categories = achievements.map { it.category }.distinct()
        val unlocked = achievements.count { it.unlocked }
        val total = achievements.size

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF6B9D), Color(0xFFFF8E53))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("$unlocked / $total", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("достижений разблокировано", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { if (total > 0) unlocked.toFloat() / total else 0f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            // Category filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(categoryLabel(cat)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF6B9D),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Achievement items
            val filtered = if (selectedCategory != null) achievements.filter { it.category == selectedCategory } else achievements
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Пока нет достижений", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                            Text("Начните общаться, чтобы разблокировать!", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { achievement ->
                AchievementCard(achievement = achievement, progress = getProgressForAchievement(achievement, progress))
            }
        }
    }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementResponse, progress: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color(0xFFFFF0F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (achievement.unlocked) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (achievement.unlocked) Color(0xFFFF6B9D).copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (achievement.unlocked) achievement.icon else "🔒",
                    fontSize = 28.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (achievement.unlocked) Color(0xFF333333) else Color.Gray
                )
                Text(
                    achievement.description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))

                if (achievement.unlocked) {
                    Text(
                        "✅ Разблокировано · +${achievement.xpReward} XP",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    val progressPercent = if (achievement.threshold > 0)
                        (progress.toFloat() / achievement.threshold).coerceIn(0f, 1f) else 1f
                    Column {
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFF6B9D),
                            trackColor = Color(0xFFE0E0E0),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$progress / ${achievement.threshold}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "chat" -> "💬 Чат"
    "streak" -> "🔥 Стрик"
    "notes" -> "📝 Заметки"
    "wishes" -> "🌈 Желания"
    "mood" -> "😊 Настроение"
    "tasks" -> "✅ Задачи"
    "gallery" -> "📷 Галерея"
    "memorial" -> "🎂 Памятные"
    "pet" -> "🐱 Питомец"
    "games" -> "🎮 Игры"
    "letters" -> "💌 Письма"
    "bucket" -> "📋 Мечты"
    "intimacy" -> "💕 Близость"
    "missyou" -> "💗 Скучаю"
    "moments" -> "📸 Моменты"
    "places" -> "📍 Места"
    else -> "🏆 Общее"
}

private fun getProgressForAchievement(
    ach: AchievementResponse,
    progress: com.example.loveapp.data.api.models.AchievementProgressResponse
): Int = when (ach.code) {
    "first_message", "chat_100", "chat_1000" -> progress.chatMessages
    "streak_7", "streak_30", "streak_100", "streak_365" -> progress.currentStreak
    "first_note", "notes_50" -> progress.notes
    "first_wish" -> progress.wishes
    "wishes_done_10" -> progress.wishesCompleted
    "first_mood", "mood_30" -> progress.moods
    "first_task" -> progress.tasks
    "tasks_done_25" -> progress.tasksCompleted
    "first_photo", "gallery_50" -> progress.galleryPhotos
    "first_memorial" -> progress.memorialDays
    "pet_level_5", "pet_level_10" -> progress.petLevel
    "games_10" -> progress.gamesPlayed
    "games_match_20" -> progress.gamesMatched
    "first_letter", "letters_10" -> progress.lettersSent
    "first_bucket" -> progress.bucketItems
    "bucket_done_5", "bucket_done_25" -> progress.bucketCompleted
    "intimacy_100", "intimacy_500" -> progress.intimacyScore
    "miss_you_50" -> progress.missYouSent
    "moments_25" -> progress.moments
    "places_10" -> progress.places
    else -> 0
}
