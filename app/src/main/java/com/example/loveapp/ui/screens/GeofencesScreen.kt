package com.example.loveapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.*
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.GeofenceViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val AccentTeal = Color(0xFF26A69A)
private val AccentPink = Color(0xFFF06292)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: GeofenceViewModel = hiltViewModel()
) {
    val geofences by viewModel.geofences.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val selectedGeofence by viewModel.selectedGeofence.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isCreatingMode by viewModel.isCreatingMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEventsSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var tapLatLng by remember { mutableStateOf<LatLng?>(null) }
    // 0 = map, 1 = list, 2 = events
    var currentTab by remember { mutableIntStateOf(0) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        locationPermLauncher.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }.toTypedArray()
        )
    }

    val filteredGeofences = remember(geofences, selectedCategory) {
        if (selectedCategory == null) geofences
        else geofences.filter { it.category == selectedCategory }
    }

    val defaultLatLng = LatLng(55.7558, 37.6173)
    val initialLatLng = geofences.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: defaultLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Геозоны",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadGeofences() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (isCreatingMode) {
                            viewModel.toggleCreatingMode()
                        } else {
                            viewModel.toggleCreatingMode()
                        }
                    },
                    containerColor = if (isCreatingMode) Color.Red else AccentTeal,
                    contentColor = Color.White
                ) {
                    Icon(
                        if (isCreatingMode) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isCreatingMode) "Отмена" else "Добавить"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = currentTab) {
                Tab(selected = currentTab == 0, onClick = { currentTab = 0 }) {
                    Text("Карта", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                }
                Tab(selected = currentTab == 1, onClick = { currentTab = 1 }) {
                    Text("Список", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                }
                Tab(selected = currentTab == 2, onClick = { currentTab = 2 }) {
                    Text("События", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                }
            }

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("Все", fontSize = 12.sp) }
                    )
                }
                items(GeofenceViewModel.CATEGORIES) { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { viewModel.setCategory(if (selectedCategory == key) null else key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            when (currentTab) {
                0 -> {
                    // ─── Map Tab ─────────────────────────────────
                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                myLocationButtonEnabled = hasLocationPermission
                            ),
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            onMapClick = { latLng ->
                                if (isCreatingMode) {
                                    tapLatLng = latLng
                                    showCreateDialog = true
                                }
                            }
                        ) {
                            filteredGeofences.forEach { gf ->
                                val pos = LatLng(gf.latitude, gf.longitude)
                                val circleColor = try {
                                    Color(android.graphics.Color.parseColor(gf.color))
                                } catch (_: Exception) { AccentTeal }

                                // Circle for radius
                                Circle(
                                    center = pos,
                                    radius = gf.radiusMeters.toDouble(),
                                    strokeColor = circleColor.copy(alpha = 0.7f),
                                    strokeWidth = 3f,
                                    fillColor = circleColor.copy(alpha = 0.15f)
                                )

                                // Marker
                                Marker(
                                    state = MarkerState(position = pos),
                                    title = "${gf.icon} ${gf.name}",
                                    snippet = GeofenceViewModel.categoryLabel(gf.category),
                                    onClick = {
                                        viewModel.selectGeofence(gf)
                                        false
                                    }
                                )
                            }
                        }

                        // Creating mode banner
                        if (isCreatingMode) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AccentTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Нажмите на карту, чтобы создать геозону",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Selected geofence info
                        selectedGeofence?.let { gf ->
                            GeofenceInfoCard(
                                geofence = gf,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                onEdit = { showEditDialog = true },
                                onDelete = {
                                    viewModel.deleteGeofence(gf.id)
                                },
                                onToggle = { active ->
                                    viewModel.toggleGeofence(gf.id, active)
                                },
                                onViewEvents = { showEventsSheet = true },
                                onDismiss = { viewModel.selectGeofence(null) }
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = AccentTeal
                            )
                        }
                    }
                }

                1 -> {
                    // ─── List Tab ────────────────────────────────
                    if (filteredGeofences.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Нет геозон", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredGeofences, key = { it.id }) { gf ->
                                GeofenceListItem(
                                    geofence = gf,
                                    onClick = {
                                        viewModel.selectGeofence(gf)
                                        currentTab = 0
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(gf.latitude, gf.longitude), 16f
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // ─── Events Tab ──────────────────────────────
                    if (recentEvents.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Нет событий", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(recentEvents, key = { it.id }) { event ->
                                GeofenceEventItem(event = event)
                            }
                        }
                    }
                }
            }
        }
    }

    // Error snackbar
    errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            // Auto-clear after 3s
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    // Create dialog
    if (showCreateDialog && tapLatLng != null) {
        CreateGeofenceDialog(
            latLng = tapLatLng!!,
            onDismiss = {
                showCreateDialog = false
                tapLatLng = null
            },
            onCreate = { request ->
                viewModel.createGeofence(request)
                showCreateDialog = false
                tapLatLng = null
            }
        )
    }

    // Edit dialog
    if (showEditDialog && selectedGeofence != null) {
        EditGeofenceDialog(
            geofence = selectedGeofence!!,
            onDismiss = { showEditDialog = false },
            onSave = { request ->
                viewModel.updateGeofence(selectedGeofence!!.id, request)
                showEditDialog = false
            }
        )
    }

    // Events bottom sheet for selected geofence
    if (showEventsSheet && selectedGeofence != null) {
        GeofenceEventsSheet(
            geofenceId = selectedGeofence!!.id,
            geofenceName = selectedGeofence!!.name,
            viewModel = viewModel,
            onDismiss = { showEventsSheet = false }
        )
    }
}

