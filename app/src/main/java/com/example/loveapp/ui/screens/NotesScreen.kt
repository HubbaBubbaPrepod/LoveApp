package com.example.loveapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.loveapp.R
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.navigation.Screen
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.components.UserAvatar
import com.example.loveapp.utils.rememberResponsiveConfig
import com.example.loveapp.viewmodel.NoteViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NotesScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (Int) -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val r = rememberResponsiveConfig()
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Reload notes each time this screen becomes the active destination
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        if (currentBackStackEntry?.destination?.route == Screen.Notes.route) {
            viewModel.loadNotes()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            IOSTopAppBar(
                title = stringResource(R.string.notes),
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                isLoading && notes.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                notes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp).size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = stringResource(R.string.no_notes),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.create_first_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(r.gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(r.hPadding),
                        verticalArrangement = Arrangement.spacedBy(r.vSpacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(r.vSpacingMedium)
                    ) {
                        items(notes) { note ->
                            NoteIOSTile(
                                note = note,
                                onClick = { onNavigateToNote(note.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // FAB — new note
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigateToNote(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New note",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun NoteIOSTile(
    note: NoteResponse,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "note-tile-scale"
    )

    // Compute isDark once when the theme background changes – not every recomposition
    val background = MaterialTheme.colorScheme.background
    val isDark = remember(background) { background.luminance() < 0.5f }
    val tileBackground = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFDE7)
    val lineColor  = remember(isDark) { if (isDark) Color(0xFF48484A).copy(alpha = 0.8f) else Color(0xFFB0BEC5).copy(alpha = 0.4f) }
    val titleColor = remember(isDark) { if (isDark) Color(0xFFF2F2F7) else Color(0xFF37474F) }
    val contentColor = remember(isDark) { if (isDark) Color(0xFF8E8E93) else Color(0xFF546E7A) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            // Scale applied via graphicsLayer (no modifier chain rebuild per frame)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 18f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor   = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(tileBackground)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
    ) {

        // Ruled lines background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 22.dp.toPx()
            val startY = 58.dp.toPx()
            var y = startY
            while (y < size.height - 8.dp.toPx()) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(16.dp.toPx(), y),
                    end = androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
                y += lineSpacing
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: title row + content
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    if (note.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp).size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }

            // Bottom: author name
            if (!note.displayName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    UserAvatar(
                        imageUrl    = note.userAvatar,
                        displayName = note.displayName ?: "",
                        size        = 14.dp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = note.displayName ?: "",
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}



