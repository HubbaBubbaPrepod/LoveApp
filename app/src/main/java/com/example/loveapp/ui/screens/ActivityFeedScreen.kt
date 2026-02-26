package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// region Activity type definitions

internal data class ActivityDef(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

internal val ACTIVITY_TYPES = listOf(
    ActivityDef("work",     "Работа",    Icons.Default.Work,            Color(0xFF1E90FF)),
    ActivityDef("computer", "Компьютер", Icons.Default.Computer,        Color(0xFF5E5CE6)),
    ActivityDef("sport",    "Спорт",     Icons.Default.FitnessCenter,   Color(0xFF30D158)),
    ActivityDef("food",     "Еда",       Icons.Default.Restaurant,      Color(0xFFFF9F0A)),
    ActivityDef("walk",     "Прогулка",  Icons.Default.DirectionsWalk,  Color(0xFF34C759)),
    ActivityDef("sleep",    "Сон",       Icons.Default.Bedtime,         Color(0xFF9C5CE6)),
    ActivityDef("reading",  "Чтение",    Icons.Default.MenuBook,        Color(0xFFFF6B9D)),
    ActivityDef("social",   "Общение",   Icons.Default.People,          Color(0xFFFF375F)),
    ActivityDef("relax",    "Отдых",     Icons.Default.SelfImprovement, Color(0xFFFFD60A)),
    ActivityDef("other",    "Другое",    Icons.Default.MoreHoriz,       Color(0xFF8E8E93))
)

internal fun activityDef(key: String) = ACTIVITY_TYPES.find { it.key == key } ?: ACTIVITY_TYPES.last()

private val ACT_MONTH_NAMES = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
                              "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
private val ACT_DOW_LABELS  = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")

// endregion

// region Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val myActivities      by viewModel.myTodayActivities.collectAsState()
    val partnerActivities by viewModel.partnerTodayActivities.collectAsState()
    val myName            by viewModel.myName.collectAsState()
    val partnerName       by viewModel.partnerName.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val errorMessage      by viewModel.errorMessage.collectAsState()
    val successMessage    by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showPicker         by remember { mutableStateOf(false) }
    var showMyHistory      by remember { mutableStateOf(false) }
    var showPartnerHistory by remember { mutableStateOf(false) }
    var showCalendar       by remember { mutableStateOf(false) }
    var showStats          by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = "Активности",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        viewModel.loadCalendarMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                        showCalendar = true
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Календарь",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showStats = true }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Статистика",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityUserCard(
                    modifier = Modifier.weight(1f),
                    name = myName ?: "Я",
                    activities = myActivities,
                    isMe = true,
                    isLoading = isLoading,
                    onCardClick = { showPicker = true },
                    onHistoryClick = { showMyHistory = true }
                )
                ActivityUserCard(
                    modifier = Modifier.weight(1f),
                    name = partnerName ?: "Партнёр",
                    activities = partnerActivities,
                    isMe = false,
                    isLoading = false,
                    onCardClick = { showPartnerHistory = true },
                    onHistoryClick = { showPartnerHistory = true }
                )
            }

            if (myActivities.isNotEmpty()) {
                Text("Сегодня", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myActivities) { a ->
                        ActivityRow(activity = a, onDelete = { viewModel.deleteActivity(a.id) })
                    }
                }
            }
        }
    }

    if (showPicker) {
        ActivityPickerSheet(
            onDismiss = { showPicker = false },
            onSave = { type, dur, time, note ->
                viewModel.createActivity(type, dur, time, note)
                showPicker = false
            }
        )
    }

    if (showMyHistory) {
        ActivityHistorySheet(
            title = myName ?: "Я",
            activities = myActivities,
            onDelete = { viewModel.deleteActivity(it) },
            onDismiss = { showMyHistory = false }
        )
    }

    if (showPartnerHistory) {
        ActivityHistorySheet(
            title = partnerName ?: "Партнёр",
            activities = partnerActivities,
            onDelete = null,
            onDismiss = { showPartnerHistory = false }
        )
    }

    if (showCalendar) {
        ActivityCalendarSheet(
            viewModel = viewModel,
            myName = myName ?: "Я",
            partnerName = partnerName ?: "Партнёр",
            onDismiss = { showCalendar = false }
        )
    }

    if (showStats) {
        ActivityStatsSheet(
            myActivities = myActivities,
            partnerActivities = partnerActivities,
            myName = myName ?: "Я",
            partnerName = partnerName ?: "Партнёр",
            onDismiss = { showStats = false }
        )
    }
}

// endregion

// region User Card

