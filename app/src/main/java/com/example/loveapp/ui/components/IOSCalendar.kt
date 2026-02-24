package com.example.loveapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * iOS-style calendar widget
 * Suitable for menstrual cycle tracking and memorable dates
 */
@Composable
fun IOSCalendar(
    selectedDates: List<LocalDate> = emptyList(),
    highlightedDates: Map<LocalDate, Color> = emptyMap(),
    onDateSelected: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now()
) {
    var currentMonth by remember { mutableStateOf(initialMonth) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous month button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { currentMonth = currentMonth.minusMonths(1) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Month and year title
            Text(
                text = currentMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )

            // Next month button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { currentMonth = currentMonth.plusMonths(1) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Day names header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar grid
        val firstDay = currentMonth.atDay(1)
        val lastDay = currentMonth.atEndOfMonth()
        val daysInMonth = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1
        val firstDayOfWeek = firstDay.dayOfWeek.value % 7 // 0 = Sunday
        
        val totalCells = firstDayOfWeek + daysInMonth
        val days = (1..totalCells).map { cell ->
            if (cell <= firstDayOfWeek || cell > firstDayOfWeek + daysInMonth) {
                null
            } else {
                firstDay.plusDays((cell - firstDayOfWeek - 1).toLong())
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days.size) { index ->
                val day = days[index]
                if (day == null) {
                    Box(modifier = Modifier.size(40.dp))
                } else {
                    CalendarDayCell(
                        date = day,
                        isSelected = day in selectedDates,
                        highlightColor = highlightedDates[day],
                        onClick = { onDateSelected(day) }
                    )
                }
            }
        }
    }
}

/**
 * Individual calendar day cell with optional highlighting
 */
@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean = false,
    highlightColor: Color? = null,
    onClick: () -> Unit = {}
) {
    val bgColor = when {
        highlightColor != null -> highlightColor.copy(alpha = 0.8f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected || highlightColor != null -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isSelected || highlightColor != null) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Compact iOS-style calendar for quick month view
 */
@Composable
fun CompactIOSCalendar(
    month: YearMonth = YearMonth.now(),
    selectedDates: List<LocalDate> = emptyList(),
    onDateSelected: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Month title
        Text(
            text = month.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Simplified day names
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp
                )
            }
        }

        // Mini calendar grid
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        val daysInMonth = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1
        val firstDayOfWeek = firstDay.dayOfWeek.value % 7

        val days = (1..(firstDayOfWeek + daysInMonth)).map { cell ->
            if (cell <= firstDayOfWeek || cell > firstDayOfWeek + daysInMonth) {
                null
            } else {
                firstDay.plusDays((cell - firstDayOfWeek - 1).toLong())
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(days.size) { index ->
                val day = days[index]
                if (day == null) {
                    Box(modifier = Modifier.size(28.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (day in selectedDates)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            )
                            .clickable { onDateSelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day in selectedDates)
                                Color.White
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
