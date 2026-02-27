package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.loveapp.viewmodel.CycleDayType
import com.example.loveapp.viewmodel.CycleViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// region Symptom / Mood definitions

internal data class SymptomDef(val key: String, val emoji: String, val label: String)
internal data class CycleMoodDef(val key: String, val emoji: String, val label: String)

internal val SYMPTOM_DEFS = listOf(
    SymptomDef("period_start", "\uD83E\uDE78", "Начало цикла"),
    SymptomDef("sex",          "\uD83D\uDC95", "Секс"),
    SymptomDef("pain_mild",    "\uD83D\uDE23", "Боль (слабая)"),
    SymptomDef("pain_severe",  "\uD83D\uDE2B", "Боль (сильная)"),
    SymptomDef("pregnancy",    "\uD83E\uDD30", "Беременность"),
    SymptomDef("medication",   "\uD83D\uDC8A", "Лекарства"),
    SymptomDef("discharge",    "\uD83D\uDCA7", "Выделения"),
    SymptomDef("spotting",     "\uD83D\uDD34", "Мажущие"),
    SymptomDef("bloating",     "\uD83E\uDEB7", "Вздутие"),
    SymptomDef("headache",     "\uD83E\uDD15", "Голов. боль"),
    SymptomDef("pms",          "\uD83D\uDE24", "ПМС"),
    SymptomDef("fatigue",      "\uD83D\uDCA4", "Усталость"),
    SymptomDef("nausea",       "\uD83E\uDD22", "Тошнота"),
    SymptomDef("temperature",  "\uD83C\uDF21", "Температура")
)

internal val MOOD_DEFS = listOf(
    CycleMoodDef("happy",    "\uD83D\uDE0A", "Радость"),
    CycleMoodDef("sad",      "\uD83D\uDE22", "Грусть"),
    CycleMoodDef("tired",    "\uD83D\uDE34", "Усталость"),
    CycleMoodDef("anxious",  "\uD83D\uDE30", "Тревога"),
    CycleMoodDef("irritable","\uD83D\uDE24", "Раздражение"),
    CycleMoodDef("romantic", "\uD83E\uDD70", "Романтично"),
    CycleMoodDef("neutral",  "\uD83D\uDE10", "Нейтрально")
)

internal fun symptomEmoji(key: String) = SYMPTOM_DEFS.find { it.key == key }?.emoji ?: ""
internal fun moodEmoji   (key: String) = MOOD_DEFS.find    { it.key == key }?.emoji ?: ""

// endregion

