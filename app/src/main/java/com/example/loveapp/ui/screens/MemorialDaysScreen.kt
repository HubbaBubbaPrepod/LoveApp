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
import com.example.loveapp.data.entity.MemorialDay
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.MemorialViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorialDaysScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemorialViewModel = hiltViewModel()
) {
    val days by viewModel.memorialDays.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            IOSTopAppBar(title = "Памятные дни", onBackClick = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { innerPadding ->
        if (isLoading && days.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (days.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Нет памятных дней", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Добавьте важные даты 💕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(days, key = { it.id }) { day ->
                    MemorialDayCard(day = day, onDelete = { viewModel.deleteMemorialDay(day.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemorialDayDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date, type, icon ->
                viewModel.addMemorialDay(title = title, date = date, type = type, icon = icon)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemorialDayCard(day: MemorialDay, onDelete: () -> Unit) {
    val daysUntil = try {
        val target = LocalDate.parse(day.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        var next = target.withYear(today.year)
        if (next.isBefore(today)) next = next.plusYears(1)
        ChronoUnit.DAYS.between(today, next)
    } catch (_: Exception) { null }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(day.icon, fontSize = 32.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(day.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if (daysUntil != null) {
                    Text(
                        text = if (daysUntil == 0L) "Сегодня! 🎉" else "Через $daysUntil дн.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemorialDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: String, type: String, icon: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💕") }
    val icons = listOf("💕", "🎂", "💍", "🌹", "⭐", "🎉", "✈️", "🏠")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый памятный день") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Иконка:", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { icon ->
                        val selected = icon == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) { Text(icon, fontSize = 22.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank() && date.isNotBlank()) onConfirm(title, date, "custom", selectedIcon) },
                enabled = title.isNotBlank() && date.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
