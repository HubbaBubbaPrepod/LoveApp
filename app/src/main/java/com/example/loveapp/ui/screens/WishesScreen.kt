package com.example.loveapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.runtime.remember
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
import com.example.loveapp.utils.rememberResponsiveConfig
import com.example.loveapp.viewmodel.WishViewModel

private val WISH_ICON_MAP: Map<String, ImageVector> = mapOf(
    "gift"      to Icons.Default.CardGiftcard,
    "heart"     to Icons.Default.Favorite,
    "star"      to Icons.Default.Star,
    "home"      to Icons.Default.Home,
    "phone"     to Icons.Default.Smartphone,
    "music"     to Icons.Default.MusicNote,
    "book"      to Icons.Default.AutoStories,
    "travel"    to Icons.Default.FlightTakeoff,
    "games"     to Icons.Default.SportsEsports,
    "cart"      to Icons.Default.ShoppingCart,
    "art"       to Icons.Default.Palette,
    "mic"       to Icons.Default.Mic,
    "camera"    to Icons.Default.PhotoCamera,
    "diamond"   to Icons.Default.Diamond,
    "sparkle"   to Icons.Default.AutoAwesome,
    "fitness"   to Icons.Default.FitnessCenter,
    "car"       to Icons.Default.DirectionsCar,
    "celebrate" to Icons.Default.Celebration,
    "beach"     to Icons.Default.BeachAccess,
    "flowers"   to Icons.Default.LocalFlorist,
    "spa"       to Icons.Default.Spa,
    "movie"     to Icons.Default.Theaters,
    "cafe"      to Icons.Default.LocalCafe,
)

private val WISH_LIGHT_BGS = listOf(
    Color(0xFFFFF0F3), Color(0xFFF0F4FF), Color(0xFFF0FFF4),
    Color(0xFFFFF9F0), Color(0xFFF5F0FF), Color(0xFFFBFFF0)
)
private val WISH_DARK_BGS = listOf(
    Color(0xFF3A2C2E), Color(0xFF1E2A38), Color(0xFF1C3228),
    Color(0xFF3A2D1E), Color(0xFF2A1E3A), Color(0xFF2A3214)
)
private val WISH_IMAGE_GRADIENT = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onNavigateToWish: (Int) -> Unit,
    viewModel: WishViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val r = rememberResponsiveConfig()
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
        contentWindowInsets = WindowInsets.navigationBars,
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
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 16.dp).size(52.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
                        columns = GridCells.Fixed(r.gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(r.hPadding),
                        verticalArrangement = Arrangement.spacedBy(r.vSpacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(r.vSpacingMedium)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "wish-tile-shadow"
    )

    val background = MaterialTheme.colorScheme.background
    val isDark = remember(background) { background.luminance() < 0.5f }

    // Cycling pastel palette (lists are top-level constants)
    val colorIdx = wish.id % WISH_LIGHT_BGS.size
    val tileBg = remember(isDark, colorIdx) { if (isDark) WISH_DARK_BGS[colorIdx] else WISH_LIGHT_BGS[colorIdx] }
    val titleColor = remember(isDark) { if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E) }
    val subColor = remember(isDark) { if (isDark) Color(0xFF8E8E93) else Color(0xFF636366) }

    val hasImage = wish.imageUrls.orEmpty().isNotBlank()
    val thumbUrl = remember(wish.imageUrls) { wish.imageUrls.orEmpty().split(",").firstOrNull { it.isNotBlank() } }
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
                interactionSource = interactionSource
            ) {
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
                    .background(WISH_IMAGE_GRADIENT)
            )
        }

        // Content column (always shown; on image tiles text is white)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (displayEmoji != null) {
                    val wishIcon = WISH_ICON_MAP[displayEmoji]
                    if (wishIcon != null) {
                        Icon(
                            imageVector = wishIcon,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp).padding(bottom = 6.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = displayEmoji,
                            fontSize = 38.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
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
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp).size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface
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

