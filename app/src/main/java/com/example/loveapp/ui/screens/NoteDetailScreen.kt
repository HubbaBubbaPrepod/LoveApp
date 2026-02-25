package com.example.loveapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NoteDetailScreen(
    noteId: Int,
    onNavigateBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isReadOnly by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val contentFocusRequester = remember { FocusRequester() }

    val today = remember {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    }

    // Load existing note if editing
    LaunchedEffect(noteId) {
        if (noteId != -1) {
            viewModel.loadNoteById(noteId)
        } else {
            isInitialized = true
        }
    }

    // Populate fields once note is loaded
    LaunchedEffect(currentNote) {
        if (noteId != -1 && currentNote != null && !isInitialized) {
            title = currentNote!!.title
            content = currentNote!!.content
            isPrivate = currentNote!!.isPrivate
            // Read-only if we know the owner and it's not us
            val ownerId = currentNote!!.userId
            if (currentUserId != null && ownerId != currentUserId) {
                isReadOnly = true
            }
            isInitialized = true
        }
    }

    // Navigate back after save succeeds
    LaunchedEffect(successMessage) {
        if (successMessage != null && isSaving) {
            viewModel.clearMessages()
            onNavigateBack()
        }
    }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            isSaving = false
        }
    }

    fun doSave() {
        if (isSaving || isReadOnly) {
            onNavigateBack()
            return
        }
        val hasContent = title.isNotBlank() || content.isNotBlank()
        if (hasContent) {
            isSaving = true
            val finalTitle = title.ifBlank { "Untitled" }
            if (noteId == -1) {
                viewModel.createNote(finalTitle, content, isPrivate)
            } else {
                viewModel.updateNote(noteId, finalTitle, content, isPrivate)
            }
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = !isSaving) { doSave() }

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
                        text = today,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { doSave() },
                        enabled = !isSaving
                    ) {
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
                // Privacy toggle row (own notes only)
                if (!isReadOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPrivate) "Только для меня" else "Видно партнёру",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Только чтение — заметка партнёра",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Title
            BasicTextField(
                value = title,
                onValueChange = { if (!isReadOnly) title = it },
                readOnly = isReadOnly,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 34.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Content
            BasicTextField(
                value = content,
                onValueChange = { if (!isReadOnly) content = it },
                readOnly = isReadOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp)
                    .focusRequester(contentFocusRequester),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "Start writing...",
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
