package com.example.loveapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.LocationPointResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.LocationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val GradientPink = Color(0xFFF06292)
private val GradientPurple = Color(0xFFAB47BC)
private val BgGradient = Brush.verticalGradient(listOf(GradientPink, GradientPurple))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationMapScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val selfLocation by viewModel.selfLocation.collectAsState()
    val partnerLocation by viewModel.partnerLocation.collectAsState()
    val orbitPath by viewModel.orbitPath.collectAsState()
    val showOrbit by viewModel.showOrbit.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    // Permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission && settings.sharingEnabled) {
            viewModel.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }.toTypedArray()
        )
        viewModel.loadPartnerHistory()
    }

    // Camera position - center on partner or self
    val defaultLatLng = LatLng(55.7558, 37.6173) // Moscow fallback
    val centerLatLng = partnerLocation?.let { LatLng(it.latitude, it.longitude) }
        ?: selfLocation?.let { LatLng(it.latitude, it.longitude) }
        ?: defaultLatLng

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLatLng, 14f)
    }

    // When both locations available, zoom to fit both markers
    LaunchedEffect(selfLocation, partnerLocation) {
        val self = selfLocation
        val partner = partnerLocation
        if (self != null && partner != null) {
            val bounds = LatLngBounds.builder()
                .include(LatLng(self.latitude, self.longitude))
                .include(LatLng(partner.latitude, partner.longitude))
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Геолокация",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.Timeline, contentDescription = "История")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = hasLocationPermission,
                    compassEnabled = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                )
            ) {
                // Partner marker
                partnerLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = loc.displayName ?: "Партнёр",
                        snippet = formatTimeAgo(loc.recordedAt),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)
                    )
                }

                // Self marker (if my location layer is disabled)
                selfLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Я",
                        snippet = formatTimeAgo(loc.recordedAt),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                // Orbit polyline (partner movement trail)
                if (showOrbit && orbitPath.size >= 2) {
                    Polyline(
                        points = orbitPath,
                        color = GradientPink,
                        width = 8f
                    )
                }
            }

            // Top info card (distance, speed)
            AnimatedVisibility(
                visible = distanceKm != null || stats != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                DistanceSpeedCard(
                    distanceKm = distanceKm,
                    partnerSpeed = stats?.partnerSpeed,
                    partnerBattery = partnerLocation?.batteryLevel,
                    isCharging = partnerLocation?.isCharging ?: false,
                    showSpeed = settings.showSpeed,
                    showBattery = settings.showBattery,
                    showDistance = settings.showDistance
                )
            }

            // Bottom: partner last-seen panel
            partnerLocation?.let { partner ->
                PartnerInfoPanel(
                    partner = partner,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            // Map control buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle orbit trail
                FloatingActionButton(
                    onClick = { viewModel.toggleOrbit() },
                    containerColor = if (showOrbit) GradientPink else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (showOrbit) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Route, contentDescription = "Орбита", modifier = Modifier.size(20.dp))
                }

                // Center on partner
                FloatingActionButton(
                    onClick = {
                        partnerLocation?.let { loc ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 16f
                                    )
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PersonPinCircle, contentDescription = "Партнёр", modifier = Modifier.size(20.dp))
                }

                // Center on both
                FloatingActionButton(
                    onClick = {
                        val self = selfLocation
                        val partner = partnerLocation
                        if (self != null && partner != null) {
                            scope.launch {
                                val bounds = LatLngBounds.builder()
                                    .include(LatLng(self.latitude, self.longitude))
                                    .include(LatLng(partner.latitude, partner.longitude))
                                    .build()
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Оба", modifier = Modifier.size(20.dp))
                }
            }

            // Error snackbar
            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) { Text(msg) }
            }

            // Loading
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GradientPink
                )
            }
        }
    }

    // Settings bottom sheet
    if (showSettings) {
        LocationSettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
            onUpdateSettings = { request ->
                viewModel.updateSettings(request)
                showSettings = false
            },
            onToggleSharing = { enabled ->
                viewModel.toggleSharing(enabled)
            }
        )
    }

    // History selector
    if (showHistory) {
        HistoryHoursSelector(
            currentHours = viewModel.historyHours.collectAsState().value,
            onSelect = { hours ->
                viewModel.setHistoryHours(hours)
                showHistory = false
            },
            onDismiss = { showHistory = false }
        )
    }
}

// ─── Distance / Speed / Battery Card ───────────────────────────────