// region Colors
private val ColorPeriodActual     = Color(0xFFFF4D6D)
private val ColorPeriodPredicted  = Color(0xFFFFB3C1)
private val ColorOvulationActual  = Color(0xFF4D7FFF)
private val ColorOvulationPredicted = Color(0xFF90B3FF)
private val ColorFertile          = Color(0xFFB8CFFF)
// endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenstrualCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CycleViewModel = hiltViewModel()
) {
    val cycles          by viewModel.cycles.collectAsState()
    val isGirl          by viewModel.isGirl.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val errorMessage    by viewModel.errorMessage.collectAsState()
    val successMessage  by viewModel.successMessage.collectAsState()
    val cycleDayMap     by viewModel.cycleDayMap.collectAsState()
    val currentPhase    by viewModel.currentPhase.collectAsState()
    val daysUntilNext   by viewModel.daysUntilNextPeriod.collectAsState()
    val nextPeriodDate  by viewModel.nextPeriodDate.collectAsState()
    val calYear         by viewModel.calendarYear.collectAsState()
    val calMonth        by viewModel.calendarMonth.collectAsState()
    val selectedDate    by viewModel.selectedDate.collectAsState()
    val avgCycleDur     by viewModel.avgCycleDuration.collectAsState()
    val avgPeriodDur    by viewModel.avgPeriodDuration.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showStats         by remember { mutableStateOf(false) }
    var showSettings      by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage)  { errorMessage?.let  { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(successMessage){ successMessage?.let{ snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = if (isGirl) "Цикл" else "Цикл партнёра",
                onBackClick = onNavigateBack,
                actions = {
                    if (isGirl && cycles.isNotEmpty()) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { showStats = true }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Статистика",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading && cycles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPeriodActual)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                //  Phase summary card 
                item {
                    PhaseCard(
                        phase        = currentPhase,
                        daysUntil    = daysUntilNext,
                        nextDate     = nextPeriodDate,
                        avgCycle     = avgCycleDur,
                        avgPeriod    = avgPeriodDur,
                        isEmpty      = cycles.isEmpty()
                    )
                }

                //  Mark period start (girl only, if no data) 
                if (isGirl && cycles.isEmpty()) {
                    item {
                        Button(
                            onClick = {
                                val today = LocalDate.now().format(CYCLE_CAL_FMT)
                                viewModel.markPeriodStart(today)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPeriodActual)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Отметить начало цикла сегодня")
                        }
                    }
                }

                //  Calendar 
                item {
                    CycleCalendar(
                        year        = calYear,
                        month       = calMonth,
                        cycleDayMap = cycleDayMap,
                        getSymptoms = { viewModel.getSymptomsForDate(it) },
                        getMood     = { viewModel.getMoodForDate(it) },
                        selectedDate = selectedDate,
                        onDayClick  = { viewModel.selectDate(it) },
                        onPrevMonth = { viewModel.prevCalendarMonth() },
                        onNextMonth = { viewModel.nextCalendarMonth() }
                    )
                }

                //  Legend 
                item { CycleLegend() }
            }
        }
    }

    //  Day Detail Sheet 
    selectedDate?.let { date ->
        val dayType = cycleDayMap[date]
        DayDetailSheet(
            date       = date,
            dayType    = dayType,
            symptoms   = viewModel.getSymptomsForDate(date),
            mood       = viewModel.getMoodForDate(date),
            isGirl     = isGirl,
            onDismiss  = { viewModel.selectDate(null) },
            onSave     = { syms, moodKey ->
                viewModel.saveDayData(date, syms, moodKey)
                viewModel.selectDate(null)
            },
            onMarkPeriodStart = {
                viewModel.markPeriodStart(date)
                viewModel.selectDate(null)
            },
            onDeleteCycle = if (isGirl && dayType == CycleDayType.PERIOD_ACTUAL) {
                {
                    viewModel.deleteCycleForDate(date)
                    viewModel.selectDate(null)
                }
            } else null
        )
    }

    //  Stats Sheet 
    if (showStats) {
        StatsSheet(
            cycles     = cycles,
            avgCycle   = avgCycleDur,
            avgPeriod  = avgPeriodDur,
            nextPeriod = nextPeriodDate,
            onDismiss  = { showStats = false }
        )
    }

    //  Cycle Settings Dialog 
    if (showSettings) {
        CycleSettingsDialog(
            initialCycleDuration  = avgCycleDur,
            initialPeriodDuration = avgPeriodDur,
            onDismiss = { showSettings = false },
            onSave    = { cd, pd ->
                viewModel.updateCycleSettings(cd, pd)
                showSettings = false
            }
        )
    }
}

// endregion

// region Phase Card

@Composable
private fun PhaseCard(
    phase: String,
    daysUntil: Int?,
    nextDate: String?,
    avgCycle: Int,
    avgPeriod: Int,
    isEmpty: Boolean
) {
    val gradientColor = when {
        phase.contains("Менструация") -> ColorPeriodActual
        phase.contains("Овуляция")    -> ColorOvulationActual
        phase.contains("Лютеиновая")  -> Color(0xFF9B59B6)
        else                          -> Color(0xFF2ECC71)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = gradientColor.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(gradientColor))
                Text(if (isEmpty) "Нет данных" else phase,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = gradientColor)
            }
            if (!isEmpty && daysUntil != null) {
                val msg = when {
                    daysUntil <= 0 -> "Месячные начались или ожидаются сегодня"
                    daysUntil == 1 -> "Месячные завтра"
                    else           -> "До месячных: $daysUntil дней"
                }
                Text(msg, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isEmpty) {
                Text("Отметьте начало цикла, чтобы начать отслеживание",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isEmpty) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("Цикл", "${avgCycle}д", ColorOvulationActual)
                    StatChip("Месячные", "${avgPeriod}д", ColorPeriodActual)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// endregion

// region Calendar

// DateTimeFormatter is thread-safe — keep a single top-level instance
private val CYCLE_CAL_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val CYCLE_STATS_DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("ru"))
private val CYCLE_MONTH_SHORT = listOf("янв","фев","мар","апр","май","июн","июл","авг","сен","окт","ноя","дек")
private val CYCLE_MONTH_FULL  = listOf("Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
private val CYCLE_DOW_LABELS  = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")

@Composable
private fun CycleCalendar(
    year: Int, month: Int,
    cycleDayMap: Map<String, CycleDayType>,
    getSymptoms: (String) -> List<String>,
    getMood: (String) -> String,
    selectedDate: String?,
    onDayClick: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = CYCLE_MONTH_FULL
    val dayLabels  = CYCLE_DOW_LABELS

    // Cache expensive grid computation — only recalculate when year or month changes
    val (calRows, dateForDay, todayStr) = remember(year, month) {
        val ym = YearMonth.of(year, month + 1)
        val firstDow = (ym.atDay(1).dayOfWeek.value - 1)
        val daysInMonth = ym.lengthOfMonth()
        val cells = (0 until firstDow).map { null } + (1..daysInMonth).map { it }
        val rows = cells.chunked(7)
        val dateMap = (1..daysInMonth).associate { d -> d to ym.atDay(d).format(CYCLE_CAL_FMT) }
        Triple(rows, dateMap, LocalDate.now().format(CYCLE_CAL_FMT))
    }

    Card(shape = RoundedCornerShape(20.dp),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
         elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Month navigation
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, null) }
                Text("${monthNames[month]} $year",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, null) }
            }

            // Day-of-week row
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEach { d ->
                    Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                calRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { day ->
                            Box(modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    val date = dateForDay[day] ?: ""
                                    val dayType = cycleDayMap[date]
                                    val syms = getSymptoms(date)
                                    val mood = getMood(date)
                                    val isSelected = selectedDate == date
                                    val isToday = date == todayStr
                                    CycleCalendarDay(
                                        day = day, date = date, dayType = dayType,
                                        symptoms = syms, mood = mood,
                                        isSelected = isSelected, isToday = isToday,
                                        onClick = { onDayClick(date) }
                                    )
                                }
                            }
                        }
                        repeat(7 - row.size) { Box(Modifier.weight(1f)) {} }
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleCalendarDay(
    day: Int, date: String, dayType: CycleDayType?,
    symptoms: List<String>, mood: String,
    isSelected: Boolean, isToday: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when (dayType) {
        CycleDayType.PERIOD_ACTUAL       -> ColorPeriodActual
        CycleDayType.PERIOD_PREDICTED    -> ColorPeriodPredicted
        CycleDayType.OVULATION_ACTUAL    -> ColorOvulationActual
        CycleDayType.OVULATION_PREDICTED -> ColorOvulationPredicted
        CycleDayType.FERTILE_PREDICTED   -> ColorFertile
        else                             -> Color.Transparent
    }
    val textColor = when (dayType) {
        CycleDayType.PERIOD_ACTUAL, CycleDayType.OVULATION_ACTUAL ->
            Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val firstEmoji = symptoms.firstOrNull()?.let { symptomEmoji(it) }
        ?: mood.takeIf { it.isNotBlank() }?.let { moodEmoji(it) }
    val hasMore = symptoms.size > 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .background(bgColor)
            .then(if (isToday && bgColor == Color.Transparent)
                Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            else Modifier)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .widthIn(min = 36.dp)
    ) {
        Text(day.toString(), style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor, fontSize = 12.sp)
        if (firstEmoji != null) {
            Text(firstEmoji + if (hasMore) "+" else "", fontSize = 9.sp,
                lineHeight = 10.sp, textAlign = TextAlign.Center)
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}

// endregion

// region Legend

@Composable
private fun CycleLegend() {
    Card(shape = RoundedCornerShape(16.dp),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Обозначения", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LegendRow(ColorPeriodActual,      "Месячные (факт.)")
                LegendRow(ColorPeriodPredicted,   "Месячные (прогноз)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LegendRow(ColorOvulationActual,    "Овуляция (факт.)")
                LegendRow(ColorFertile,            "Фертильные дни")
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// endregion

// region Day Detail Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    date: String,
    dayType: CycleDayType?,
    symptoms: List<String>,
    mood: String,
    isGirl: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<String>, String) -> Unit,
    onMarkPeriodStart: () -> Unit,
    onDeleteCycle: (() -> Unit)? = null
) {
    val parsedDate = remember(date) { runCatching { LocalDate.parse(date, CYCLE_CAL_FMT) }.getOrNull() }
    val displayDate = remember(parsedDate) {
        parsedDate?.let { "${it.dayOfMonth} ${CYCLE_MONTH_SHORT[it.monthValue - 1]} ${it.year}" } ?: date
    }

    var selectedSymptoms by remember(date) { mutableStateOf(symptoms.toMutableList()) }
    var selectedMood     by remember(date) { mutableStateOf(mood) }
    var edited           by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text(displayDate, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

            // Current day type indicator
            if (dayType != null && dayType != CycleDayType.NORMAL) {
                val (color, label) = when (dayType) {
                    CycleDayType.PERIOD_ACTUAL    -> ColorPeriodActual to "Менструация"
                    CycleDayType.PERIOD_PREDICTED -> ColorPeriodPredicted to "Прогноз: месячные"
                    CycleDayType.OVULATION_ACTUAL -> ColorOvulationActual to "Овуляция"
                    CycleDayType.OVULATION_PREDICTED -> ColorOvulationPredicted to "Прогноз: овуляция"
                    CycleDayType.FERTILE_PREDICTED -> ColorFertile to "Фертильный день"
                    else -> Color.Transparent to ""
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
                }
            }

            // Mark period start (girl only, if not already actual period day)
            if (isGirl && dayType != CycleDayType.PERIOD_ACTUAL) {
                OutlinedButton(onClick = onMarkPeriodStart, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPeriodActual)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Отметить начало цикла")
                }
            }

            // Delete cycle (girl only, on actual period days  deletes the whole cycle entry)
            if (isGirl && onDeleteCycle != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Отменить/удалить начало цикла")
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        confirmButton = {
                            TextButton(onClick = { showDeleteConfirm = false; onDeleteCycle() }) {
                                Text("Удалить", color = Color(0xFFE53935))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
                        },
                        title = { Text("Удалить запись?") },
                        text = { Text("Это удалит весь цикл для этой даты, включая все симптомы и настроение. Продолжить?")
                        }
                    )
                }
            }

            if (isGirl) {
                // Symptom picker
                Text("Симптомы", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                LazyVerticalGrid(columns = GridCells.Fixed(4),
                    modifier = Modifier.height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SYMPTOM_DEFS) { def ->
                        val sel = def.key in selectedSymptoms
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) ColorPeriodActual.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (sel) 1.5.dp else 0.dp,
                                        if (sel) ColorPeriodActual else Color.Transparent,
                                        RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedSymptoms = if (sel) selectedSymptoms.toMutableList().also { it.remove(def.key) }
                                               else selectedSymptoms.toMutableList().also { it.add(def.key) }
                                    edited = true
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(def.emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                            Text(def.label, style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center, maxLines = 2,
                                fontSize = 9.sp, lineHeight = 11.sp)
                        }
                    }
                }

                // Mood picker
                Text("Настроение", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MOOD_DEFS.forEach { def ->
                        val sel = selectedMood == def.key
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (sel) ColorOvulationActual.copy(alpha = 0.15f) else Color.Transparent)
                                .border(if (sel) 1.5.dp else 0.dp,
                                        if (sel) ColorOvulationActual else Color.Transparent, CircleShape)
                                .clickable { selectedMood = if (sel) "" else def.key; edited = true }
                                .padding(6.dp)
                        ) {
                            Text(def.emoji, fontSize = 22.sp)
                            Text(def.label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                        }
                    }
                }

                // Save button
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Закрыть") }
                    Button(onClick = { onSave(selectedSymptoms, selectedMood) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPeriodActual)) {
                        Text("Сохранить")
                    }
                }
            } else {
                // View-only mode for the guy
                if (symptoms.isEmpty() && mood.isBlank()) {
                    Text("Нет записей за этот день",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    if (symptoms.isNotEmpty()) {
                        Text("Симптомы:", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        symptoms.forEach { key ->
                            val def = SYMPTOM_DEFS.find { it.key == key }
                            if (def != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(def.emoji, fontSize = 18.sp)
                                    Text(def.label)
                                }
                            }
                        }
                    }
                    if (mood.isNotBlank()) {
                        val mDef = MOOD_DEFS.find { it.key == mood }
                        if (mDef != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(mDef.emoji, fontSize = 18.sp)
                                Text("Настроение: ${mDef.label}")
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
            }
        }
    }
}

// endregion

// region Stats Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsSheet(
    cycles: List<com.example.loveapp.data.api.models.CycleResponse>,
    avgCycle: Int,
    avgPeriod: Int,
    nextPeriod: String?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("Статистика цикла", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

            // Summary cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSummaryCard(Modifier.weight(1f), "Ср. цикл", "${avgCycle}д", ColorOvulationActual)
                StatSummaryCard(Modifier.weight(1f), "Ср. месячные", "${avgPeriod}д", ColorPeriodActual)
                StatSummaryCard(Modifier.weight(1f), "Кол-во", "${cycles.size}", Color(0xFFAA84F7))
            }

            // Next period
            if (nextPeriod != null) {
                val d = runCatching { LocalDate.parse(nextPeriod, CYCLE_CAL_FMT).format(CYCLE_STATS_DISPLAY_FMT) }.getOrNull() ?: nextPeriod
                Card(colors = CardDefaults.cardColors(containerColor = ColorPeriodPredicted.copy(alpha = 0.3f)),
                     shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(ColorPeriodActual))
                        Text("Следующие месячные: $d",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Cycle length bar chart (last 6 cycles)
            if (cycles.size >= 2) {
                Text("Длина последних циклов", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                val sorted = cycles.sortedBy { it.cycleStartDate }
                val lengths = sorted.zip(sorted.drop(1)).map { (a, b) ->
                    val la = runCatching { LocalDate.parse(a.cycleStartDate, CYCLE_CAL_FMT) }.getOrNull()
                    val lb = runCatching { LocalDate.parse(b.cycleStartDate, CYCLE_CAL_FMT) }.getOrNull()
                    if (la != null && lb != null) java.time.temporal.ChronoUnit.DAYS.between(la, lb).toInt() else null
                }.filterNotNull().takeLast(6)
                val maxLen = lengths.maxOrNull()?.coerceAtLeast(1) ?: 1

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lengths.forEachIndexed { i, len ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Цикл ${i + 1}", style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(54.dp))
                            Box(modifier = Modifier.height(16.dp)
                                .fillMaxWidth(fraction = (len.toFloat() / maxLen).coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorOvulationActual))
                            Text("${len}д", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Symptom frequency
            val allSymptoms = cycles.flatMap { c -> c.symptoms.values.flatten() }
            if (allSymptoms.isNotEmpty()) {
                Text("Частые симптомы", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                val freqMap = allSymptoms.groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }.take(5)
                val maxFreq = freqMap.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    freqMap.forEach { (key, count) ->
                        val def = SYMPTOM_DEFS.find { it.key == key }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(def?.emoji ?: "", fontSize = 14.sp, modifier = Modifier.width(24.dp))
                            Box(modifier = Modifier.height(12.dp)
                                .fillMaxWidth(fraction = (count.toFloat() / maxFreq).coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorPeriodActual))
                            Text("${count}р.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
        }
    }
}

@Composable
private fun StatSummaryCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
         colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(),
               horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

// endregion

// region Cycle Settings Dialog

@Composable
private fun CycleSettingsDialog(
    initialCycleDuration: Int,
    initialPeriodDuration: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var cycleDur  by remember { mutableStateOf(initialCycleDuration.coerceIn(21, 45)) }
    var periodDur by remember { mutableStateOf(initialPeriodDuration.coerceIn(2, 10)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(cycleDur, periodDur) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Настройки цикла") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cycle duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Длина цикла",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$cycleDur дней",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOvulationActual,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value       = cycleDur.toFloat(),
                        onValueChange = { cycleDur = it.toInt() },
                        valueRange  = 21f..45f,
                        steps       = 23,  // 45-21-1 = 23 steps
                        colors      = SliderDefaults.colors(
                            thumbColor       = ColorOvulationActual,
                            activeTrackColor = ColorOvulationActual
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("21 д", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("45 д", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Period duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Длина месячных",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$periodDur дней",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorPeriodActual,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value       = periodDur.toFloat(),
                        onValueChange = { periodDur = it.toInt() },
                        valueRange  = 2f..10f,
                        steps       = 7,   // 10-2-1 = 7 steps
                        colors      = SliderDefaults.colors(
                            thumbColor       = ColorPeriodActual,
                            activeTrackColor = ColorPeriodActual
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2 д", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10 д", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}

// endregion

// endregion