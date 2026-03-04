package com.example.loveapp.ui.art

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveapp.ui.screens.ART_PALETTE
import com.example.loveapp.ui.theme.LoveAppTheme
import com.example.loveapp.viewmodel.ArtViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Full-screen drawing activity shown on the lock screen.
 * Opens the last active canvas so the user can draw without unlocking the device.
 *
 * Add to AndroidManifest with:
 *   android:showWhenLocked="true"
 *   android:turnScreenOn="true"
 */
@AndroidEntryPoint
class LockScreenDrawingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow showing over lock screen (API 27+ uses attributes; older uses window flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val canvasId = intent.getIntExtra("canvas_id", -1)

        setContent {
            LoveAppTheme {
                val viewModel: ArtViewModel = hiltViewModel()
                LockScreenCanvas(
                    canvasId = canvasId,
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun LockScreenCanvas(
    canvasId: Int,
    viewModel: ArtViewModel,
    onClose: () -> Unit
) {
    val strokes           by viewModel.strokes.collectAsState()
    val liveStroke        by viewModel.liveStroke.collectAsState()
    val partnerLiveStroke by viewModel.partnerLiveStroke.collectAsState()
    val selectedColor     by viewModel.selectedColor.collectAsState()
    val strokeWidth       by viewModel.strokeWidth.collectAsState()
    val isEraser          by viewModel.isEraser.collectAsState()
    val activeCanvas      by viewModel.activeCanvas.collectAsState()

    var showToolbar by remember { mutableStateOf(true) }

    // Open the canvas identified by intent extra (if valid), else the first available
    val canvases by viewModel.canvases.collectAsState()
    LaunchedEffect(canvasId, canvases) {
        if (activeCanvas == null) {
            val target = if (canvasId > 0) canvases.find { it.id == canvasId } else canvases.firstOrNull()
            target?.let { viewModel.openCanvas(it) }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {

        // Drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { viewModel.strokeStart(it.x, it.y) },
                        onDrag = { change, _ ->
                            change.consume()
                            viewModel.strokeMove(change.position.x, change.position.y)
                        },
                        onDragEnd = { viewModel.strokeEnd() },
                        onDragCancel = { viewModel.strokeEnd() }
                    )
                }
        ) {
            strokes.forEach { dp ->
                drawPath(dp.path, dp.color, style = Stroke(dp.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            liveStroke?.let { dp ->
                drawPath(dp.path, dp.color, style = Stroke(dp.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            partnerLiveStroke?.let { dp ->
                drawPath(dp.path, dp.color, style = Stroke(dp.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Tap to toggle toolbar
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { showToolbar = !showToolbar }
        )

        // Title
        if (showToolbar && activeCanvas != null) {
            Text(
                text = activeCanvas!!.title,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.Gray
            )
        }

        // Top action row
        if (showToolbar) {
            Row(
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LockToolButton(Icons.Default.Close, "Закрыть") { onClose() }
                Spacer(Modifier.width(4.dp))
                LockToolButton(Icons.Default.Undo, "Отменить") { viewModel.undo() }
                LockToolButton(Icons.Default.Redo, "Повторить") { viewModel.redo() }
                LockToolButton(Icons.Default.Delete, "Очистить") { viewModel.clearCanvas() }
            }
        }

        // Bottom palette
        if (showToolbar) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ART_PALETTE.forEach { color ->
                        val sel = !isEraser && selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(if (sel) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                                .clickable { viewModel.setColor(color); viewModel.setEraser(false) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LinearScale, null, Modifier.size(20.dp), tint = Color.Gray)
                    Slider(
                        value = strokeWidth,
                        onValueChange = { viewModel.setStrokeWidth(it) },
                        valueRange = 2f..32f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { viewModel.setEraser(!isEraser) },
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (isEraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        Icon(Icons.Default.AutoFixNormal, "Ластик",
                            tint = if (isEraser) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f))
    ) {
        Icon(icon, description, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
    }
}
