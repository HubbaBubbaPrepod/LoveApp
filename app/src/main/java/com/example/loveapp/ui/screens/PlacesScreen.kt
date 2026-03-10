package com.example.loveapp.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.PlaceResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.PlacesViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val categoryLabels = mapOf(
    "restaurant" to "🍽 Ресторан",
    "cafe" to "☕ Кафе",
    "park" to "🌳 Парк",
    "cinema" to "🎬 Кино",
    "hotel" to "🏨 Отель",
    "home" to "🏠 Дом",
    "travel" to "✈️ Путешествие",
    "date" to "💕 Свидание",
    "other" to "📍 Другое"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlacesViewModel = hiltViewModel()
) {
    val places by viewModel.places.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val placeSaved by viewModel.placeSaved.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showMapView by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(placeSaved) {
        if (placeSaved) {
            snackbarHostState.showSnackbar("Место сохранено! 📍")
            viewModel.clearMessages()
            showAddDialog = false
        }
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(title = "Наши места", onBackClick = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Map/list toggle FAB
                SmallFloatingActionButton(
                    onClick = { showMapView = !showMapView },
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF6B9D)
                ) {
                    Icon(
                        if (showMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = if (showMapView) "Список" else "Карта"
                    )
                }
                // Add place FAB
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFFF6B9D),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить место")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Category filter chips
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("Все") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF6B9D),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(categoryLabels[cat] ?: cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF6B9D),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && places.isEmpty()) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF6B9D),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (places.isEmpty() && !showMapView) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🗺️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Пока нет мест",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Сохраните ваши любимые\nместа вместе!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (showMapView) {
                    PlacesMapView(places = places)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(places, key = { it.id }) { place ->
                            PlaceCard(
                                place = place,
                                onDelete = { viewModel.deletePlace(place.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaceDialog(
            currentLocation = currentLocation,
            isLocating = isLocating,
            onRequestLocation = { viewModel.requestCurrentLocation() },
            onDismiss = {
                showAddDialog = false
                viewModel.clearCurrentLocation()
            },
            onSave = { name, address, category, note, lat, lon ->
                viewModel.addPlace(name = name, address = address, category = category, note = note, latitude = lat, longitude = lon)
            }
        )
    }
}

@Composable
private fun PlacesMapView(places: List<PlaceResponse>) {
    // Calculate center from places with valid coordinates, default to Moscow
    val placesWithCoords = places.filter { it.latitude != 0.0 || it.longitude != 0.0 }
    val center = if (placesWithCoords.isNotEmpty()) {
        LatLng(
            placesWithCoords.map { it.latitude }.average(),
            placesWithCoords.map { it.longitude }.average()
        )
    } else {
        LatLng(55.7558, 37.6173) // Moscow
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
    ) {
        placesWithCoords.forEach { place ->
            val emoji = categoryLabels[place.category]?.take(2) ?: "📍"
            Marker(
                state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                title = place.name,
                snippet = place.address ?: (categoryLabels[place.category] ?: "")
            )
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceResponse, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFF6B9D), Color(0xFFFF8E8E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryLabels[place.category]?.take(2) ?: "📍",
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (place.address != null) {
                        Text(
                            text = place.address,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6B9D).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = categoryLabels[place.category] ?: place.category ?: "Другое",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color(0xFFFF6B9D)
                    )
                }
            }
            if (place.note != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = place.note,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Добавил: ${place.displayName ?: "Партнёр"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaceDialog(
    currentLocation: Pair<Double, Double>?,
    isLocating: Boolean,
    onRequestLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String?, category: String, note: String?, latitude: Double?, longitude: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("other") }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) onRequestLocation()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Новое место", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        cursorColor = Color(0xFFFF6B9D)
                    )
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        cursorColor = Color(0xFFFF6B9D)
                    )
                )

                // Location button
                OutlinedButton(
                    onClick = {
                        if (locationPermissionGranted) {
                            onRequestLocation()
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLocating
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFFF6B9D))
                        Spacer(Modifier.width(8.dp))
                        Text("Определяем...")
                    } else if (currentLocation != null) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("📍 Геолокация добавлена", color = Color(0xFF4CAF50))
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Моё местоположение")
                    }
                }

                Text("Категория:", fontSize = 13.sp, color = Color.Gray)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryLabels.forEach { (key, label) ->
                        val isSelected = selectedCategory == key
                        Surface(
                            onClick = { selectedCategory = key },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFFF6B9D) else Color(0xFFF5F5F5),
                            contentColor = if (isSelected) Color.White else Color.DarkGray
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        cursorColor = Color(0xFFFF6B9D)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        address.ifBlank { null },
                        selectedCategory,
                        note.ifBlank { null },
                        currentLocation?.first,
                        currentLocation?.second
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.Gray)
            }
        }
    )
}
