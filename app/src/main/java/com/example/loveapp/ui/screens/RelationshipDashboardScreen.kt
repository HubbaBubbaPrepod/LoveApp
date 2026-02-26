package com.example.loveapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.theme.AccentPurple
import com.example.loveapp.ui.theme.PrimaryPink
import com.example.loveapp.viewmodel.MilestoneEvent
import com.example.loveapp.viewmodel.MilestoneType
import com.example.loveapp.viewmodel.RelationshipViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val MILESTONE_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))
private val HEADER_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

//  Main Screen 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: RelationshipViewModel = hiltViewModel()
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val relationship      by viewModel.relationship.collectAsState()
    val partnerDisplayName by viewModel.partnerDisplayName.collectAsState()
    val daysSinceStart    by viewModel.daysSinceStart.collectAsState()
    val milestones        by viewModel.milestones.collectAsState()
    val selectedTab       by viewModel.selectedTab.collectAsState()
    val calendarYearMonth by viewModel.calendarYearMonth.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val errorMessage      by viewModel.errorMessage.collectAsState()
    val successMessage    by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    // Parse start date once for the calendar tab
    val startDate: LocalDate? = remember(relationship?.relationshipStartDate) {
        try {
            val s = relationship?.relationshipStartDate?.split("T")?.get(0) ?: return@remember null
            LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) { null }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = "Наши отношения",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                relationship == null -> {
                    //  Empty state 
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = PrimaryPink.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Добавьте информацию\nо ваших отношениях",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(28.dp))
                        Button(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Добавить")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        //  Gradient header card 
                        item {
                            RelationshipHeaderCard(
                                daysSinceStart = daysSinceStart,
                                startDate = startDate,
                                partnerDisplayName = partnerDisplayName,
                                onEditClick = { showEditDialog = true }
                            )
                        }

                        //  Tab row 
                        item {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { viewModel.selectTab(0) },
                                    text = { Text("События") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { viewModel.selectTab(1) },
                                    text = { Text("Календарь") }
                                )
                            }
                        }

                        //  Tab 0  Milestones 
                        if (selectedTab == 0) {
                            val upcoming = milestones
                                .filter { it.daysRelative >= 0 }
                                .take(15)
                            val past = milestones
                                .filter { it.daysRelative < 0 }
                                .takeLast(5)
                                .reversed()

                            if (upcoming.isNotEmpty()) {
                                item {
                                    MilestoneSectionHeader(
                                        title = "Предстоящее",
                                        modifier = Modifier.padding(
                                            start = 16.dp, end = 16.dp,
                                            top = 16.dp, bottom = 4.dp
                                        )
                                    )
                                }
                                items(upcoming, key = { "${it.date}_${it.title}" }) { m ->
                                    MilestoneRow(m)
                                }
                            }

                            if (past.isNotEmpty()) {
                                item {
                                    MilestoneSectionHeader(
                                        title = "Прошедшее",
                                        modifier = Modifier.padding(
                                            start = 16.dp, end = 16.dp,
                                            top = 20.dp, bottom = 4.dp
                                        )
                                    )
                                }
                                items(past, key = { "past_${it.date}_${it.title}" }) { m ->
                                    MilestoneRow(m)
                                }
                            }
                        }

                        //  Tab 1  Relationship Calendar 
                        if (selectedTab == 1) {
                            item {
                                RelationshipCalendarView(
                                    yearMonth = calendarYearMonth,
                                    startDate = startDate,
                                    onPrev = { viewModel.prevMonth() },
                                    onNext = { viewModel.nextMonth() },
                                    modifier = Modifier.padding(
                                        start = 12.dp, end = 12.dp, top = 12.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            EditRelationshipDialog(
                relationship = relationship,
                onDismiss = { showEditDialog = false },
                onUpdate = { sDate, kissDate, anniversaryDate, myBirthday, partnerBirthday ->
                    viewModel.updateRelationship(sDate, kissDate, anniversaryDate, myBirthday, partnerBirthday)
                    showEditDialog = false
                }
            )
        }
    }
}

//  Header Card 

@Composable
private fun RelationshipHeaderCard(
    daysSinceStart: Long,
    startDate: LocalDate?,
    partnerDisplayName: String?,
    onEditClick: () -> Unit
) {
    val gradient = remember { Brush.linearGradient(listOf(PrimaryPink, AccentPurple)) }
    val dayWord = RelationshipViewModel.dayForm(daysSinceStart)
    val startLabel = remember(startDate) { startDate?.format(HEADER_DATE_FMT)?.let { "с $it" } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        // Edit button  top right
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Редактировать",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!partnerDisplayName.isNullOrBlank()) {
                Text(
                    text = "❤️ $partnerDisplayName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "Мы вместе",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$daysSinceStart",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 72.sp
                ),
                color = Color.White
            )
            Text(
                text = dayWord,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (startLabel != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

//  Milestone Section Header 

@Composable
private fun MilestoneSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

//  Milestone Row 

@Composable
private fun MilestoneRow(milestone: MilestoneEvent) {
    val accentColor = when (milestone.type) {
        MilestoneType.DAY_COUNT   -> PrimaryPink
        MilestoneType.ANNIVERSARY -> Color(0xFFFFC107)
        MilestoneType.HOLIDAY     -> AccentPurple
        MilestoneType.BIRTHDAY    -> Color(0xFFFF9800)
    }
    val icon = when (milestone.type) {
        MilestoneType.DAY_COUNT   -> Icons.Default.Favorite
        MilestoneType.ANNIVERSARY -> Icons.Default.Star
        MilestoneType.HOLIDAY     -> Icons.Default.DateRange
        MilestoneType.BIRTHDAY    -> Icons.Default.Cake
    }
    val dateStr = remember(milestone.date) { milestone.date.format(MILESTONE_DATE_FMT) }
    val relLabel = remember(milestone.isToday, milestone.daysRelative) {
        when {
            milestone.isToday              -> "Сегодня!"
            milestone.daysRelative == 1L   -> "Завтра"
            milestone.daysRelative == -1L  -> "Вчера"
            milestone.daysRelative > 0     -> "Через ${milestone.daysRelative} дн."
            else                           -> "${-milestone.daysRelative} дн. назад"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    accentColor.copy(alpha = if (milestone.isPast) 0.18f else 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = if (milestone.isPast) 0.45f else 1f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = milestone.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (milestone.isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (milestone.isPast)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "день ${milestone.dayNumber}  $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = relLabel,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                milestone.isToday  -> PrimaryPink
                milestone.isPast   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                else                -> accentColor
            },
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }
}

//  Relationship Calendar View 

private val DOW_HEADERS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
private fun RelationshipCalendarView(
    yearMonth: YearMonth,
    startDate: LocalDate?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = yearMonth.month
        .getDisplayName(JTextStyle.FULL_STANDALONE, Locale("ru"))
        .replaceFirstChar { it.uppercase() }
    val year = yearMonth.year
    val firstDay = yearMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1  // Mon=0  Sun=6
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrev) {
                    Text("<", style = MaterialTheme.typography.titleLarge,
                         color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "$monthName $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onNext) {
                    Text(">", style = MaterialTheme.typography.titleLarge,
                         color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                DOW_HEADERS.forEach { dow ->
                    Text(
                        text = dow,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Calendar grid
            val weeks = totalCells / 7
            for (week in 0 until weeks) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIdx = week * 7 + col
                        val dayNum = cellIdx - startOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum)
                            val relDay: Long? = if (startDate != null && !date.isBefore(startDate)) {
                                ChronoUnit.DAYS.between(startDate, date) + 1
                            } else null
                            RelCalendarDayCell(
                                date = date,
                                relDay = relDay,
                                startDate = startDate,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelCalendarDayCell(
    date: LocalDate,
    relDay: Long?,
    startDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = date == today
    val isStart = startDate != null && date == startDate

    Column(
        modifier = modifier.padding(horizontal = 1.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(
                    when {
                        isToday -> Modifier.background(PrimaryPink, CircleShape)
                        isStart -> Modifier.border(1.5.dp, AccentPurple, CircleShape)
                        else    -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isStart) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        if (relDay != null) {
            Text(
                text = "д.$relDay",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = if (isToday) PrimaryPink else AccentPurple.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        } else {
            Text(text = "", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
        }
    }
}

//  Edit Relationship Dialog 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRelationshipDialog(
    relationship: com.example.loveapp.data.api.models.RelationshipResponse?,
    onDismiss: () -> Unit,
    onUpdate: (String, String?, String?, String?, String?) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var startDate by remember {
        mutableStateOf(relationship?.relationshipStartDate?.split("T")?.get(0) ?: "")
    }
    var kissDate by remember {
        mutableStateOf(relationship?.firstKissDate?.split("T")?.get(0) ?: "")
    }
    var anniversaryDate by remember {
        mutableStateOf(relationship?.anniversaryDate?.split("T")?.get(0) ?: "")
    }
    var myBirthday by remember {
        mutableStateOf(relationship?.myBirthday?.split("T")?.get(0) ?: "")
    }
    var partnerBirthday by remember {
        mutableStateOf(relationship?.partnerBirthday?.split("T")?.get(0) ?: "")
    }

    var showStartDatePicker         by remember { mutableStateOf(false) }
    var showKissDatePicker          by remember { mutableStateOf(false) }
    var showAnniversaryDatePicker   by remember { mutableStateOf(false) }
    var showMyBirthdayPicker        by remember { mutableStateOf(false) }
    var showPartnerBirthdayPicker   by remember { mutableStateOf(false) }

    fun parseToMillis(dateString: String): Long = try {
        LocalDate.parse(dateString, dateFormatter)
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } catch (e: Exception) { System.currentTimeMillis() }

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            .format(dateFormatter)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onUpdate(
                        startDate,
                        kissDate.ifEmpty { null },
                        anniversaryDate.ifEmpty { null },
                        myBirthday.ifEmpty { null },
                        partnerBirthday.ifEmpty { null }
                    )
                },
                enabled = startDate.isNotEmpty()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Информация об отношениях") },
        text = {
            Column {
                // Start date
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(if (startDate.isEmpty()) "Дата начала отношений" else startDate)
                }
                if (showStartDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = parseToMillis(startDate),
                        onDateSelected = { millis -> startDate = formatDate(millis); showStartDatePicker = false },
                        onDismiss = { showStartDatePicker = false }
                    )
                }

                // First kiss date
                Button(
                    onClick = { showKissDatePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(if (kissDate.isEmpty()) "Первый поцелуй (необязательно)" else kissDate)
                }
                if (showKissDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (kissDate.isEmpty()) System.currentTimeMillis() else parseToMillis(kissDate),
                        onDateSelected = { millis -> kissDate = formatDate(millis); showKissDatePicker = false },
                        onDismiss = { showKissDatePicker = false }
                    )
                }

                // Anniversary date
                Button(
                    onClick = { showAnniversaryDatePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(if (anniversaryDate.isEmpty()) "Годовщина (необязательно)" else anniversaryDate)
                }
                if (showAnniversaryDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (anniversaryDate.isEmpty()) System.currentTimeMillis() else parseToMillis(anniversaryDate),
                        onDateSelected = { millis -> anniversaryDate = formatDate(millis); showAnniversaryDatePicker = false },
                        onDismiss = { showAnniversaryDatePicker = false }
                    )
                }

                // My birthday
                Button(
                    onClick = { showMyBirthdayPicker = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Cake, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text(if (myBirthday.isEmpty()) "🎂 Мой день рождения (необязательно)" else "🎂 Я: $myBirthday")
                }
                if (showMyBirthdayPicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (myBirthday.isEmpty()) System.currentTimeMillis() else parseToMillis(myBirthday),
                        onDateSelected = { millis -> myBirthday = formatDate(millis); showMyBirthdayPicker = false },
                        onDismiss = { showMyBirthdayPicker = false }
                    )
                }

                // Partner birthday
                Button(
                    onClick = { showPartnerBirthdayPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cake, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text(if (partnerBirthday.isEmpty()) "🎂 День рождения партнёра (необязательно)" else "🎂 Партнёр: $partnerBirthday")
                }
                if (showPartnerBirthdayPicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (partnerBirthday.isEmpty()) System.currentTimeMillis() else parseToMillis(partnerBirthday),
                        onDateSelected = { millis -> partnerBirthday = formatDate(millis); showPartnerBirthdayPicker = false },
                        onDismiss = { showPartnerBirthdayPicker = false }
                    )
                }
            }
        }
    )
}

//  DatePicker Dialog Wrapper 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogWrapper(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
