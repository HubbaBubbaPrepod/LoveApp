package com.example.loveapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.loveapp.R
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.navigation.Screen
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.WishViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onNavigateToWish: (Int) -> Unit,
    viewModel: WishViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val wishes by viewModel.wishes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        if (currentBackStackEntry?.destination?.route == Screen.Wishes.route) {
            viewModel.loadWishes()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = stringResource(R.string.wishes),
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
                isLoading && wishes.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                wishes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 52.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_wishes),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.create_first_wish),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(wishes) { wish ->
                            WishIOSTile(
                                wish = wish,
                                onClick = { onNavigateToWish(wish.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // FAB — new wish
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigateToWish(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New wish",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun WishIOSTile(wish: WishResponse, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val shadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "wish-tile-shadow"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Cycling pastel palette
    val lightBgs = listOf(
        Color(0xFFFFF0F3), Color(0xFFF0F4FF), Color(0xFFF0FFF4),
        Color(0xFFFFF9F0), Color(0xFFF5F0FF), Color(0xFFFBFFF0)
    )
    val darkBgs = listOf(
        Color(0xFF3A2C2E), Color(0xFF1E2A38), Color(0xFF1C3228),
        Color(0xFF3A2D1E), Color(0xFF2A1E3A), Color(0xFF2A3214)
    )
    val colorIdx = wish.id % lightBgs.size
    val tileBg = if (isDark) darkBgs[colorIdx] else lightBgs[colorIdx]
    val titleColor = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val subColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

    val hasImage = wish.imageUrls.isNotBlank()
    val thumbUrl = wish.imageUrls.split(",").firstOrNull { it.isNotBlank() }
    val displayEmoji = wish.emoji.orEmpty().ifBlank { null }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(tileBg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                isPressed = true
                onClick()
            }
    ) {
        if (hasImage) {
            // Image tile: photo + gradient overlay
            AsyncImage(
                model = thumbUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )
            // Show emoji as overlay badge on image
            if (displayEmoji != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(
                            Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(displayEmoji, fontSize = 20.sp)
                }
            }
        }

        // Content column (always shown; on image tiles text is white)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (!hasImage && displayEmoji != null) {
                    Text(
                        text = displayEmoji,
                        fontSize = 38.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = wish.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasImage) Color.White else titleColor,
                        maxLines = if (displayEmoji != null) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    if (wish.isPrivate) {
                        Text(
                            text = "🔒",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                        )
                    }
                }
                if (wish.description.isNotBlank()) {
                    Text(
                        text = wish.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasImage) Color.White.copy(alpha = 0.8f) else subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (!wish.displayName.isNullOrBlank()) {
                Text(
                    text = wish.displayName,
                    fontSize = 10.sp,
                    color = (if (hasImage) Color.White else subColor).copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

