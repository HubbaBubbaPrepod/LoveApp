package com.example.loveapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.viewmodel.ArtViewModel
import com.example.loveapp.viewmodel.DrawPath
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

val ART_PALETTE = listOf(
    Color.Black,
    Color.White,
    Color(0xFFE53935), // red
    Color(0xFFE91E63), // pink
    Color(0xFF9C27B0), // purple
    Color(0xFF1E88E5), // blue
    Color(0xFF00ACC1), // cyan
    Color(0xFF43A047), // green
    Color(0xFFFFB300), // amber
    Color(0xFFFF7043), // deep orange
    Color(0xFF6D4C41), // brown
    Color(0xFF78909C)  // blue grey
)

@Composable
fun CanvasEditorScreen(
    canvasId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ArtViewModel = hiltViewModel()
) {
    val scope  = rememberCoroutineScope()
    val strokes          by viewModel.strokes.collectAsState()
    val liveStroke       by viewModel.liveStroke.collectAsState()
    val partnerLiveStroke by viewModel.partnerLiveStroke.collectAsState()
    val selectedColor    by viewModel.selectedColor.collectAsState()
    val strokeWidth      by viewModel.strokeWidth.collectAsState()
    val isEraser         by viewModel.isEraser.collectAsState()
    val isSaving         by viewModel.isSaving.collectAsState()
    val activeCanvas     by viewModel.activeCanvas.collectAsState()

    var showToolbar by remember { mutableStateOf(true) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Open the canvas when we land on this screen
    val canvases by viewModel.canvases.collectAsState()
    LaunchedEffect(canvasId, canvases) {
        if (activeCanvas?.id != canvasId) {
            val target = canvases.find { it.id == canvasId }
            target?.let { viewModel.openCanvas(it) }
        }
    }

    fun captureAndExit() {
        scope.launch {
            val w = canvasSize.width
            val h = canvasSize.height
            if (w > 0 && h > 0) {
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val androidCanvas = android.graphics.Canvas(bm)
                androidCanvas.drawColor(android.graphics.Color.WHITE)
                val paint = AndroidPaint().apply {
                    isAntiAlias = true
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                viewModel.strokes.value.forEach { dp ->
                    paint.color = dp.color.toArgb()
                    paint.strokeWidth = dp.strokeWidth
                    androidCanvas.drawPath(dp.path.asAndroidPath(), paint)
                }
                // Save strokes and thumbnail in parallel
                kotlinx.coroutines.coroutineScope {
                    val strokesJob = launch { viewModel.saveCurrentStrokes() }
                    val thumbnailJob = launch { viewModel.saveSnapshot(bm) }
                    strokesJob.join()
                    thumbnailJob.join()
                }
            }
            viewModel.closeCanvas()
            onNavigateBack()
        }
    }

    BackHandler { captureAndExit() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Drawing canvas ─────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var didDrag = false
                        val touchSlop = viewConfiguration.touchSlop
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!didDrag) {
                                val delta = change.position - down.position
                                if (delta.getDistance() > touchSlop) {
                                    didDrag = true
                                    viewModel.strokeStart(down.position.x, down.position.y)
                                }
                            }
                            if (didDrag) {
                                change.consume()
                                viewModel.strokeMove(change.position.x, change.position.y)
                            }
                        } while (event.changes.any { it.pressed })
                        if (didDrag) viewModel.strokeEnd()
                        else showToolbar = !showToolbar
                    }
                }
        ) {
            // committed strokes
            strokes.forEach { dp ->
                drawPath(
                    path = dp.path,
                    color = dp.color,
                    style = Stroke(
                        width = dp.strokeWidth,
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round
                    )
                )
            }
            // current user's live stroke
            liveStroke?.let { dp ->
                drawPath(
                    path = dp.path,
                    color = dp.color,
                    style = Stroke(
                        width = dp.strokeWidth,
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round
                    )
                )
            }
            // partner's live stroke
            partnerLiveStroke?.let { dp ->
                drawPath(
                    path = dp.path,
                    color = dp.color,
                    style = Stroke(
                        width = dp.strokeWidth,
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round
                    )
                )
            }
        }

        // ── Canvas title ───────────────────────────────────────────────────
        if (showToolbar) {
            Text(
                text = activeCanvas?.title ?: "",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
        }

        // ── Top toolbar (back, undo, redo, clear) ──────────────────────────
        if (showToolbar) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallToolButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад") { captureAndExit() }
                Spacer(Modifier.width(4.dp))
                SmallToolButton(Icons.Default.Undo, "Отменить") { viewModel.undo() }
                SmallToolButton(Icons.Default.Redo, "Повторить") { viewModel.redo() }
                SmallToolButton(Icons.Default.Delete, "Очистить") { viewModel.clearCanvas() }
            }
            if (isSaving) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("Сохраняю…", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // ── Bottom toolbar (palette + stroke width + eraser) ───────────────
        if (showToolbar) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color palette
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ART_PALETTE.forEach { color ->
                        val isSelected = !isEraser && selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    CircleShape
                                )
                                .clickable { viewModel.setColor(color); viewModel.setEraser(false) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Stroke width + eraser
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LinearScale, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                    Slider(
                        value = strokeWidth,
                        onValueChange = { viewModel.setStrokeWidth(it) },
                        valueRange = 2f..32f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Eraser toggle
                    IconButton(
                        onClick = { viewModel.setEraser(!isEraser) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEraser) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            Icons.Default.AutoFixNormal,
                            contentDescription = "Ластик",
                            tint = if (isEraser) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f))
    ) {
        Icon(icon, contentDescription = description, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
    }
}


