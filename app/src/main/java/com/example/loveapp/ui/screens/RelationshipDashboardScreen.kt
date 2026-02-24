package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.loveapp.R
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.theme.AccentPurple
import com.example.loveapp.ui.theme.PrimaryPink
import com.example.loveapp.viewmodel.RelationshipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: RelationshipViewModel = hiltViewModel()
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val relationship by viewModel.relationship.collectAsState()
    val daysSinceStart by viewModel.daysSinceStart.collectAsState()
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
                title = stringResource(R.string.relationship),
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_relationship_info),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.add_relationship_info))
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .animateContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Days Counter
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PrimaryPink)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$daysSinceStart",
                                            style = MaterialTheme.typography.displayMedium.copy(
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = stringResource(R.string.days_together),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.happy_together),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Dates Info
                        relationship?.let { rel ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    InfoRow(stringResource(R.string.started), DateUtils.formatDateForDisplay(rel.relationshipStartDate))
                                    if (!rel.firstKissDate.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        InfoRow(stringResource(R.string.first_kiss_label), DateUtils.formatDateForDisplay(rel.firstKissDate))
                                    }
                                    if (!rel.anniversaryDate.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        InfoRow(stringResource(R.string.anniversary_label), DateUtils.formatDateForDisplay(rel.anniversaryDate))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.edit_relationship_info))
                        }
                    }
                }
            }
        }

        if (showEditDialog && relationship != null) {
            EditRelationshipDialog(
                relationship = relationship!!,
                onDismiss = { showEditDialog = false },
                onUpdate = { startDate, kissDate, anniversaryDate ->
                    viewModel.updateRelationship(startDate, kissDate, anniversaryDate)
                    showEditDialog = false
                }
            )
        } else if (showEditDialog) {
            EditRelationshipDialog(
                relationship = null,
                onDismiss = { showEditDialog = false },
                onUpdate = { startDate, kissDate, anniversaryDate ->
                    viewModel.updateRelationship(startDate, kissDate, anniversaryDate)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EditRelationshipDialog(
    relationship: com.example.loveapp.data.api.models.RelationshipResponse?,
    onDismiss: () -> Unit,
    onUpdate: (String, String?, String?) -> Unit
) {
    var startDate by remember { mutableStateOf(relationship?.relationshipStartDate?.split("T")?.get(0) ?: "") }
    var kissDate by remember { mutableStateOf(relationship?.firstKissDate?.split("T")?.get(0) ?: "") }
    var anniversaryDate by remember { mutableStateOf(relationship?.anniversaryDate?.split("T")?.get(0) ?: "") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showKissDatePicker by remember { mutableStateOf(false) }
    var showAnniversaryDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    fun parseToMillis(dateString: String): Long {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    fun formatDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(dateFormatter)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onUpdate(startDate, kissDate.ifEmpty { null }, anniversaryDate.ifEmpty { null }) },
                enabled = startDate.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.relationship_info)) },
        text = {
            Column {
                // Start Date
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(if (startDate.isEmpty()) stringResource(R.string.start_date) else startDate)
                }
                
                if (showStartDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = parseToMillis(startDate),
                        onDateSelected = { millis ->
                            startDate = formatDate(millis)
                            showStartDatePicker = false
                        },
                        onDismiss = { showStartDatePicker = false }
                    )
                }
                
                // First Kiss Date
                Button(
                    onClick = { showKissDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(if (kissDate.isEmpty()) stringResource(R.string.first_kiss) else kissDate)
                }
                
                if (showKissDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (kissDate.isEmpty()) System.currentTimeMillis() else parseToMillis(kissDate),
                        onDateSelected = { millis ->
                            kissDate = formatDate(millis)
                            showKissDatePicker = false
                        },
                        onDismiss = { showKissDatePicker = false }
                    )
                }
                
                // Anniversary Date
                Button(
                    onClick = { showAnniversaryDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (anniversaryDate.isEmpty()) stringResource(R.string.anniversary) else anniversaryDate)
                }
                
                if (showAnniversaryDatePicker) {
                    DatePickerDialogWrapper(
                        initialDateMillis = if (anniversaryDate.isEmpty()) System.currentTimeMillis() else parseToMillis(anniversaryDate),
                        onDateSelected = { millis ->
                            anniversaryDate = formatDate(millis)
                            showAnniversaryDatePicker = false
                        },
                        onDismiss = { showAnniversaryDatePicker = false }
                    )
                }
            }
        }
    )
}

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
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
