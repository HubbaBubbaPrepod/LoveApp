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
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.viewmodel.ActivityViewModel
import com.example.loveapp.utils.rememberResponsiveConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// region Activity type definitions

internal data class ActivityDef(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val emoji: String? = null,
    val color: Color
)

internal val ACTIVITY_TYPES = listOf(
    ActivityDef("work",     "Работа",    Icons.Default.Work,            color = Color(0xFF1E90FF)),
    ActivityDef("computer", "Компьютер", Icons.Default.Computer,        color = Color(0xFF5E5CE6)),
    ActivityDef("sport",    "Спорт",     Icons.Default.FitnessCenter,   color = Color(0xFF30D158)),
    ActivityDef("food",     "Еда",       Icons.Default.Restaurant,      color = Color(0xFFFF9F0A)),
    ActivityDef("walk",     "Прогулка",  Icons.Default.DirectionsWalk,  color = Color(0xFF34C759)),
    ActivityDef("sleep",    "Сон",       Icons.Default.Bedtime,         color = Color(0xFF9C5CE6)),
    ActivityDef("reading",  "Чтение",    Icons.Default.MenuBook,        color = Color(0xFFFF6B9D)),
    ActivityDef("social",   "Общение",   Icons.Default.People,          color = Color(0xFFFF375F)),
    ActivityDef("relax",    "Отдых",     Icons.Default.SelfImprovement, color = Color(0xFFFFD60A)),
    ActivityDef("other",    "Другое",    Icons.Default.MoreHoriz,       color = Color(0xFF8E8E93))
)

/** Parses #RRGGBB to Compose Color, falls back to grey on error. */
private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF8E8E93)
}

/** Converts a CustomActivityTypeResponse into an ActivityDef for display. */
internal fun customActivityDef(ct: CustomActivityTypeResponse): ActivityDef =
    ActivityDef(
        key   = "c_${ct.id}",
        label = ct.name,
        emoji = ct.emoji,
        color = parseHexColor(ct.colorHex)
    )

/**
 * Returns the ActivityDef for the given activity_type key.
 * Built-in keys are resolved from ACTIVITY_TYPES; custom keys ("c_{id}") are
 * resolved from the provided list.
 */
internal fun activityDef(
    key: String,
    customTypes: List<CustomActivityTypeResponse> = emptyList()
): ActivityDef {
    ACTIVITY_TYPES.find { it.key == key }?.let { return it }
    if (key.startsWith("c_")) {
        val id = key.removePrefix("c_").toIntOrNull()
        customTypes.find { it.id == id }?.let { return customActivityDef(it) }
    }
    return ACTIVITY_TYPES.last()
}

/** CompositionLocal that carries the current user's + partner's custom activity types. */
val LocalCustomActivityTypes = compositionLocalOf<List<CustomActivityTypeResponse>> { emptyList() }

