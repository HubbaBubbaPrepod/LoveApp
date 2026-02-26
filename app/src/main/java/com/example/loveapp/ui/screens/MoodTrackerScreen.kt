package com.example.loveapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.MoodViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Mood definitions — icons from Material Icons Extended (consistent style)
// ─────────────────────────────────────────────────────────────────────────────
internal data class MoodDef(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

internal val MOODS = listOf(
    MoodDef("rad",   "Отлично",    Icons.Default.SentimentVerySatisfied,    Color(0xFFFF375F)),
    MoodDef("good",  "Хорошо",     Icons.Default.SentimentSatisfied,        Color(0xFF30D158)),
    MoodDef("meh",   "Нейтрально", Icons.Default.SentimentNeutral,          Color(0xFFFFD60A)),
    MoodDef("bad",   "Плохо",      Icons.Default.SentimentDissatisfied,     Color(0xFFFF9F0A)),
    MoodDef("awful", "Ужасно",     Icons.Default.SentimentVeryDissatisfied, Color(0xFF5E5CE6)),
)

fun moodColor(type: String): Color =
    MOODS.find { it.key.equals(type, true) }?.color ?: when (type.lowercase()) {
        "very good", "excellent" -> Color(0xFFFF375F)
        "good"                   -> Color(0xFF30D158)
        "neutral", "meh"         -> Color(0xFFFFD60A)
        "bad"                    -> Color(0xFFFF9F0A)
        "very bad", "awful"      -> Color(0xFF5E5CE6)
        else                     -> Color(0xFF8E8E93)
    }

fun moodIcon(type: String): ImageVector =
    MOODS.find { it.key.equals(type, true) }?.icon ?: when (type.lowercase()) {
        "very good", "excellent" -> Icons.Default.SentimentVerySatisfied
        "good"                   -> Icons.Default.SentimentSatisfied
        "neutral", "meh"         -> Icons.Default.SentimentNeutral
        "bad"                    -> Icons.Default.SentimentDissatisfied
        "very bad", "awful"      -> Icons.Default.SentimentVeryDissatisfied
        else                     -> Icons.Default.SentimentNeutral
    }

fun moodLabel(type: String): String =
    MOODS.find { it.key.equals(type, true) }?.label ?: when (type.lowercase()) {
        "very good", "excellent" -> "Отлично"
        "good"                   -> "Хорошо"
        "neutral", "meh"         -> "Нейтрально"
        "bad"                    -> "Плохо"
        "very bad", "awful"      -> "Ужасно"
        else                     -> type
    }

private val MOOD_CAL_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun blendMoodColors(moods: List<MoodResponse>): Color {
    if (moods.isEmpty()) return Color.Transparent
    val r = moods.map { moodColor(it.moodType).red }.average().toFloat()
    val g = moods.map { moodColor(it.moodType).green }.average().toFloat()
    val b = moods.map { moodColor(it.moodType).blue }.average().toFloat()
    return Color(r, g, b)
}

private fun pluralMoods(n: Int) = when {
    n % 10 == 1 && n % 100 != 11         -> "запись"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "записи"
    else                                   -> "записей"
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val myToday          by viewModel.myTodayMoods.collectAsState()
    val partnerToday     by viewModel.partnerTodayMoods.collectAsState()
    val partnerName      by viewModel.partnerName.collectAsState()
    val myName           by viewModel.myName.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val errorMessage     by viewModel.errorMessage.collectAsState()
    val successMessage   by viewModel.successMessage.collectAsState()

    var showPicker         by remember { mutableStateOf(false) }
    var showPartnerHistory by remember { mutableStateOf(false) }
    var showCalendar       by remember { mutableStateOf(false) }
    var showStats          by remember { mutableStateOf(false) }
    var pickerNote         by remember { mutableStateOf("") }
    var selectedMoodKey    by remember { mutableStateOf<String?>(null) }

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
                title = "Настроение",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showCalendar = true }) {
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
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MoodFlask(
                            moods = myToday, label = myName ?: "Я",
                            isMyFlask = true, onClick = { showPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        MoodFlask(
                            moods = partnerToday, label = partnerName ?: "Партнёр",
                            isMyFlask = false, onClick = { showPartnerHistory = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (myToday.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SentimentNeutral,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text("Как ты сегодня?",
                                    style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Нажми на свою колбочку, чтобы добавить запись",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "Сегодня",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(myToday) { mood ->
                        MoodEntryRow(mood = mood, onDelete = { viewModel.deleteMood(mood.id) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false; selectedMoodKey = null; pickerNote = "" },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            MoodPickerContent(
                selectedKey    = selectedMoodKey,
                note           = pickerNote,
                isLoading      = isLoading,
                onMoodSelected = { selectedMoodKey = it },
                onNoteChanged  = { pickerNote = it },
                onSave = {
                    selectedMoodKey?.let { key ->
                        viewModel.addMood(key, pickerNote.trim())
                        showPicker = false; selectedMoodKey = null; pickerNote = ""
                    }
                },
                onCancel = { showPicker = false; selectedMoodKey = null; pickerNote = "" }
            )
        }
    }

    if (showPartnerHistory) {
        ModalBottomSheet(
            onDismissRequest = { showPartnerHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PartnerHistoryContent(
                partnerName = partnerName ?: "Партнёр",
                moods       = partnerToday
            )
        }
    }

    if (showCalendar) {
        val calYear           by viewModel.calendarYear.collectAsState()
        val calMonth          by viewModel.calendarMonth.collectAsState()
        val myMonthMoods      by viewModel.myMonthMoods.collectAsState()
        val partnerMonthMoods by viewModel.partnerMonthMoods.collectAsState()
        val isCalLoading      by viewModel.isCalendarLoading.collectAsState()
        LaunchedEffect(Unit) { viewModel.loadCalendarMonth(calYear, calMonth) }
        ModalBottomSheet(
            onDismissRequest = { showCalendar = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CalendarContent(
                year          = calYear,
                month         = calMonth,
                myMoods       = myMonthMoods,
                partnerMoods  = partnerMonthMoods,
                myName        = myName ?: "Я",
                partnerName   = partnerName ?: "Партнёр",
                isLoading     = isCalLoading,
                onMonthChange = { y, m -> viewModel.loadCalendarMonth(y, m) }
            )
        }
    }

    if (showStats) {
        val calYear           by viewModel.calendarYear.collectAsState()
        val calMonth          by viewModel.calendarMonth.collectAsState()
        val myMonthMoods      by viewModel.myMonthMoods.collectAsState()
        val partnerMonthMoods by viewModel.partnerMonthMoods.collectAsState()
        val isCalLoading      by viewModel.isCalendarLoading.collectAsState()
        LaunchedEffect(Unit) { viewModel.loadCalendarMonth(calYear, calMonth) }
        ModalBottomSheet(
            onDismissRequest = { showStats = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            StatsContent(
                myMoods      = myMonthMoods,
                partnerMoods = partnerMonthMoods,
                myName       = myName ?: "Я",
                partnerName  = partnerName ?: "Партнёр",
                isLoading    = isCalLoading
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Flask composable — animated liquid-fill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoodFlask(
    moods: List<MoodResponse>,
    label: String,
    isMyFlask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background  = MaterialTheme.colorScheme.background
    val isDark      = remember(background) { background.luminance() < 0.5f }
    val cardBg      = remember(isDark) { if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7) }
    val labelColor  = remember(isDark) { if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E) }
    val emptyClr    = remember(isDark) { if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6) }
    val outlineClr  = remember(isDark) { if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC) }
    val subColor    = remember(isDark) { if (isDark) Color(0xFF8E8E93) else Color(0xFF636366) }

    val targetFill = if (moods.isEmpty()) 0f
                     else (0.22f + moods.size * 0.18f).coerceAtMost(0.88f)
    val fillFrac by animateFloatAsState(
        targetValue   = targetFill,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f),
        label = "fill"
    )
    val blendedColor  = remember(moods) { blendMoodColors(moods) }
    val liquidColor by animateColorAsState(
        targetValue   = if (moods.isEmpty()) Color.Transparent else blendedColor,
        animationSpec = tween(700), label = "liq"
    )
    val dominant = remember(moods) { moods.groupBy { it.moodType }.maxByOrNull { it.value.size }?.key }

    // Cache the flask Path — only rebuilt when the Canvas size changes, not every animation frame
    val flaskPath = remember { Path() }
    val flaskGeom = remember { FloatArray(5) } // [w, h, shldrBotY, bodyEndY, strokePx]

    Surface(
        modifier        = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        color           = cardBg,
        shape           = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = labelColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.padding(bottom = 10.dp)
            )

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val w = size.width; val h = size.height

                // Rebuild geometry and path ONLY when size changes — not every animation frame
                if (flaskGeom[0] != w || flaskGeom[1] != h) {
                    val neckW     = w * 0.38f
                    val neckLeft  = (w - neckW) / 2f
                    val neckRight = (w + neckW) / 2f
                    val neckBotY  = h * 0.28f
                    val shldrBotY = h * 0.42f
                    val bodyEndY  = h * 0.80f
                    flaskPath.reset()
                    flaskPath.moveTo(neckLeft, neckBotY * 0.18f)
                    flaskPath.arcTo(
                        rect = Rect(neckLeft, 0f, neckRight, neckBotY * 0.36f),
                        startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false
                    )
                    flaskPath.lineTo(neckRight, neckBotY)
                    flaskPath.cubicTo(neckRight, shldrBotY, w, shldrBotY, w, shldrBotY + h * 0.04f)
                    flaskPath.lineTo(w, bodyEndY)
                    flaskPath.arcTo(
                        rect = Rect(0f, 2f * bodyEndY - h, w, h),
                        startAngleDegrees = 0f, sweepAngleDegrees = 180f, forceMoveTo = false
                    )
                    flaskPath.lineTo(0f, shldrBotY + h * 0.04f)
                    flaskPath.cubicTo(0f, shldrBotY, neckLeft, shldrBotY, neckLeft, neckBotY)
                    flaskPath.close()
                    flaskGeom[0] = w; flaskGeom[1] = h
                    flaskGeom[2] = shldrBotY; flaskGeom[3] = bodyEndY
                    flaskGeom[4] = 1.8.dp.toPx()
                }
                val shldrBotY = flaskGeom[2]

                drawPath(flaskPath, emptyClr)

                if (fillFrac > 0f && liquidColor != Color.Transparent) {
                    val liquidTopY = h - fillFrac * (h - shldrBotY)
                    clipPath(flaskPath) {
                        drawRect(
                            color   = liquidColor.copy(alpha = 0.80f),
                            topLeft = Offset(0f, liquidTopY),
                            size    = Size(w, h - liquidTopY)
                        )
                        drawRect(
                            color   = Color.White.copy(alpha = 0.13f),
                            topLeft = Offset(0f, liquidTopY),
                            size    = Size(w * 0.28f, h - liquidTopY)
                        )
                        drawOval(
                            color   = Color.White.copy(alpha = 0.20f),
                            topLeft = Offset(w * 0.12f, liquidTopY + 4.dp.toPx()),
                            size    = Size(w * 0.20f, 6.dp.toPx())
                        )
                    }
                }

                drawPath(flaskPath, outlineClr,
                    style = Stroke(flaskGeom[4], cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            Spacer(Modifier.height(8.dp))
            if (dominant != null) {
                Icon(
                    imageVector = moodIcon(dominant),
                    contentDescription = moodLabel(dominant),
                    modifier = Modifier.size(28.dp),
                    tint = moodColor(dominant)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SentimentNeutral,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = subColor
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text      = if (moods.isEmpty()) "Нет записей"
                            else "${moods.size} ${pluralMoods(moods.size)}",
                style     = MaterialTheme.typography.labelSmall,
                color     = subColor,
                textAlign = TextAlign.Center
            )
            if (isMyFlask) {
                Spacer(Modifier.height(6.dp))
                Text("+ Добавить",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Today entry row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoodEntryRow(mood: MoodResponse, onDelete: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val isDark = remember(background) { background.luminance() < 0.5f }
    val cardBg = remember(isDark) { if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7) }
    val accent = moodColor(mood.moodType)
    Surface(shape = RoundedCornerShape(14.dp), color = cardBg,
            shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = moodIcon(mood.moodType),
                    contentDescription = moodLabel(mood.moodType),
                    modifier = Modifier.size(26.dp),
                    tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(moodLabel(mood.moodType), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (mood.note.isNotBlank()) {
                    Text(mood.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp))
                }
                Text(mood.timestamp.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mood picker (inside bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MoodPickerContent(
    selectedKey: String?,
    note: String,
    isLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text("Как ты себя чувствуешь?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MOODS.forEach { mood ->
                val isSelected = selectedKey == mood.key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMoodSelected(mood.key) }
                        .background(if (isSelected) mood.color.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .then(if (isSelected) Modifier.shadow(4.dp, CircleShape) else Modifier)
                            .background(
                                if (isSelected) mood.color else mood.color.copy(alpha = 0.14f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = mood.label,
                            modifier = Modifier.size(26.dp),
                            tint = if (isSelected) Color.White else mood.color
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        mood.label,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) mood.color
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign  = TextAlign.Center, maxLines = 1,
                        fontSize   = 9.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = note, onValueChange = onNoteChanged,
            label = { Text("Комментарий (необязательно)") },
            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
            shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)) { Text("Отмена") }
            Button(onClick = onSave, enabled = selectedKey != null && !isLoading,
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                if (isLoading)
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = Color.White)
                else Text("Сохранить")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Partner history (inside bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PartnerHistoryContent(partnerName: String, moods: List<MoodResponse>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text("Настроение: $partnerName",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp))
        if (moods.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Сегодня нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                items(moods) { mood ->
                    Surface(shape = RoundedCornerShape(14.dp),
                        color = moodColor(mood.moodType).copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = moodIcon(mood.moodType),
                                contentDescription = moodLabel(mood.moodType),
                                modifier = Modifier.size(28.dp).padding(end = 0.dp),
                                tint = moodColor(mood.moodType)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(moodLabel(mood.moodType),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                if (mood.note.isNotBlank()) {
                                    Text(mood.note, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar (inside bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarContent(
    year: Int, month: Int,
    myMoods: Map<String, List<MoodResponse>>,
    partnerMoods: Map<String, List<MoodResponse>>,
    myName: String,
    partnerName: String,
    isLoading: Boolean,
    onMonthChange: (Int, Int) -> Unit
) {
    val fmt        = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val monthFmt   = remember { SimpleDateFormat("LLLL yyyy", Locale("ru")) }
    val cal        = remember(year, month) { Calendar.getInstance().also { it.set(year, month, 1) } }
    val monthLabel = remember(year, month) {
        monthFmt.format(cal.time).replaceFirstChar { it.titlecase() }
    }
    val daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    var selectedDay by remember(year, month) { mutableStateOf<String?>(null) }
    val today = remember { fmt.format(System.currentTimeMillis()) }

    val cellDateStrings = remember(year, month) {
        val c = Calendar.getInstance()
        val result = HashMap<Int, String>(daysInMonth)
        for (d in 1..daysInMonth) { c.set(year, month, d); result[d] = fmt.format(c.time) }
        result
    }
    val myBlended = remember(year, month, myMoods) {
        cellDateStrings.values.associateWith { ds ->
            myMoods[ds]?.takeIf { it.isNotEmpty() }?.let { blendMoodColors(it) }
        }
    }
    val partBlended = remember(year, month, partnerMoods) {
        cellDateStrings.values.associateWith { ds ->
            partnerMoods[ds]?.takeIf { it.isNotEmpty() }?.let { blendMoodColors(it) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = {
                    val c = Calendar.getInstance().also { it.set(year, month - 1, 1) }
                    onMonthChange(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
                }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                Text(monthLabel, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                IconButton(onClick = {
                    val c = Calendar.getInstance().also { it.set(year, month + 1, 1) }
                    onMonthChange(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
                }) { Icon(Icons.Default.KeyboardArrowRight, null) }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    MOOD_CAL_DOW.forEach { d ->
                        Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val rowCount = (firstDayOfWeek + daysInMonth + 6) / 7
            items(rowCount) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val dayNum = row * 7 + col - firstDayOfWeek + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).height(44.dp))
                        } else {
                            val dayStr = cellDateStrings[dayNum] ?: ""
                            val myDay  = myMoods[dayStr] ?: emptyList()
                            val pDay   = partnerMoods[dayStr] ?: emptyList()
                            val isSel  = selectedDay == dayStr
                            val isTod  = dayStr == today
                            Box(modifier = Modifier
                                .weight(1f).height(44.dp).padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent)
                                .then(if (isTod) Modifier.border(1.5.dp,
                                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                    else Modifier)
                                .clickable {
                                    selectedDay = if (selectedDay == dayStr) null else dayStr
                                },
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$dayNum", style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isTod) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTod) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        myBlended[dayStr]?.let { c ->
                                            Box(Modifier.size(6.dp).background(c, CircleShape))
                                        }
                                        partBlended[dayStr]?.let { c ->
                                            Box(Modifier.size(6.dp).background(c, CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                selectedDay?.let { day ->
                    val myDay = myMoods[day] ?: emptyList()
                    val pDay  = partnerMoods[day] ?: emptyList()
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    Text(day, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp))
                    if (myDay.isEmpty() && pDay.isEmpty())
                        Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (myDay.isNotEmpty()) {
                        Text("$myName:", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        myDay.forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            ) {
                                Icon(moodIcon(m.moodType), contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = moodColor(m.moodType))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    moodLabel(m.moodType) + if (m.note.isNotBlank()) " — ${m.note}" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (pDay.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("$partnerName:", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        pDay.forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            ) {
                                Icon(moodIcon(m.moodType), contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = moodColor(m.moodType))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    moodLabel(m.moodType) + if (m.note.isNotBlank()) " — ${m.note}" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats (inside bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsContent(
    myMoods: Map<String, List<MoodResponse>>,
    partnerMoods: Map<String, List<MoodResponse>>,
    myName: String,
    partnerName: String,
    isLoading: Boolean
) {
    val myAll      = remember(myMoods)      { myMoods.values.flatten() }
    val partnerAll = remember(partnerMoods) { partnerMoods.values.flatten() }
    val myCounts   = remember(myMoods)      { myAll.groupingBy { it.moodType.lowercase() }.eachCount() }
    val ptCounts   = remember(partnerMoods) { partnerAll.groupingBy { it.moodType.lowercase() }.eachCount() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Text("Статистика за месяц", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val myTotal      = myAll.size.coerceAtLeast(1)
            val partnerTotal = partnerAll.size.coerceAtLeast(1)
            MOODS.forEach { mood ->
                val myCount = myCounts[mood.key] ?: 0
                val pCount  = ptCounts[mood.key] ?: 0
                if (myCount == 0 && pCount == 0) return@forEach
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = mood.icon,
                        contentDescription = mood.label,
                        modifier = Modifier.size(24.dp).padding(end = 0.dp),
                        tint = mood.color
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mood.label, style = MaterialTheme.typography.labelMedium)
                        if (myCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp)) {
                                Text(myName.take(4).padEnd(4), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(36.dp))
                                Box(Modifier
                                    .height(8.dp)
                                    .fillMaxWidth((myCount.toFloat() / myTotal * 0.85f + 0.10f).coerceAtMost(1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(mood.color))
                                Spacer(Modifier.width(4.dp))
                                Text("$myCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (pCount > 0 && partnerAll.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp)) {
                                Text("${partnerName.take(2)}  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(30.dp))
                                Box(Modifier
                                    .height(8.dp)
                                    .fillMaxWidth((pCount.toFloat() / partnerTotal * 0.85f + 0.10f).coerceAtMost(1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(mood.color.copy(alpha = 0.50f)))
                                Spacer(Modifier.width(4.dp))
                                Text("$pCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 2.dp))
            }
            if (myAll.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Нет данных за этот месяц",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Compatibility shim (color only — emoji shim removed, use moodIcon() instead)
fun getMoodColor(moodType: String): Color = moodColor(moodType)
