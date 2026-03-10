package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.SparkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparkScreen(
    onNavigateBack: () -> Unit,
    viewModel: SparkViewModel = hiltViewModel()
) {
    val streak by viewModel.streak.collectAsState()
    val history by viewModel.history.collectAsState()
    val breakdown by viewModel.breakdown.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            IOSTopAppBar(title = "Искра 🔥", onBackClick = onNavigateBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streak card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF6D00),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${streak?.currentStreak ?: 0}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6D00)
                        )
                        Text(
                            text = "дней подряд",
                            fontSize = 16.sp,
                            color = Color(0xFFBF360C)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("Рекорд", "${streak?.longestStreak ?: 0}")
                            StatItem("Всего", "${streak?.totalSparks ?: 0}")
                        }
                    }
                }
            }

            // Send spark button
            item {
                Button(
                    onClick = { viewModel.logSpark() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить искру 💕", color = Color.White, fontSize = 16.sp)
                }
            }

            // Breakdown by type
            if (breakdown != null && breakdown!!.types.isNotEmpty()) {
                item {
                    Text("По типу активности", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                item {
                    val bd = breakdown!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val maxCnt = bd.types.maxOfOrNull { it.count } ?: 1
                            bd.types.forEach { t ->
                                val fraction = t.count.toFloat() / maxCnt
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        sparkTypeEmoji(t.sparkType),
                                        fontSize = 20.sp,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        sparkTypeLabel(t.sparkType),
                                        modifier = Modifier.width(100.dp),
                                        fontSize = 13.sp
                                    )
                                    Box(
                                        Modifier.weight(1f).height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Color(0xFFFF6D00).copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            Modifier.fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(Color(0xFFFF6D00))
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${t.count}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(28.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weekly summary
            if (breakdown != null && breakdown!!.weekly.isNotEmpty()) {
                item {
                    Text("По неделям", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                val weeklyItems = breakdown!!.weekly
                items(weeklyItems) { week ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(week.weekStart.take(10), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "${week.activeDays} акт. дней • ${week.totalSparks} искр",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // History
            if (history.isNotEmpty()) {
                item {
                    Text("История", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                items(history) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (item.participants >= 2) "🔥" else "✨",
                                fontSize = 24.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.date, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    text = if (item.participants >= 2) "Оба партнёра" else "Частичный",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${item.sparkCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFBF360C))
        Text(label, fontSize = 12.sp, color = Color(0xFFBF360C).copy(alpha = 0.7f))
    }
}

private fun sparkTypeEmoji(type: String): String = when (type.lowercase()) {
    "manual"       -> "💕"
    "interaction"  -> "💬"
    "game_complete" -> "🎮"
    "mood"         -> "😊"
    "love_touch"   -> "✋"
    "miss_you"     -> "💗"
    else           -> "✨"
}

private fun sparkTypeLabel(type: String): String = when (type.lowercase()) {
    "manual"       -> "Вручную"
    "interaction"  -> "Общение"
    "game_complete" -> "Игра"
    "mood"         -> "Настроение"
    "love_touch"   -> "Прикосновение"
    "miss_you"     -> "Скучаю"
    else           -> type
}