// ─── Geofence Info Card (bottom of map) ──────────────────────────────────────

@Composable
private fun GeofenceInfoCard(
    geofence: GeofenceResponse,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onViewEvents: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(geofence.icon, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            geofence.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            GeofenceViewModel.categoryLabel(geofence.category),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(20.dp))
                }
            }

            geofence.address?.let { addr ->
                if (addr.isNotBlank()) {
                    Text(addr, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Text(
                "Радиус: ${geofence.radiusMeters} м  •  ${if (geofence.isActive) "Активна" else "Выключена"}",
                fontSize = 12.sp,
                color = if (geofence.isActive) AccentTeal else Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (geofence.creatorName != null) {
                Text(
                    "Создал(а): ${geofence.creatorName}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Изменить", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onViewEvents, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("События", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = geofence.isActive,
                        onCheckedChange = { onToggle(it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (geofence.isActive) "Активна" else "Выкл", fontSize = 13.sp)
                }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Удалить", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Geofence List Item ──────────────────────────────────────────────────────

@Composable
private fun GeofenceListItem(
    geofence: GeofenceResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(geofence.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(geofence.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    GeofenceViewModel.categoryLabel(geofence.category),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                geofence.address?.let {
                    if (it.isNotBlank()) Text(it, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${geofence.radiusMeters}м", fontSize = 12.sp, color = AccentTeal)
                Text(
                    if (geofence.isActive) "✓ Вкл" else "✗ Выкл",
                    fontSize = 11.sp,
                    color = if (geofence.isActive) AccentTeal else Color.Gray
                )
            }
        }
    }
}

// ─── Event Item ──────────────────────────────────────────────────────────────

@Composable
private fun GeofenceEventItem(event: GeofenceEventResponse) {
    val eventEmoji = when (event.eventType) {
        "enter" -> "📥"
        "exit" -> "📤"
        "dwell" -> "🕐"
        else -> "📍"
    }
    val eventLabel = when (event.eventType) {
        "enter" -> "Прибытие"
        "exit" -> "Уход"
        "dwell" -> "Пребывание"
        else -> event.eventType
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$eventEmoji ${event.geofenceIcon ?: "📍"}", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${event.displayName ?: "—"} — $eventLabel",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    event.geofenceName ?: "Неизвестная зона",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                formatEventTime(event.triggeredAt),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

// ─── Create Geofence Dialog ──────────────────────────────────────────────────

@Composable
private fun CreateGeofenceDialog(
    latLng: LatLng,
    onDismiss: () -> Unit,
    onCreate: (GeofenceRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("200") }
    var selectedCategory by remember { mutableStateOf("other") }
    var address by remember { mutableStateOf("") }
    var notifyEnter by remember { mutableStateOf(true) }
    var notifyExit by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая геозона") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Адрес (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it.filter { c -> c.isDigit() } },
                        label = { Text("Радиус (метры)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    Text("Категория", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GeofenceViewModel.CATEGORIES) { (key, label) ->
                            FilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Уведомлять о прибытии", fontSize = 13.sp)
                        Switch(checked = notifyEnter, onCheckedChange = { notifyEnter = it })
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Уведомлять об уходе", fontSize = 13.sp)
                        Switch(checked = notifyExit, onCheckedChange = { notifyExit = it })
                    }
                }
                item {
                    Text(
                        "📍 ${String.format("%.5f", latLng.latitude)}, ${String.format("%.5f", latLng.longitude)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(
                            GeofenceRequest(
                                name = name.trim(),
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                radiusMeters = radius.toIntOrNull() ?: 200,
                                category = selectedCategory,
                                address = address.ifBlank { null },
                                notifyOnEnter = notifyEnter,
                                notifyOnExit = notifyExit
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ─── Edit Geofence Dialog ────────────────────────────────────────────────────

@Composable
private fun EditGeofenceDialog(
    geofence: GeofenceResponse,
    onDismiss: () -> Unit,
    onSave: (GeofenceUpdateRequest) -> Unit
) {
    var name by remember { mutableStateOf(geofence.name) }
    var radius by remember { mutableStateOf(geofence.radiusMeters.toString()) }
    var category by remember { mutableStateOf(geofence.category) }
    var address by remember { mutableStateOf(geofence.address ?: "") }
    var notifyEnter by remember { mutableStateOf(geofence.notifyOnEnter) }
    var notifyExit by remember { mutableStateOf(geofence.notifyOnExit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        label = { Text("Адрес") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it.filter { c -> c.isDigit() } },
                        label = { Text("Радиус (м)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    Text("Категория", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GeofenceViewModel.CATEGORIES) { (key, label) ->
                            FilterChip(
                                selected = category == key,
                                onClick = { category = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Уведомлять о прибытии", fontSize = 13.sp)
                        Switch(checked = notifyEnter, onCheckedChange = { notifyEnter = it })
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Уведомлять об уходе", fontSize = 13.sp)
                        Switch(checked = notifyExit, onCheckedChange = { notifyExit = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        GeofenceUpdateRequest(
                            name = name.trim().takeIf { it != geofence.name },
                            radiusMeters = radius.toIntOrNull()?.takeIf { it != geofence.radiusMeters },
                            category = category.takeIf { it != geofence.category },
                            address = address.ifBlank { null },
                            notifyOnEnter = notifyEnter.takeIf { it != geofence.notifyOnEnter },
                            notifyOnExit = notifyExit.takeIf { it != geofence.notifyOnExit }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ─── Events Bottom Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeofenceEventsSheet(
    geofenceId: Long,
    geofenceName: String,
    viewModel: GeofenceViewModel,
    onDismiss: () -> Unit
) {
    val events = remember { mutableStateOf<List<GeofenceEventResponse>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    // Filter events for this geofence from the recent events list
    LaunchedEffect(geofenceId) {
        events.value = viewModel.recentEvents.value.filter { it.geofenceId == geofenceId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "📋 События: $geofenceName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (events.value.isEmpty()) {
                Text("Нет событий для этой геозоны", color = Color.Gray, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(events.value) { event ->
                        GeofenceEventItem(event = event)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatEventTime(isoTime: String): String {
    if (isoTime.isBlank()) return "—"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoTime) ?: return isoTime
        val out = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        out.format(date)
    } catch (_: Exception) {
        isoTime
    }
}
