package com.example.loveapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.viewmodel.ActivityViewModel

@Composable
fun ActivityFeedScreen(
    navController: NavHostController,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val activities by viewModel.activities.collectAsState()
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
        topBar = {
            TopAppBar(
                title = { Text("Activity Feed") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && activities.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                activities.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No activities yet", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Start logging your couple activities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        items(activities) { activity ->
                            ActivityCard(
                                activity = activity,
                                onDelete = { viewModel.deleteActivity(it) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddActivityDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, description ->
                    viewModel.createActivity(title, description, "General")
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityResponse,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Date: ${activity.date}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = { onDelete(activity.id) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddActivityDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onAdd(title, description) },
                enabled = title.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Log Activity") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    )
}




























































































































































































































}    )        }            }                )                    minLines = 3                    modifier = Modifier.fillMaxWidth(),                    label = { Text("What happened?") },                    onValueChange = { description = it },                    value = description,                OutlinedTextField(                )                        .padding(bottom = 12.dp)                        .fillMaxWidth()                    modifier = Modifier                    label = { Text("Activity Title") },                    onValueChange = { title = it },                    value = title,                OutlinedTextField(            Column {        text = {        title = { Text("Log an Activity") },        },            }                Text("Cancel")            androidx.compose.material3.TextButton(onClick = onDismiss) {        dismissButton = {        },            }                Text("Add")            ) {                enabled = title.isNotEmpty()                onClick = { onAdd(title, description) },            androidx.compose.material3.TextButton(        confirmButton = {        onDismissRequest = onDismiss,    androidx.compose.material3.AlertDialog(    var description by remember { mutableStateOf("") }    var title by remember { mutableStateOf("") }) {    onAdd: (String, String) -> Unit    onDismiss: () -> Unit,fun AddActivityDialog(@Composable}    }        }            }                )                    modifier = Modifier.padding(top = 4.dp)                    maxLines = 2,                    style = MaterialTheme.typography.bodySmall,                    text = activity.description,                Text(                )                    modifier = Modifier.padding(top = 4.dp)                    color = MaterialTheme.colorScheme.outline,                    style = MaterialTheme.typography.labelSmall,                    text = activity.date,                Text(                )                    color = MaterialTheme.colorScheme.primary                    style = MaterialTheme.typography.headlineSmall,                    text = activity.title,                Text(            ) {                    .padding(start = 16.dp)                    .weight(1f)                modifier = Modifier            Column(            }                )                    style = MaterialTheme.typography.headlineSmall                    color = Color.White,                    text = activity.title.take(1).uppercase(),                Text(            ) {                contentAlignment = Alignment.Center                    .background(activity.color),                    .clip(CircleShape)                    .size(56.dp)                modifier = Modifier            Box(        ) {            verticalAlignment = Alignment.Top                .padding(16.dp),                .fillMaxWidth()            modifier = Modifier        Row(    ) {            .padding(4.dp)            .fillMaxWidth()        modifier = Modifier    Card(fun ActivityCard(activity: ActivityItem) {@Composable}    }        }            )                }                    showAddDialog = false                    activities = activities + newActivity                    )                        color = MaterialTheme.colorScheme.primary                        category = "Adventure",                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),                        description = description,                        title = title,                        id = activities.size + 1,                    val newActivity = ActivityItem(                onAdd = { title, description ->                onDismiss = { showAddDialog = false },            AddActivityDialog(        if (showAddDialog) {        }            }                }                    }                        Spacer(modifier = Modifier.height(12.dp))                        ActivityCard(activity)                    items(activities) { activity ->                LazyColumn {            } else {                }                    Text("No activities recorded yet. Add one to get started!")                ) {                    contentAlignment = Alignment.Center                    modifier = Modifier.fillMaxSize(),                Box(            if (activities.isEmpty()) {        ) {                .padding(16.dp)                .padding(innerPadding)                .fillMaxSize()            modifier = Modifier        Column(    ) { innerPadding ->        }            }                Icon(Icons.Default.Add, contentDescription = "Add Activity", tint = Color.White)            ) {                containerColor = MaterialTheme.colorScheme.primary                onClick = { showAddDialog = true },            FloatingActionButton(        floatingActionButton = {        },            )                }                    }                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")                    IconButton(onClick = { navController.popBackStack() }) {                navigationIcon = {                title = { Text("What We Did Today") },            TopAppBar(        topBar = {    Scaffold(    var activityDescription by remember { mutableStateOf("") }    var activityTitle by remember { mutableStateOf("") }    var activities by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }    var showAddDialog by remember { mutableStateOf(false) }fun ActivityFeedScreen(navController: NavHostController) {@Composable)    val color: Color    val category: String,    val date: String,    val description: String,    val title: String,    val id: Int,data class ActivityItem(import java.util.Localeimport java.util.Dateimport java.text.SimpleDateFormatimport androidx.navigation.NavHostControllerimport androidx.compose.ui.unit.dpimport androidx.compose.ui.graphics.Colorimport androidx.compose.ui.draw.clipimport androidx.compose.ui.Modifierimport androidx.compose.ui.Alignmentimport androidx.compose.runtime.setValueimport androidx.compose.runtime.rememberimport androidx.compose.runtime.mutableStateOfimport androidx.compose.runtime.getValueimport androidx.compose.runtime.Composableimport androidx.compose.material3.TopAppBarimport androidx.compose.material3.Textimport androidx.compose.material3.Scaffoldimport androidx.compose.material3.OutlinedTextFieldimport androidx.compose.material3.MaterialThemeimport androidx.compose.material3.IconButtonimport androidx.compose.material3.Iconimport androidx.compose.material3.FloatingActionButtonimport androidx.compose.material3.Cardimport androidx.compose.material.icons.filled.ArrowBackimport androidx.compose.material.icons.filled.Addimport androidx.compose.material.icons.Iconsimport androidx.compose.foundation.shape.RoundedCornerShapeimport androidx.compose.foundation.shape.CircleShapeimport androidx.compose.foundation.lazy.itemsimport androidx.compose.foundation.lazy.LazyColumnimport androidx.compose.foundation.layout.sizeimport androidx.compose.foundation.layout.paddingimport androidx.compose.foundation.layout.heightimport androidx.compose.foundation.layout.fillMaxWidthimport androidx.compose.foundation.layout.fillMaxSizeimport androidx.compose.foundation.layout.Spacerimport androidx.compose.foundation.layout.Rowimport androidx.compose.foundation.layout.Columnimport androidx.compose.foundation.layout.Boximport androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

data class ActivityItem(
    val id: Int,
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    val imageUrl: String? = null
)

@Composable
fun ActivityFeedScreen(navController: NavHostController) {
    var showAddDialog by remember { mutableStateOf(false) }
    var activities by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var activityTitle by remember { mutableStateOf("") }
    var activityDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What We Did Today") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities recorded yet. Create one!")
                }
            } else {
                LazyColumn(
                    reverseLayout = true // Most recent first
                ) {
                    items(activities) { activity ->
                        ActivityCard(activity)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        if (showAddDialog) {
            AddActivityDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, description ->
                    val newActivity = ActivityItem(
                        id = activities.size + 1,
                        title = title,
                        description = description,
                        date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date()),
                        category = "memory"
                    )
                    activities = activities + newActivity
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activity.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AddActivityDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onAdd(title, description) },
                enabled = title.isNotEmpty() && description.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("What did we do?") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    )
}
