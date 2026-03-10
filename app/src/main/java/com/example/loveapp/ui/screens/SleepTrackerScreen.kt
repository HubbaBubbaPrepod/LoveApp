package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.entity.SleepEntry
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.SleepViewModel
import com.example.loveapp.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SleepViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            IOSTopAppBar(title = "Сон 🌙", onBackClick = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF5E5CE6)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF5E5CE6), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Статистика (7 дней)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SleepStatItem("Среднее", stats?.avgDuration?.let { "${it / 60}ч ${it % 60}м" } ?: "—")
                            SleepStatItem("Качество", stats?.avgQuality?.let { "%.1f/5".format(it) } ?: "—")
                            SleepStatItem("Мин.", stats?.minDuration?.let { "${it / 60}ч" } ?: "—")
                            SleepStatItem("Макс.", stats?.maxDuration?.let { "${it / 60}ч" } ?: "—")
                        }
                    }
                }
            }

            if (isLoading && entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Нет записей сна", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(entries, key = { it.id }) { entry ->
                SleepEntryCard(entry = entry, onDelete = { viewModel.deleteSleepEntry(entry.id) })
            }
        }
    }

    if (showAddDialog) {
        AddSleepDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { bedtime, wakeTime, quality, note ->
                val today = DateUtils.getTodayDateString()
                // Calculate duration from bedtime/wakeTime
                val duration = calculateDuration(bedtime, wakeTime)
                viewModel.saveSleepEntry(today, bedtime, wakeTime, duration, quality, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SleepStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5E5CE6))
        Text(label, fontSize = 11.sp, color = Color(0xFF5E5CE6).copy(alpha = 0.7f))
    }
}

@Composable
private fun SleepEntryCard(entry: SleepEntry, onDelete: () -> Unit) {
    val qualityEmoji = when (entry.quality) {
        1 -> "😴"; 2 -> "😔"; 3 -> "😐"; 4 -> "😊"; 5 -> "🌟"; else -> "💤"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(qualityEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.date, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row {
                    entry.bedtime?.let { Text("🌙 $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    entry.wakeTime?.let { Text("  ☀️ $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                entry.durationMinutes?.let {
                    Text("${it / 60}ч ${it % 60}м", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddSleepDialog(
    onDismiss: () -> Unit,
    onConfirm: (bedtime: String?, wakeTime: String?, quality: Int?, note: String) -> Unit
) {
    var bedtime by remember { mutableStateOf("") }
    var wakeTime by remember { mutableStateOf("") }
    var quality by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Записать сон") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bedtime,
                    onValueChange = { bedtime = it },
                    label = { Text("Лёг в (ЧЧ:ММ)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wakeTime,
                    onValueChange = { wakeTime = it },
                    label = { Text("Встал в (ЧЧ:ММ)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Качество сна: $quality/5", fontSize = 14.sp)
                Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    bedtime.ifBlank { null },
                    wakeTime.ifBlank { null },
                    quality,
                    note
                )
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun calculateDuration(bedtime: String?, wakeTime: String?): Int? {
    if (bedtime.isNullOrBlank() || wakeTime.isNullOrBlank()) return null
    return try {
        val (bh, bm) = bedtime.split(":").map { it.toInt() }
        val (wh, wm) = wakeTime.split(":").map { it.toInt() }
        val bedMin = bh * 60 + bm
        val wakeMin = wh * 60 + wm
        val diff = if (wakeMin > bedMin) wakeMin - bedMin else (24 * 60 - bedMin) + wakeMin
        diff
    } catch (_: Exception) { null }
}
