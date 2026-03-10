package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.ChatSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage, successMessage) {
        val msg = errorMessage ?: successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = { IOSTopAppBar(title = "Настройки чата", onBackClick = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading && settings == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B9D))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Wallpaper Section ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = Color(0xFFFF6B9D))
                            Spacer(Modifier.width(8.dp))
                            Text("Обои чата", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }

                        Text(
                            "Выберите фон для чата",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )

                        // No wallpaper option
                        OutlinedButton(
                            onClick = { viewModel.selectWallpaper(null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (settings?.wallpaperUrl.isNullOrBlank()) Color(0xFFFF6B9D)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Text("Без обоев")
                            if (settings?.wallpaperUrl.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Wallpaper grid
                        if (wallpapers.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.height(240.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(wallpapers) { wp ->
                                    val isSelected = settings?.wallpaperUrl == wp.url
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFFF6B9D) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectWallpaper(wp.url) }
                                    ) {
                                        AsyncImage(
                                            model = wp.preview,
                                            contentDescription = wp.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .background(Color(0xFFFF6B9D), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(
                                            wp.name,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(4.dp),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Bubble Color Section ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = Color(0xFFFF6B9D))
                            Spacer(Modifier.width(8.dp))
                            Text("Стиль пузырей", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }

                        Text(
                            "Цвет ваших сообщений",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(viewModel.bubbleColors) { (colorKey, label) ->
                                val isSelected = settings?.bubbleColor == colorKey ||
                                        (settings?.bubbleColor.isNullOrBlank() && colorKey == "default")
                                val chipColor = when (colorKey) {
                                    "rose" -> Color(0xFFFF6B9D)
                                    "sky" -> Color(0xFF64B5F6)
                                    "lavender" -> Color(0xFFCE93D8)
                                    "mint" -> Color(0xFF81C784)
                                    "sunset" -> Color(0xFFFF8A65)
                                    "ocean" -> Color(0xFF4DD0E1)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.selectBubbleColor(colorKey) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // ── Bubble Shape Section ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(0xFFFF6B9D))
                            Spacer(Modifier.width(8.dp))
                            Text("Форма пузырей", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }

                        Text(
                            "Выберите форму сообщений",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(viewModel.bubbleShapes) { (shapeKey, label) ->
                                val isSelected = settings?.bubbleShape == shapeKey ||
                                        (settings?.bubbleShape.isNullOrBlank() && shapeKey == "rounded")
                                val shapePreview = when (shapeKey) {
                                    "classic" -> RoundedCornerShape(12.dp)
                                    "sharp" -> RoundedCornerShape(4.dp)
                                    "tail" -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                                    else -> RoundedCornerShape(16.dp)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.selectBubbleShape(shapeKey) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp, 36.dp)
                                            .clip(shapePreview)
                                            .background(
                                                if (isSelected) Color(0xFFFF6B9D)
                                                else MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                else Color.Transparent,
                                                shape = shapePreview
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
