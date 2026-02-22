package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.loveapp.ui.components.IOSTopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.loveapp.R
import com.example.loveapp.viewmodel.CycleViewModel
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenstrualCalendarScreen(
    navController: NavHostController,
    viewModel: CycleViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    val cycles by viewModel.cycles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && cycles.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.your_cycle),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (cycles.isEmpty()) {
                            Text(
                                stringResource(R.string.no_cycle_entries),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            val currentCycle = cycles.firstOrNull()
                            currentCycle?.let {
                                Text(
                                    text = stringResource(R.string.current_cycle_day),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Calendar grid
                                Text(
                                    text = getMonthYearString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Day headers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    val dayHeaders = listOf(
                                        stringResource(R.string.day_sun), stringResource(R.string.day_mon),
                                        stringResource(R.string.day_tue), stringResource(R.string.day_wed),
                                        stringResource(R.string.day_thu), stringResource(R.string.day_fri),
                                        stringResource(R.string.day_sat)
                                    )
                                    dayHeaders.forEach { day ->
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Calendar grid
                                val daysInMonth = getDaysInCurrentMonth()
                                val firstDayOfWeek = getFirstDayOfMonth()
                                
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(firstDayOfWeek + daysInMonth) { index ->
                                        if (index < firstDayOfWeek) {
                                            Box(modifier = Modifier.size(40.dp))
                                        } else {
                                            val dayNum = index - firstDayOfWeek + 1
                                            val cycleLengthDays = 28
                                            val periodLengthDays = 5
                                            val cycleDay = dayNum % cycleLengthDays
                                            
                                            val cycleColor = when {
                                                cycleDay <= periodLengthDays -> Color(0xFFFF6B9D)
                                                cycleDay in (periodLengthDays + 1)..(periodLengthDays + 8) -> Color(0xFFFFB6C1)
                                                cycleDay == periodLengthDays + 9 -> Color(0xFFFF1493)
                                                else -> Color(0xFFFFE4E1)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .size(40.dp)
                                                    .background(cycleColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                // Legend
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.legend),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                LegendItem(stringResource(R.string.menstruation), Color(0xFFFF6B9D))
                                LegendItem(stringResource(R.string.fertile_window), Color(0xFFFFB6C1))
                                LegendItem(stringResource(R.string.ovulation), Color(0xFFFF1493))
                                LegendItem(stringResource(R.string.luteal_phase), Color(0xFFFFE4E1))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

fun getMonthYearString(): String {
    val calendar = Calendar.getInstance(Locale("ru", "RU"))
    val format = SimpleDateFormat("LLLL yyyy", Locale("ru", "RU"))
    return format.format(calendar.time)
}

fun getDaysInCurrentMonth(): Int {
    val calendar = Calendar.getInstance()
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun getFirstDayOfMonth(): Int {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.get(Calendar.DAY_OF_WEEK) - 1
}
