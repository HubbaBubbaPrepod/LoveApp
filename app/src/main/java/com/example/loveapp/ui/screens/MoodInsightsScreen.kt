package com.example.loveapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.MoodAnalyticsResponse
import com.example.loveapp.data.api.models.MoodDailyTrendItem
import com.example.loveapp.data.api.models.MoodDistributionItem
import com.example.loveapp.viewmodel.MoodInsightsViewModel

private val DAY_NAMES = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodInsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoodInsightsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text("Аналитика настроения") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            analytics != null -> {
                AnalyticsContent(analytics!!, Modifier.padding(padding))
            }
            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Нет данных для анализа", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(data: MoodAnalyticsResponse, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Summary cards row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Серия",
                value = "${data.moodStreak}",
                subtitle = "дней подряд",
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFF6D00),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Этот месяц",
                value = "${data.thisMonthCount}",
                subtitle = "записей",
                icon = Icons.Default.CalendarMonth,
                color = Color(0xFF5E5CE6),
                modifier = Modifier.weight(1f)
            )
            val changeColor = when {
                (data.monthChangePercent ?: 0) > 0 -> Color(0xFF30D158)
                (data.monthChangePercent ?: 0) < 0 -> Color(0xFFFF375F)
                else -> Color(0xFF8E8E93)
            }
            val changeIcon = if ((data.monthChangePercent ?: 0) >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
            SummaryCard(
                title = "Изменение",
                value = "${data.monthChangePercent ?: 0}%",
                subtitle = "vs прошлый мес.",
                icon = changeIcon,
                color = changeColor,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Most common mood ──
        if (data.mostCommonMood != null) {
            val topColor = moodColor(data.mostCommonMood)
            val topLabel = moodLabel(data.mostCommonMood)
            Card(
                colors = CardDefaults.cardColors(containerColor = topColor.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(moodIcon(data.mostCommonMood), contentDescription = null, tint = topColor, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Преобладающее настроение", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(topLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = topColor)
                    }
                }
            }
        }

        // ── Daily trend chart (last 14 days) ──
        if (data.dailyTrend.isNotEmpty()) {
            SectionTitle("Тренд за 14 дней")
            DailyTrendChart(data.dailyTrend)
        }

        // ── Mood distribution bar ──
        if (data.distribution.isNotEmpty()) {
            SectionTitle("Распределение (30 дней)")
            MoodDistributionBars(data.distribution)
        }

        // ── Partner comparison ──
        if (data.partnerDistribution.isNotEmpty()) {
            SectionTitle("Сравнение с партнёром")
            PartnerComparisonBars(data.distribution, data.partnerDistribution)
        }

        // ── Day of week pattern ──
        if (data.dayOfWeek.isNotEmpty()) {
            SectionTitle("Настроение по дням недели")
            DayOfWeekPattern(data.dayOfWeek)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun DailyTrendChart(trend: List<MoodDailyTrendItem>) {
    val maxCount = trend.maxOfOrNull { it.count } ?: 1
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Bar chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barWidth = size.width / trend.size * 0.6f
                val gap = size.width / trend.size * 0.4f
                trend.forEachIndexed { i, item ->
                    val barHeight = if (maxCount > 0) (item.count.toFloat() / maxCount) * size.height * 0.85f else 0f
                    val color = item.dominantMood?.let { moodColorForChart(it) } ?: Color(0xFF8E8E93)
                    val x = i * (barWidth + gap) + gap / 2
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 4, barWidth / 4)
                    )
                }
                // Trend line
                if (trend.size > 1) {
                    val path = Path()
                    trend.forEachIndexed { i, item ->
                        val x = i * (barWidth + gap) + gap / 2 + barWidth / 2
                        val y = size.height - (item.count.toFloat() / maxCount) * size.height * 0.85f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Date labels (show first, middle, last)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val labels = listOf(trend.first(), trend[trend.size / 2], trend.last())
                labels.forEach {
                    Text(
                        it.date.takeLast(5), // "MM-DD"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodDistributionBars(distribution: List<MoodDistributionItem>) {
    if (distribution.isEmpty()) return
    val total = distribution.sumOf { it.count }.coerceAtLeast(1)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            distribution.forEach { item ->
                val fraction = item.count.toFloat() / total
                val color = moodColor(item.moodType)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(moodIcon(item.moodType), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(moodLabel(item.moodType), modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnerComparisonBars(mine: List<MoodDistributionItem>, partner: List<MoodDistributionItem>) {
    if (mine.isEmpty() && partner.isEmpty()) return
    val allTypes = (mine.map { it.moodType } + partner.map { it.moodType }).distinct()
    if (allTypes.isEmpty()) return
    val myTotal = mine.sumOf { it.count }.coerceAtLeast(1)
    val ptTotal = partner.sumOf { it.count }.coerceAtLeast(1)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF5E5CE6)))
                Spacer(Modifier.width(4.dp))
                Text("Я", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF6B9D)))
                Spacer(Modifier.width(4.dp))
                Text("Партнёр", style = MaterialTheme.typography.labelSmall)
            }
            allTypes.forEach { type ->
                val myPct = (mine.find { it.moodType == type }?.count ?: 0).toFloat() / myTotal
                val ptPct = (partner.find { it.moodType == type }?.count ?: 0).toFloat() / ptTotal
                val color = moodColor(type)
                Column {
                    Text(moodLabel(type), style = MaterialTheme.typography.labelSmall, color = color)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF5E5CE6).copy(alpha = 0.15f))
                        ) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(myPct).clip(RoundedCornerShape(4.dp)).background(Color(0xFF5E5CE6)))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("${(myPct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF6B9D).copy(alpha = 0.15f))
                        ) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(ptPct).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF6B9D)))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("${(ptPct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekPattern(items: List<com.example.loveapp.data.api.models.MoodDayOfWeekItem>) {
    val maxCount = items.maxOfOrNull { it.count } ?: 1
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.sortedBy { it.dow }.forEach { item ->
                val dayName = DAY_NAMES.getOrElse(item.dow) { "?" }
                val color = item.dominantMood?.let { moodColor(it) } ?: Color(0xFF8E8E93)
                val barFraction = item.count.toFloat() / maxCount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(60.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(barFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(color)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(dayName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text("${item.count}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/** Maps mood type string to color for use inside Canvas (no Composable context). */
private fun moodColorForChart(type: String): Color = when (type.lowercase()) {
    "rad", "very good", "excellent" -> Color(0xFFFF375F)
    "good"                          -> Color(0xFF30D158)
    "neutral", "meh"                -> Color(0xFFFFD60A)
    "bad"                           -> Color(0xFFFF9F0A)
    "awful", "very bad"             -> Color(0xFF5E5CE6)
    else                            -> Color(0xFF8E8E93)
}
