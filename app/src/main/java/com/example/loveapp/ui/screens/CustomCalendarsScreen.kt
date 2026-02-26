package com.example.loveapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.viewmodel.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

//  Color helpers 

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> 0xFF000000.toInt() or cleaned.toInt(16)
            8 -> cleaned.toLong(16).toInt()
            else -> 0xFFBBBBBB.toInt()
        }
        Color(argb)
    } catch (_: Exception) { Color(0xFFBBBBBB) }
}

private fun calendarAccentColor(colorHex: String): Color = parseHexColor(colorHex)

private val CUSTOM_CAL_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val CAL_DOW_LABELS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

//  Entry composable 

@Composable
fun CustomCalendarsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    var selectedCalendarId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage  by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        AnimatedContent(
            targetState = selectedCalendarId,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            label = "calendar_anim"
        ) { calId ->
            if (calId == null) {
                CalendarListScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onCalendarClick = { id ->
                        viewModel.selectCalendar(id)
                        selectedCalendarId = id
                    }
                )
            } else {
                CalendarDetailScreen(
                    calendarId = calId,
                    viewModel = viewModel,
                    onBack = { selectedCalendarId = null }
                )
            }
        }
    }
}

//  Calendar List Screen 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarListScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit,
    onCalendarClick: (Int) -> Unit
) {
    val calendars  by viewModel.calendars.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val events     by viewModel.events.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Календари", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                isLoading && calendars.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                calendars.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Нет календарей",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Нажмите + чтобы создать первый",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                else -> {
                    val today = LocalDate.now()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(calendars) { cal ->
                            val markedDates = events[cal.id]?.map { it.eventDate }?.toSet() ?: emptySet()
                            CalendarTile(
                                name       = cal.name,
                                colorHex   = cal.colorHex,
                                markedDates = markedDates,
                                today      = today,
                                onClick    = { onCalendarClick(cal.id) },
                                onDelete   = { viewModel.deleteCalendar(cal.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateCalendarDialog(
            onDismiss = { showDialog = false },
            onCreate  = { name, color ->
                viewModel.createCalendar(name, color)
                showDialog = false
            }
        )
    }
}

//  Calendar Tile 

@Composable
private fun CalendarTile(
    name: String,
    colorHex: String,
    markedDates: Set<String>,
    today: LocalDate,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val accent  = calendarAccentColor(colorHex)
    // Blend accent 20% with the theme surface color (white in light, dark in dark)
    val base    = MaterialTheme.colorScheme.surface
    val surface = Color(
        red   = accent.red   * 0.20f + base.red   * 0.80f,
        green = accent.green * 0.20f + base.green * 0.80f,
        blue  = accent.blue  * 0.20f + base.blue  * 0.80f,
        alpha = 1f
    )
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Colour dot + name — top-left, no outer padding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp, end = 36.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }

            // Delete button — top-right corner, no outer padding
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Mini-week — pinned to the bottom, no outer padding
            MiniWeekView(
                today       = today,
                markedDates = markedDates,
                accent      = accent,
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить $name?") },
            text  = { Text("Все отмеченные дни этого календаря будут удалены.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

//  Mini-week (current MonSun) 

@Composable
private fun MiniWeekView(
    today: LocalDate,
    markedDates: Set<String>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val (monday, week) = remember(today) {
        val mon = today.with(DayOfWeek.MONDAY)
        mon to (0..6).map { mon.plusDays(it.toLong()) }
    }

    Column(modifier) {
        // Day labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CAL_DOW_LABELS.forEach { label ->
                Text(
                    text  = label,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        // Day circles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day ->
                val dateStr  = day.format(CUSTOM_CAL_FMT)
                val isMarked = dateStr in markedDates
                val isToday  = day == today

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isMarked -> accent
                                isToday  -> accent.copy(alpha = 0.15f)
                                else     -> Color.Transparent
                            }
                        )
                        .then(
                            if (isToday && !isMarked)
                                Modifier.border(1.dp, accent, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMarked) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    } else {
                        Text(
                            text     = day.dayOfMonth.toString(),
                            fontSize = 9.sp,
                            color    = if (isToday) accent else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

//  Create Calendar Dialog 

@Composable
private fun CreateCalendarDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    val palette = listOf(
        "#FF4D6D", "#FF9F0A", "#30D158", "#4D7FFF",
        "#BF5AF2", "#FF6B6B", "#00C9C9", "#A0522D"
    )
    var name        by remember { mutableStateOf("") }
    var selectedHex by remember { mutableStateOf(palette[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый календарь", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Название") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )
                Text(
                    text  = "Цвет",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    palette.forEach { hex ->
                        val color   = parseHexColor(hex)
                        val selected = hex == selectedHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.border(1.dp, color.copy(alpha = 0.3f), CircleShape)
                                )
                                .clickable { selectedHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (name.isNotBlank()) onCreate(name.trim(), selectedHex) },
                enabled  = name.isNotBlank(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = parseHexColor(selectedHex))
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

//  Calendar Detail Screen 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDetailScreen(
    calendarId: Int,
    viewModel: CalendarViewModel,
    onBack: () -> Unit
) {
    val calendar     = viewModel.calendarById(calendarId)
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val events        by viewModel.events.collectAsState()
    val marked        = events[calendarId]?.map { it.eventDate }?.toSet() ?: emptySet()

    val accent  = if (calendar != null) calendarAccentColor(calendar.colorHex) else MaterialTheme.colorScheme.primary

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = calendar?.name ?: "Календарь",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            // Month navigation row
            MonthNavigationRow(
                month    = calendarMonth,
                onPrev   = { viewModel.prevMonth() },
                onNext   = { viewModel.nextMonth() },
                accent   = accent
            )

            // Day-of-week headers
            DayHeaders()

            // Day grid
            MonthDayGrid(
                month       = calendarMonth,
                markedDates = marked,
                today       = LocalDate.now(),
                accent      = accent,
                onDayClick  = { day -> viewModel.toggleDay(calendarId, day) }
            )

            Spacer(Modifier.height(24.dp))

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text("Отмеченный день",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(Color.Transparent)
                        .border(1.5.dp, accent, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text("Сегодня",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthNavigationRow(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    accent: Color
) {
    val locale   = Locale("ru")
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Пред. месяц", tint = accent)
        }
        Text(
            text       = "$monthName ${month.year}",
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "След. месяц", tint = accent)
        }
    }
}

@Composable
private fun DayHeaders() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CAL_DOW_LABELS.forEach { label ->
            Text(
                text      = label,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MonthDayGrid(
    month: YearMonth,
    markedDates: Set<String>,
    today: LocalDate,
    accent: Color,
    onDayClick: (LocalDate) -> Unit
) {
    val (startOffset, daysInMonth, gridRows) = remember(month) {
        val firstDay  = month.atDay(1)
        val so = (firstDay.dayOfWeek.value - 1)
        val dim = month.lengthOfMonth()
        Triple(so, dim, (so + dim + 6) / 7)
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (row in 0 until gridRows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day      = month.atDay(dayNumber)
                        val dateStr  = day.format(CUSTOM_CAL_FMT)
                        val isMarked = dateStr in markedDates
                        val isToday  = day == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isMarked -> accent
                                        isToday  -> accent.copy(alpha = 0.12f)
                                        else     -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isToday && !isMarked)
                                        Modifier.border(1.5.dp, accent, CircleShape)
                                    else Modifier
                                )
                                .clickable { onDayClick(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMarked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize   = 13.sp,
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint   = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text   = dayNumber.toString(),
                                    fontSize = 14.sp,
                                    color  = when {
                                        isToday -> accent
                                        else    -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}