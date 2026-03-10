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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.BucketItemResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.viewmodels.BucketListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketListScreen(
    onNavigateBack: () -> Unit,
    viewModel: BucketListViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val showCompleted by viewModel.showCompleted.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showCreateDialog) {
        CreateBucketItemDialog(
            categories = categories,
            onDismiss = { viewModel.toggleCreateDialog() },
            onCreate = { title, desc, cat, emoji, date ->
                viewModel.createItem(title, desc, cat, emoji, date)
            }
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
                Icon(Icons.Default.Add, contentDescription = "Добавить мечту")
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
                title = "Список мечт",
                onBackClick = onNavigateBack
            )

            if (isLoading && items.isEmpty()) {
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
                                        listOf(Color(0xFF7C4DFF), Color(0xFFFF6B9D))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌟", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${stats.completed} / ${stats.total}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("мечт исполнено", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                                Spacer(Modifier.height(12.dp))
                                if (stats.total > 0) {
                                    LinearProgressIndicator(
                                        progress = { stats.completed.toFloat() / stats.total },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }

                // Category + status filter
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = showCompleted == null,
                                onClick = { viewModel.setShowCompleted(null) },
                                label = { Text("Все") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = showCompleted == false,
                                onClick = { viewModel.setShowCompleted(false) },
                                label = { Text("⏳ Активные") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = showCompleted == true,
                                onClick = { viewModel.setShowCompleted(true) },
                                label = { Text("✅ Исполнено") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B9D),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = { Text("${categoryEmoji(cat)} ${categoryLabel(cat)}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7C4DFF),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✨", fontSize = 56.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Список мечт пуст", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                                Text(
                                    "Добавьте то, что хотите сделать вместе!",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                items(items, key = { it.id }) { item ->
                    BucketItemCard(
                        item = item,
                        onComplete = { viewModel.completeItem(item.id) },
                        onUncomplete = { viewModel.uncompleteItem(item.id) },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BucketItemCard(
    item: BucketItemResponse,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) Color(0xFFF1F8E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            IconButton(
                onClick = { if (item.isCompleted) onUncomplete() else onComplete() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.isCompleted) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        color = if (item.isCompleted) Color.Gray else Color(0xFF333333),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor(item.category).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${categoryEmoji(item.category)} ${categoryLabel(item.category)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = categoryColor(item.category)
                        )
                    }

                    if (item.createdByName != null) {
                        Text("от ${item.createdByName}", fontSize = 11.sp, color = Color.Gray)
                    }

                    if (item.isCompleted && item.completedByName != null) {
                        Text("✅ ${item.completedByName}", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    }
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color.Red) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBucketItemDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String?, cat: String, emoji: String, targetDate: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("other") }
    var selectedEmoji by remember { mutableStateOf("✨") }

    val emojis = listOf("✨", "🌍", "🎯", "❤️", "🎉", "🏔️", "🍽️", "📚", "🏃", "🎨", "🎵", "🏠")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✨ Новая мечта", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(300) },
                    label = { Text("Название *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Text("Иконка:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(emojis) { emoji ->
                        FilterChip(
                            selected = selectedEmoji == emoji,
                            onClick = { selectedEmoji = emoji },
                            label = { Text(emoji, fontSize = 18.sp) },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }

                Text("Категория:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cats = if (categories.isNotEmpty()) categories else listOf(
                        "travel", "food", "adventure", "romance",
                        "lifestyle", "learning", "health", "creativity", "other"
                    )
                    items(cats) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${categoryEmoji(cat)} ${categoryLabel(cat)}", fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            title,
                            description.ifBlank { null },
                            selectedCategory,
                            selectedEmoji,
                            null
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Добавить ✨")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun categoryEmoji(cat: String): String = when (cat) {
    "travel" -> "✈️"
    "food" -> "🍽️"
    "adventure" -> "🏔️"
    "romance" -> "❤️"
    "lifestyle" -> "🏠"
    "learning" -> "📚"
    "health" -> "🏃"
    "creativity" -> "🎨"
    else -> "✨"
}

private fun categoryLabel(cat: String): String = when (cat) {
    "travel" -> "Путешествия"
    "food" -> "Еда"
    "adventure" -> "Приключения"
    "romance" -> "Романтика"
    "lifestyle" -> "Стиль жизни"
    "learning" -> "Обучение"
    "health" -> "Здоровье"
    "creativity" -> "Творчество"
    else -> "Другое"
}

private fun categoryColor(cat: String): Color = when (cat) {
    "travel" -> Color(0xFF2196F3)
    "food" -> Color(0xFFFF9800)
    "adventure" -> Color(0xFF4CAF50)
    "romance" -> Color(0xFFE91E63)
    "lifestyle" -> Color(0xFF9C27B0)
    "learning" -> Color(0xFF3F51B5)
    "health" -> Color(0xFF00BCD4)
    "creativity" -> Color(0xFFFF5722)
    else -> Color(0xFF607D8B)
}
