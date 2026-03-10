package com.example.loveapp.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.LoveLetterResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.viewmodels.LettersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LettersScreen(
    onNavigateBack: () -> Unit,
    viewModel: LettersViewModel = hiltViewModel()
) {
    val letters by viewModel.letters.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedLetter by viewModel.selectedLetter.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Create letter dialog
    if (showCreateDialog) {
        CreateLetterDialog(
            onDismiss = { viewModel.toggleCreateDialog() },
            onCreate = { title, content, mood, openDate ->
                viewModel.createLetter(title, content, mood, openDate)
            }
        )
    }

    // View letter dialog
    selectedLetter?.let { letter ->
        LetterDetailDialog(
            letter = letter,
            onDismiss = { viewModel.closeLetter() },
            onDelete = { viewModel.deleteLetter(letter.id) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleCreateDialog() },
                containerColor = Color(0xFFFF6B9D),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Написать письмо")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F0F5))
                .padding(padding)
        ) {
            IOSTopAppBar(
                title = "Любовные письма",
                onBackClick = onNavigateBack
            )

            if (isLoading && letters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF6B9D))
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE91E63), Color(0xFFFF6B9D))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("💌", "${stats.total}", "Всего")
                                StatItem("📬", "${stats.readyToOpen}", "Готово")
                                StatItem("🔒", "${stats.sealed}", "Запечатано")
                                StatItem("📖", "${stats.opened}", "Открыто")
                            }
                        }
                    }
                }

                // Filter chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filter == null,
                                onClick = { viewModel.setFilter(null) },
                                label = { Text("Все") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == "available",
                                onClick = { viewModel.setFilter("available") },
                                label = { Text("📬 Можно открыть") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == "sealed",
                                onClick = { viewModel.setFilter("sealed") },
                                label = { Text("🔒 Запечатанные") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (letters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💌", fontSize = 56.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Пока нет писем", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                                Text("Напишите первое любовное письмо!", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                items(letters, key = { it.id }) { letter ->
                    LetterCard(
                        letter = letter,
                        onClick = { viewModel.openLetter(letter.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun LetterCard(letter: LoveLetterResponse, onClick: () -> Unit) {
    val isSealed = !letter.canOpen
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSealed -> Color(0xFFFFF3E0)
                letter.isOpened -> Color.White
                else -> Color(0xFFFFF0F5)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Envelope icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        when {
                            isSealed -> Color(0xFFFF9800).copy(alpha = 0.15f)
                            letter.isOpened -> Color.Gray.copy(alpha = 0.1f)
                            else -> Color(0xFFFF6B9D).copy(alpha = 0.15f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        isSealed -> "🔒"
                        letter.isOpened -> "📖"
                        else -> "💌"
                    },
                    fontSize = 26.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    letter.title.ifEmpty { "Без названия" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "От ${letter.senderName ?: "..."}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        isSealed -> "🔒 Откроется через ${letter.daysUntilOpen ?: "?"} дн."
                        !letter.isOpened -> "💌 Можно открыть!"
                        else -> "📖 Прочитано"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isSealed -> Color(0xFFFF9800)
                        !letter.isOpened -> Color(0xFFFF6B9D)
                        else -> Color.Gray
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateLetterDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, content: String, mood: String?, openDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var daysUntilOpen by remember { mutableStateOf("1") }
    var selectedMood by remember { mutableStateOf<String?>(null) }

    val moods = listOf("❤️" to "love", "🥰" to "tender", "😊" to "happy", "🤗" to "grateful", "💋" to "passionate")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✉️ Новое письмо", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(200) },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Текст письма *") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 8
                )
                Text("Настроение:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEach { (emoji, mood) ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = if (selectedMood == mood) null else mood },
                            label = { Text(emoji) }
                        )
                    }
                }
                OutlinedTextField(
                    value = daysUntilOpen,
                    onValueChange = { v -> daysUntilOpen = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Через сколько дней открыть") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("1 = завтра, 0 = сегодня") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        val days = daysUntilOpen.toIntOrNull() ?: 1
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, days)
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        onCreate(title, content, selectedMood, sdf.format(cal.time))
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("Отправить 💌")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun LetterDetailDialog(
    letter: LoveLetterResponse,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить письмо?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    if (letter.canOpen || letter.isOpened) letter.title.ifEmpty { "💌 Письмо" }
                    else "🔒 Запечатанное письмо",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "От ${letter.senderName ?: "..."}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (letter.content != null) {
                    Text(letter.content, fontSize = 15.sp, lineHeight = 22.sp)
                    if (letter.mood.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Настроение: ${letterMoodEmoji(letter.mood)}", fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Письмо ещё запечатано", fontWeight = FontWeight.Medium)
                            Text(
                                "Откроется через ${letter.daysUntilOpen ?: "?"} дн.",
                                fontSize = 13.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text("Удалить", color = Color.Red)
            }
        }
    )
}

private fun letterMoodEmoji(mood: String): String = when (mood) {
    "love" -> "❤️ Любовь"
    "tender" -> "🥰 Нежность"
    "happy" -> "😊 Счастье"
    "grateful" -> "🤗 Благодарность"
    "passionate" -> "💋 Страсть"
    else -> mood
}
