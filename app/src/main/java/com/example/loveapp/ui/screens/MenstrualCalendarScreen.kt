package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.loveapp.R
import com.example.loveapp.ui.components.IOSCalendar
import com.example.loveapp.ui.components.IOSCard
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.theme.iOSCycleFertile
import com.example.loveapp.ui.theme.iOSCycleLuteal
import com.example.loveapp.ui.theme.iOSCycleMenstruation
import com.example.loveapp.ui.theme.iOSCycleOvulation
import com.example.loveapp.viewmodel.CycleViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenstrualCalendarScreen(
    onNavigateBack: () -> Unit,
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
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = stringResource(R.string.cycle),
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                isLoading && cycles.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.your_cycle),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (cycles.isEmpty()) {
                            item {
                                IOSCard(
                                    modifier = Modifier.fillParentMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).padding(bottom = 12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            stringResource(R.string.no_cycle_entries),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                val highlightedDates = mutableMapOf<LocalDate, androidx.compose.ui.graphics.Color>()
                                // Group cycle days by phase and color them
                                cycles.forEach { cycle ->
                                    // Assuming cycle has date and phase information
                                    // This would need adjustment based on actual data structure
                                    highlightedDates[LocalDate.now()] = iOSCycleMenstruation
                                }

                                IOSCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IOSCalendar(
                                        highlightedDates = highlightedDates,
                                        initialMonth = YearMonth.now()
                                    )
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.padding(vertical = 24.dp)
                            ) {
                                Text(
                                    "Cycle Phases",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                CyclePhaseIndicator(
                                    phaseName = "Menstruation",
                                    color = iOSCycleMenstruation,
                                    days = "Days 1-5"
                                )
                                CyclePhaseIndicator(
                                    phaseName = "Follicular",
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                    days = "Days 6-13"
                                )
                                CyclePhaseIndicator(
                                    phaseName = "Ovulation",
                                    color = iOSCycleOvulation,
                                    days = "Days 14-16"
                                )
                                CyclePhaseIndicator(
                                    phaseName = "Luteal",
                                    color = iOSCycleLuteal,
                                    days = "Days 17-28"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
 
@Composable
private fun CyclePhaseIndicator(
    phaseName: String,
    color: androidx.compose.ui.graphics.Color,
    days: String
) {
    IOSCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(
                            color = color.copy(alpha = 0.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )

                Column {
                    Text(
                        text = phaseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = days,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
