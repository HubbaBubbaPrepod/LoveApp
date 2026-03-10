package com.example.loveapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveapp.ui.components.IOSTopAppBar
import java.io.ByteArrayOutputStream

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    onNavigateBack: () -> Unit,
    onSendDrawing: (ByteArray) -> Unit
) {
    val paths = remember { mutableStateListOf<DrawPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val colors = listOf(
        Color.Black, Color.Red, Color(0xFFFF6B9D), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFF9C27B0), Color.White
    )

    // Canvas dimensions for bitmap export
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Рисунок",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Drawing Canvas ──
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
                    .pointerInput(selectedColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // Force recomposition
                                currentPath = currentPath?.let { Path().apply { addPath(it) } }
                            },
                            onDragEnd = {
                                currentPath?.let {
                                    paths.add(DrawPath(it, selectedColor, strokeWidth))
                                }
                                currentPath = null
                            }
                        )
                    }
            ) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()

                // Draw completed paths
                paths.forEach { dp ->
                    drawPath(
                        path = dp.path,
                        color = dp.color,
                        style = Stroke(
                            width = dp.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                // Draw current active path
                currentPath?.let { p ->
                    drawPath(
                        path = p,
                        color = selectedColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // ── Toolbar ──
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    // Color palette
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(if (c == selectedColor) 36.dp else 28.dp)
                                    .background(
                                        c,
                                        CircleShape
                                    )
                                    .then(
                                        if (c == selectedColor) Modifier.background(
                                            Color.Transparent,
                                            CircleShape
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { selectedColor = c },
                                    modifier = Modifier.size(36.dp)
                                ) {}
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Stroke width slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Тонкий", fontSize = 11.sp)
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..24f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("Толстый", fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Undo
                        OutlinedButton(
                            onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex) }
                        ) {
                            Icon(Icons.Default.Undo, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отменить")
                        }
                        // Clear
                        OutlinedButton(onClick = { paths.clear() }) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Очистить")
                        }
                        // Send
                        Button(
                            onClick = {
                                if (paths.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                                    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.WHITE)
                                    paths.forEach { dp ->
                                        val paint = android.graphics.Paint().apply {
                                            color = dp.color.hashCode()
                                            style = android.graphics.Paint.Style.STROKE
                                            strokeWidth = dp.strokeWidth
                                            strokeCap = android.graphics.Paint.Cap.ROUND
                                            strokeJoin = android.graphics.Paint.Join.ROUND
                                            isAntiAlias = true
                                        }
                                        canvas.drawPath(dp.path.asAndroidPath(), paint)
                                    }
                                    val baos = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                                    bitmap.recycle()
                                    onSendDrawing(baos.toByteArray())
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отправить")
                        }
                    }
                }
            }
        }
    }
}
