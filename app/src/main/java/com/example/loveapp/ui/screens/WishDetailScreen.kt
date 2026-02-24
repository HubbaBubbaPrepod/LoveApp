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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.loveapp.viewmodel.WishViewModel

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
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isPrivate by remember { mutableStateOf(false) }
    var isReadOnly by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

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
            imageUrl = currentWish!!.imageUrl
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
            imageUrl = it
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
            if (wishId == -1) {
                viewModel.createWish(finalTitle, description, isPrivate = isPrivate, imageUrl = imageUrl)
            } else {
                viewModel.updateWish(wishId, finalTitle, description, isPrivate, imageUrl)
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
        contentWindowInsets = WindowInsets(0),
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
            // Photo area — shrinks to a button bar if no image, expands to full image otherwise
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (!imageUrl.isNullOrBlank()) 220.dp else 52.dp)
                    .background(
                        if (!imageUrl.isNullOrBlank()) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    )
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Wish photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (!isReadOnly) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (imageUrl.isNullOrBlank()) "Add photo" else "Change photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

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
