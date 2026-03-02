package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.loveapp.data.api.models.ArtCanvasResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.ArtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtGalleryScreen(
    onNavigateBack: () -> Unit,
    onOpenCanvas: (ArtCanvasResponse) -> Unit,
    viewModel: ArtViewModel = hiltViewModel()
) {
    val canvases   by viewModel.canvases.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val error      by viewModel.errorMessage.collectAsState()
    val snack      = remember { SnackbarHostState() }

    var showCreate by remember { mutableStateOf(false) }
    var renamingCanvas by remember { mutableStateOf<ArtCanvasResponse?>(null) }
    var deleteTarget   by remember { mutableStateOf<ArtCanvasResponse?>(null) }

    LaunchedEffect(error) {
        error?.let { snack.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Художества",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (isLoading && canvases.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (canvases.isEmpty()) {
                EmptyArtState(onCreateClick = { showCreate = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding    = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(canvases, key = { it.id }) { canvas ->
                        ArtTile(
                            canvas = canvas,
                            onClick = { onOpenCanvas(canvas) },
                            onLongPress = { renamingCanvas = canvas },
                            onDelete = { deleteTarget = canvas }
                        )
                    }
                }
            }
        }
    }

    // ── Create dialog ──────────────────────────────────────────────────────
    if (showCreate) {
        var titleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Новый холст") },
            text = {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Название") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = titleInput.trim().ifBlank { "Без названия" }
                    viewModel.createCanvas(t) { canvas -> onOpenCanvas(canvas) }
                    showCreate = false
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Отмена") }
            }
        )
    }

    // ── Rename dialog ──────────────────────────────────────────────────────
    renamingCanvas?.let { canvas ->
        var titleInput by remember(canvas.id) { mutableStateOf(canvas.title) }
        AlertDialog(
            onDismissRequest = { renamingCanvas = null },
            title = { Text("Переименовать") },
            text = {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameCanvas(canvas.id, titleInput.trim().ifBlank { canvas.title })
                    renamingCanvas = null
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { renamingCanvas = null }) { Text("Отмена") }
            }
        )
    }

    // ── Delete confirm ─────────────────────────────────────────────────────
    deleteTarget?.let { canvas ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить «${canvas.title}»?") },
            text  = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCanvas(canvas.id)
                    deleteTarget = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ArtTile(
    canvas: ArtCanvasResponse,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F8))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (canvas.thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = canvas.thumbnailUrl,
                    contentDescription = canvas.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    loading = { CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp)) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFFFE4F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFFF6B9D).copy(alpha = 0.4f))
                }
            }

            // Title overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = canvas.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Переименовать") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { showMenu = false; onLongPress() }
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EmptyArtState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Brush, null, modifier = Modifier.size(72.dp),
            tint = Color(0xFFFF6B9D).copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text("Пока нет холстов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Создайте первый холст, чтобы начать рисовать вместе",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(4.dp))
            Text("Новый холст")
        }
    }
}