@Composable
private fun DistanceSpeedCard(
    distanceKm: Double?,
    partnerSpeed: Float?,
    partnerBattery: Int?,
    isCharging: Boolean,
    showSpeed: Boolean,
    showBattery: Boolean,
    showDistance: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDistance && distanceKm != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍", fontSize = 18.sp)
                    Text(
                        text = formatDistance(distanceKm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("расстояние", fontSize = 10.sp, color = Color.Gray)
                }
            }
            if (showSpeed && partnerSpeed != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏃", fontSize = 18.sp)
                    Text(
                        text = "%.1f км/ч".format(partnerSpeed * 3.6f), // m/s -> km/h
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("скорость", fontSize = 10.sp, color = Color.Gray)
                }
            }
            if (showBattery && partnerBattery != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isCharging) "🔌" else "🔋", fontSize = 18.sp)
                    Text(
                        text = "$partnerBattery%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            partnerBattery > 50 -> Color(0xFF4CAF50)
                            partnerBattery > 20 -> Color(0xFFFFA726)
                            else -> Color(0xFFEF5350)
                        }
                    )
                    Text("батарея", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ─── Partner Info Panel ────────────────────────────────────────────

@Composable
private fun PartnerInfoPanel(
    partner: LocationPointResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partner.displayName ?: "Партнёр",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val timeAgo = formatTimeAgo(partner.recordedAt)
                Text(
                    text = "Обновлено: $timeAgo",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (partner.activityType != "unknown") {
                    Text(
                        text = activityLabel(partner.activityType),
                        fontSize = 12.sp,
                        color = GradientPink
                    )
                }
            }
            // Accuracy indicator
            partner.accuracy?.let { acc ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = if (acc < 30) Color(0xFF4CAF50) else Color(0xFFFFA726),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("±${acc.toInt()}м", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ─── Settings Bottom Sheet ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSettingsSheet(
    settings: com.example.loveapp.data.api.models.LocationSettingsResponse,
    onDismiss: () -> Unit,
    onUpdateSettings: (com.example.loveapp.data.api.models.LocationSettingsRequest) -> Unit,
    onToggleSharing: (Boolean) -> Unit
) {
    var sharingEnabled by remember { mutableStateOf(settings.sharingEnabled) }
    var showSpeed by remember { mutableStateOf(settings.showSpeed) }
    var showBattery by remember { mutableStateOf(settings.showBattery) }
    var showDistance by remember { mutableStateOf(settings.showDistance) }
    var intervalSec by remember { mutableIntStateOf(settings.updateIntervalSec) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Настройки геолокации",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sharing toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Делиться местоположением")
                Switch(
                    checked = sharingEnabled,
                    onCheckedChange = {
                        sharingEnabled = it
                        onToggleSharing(it)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Update interval
            Text("Интервал обновления", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10 to "10с", 30 to "30с", 60 to "1м", 120 to "2м", 300 to "5м").forEach { (sec, label) ->
                    FilterChip(
                        selected = intervalSec == sec,
                        onClick = { intervalSec = sec },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Display options
            Text("Отображение", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Скорость партнёра", fontSize = 14.sp)
                Switch(checked = showSpeed, onCheckedChange = { showSpeed = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Батарея партнёра", fontSize = 14.sp)
                Switch(checked = showBattery, onCheckedChange = { showBattery = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Расстояние", fontSize = 14.sp)
                Switch(checked = showDistance, onCheckedChange = { showDistance = it })
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onUpdateSettings(
                        com.example.loveapp.data.api.models.LocationSettingsRequest(
                            sharingEnabled = sharingEnabled,
                            updateIntervalSec = intervalSec,
                            showSpeed = showSpeed,
                            showBattery = showBattery,
                            showDistance = showDistance
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GradientPink)
            ) {
                Text("Сохранить", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── History Hours Dialog ──────────────────────────────────────────

@Composable
private fun HistoryHoursSelector(
    currentHours: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("История перемещений") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    2 to "Последние 2 часа",
                    6 to "Последние 6 часов",
                    12 to "Последние 12 часов",
                    24 to "Последние 24 часа",
                    72 to "Последние 3 дня",
                    168 to "Последняя неделя"
                ).forEach { (hours, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentHours == hours) GradientPink.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(hours) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentHours == hours, onClick = { onSelect(hours) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

// ─── Helpers ───────────────────────────────────────────────────────

private fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).toInt()} м"
    } else {
        "%.1f км".format(km)
    }
}

private fun formatTimeAgo(isoTime: String): String {
    if (isoTime.isBlank()) return "—"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoTime) ?: return isoTime
        val diffMs = System.currentTimeMillis() - date.time
        val diffMin = diffMs / 60_000
        when {
            diffMin < 1 -> "только что"
            diffMin < 60 -> "$diffMin мин назад"
            diffMin < 1440 -> "${diffMin / 60} ч назад"
            else -> "${diffMin / 1440} дн назад"
        }
    } catch (_: Exception) {
        isoTime
    }
}

private fun activityLabel(type: String): String {
    return when (type) {
        "still" -> "🧍 На месте"
        "walking" -> "🚶 Идёт"
        "running" -> "🏃 Бежит"
        "driving" -> "🚗 Едет"
        "cycling" -> "🚴 На велосипеде"
        else -> ""
    }
}
