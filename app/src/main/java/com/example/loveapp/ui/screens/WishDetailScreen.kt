package com.example.loveapp.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.loveapp.viewmodel.WishViewModel

private val WISH_ICON_OPTIONS: List<Pair<String, ImageVector>> = listOf(
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

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun WishDetailScreen(
    wishId: Int,
    onNavigateBack: () -> Unit,
    viewModel: WishViewModel = hiltViewModel()
) {
    val currentWish by viewModel.currentWish.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPrivate by remember { mutableStateOf(false) }
    var isReadOnly by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf("") }

    val uriHandler = LocalUriHandler.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploading = true
            viewModel.uploadImage(it)
        }
    }

    // Cleanup on screen leave
    DisposableEffect(Unit) {
        onDispose { viewModel.clearCurrentWish() }
    }

    // Load existing wish
    LaunchedEffect(wishId) {
        if (wishId != -1) viewModel.loadWishById(wishId)
        else isInitialized = true
    }

    // Populate fields from loaded wish
    LaunchedEffect(currentWish) {
        if (wishId != -1 && currentWish != null && !isInitialized) {
            title = currentWish!!.title
            description = currentWish!!.description
            imageUrls = currentWish!!.imageUrls.orEmpty().split(",").filter { it.isNotBlank() }
            emoji = currentWish!!.emoji.orEmpty()
            isPrivate = currentWish!!.isPrivate
            if (currentUserId != null && currentWish!!.userId != currentUserId) {
                isReadOnly = true
            }
            isInitialized = true
        }
    }

    // Apply uploaded image URL when upload finishes
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let {
            imageUrls = imageUrls + it
            isUploading = false
        }
    }

    // Navigate back after save succeeds
    LaunchedEffect(successMessage) {
        if (successMessage != null && isSaving) {
            viewModel.clearMessages()
            onNavigateBack()
        }
    }

    // Show errors in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            isSaving = false
            isUploading = false
        }
    }

    fun doSave() {
        if (isSaving || isReadOnly) { onNavigateBack(); return }
        val hasContent = title.isNotBlank() || description.isNotBlank()
        if (hasContent) {
            isSaving = true
            val finalTitle = title.ifBlank { "Untitled" }
            val urlsJoined = imageUrls.joinToString(",").ifBlank { null }
            if (wishId == -1) {
                viewModel.createWish(finalTitle, description, isPrivate = isPrivate, imageUrls = urlsJoined, emoji = emoji.ifBlank { null })
            } else {
                viewModel.updateWish(wishId, finalTitle, description, isPrivate, urlsJoined, emoji.ifBlank { null })
            }
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = !isSaving) { doSave() }

    // Detect URLs in description text
    val detectedLinks = remember(description) {
        val matcher = Patterns.WEB_URL.matcher(description)
        val links = mutableListOf<String>()
        while (matcher.find()) {
            val match = matcher.group() ?: continue
            if (match.isNotEmpty()) links.add(match)
        }
        links
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { doSave() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (wishId == -1) "New Wish" else if (isReadOnly) "Partner's Wish" else "Edit Wish",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { doSave() }, enabled = !isSaving) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = if (isReadOnly) "Close" else "Done",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Privacy row (own) or read-only badge (partner's)
                if (!isReadOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPrivate) "🔒 Private (only you)" else "👀 Visible to partner",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    }
                } else {
                    Text(
                        text = "👁 Read only — partner's wish",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Photos strip
            if (imageUrls.isNotEmpty() || !isReadOnly) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (imageUrls.isNotEmpty()) 180.dp else 52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(imageUrls) { index, url ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .fillMaxHeight()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (!isReadOnly) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable { imageUrls = imageUrls.toMutableList().also { it.removeAt(index) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    if (!isReadOnly) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(if (imageUrls.isEmpty()) 120.dp else 64.dp)
                                    .fillMaxHeight()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { if (!isUploading) imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add photo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        if (imageUrls.isEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Add photos",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Icon picker
            val selectedIconVector = remember(emoji) {
                WISH_ICON_OPTIONS.find { it.first == emoji }?.second
                    ?: Icons.Default.AutoAwesome
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = selectedIconVector,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Иконка",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (emoji.isNotBlank() && !isReadOnly) {
                            TextButton(
                                onClick = { emoji = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Сбросить",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (!isReadOnly) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WISH_ICON_OPTIONS) { (key, icon) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (emoji == key)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                                    )
                                    .clickable { emoji = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = key,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (emoji == key)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Title
            BasicTextField(
                value = title,
                onValueChange = { if (!isReadOnly) title = it },
                readOnly = isReadOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                textStyle = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 32.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
            Divider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))

            // Description
            BasicTextField(
                value = description,
                onValueChange = { if (!isReadOnly) description = it },
                readOnly = isReadOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .defaultMinSize(minHeight = 300.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (description.isEmpty()) {
                            Text(
                                text = "Write your wish...",
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Detected links section
            if (detectedLinks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "🔗 Links",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    detectedLinks.forEach { link ->
                        Text(
                            text = link,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clickable {
                                    try {
                                        val url = if (!link.startsWith("http")) "https://$link" else link
                                        uriHandler.openUri(url)
                                    } catch (_: Exception) {}
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