/** Renders the icon for an [ActivityDef] — vector icon or emoji text. */
@Composable
private fun ActivityIconView(def: ActivityDef, sizeDp: Float, tint: Color) {
    if (def.emoji != null) {
        Text(
            text = def.emoji,
            fontSize = (sizeDp * 0.75f).sp,
            textAlign = TextAlign.Center
        )
    } else {
        Icon(
            imageVector = def.icon!!,
            contentDescription = def.label,
            tint = tint,
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}

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
    val customTypes       by viewModel.customActivityTypes.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val r = rememberResponsiveConfig()

    var showPicker              by remember { mutableStateOf(false) }
    var showMyHistory           by remember { mutableStateOf(false) }
    var showPartnerHistory      by remember { mutableStateOf(false) }
    var showCalendar            by remember { mutableStateOf(false) }
    var showStats               by remember { mutableStateOf(false) }
    var showCreateCustomActivity by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    CompositionLocalProvider(LocalCustomActivityTypes provides customTypes) {
        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
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
            onCreateCustom = { showPicker = false; showCreateCustomActivity = true },
            onSave = { type, dur, time, note ->
                viewModel.createActivity(type, dur, time, note)
                showPicker = false
            }
        )
    }

    if (showCreateCustomActivity) {
        CreateCustomActivitySheet(
            onDismiss = { showCreateCustomActivity = false },
            onCreate  = { name, emoji, colorHex ->
                viewModel.createCustomActivityType(name, emoji, colorHex)
                showCreateCustomActivity = false
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
    } // end CompositionLocalProvider
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
    val customTypes = LocalCustomActivityTypes.current
    val totalMin = activities.sumOf { it.durationMinutes }
    val lastDef  = activities.maxByOrNull { it.id }?.let { activityDef(it.activityType, customTypes) }

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
                        ActivityIconView(lastDef, 30f, lastDef.color)
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
    val customTypes = LocalCustomActivityTypes.current
    val def = activityDef(activity.activityType, customTypes)
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
                ActivityIconView(def, 20f, def.color)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                // For custom activity types, prefer the stored title; for built-in, use def.label
                val displayLabel = if (activity.activityType.startsWith("c_"))
                    activity.title.ifBlank { def.label }
                else
                    def.label
                Text(displayLabel, style = MaterialTheme.typography.bodyMedium,
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
    onCreateCustom: () -> Unit,
    onSave: (type: String, durationMinutes: Int, startTime: String, note: String) -> Unit
) {
    val customTypes = LocalCustomActivityTypes.current
    val customDefs  = customTypes.map { customActivityDef(it) }

    var selectedType      by remember { mutableStateOf("") }
    var durationHoursText by remember { mutableStateOf("") }
    var durationMinsText  by remember { mutableStateOf("") }
    var startTime         by remember { mutableStateOf("") }
    var note          by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    // total chips = built-in + custom + 1 "add" chip; each row has 5
    val allDefs   = ACTIVITY_TYPES + customDefs
    val gridRows  = ((allDefs.size + 1) + 4) / 5   // ceil((items+1)/5)
    val gridHeight = (gridRows * 76).dp

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

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(gridHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allDefs) { def ->
                    ActivityTypeChip(def = def, selected = selectedType == def.key,
                        onClick = { selectedType = def.key })
                }
                // "+" chip to create a new custom activity type
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCreateCustom() }
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Своя активность",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                        Text(
                            "Своя", style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontSize = 9.sp
                        )
                    }
                }
            }

            // ── Duration picker ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Timer, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Text("Продолжительность",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    val h = durationHoursText.toIntOrNull() ?: 0
                    val m = durationMinsText.toIntOrNull() ?: 0
                    val totalMin = h * 60 + m
                    Text(
                        text = if (totalMin == 0) "не указано"
                               else if (h > 0 && m > 0) "${h}ч ${m}мин"
                               else if (h > 0) "${h} ч"
                               else "${m} мин",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = durationHoursText,
                        onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) durationHoursText = v },
                        label = { Text("Часы") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationMinsText,
                        onValueChange = { v ->
                            if (v.length <= 2 && v.all { it.isDigit() }) {
                                val num = v.toIntOrNull()
                                if (num == null || num in 0..59) durationMinsText = v
                            }
                        },
                        label = { Text("Минуты (0–59)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Start time picker ────────────────────────────────────
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (startTime.isEmpty()) "Время начала (необязательно)" else "Начало: $startTime")
            }
            if (startTime.isNotEmpty()) {
                TextButton(
                    onClick = { startTime = "" },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Очистить время",
                    style = MaterialTheme.typography.labelSmall) }
            }

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
                        val totalMin = (durationHoursText.toIntOrNull() ?: 0) * 60 +
                                       (durationMinsText.toIntOrNull() ?: 0)
                        onSave(selectedType.ifBlank { "other" }, totalMin, startTime.trim(), note.trim())
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedType.isNotBlank() ||
                              (durationHoursText.toIntOrNull() ?: 0) > 0 ||
                              (durationMinsText.toIntOrNull() ?: 0) > 0
                ) { Text("Сохранить") }
            }
        }
    }

    if (showTimePicker) {
        ActivityTimePickerDialog(
            onDismiss  = { showTimePicker = false },
            onConfirm  = { h, m ->
                startTime = "%02d:%02d".format(h, m)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun ActivityTypeChip(def: ActivityDef, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(if (selected) def.color else def.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            ActivityIconView(
                def  = def,
                sizeDp = 22f,
                tint = if (selected) Color.White else def.color
            )
        }
        Text(def.label, style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 9.sp)
    }
}

// endregion

// region Create Custom Activity Sheet

private val CUSTOM_EMOJI_OPTIONS = listOf(
    "🎮","🎨","🎵","🎬","📸","🏋️","🎯","🧘","🚴","🏊",
    "🛒","🍳","🧹","🌱","✈️","🎭","🎲","🏃","💃","🎤",
    "🧩","🦴","🌸","🎸","🐕","🐈","✍️","🎻","🏄","🛁"
)

private val CUSTOM_COLOR_OPTIONS = listOf(
    "#E53935","#D81B60","#8E24AA","#5E35B1","#3949AB",
    "#1E88E5","#039BE5","#00ACC1","#00897B","#43A047",
    "#7CB342","#F9A825","#FB8C00","#F4511E","#6D4C41"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCustomActivitySheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, emoji: String, colorHex: String) -> Unit
) {
    var name          by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(CUSTOM_EMOJI_OPTIONS.first()) }
    var selectedColor by remember { mutableStateOf(CUSTOM_COLOR_OPTIONS.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Создать свою активность",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            // ── Name field ──────────────────────────────────────────
            OutlinedTextField(
                value          = name,
                onValueChange  = { if (it.length <= 30) name = it },
                label          = { Text("Название") },
                singleLine     = true,
                leadingIcon    = {
                    Text(selectedEmoji, fontSize = 18.sp,
                        modifier = Modifier.padding(start = 4.dp))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Emoji picker ────────────────────────────────────────
            Text("Иконка", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(
                columns               = GridCells.Fixed(6),
                modifier              = Modifier.height(148.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp)
            ) {
                items(CUSTOM_EMOJI_OPTIONS) { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) parseHexColor(selectedColor).copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedEmoji = emoji }
                    ) {
                        Text(emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // ── Color picker ────────────────────────────────────────
            Text("Цвет", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(
                columns               = GridCells.Fixed(5),
                modifier              = Modifier.height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                items(CUSTOM_COLOR_OPTIONS) { hex ->
                    val isSelected = hex == selectedColor
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .clickable { selectedColor = hex }
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── Preview ─────────────────────────────────────────────
            if (name.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(parseHexColor(selectedColor).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Text(selectedEmoji, fontSize = 20.sp) }
                    Text(name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                }
            }

            // ── Buttons ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Отмена")
                }
                Button(
                    onClick  = { onCreate(name.trim(), selectedEmoji, selectedColor) },
                    modifier = Modifier.weight(1f),
                    enabled  = name.isNotBlank()
                ) { Text("Сохранить") }
            }
        }
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
                val customTypes = LocalCustomActivityTypes.current
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendChip(color = MaterialTheme.colorScheme.primary, label = myName)
                    LegendChip(color = MaterialTheme.colorScheme.secondary, label = partnerName)
                }
                allKeys.forEach { key ->
                    val def   = activityDef(key, customTypes)
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
            ActivityIconView(def, 18f, def.color)
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

// region Time picker dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val defaultHour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
    val defaultMinute = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
    }
    val state = rememberTimePickerState(
        initialHour = defaultHour,
        initialMinute = defaultMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Ок") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Время начала") },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        }
    )
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