@Composable
private fun ActivityUserCard(
    modifier: Modifier = Modifier,
    name: String,
    activities: List<ActivityResponse>,
    isMe: Boolean,
    isLoading: Boolean,
    onCardClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val totalMin = activities.sumOf { it.durationMinutes }
    val lastDef  = activities.maxByOrNull { it.id }?.let { activityDef(it.activityType) }

    Card(
        modifier = modifier.aspectRatio(0.85f).clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(name.take(10), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onHistoryClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.History, contentDescription = "История",
                            modifier = Modifier.size(18.dp),
                            tint = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.fillMaxWidth()) {
                if (lastDef != null) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(lastDef.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        Icon(lastDef.icon, contentDescription = lastDef.label,
                            tint = lastDef.color, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(lastDef.label, style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                } else {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Нет активностей", style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column {
                Text("${activities.size} активн.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                if (totalMin > 0) {
                    Text(formatMinutes(totalMin),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// endregion

// region Activity Row

@Composable
private fun ActivityRow(activity: ActivityResponse, onDelete: (() -> Unit)?) {
    val def = activityDef(activity.activityType)
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(def.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(def.icon, contentDescription = def.label,
                    tint = def.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(def.label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            val meta = remember(activity.startTime, activity.durationMinutes, activity.note) {
                buildString {
                    if (activity.startTime.isNotBlank()) append(activity.startTime)
                    if (activity.durationMinutes > 0) {
                        if (isNotEmpty()) append("  ")
                        append(formatMinutes(activity.durationMinutes))
                    }
                    if (activity.note.isNotBlank()) {
                        if (isNotEmpty()) append("  ")
                        append(activity.note.take(30))
                    }
                }
            }
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// endregion

// region Picker Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityPickerSheet(
    onDismiss: () -> Unit,
    onSave: (type: String, durationMinutes: Int, startTime: String, note: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var startTime    by remember { mutableStateOf("") }
    var note         by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Записать активность", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

            LazyVerticalGrid(columns = GridCells.Fixed(5),
                modifier = Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ACTIVITY_TYPES) { def ->
                    ActivityTypeChip(def = def, selected = selectedType == def.key,
                        onClick = { selectedType = def.key })
                }
            }

            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                label = { Text("Продолжительность (мин)") },
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Время начала (необязательно)") },
                placeholder = { Text("09:30") },
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        val dur = durationText.toIntOrNull() ?: 0
                        onSave(selectedType.ifBlank { "other" }, dur, startTime.trim(), note.trim())
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedType.isNotBlank() || durationText.isNotBlank()
                ) { Text("Сохранить") }
            }
        }
    }
}

@Composable
private fun ActivityTypeChip(def: ActivityDef, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(if (selected) def.color else def.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(def.icon, contentDescription = def.label,
                tint = if (selected) Color.White else def.color,
                modifier = Modifier.size(22.dp))
        }
        Text(def.label, style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 9.sp)
    }
}

// endregion

// region History Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityHistorySheet(
    title: String,
    activities: List<ActivityResponse>,
    onDelete: ((Int) -> Unit)?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            if (activities.isEmpty()) {
                Text("Нет активностей за сегодня",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp))
            } else {
                activities.forEach { a ->
                    ActivityRow(activity = a,
                        onDelete = if (onDelete != null) ({ onDelete(a.id) }) else null)
                }
            }
        }
    }
}

// endregion

// region Calendar Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCalendarSheet(
    viewModel: ActivityViewModel,
    myName: String,
    partnerName: String,
    onDismiss: () -> Unit
) {
    val year    by viewModel.calendarYear.collectAsState()
    val month   by viewModel.calendarMonth.collectAsState()
    val myMap   by viewModel.myMonthActivities.collectAsState()
    val partMap by viewModel.partnerMonthActivities.collectAsState()
    val loading by viewModel.isCalendarLoading.collectAsState()

    var selectedDay by remember { mutableStateOf<String?>(null) }

    val monthNames = ACT_MONTH_NAMES
    val dayLabels  = ACT_DOW_LABELS

    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    val (y, m) = prevMonth(year, month)
                    viewModel.loadCalendarMonth(y, m)
                }) { Icon(Icons.Default.ChevronLeft, null) }
                Text("${monthNames[month]} $year",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val (y, m) = nextMonth(year, month)
                    viewModel.loadCalendarMonth(y, m)
                }) { Icon(Icons.Default.ChevronRight, null) }
            }

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEach { d ->
                        Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val (calRows, dateForDay) = remember(year, month) {
                    val cal = Calendar.getInstance()
                    cal.set(year, month, 1)
                    val firstDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val cells = (0 until firstDow).map { null } + (1..daysInMonth).map { it }
                    val rows = cells.chunked(7)
                    val dateMap = (1..daysInMonth).associate { d ->
                        cal.set(year, month, d)
                        d to fmt.format(cal.time)
                    }
                    Pair(rows, dateMap)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    calRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { day ->
                                Box(modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center) {
                                    if (day != null) {
                                        val dateStr = dateForDay[day] ?: ""
                                        val myList  = myMap[dateStr] ?: emptyList()
                                        val ptList  = partMap[dateStr] ?: emptyList()
                                        val isSelected = selectedDay == dateStr
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(2.dp).clip(CircleShape)
                                                .then(if (isSelected) Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer)
                                                else Modifier)
                                                .clickable {
                                                    selectedDay = if (isSelected) null else dateStr
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Text(day.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold
                                                             else FontWeight.Normal)
                                            if (myList.isNotEmpty() || ptList.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    if (myList.isNotEmpty()) Dot(MaterialTheme.colorScheme.primary)
                                                    if (ptList.isNotEmpty()) Dot(MaterialTheme.colorScheme.secondary)
                                                }
                                            } else {
                                                Spacer(Modifier.height(6.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(7 - row.size) { Box(modifier = Modifier.weight(1f)) {} }
                        }
                    }
                }

                selectedDay?.let { day ->
                    HorizontalDivider()
                    val myList = myMap[day] ?: emptyList()
                    val ptList = partMap[day] ?: emptyList()
                    Text(DateUtils.formatDateForDisplay(day),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (myList.isEmpty() && ptList.isEmpty()) {
                        Text("Нет активностей", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (myList.isNotEmpty()) {
                        Text(myName, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        myList.forEach { a -> ActivityRow(activity = a, onDelete = null) }
                    }
                    if (ptList.isNotEmpty()) {
                        Text(partnerName, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary)
                        ptList.forEach { a -> ActivityRow(activity = a, onDelete = null) }
                    }
                }
            }
        }
    }
}

// endregion

// region Stats Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityStatsSheet(
    myActivities: List<ActivityResponse>,
    partnerActivities: List<ActivityResponse>,
    myName: String,
    partnerName: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Статистика  сегодня", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

            val myByType  = remember(myActivities) { myActivities.groupBy { it.activityType } }
            val ptByType  = remember(partnerActivities) { partnerActivities.groupBy { it.activityType } }
            val allKeys   = remember(myActivities, partnerActivities) { (myByType.keys + ptByType.keys).distinct() }
            val maxMin    = remember(myActivities, partnerActivities) {
                allKeys.maxOfOrNull { key ->
                    maxOf(myByType[key]?.sumOf { it.durationMinutes } ?: 0,
                          ptByType[key]?.sumOf { it.durationMinutes } ?: 0)
                }?.coerceAtLeast(1) ?: 1
            }

            if (allKeys.isEmpty()) {
                Text("Нет данных для отображения",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendChip(color = MaterialTheme.colorScheme.primary, label = myName)
                    LegendChip(color = MaterialTheme.colorScheme.secondary, label = partnerName)
                }
                allKeys.forEach { key ->
                    val def   = activityDef(key)
                    val myMin = myByType[key]?.sumOf { it.durationMinutes } ?: 0
                    val ptMin = ptByType[key]?.sumOf { it.durationMinutes } ?: 0
                    StatsBar(def = def, myMin = myMin, ptMin = ptMin, maxMin = maxMin,
                        myColor = MaterialTheme.colorScheme.primary,
                        ptColor = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider()
                val myTotal = myActivities.sumOf { it.durationMinutes }
                val ptTotal = partnerActivities.sumOf { it.durationMinutes }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$myName: ${formatMinutes(myTotal)}")
                    Text("$partnerName: ${formatMinutes(ptTotal)}")
                }
            }
        }
    }
}

@Composable
private fun StatsBar(
    def: ActivityDef, myMin: Int, ptMin: Int, maxMin: Int,
    myColor: Color, ptColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(def.icon, contentDescription = def.label,
                tint = def.color, modifier = Modifier.size(18.dp))
            Text(def.label, style = MaterialTheme.typography.bodySmall)
        }
        if (myMin > 0) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.height(10.dp)
                        .fillMaxWidth(fraction = (myMin.toFloat() / maxMin).coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(myColor))
                Text(formatMinutes(myMin), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (ptMin > 0) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.height(10.dp)
                        .fillMaxWidth(fraction = (ptMin.toFloat() / maxMin).coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(ptColor))
                Text(formatMinutes(ptMin), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
}

// endregion

// region Helpers

private fun formatMinutes(min: Int): String {
    if (min <= 0) return "0 мин"
    val h = min / 60; val m = min % 60
    return if (h > 0) "${h}ч ${m}мин" else "${m} мин"
}

private fun prevMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 0) Pair(year - 1, 11) else Pair(year, month - 1)

private fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 11) Pair(year + 1, 0) else Pair(year, month + 1)

// endregion