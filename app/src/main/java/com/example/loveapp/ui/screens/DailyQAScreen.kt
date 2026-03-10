package com.example.loveapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.DailyQAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQAScreen(
    onNavigateBack: () -> Unit,
    viewModel: DailyQAViewModel = hiltViewModel()
) {
    val today by viewModel.today.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var freeTextAnswer by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = { IOSTopAppBar(title = "Вопрос дня", onBackClick = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading && today == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B9D))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Today's Question Card ──
                item {
                    today?.let { data ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Category chip
                                if (data.question.category.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFF6B9D).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            data.question.category,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            color = Color(0xFFFF6B9D),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Question text
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Filled.QuestionMark,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B9D),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        data.question.questionText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 24.sp
                                    )
                                }

                                // Answer Section
                                val hasMyAnswer = data.myAnswer != null

                                if (!hasMyAnswer) {
                                    // Options or free text
                                    val options = data.question.options
                                    if (!options.isNullOrEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            options.forEach { option ->
                                                val isSelected = selectedOption == option
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { selectedOption = option },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) Color(0xFFFF6B9D).copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surface,
                                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder(true)
                                                    else null
                                                ) {
                                                    Row(
                                                        Modifier.padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(option, modifier = Modifier.weight(1f))
                                                        if (isSelected) {
                                                            Icon(
                                                                Icons.Filled.Check,
                                                                null,
                                                                tint = Color(0xFFFF6B9D),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = freeTextAnswer,
                                            onValueChange = { freeTextAnswer = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Ваш ответ...") },
                                            shape = RoundedCornerShape(12.dp),
                                            maxLines = 3
                                        )
                                    }

                                    // Submit button
                                    val answer = if (!data.question.options.isNullOrEmpty()) selectedOption else freeTextAnswer.trim().ifBlank { null }
                                    Button(
                                        onClick = { answer?.let { viewModel.submitAnswer(it) } },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = answer != null && !isSubmitting,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ответить")
                                        }
                                    }
                                } else {
                                    // Already answered — show results
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + slideInVertically()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            // My answer
                                            AnswerBubble(
                                                label = "Ваш ответ",
                                                answer = data.myAnswer!!.answer,
                                                color = Color(0xFFFF6B9D)
                                            )

                                            // Partner answer
                                            if (data.bothAnswered && data.partnerAnswer != null) {
                                                AnswerBubble(
                                                    label = data.partnerAnswer.displayName ?: "Партнёр",
                                                    answer = data.partnerAnswer.answer,
                                                    color = Color(0xFF64B5F6)
                                                )
                                            } else {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                                    )
                                                ) {
                                                    Text(
                                                        "Ожидаем ответ партнёра... 💭",
                                                        modifier = Modifier.padding(14.dp),
                                                        fontSize = 14.sp,
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── History Section ──
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            "История вопросов",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(history) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.category.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFF6B9D).copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                item.category,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                color = Color(0xFFFF6B9D)
                                            )
                                        }
                                    }
                                    Text(item.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }

                                Text(
                                    item.questionText,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item.myAnswer?.let {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFF6B9D).copy(alpha = 0.1f)
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("Вы", fontSize = 11.sp, color = Color(0xFFFF6B9D), fontWeight = FontWeight.Medium)
                                                Text(it, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    item.partnerAnswer?.let {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF64B5F6).copy(alpha = 0.1f)
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("Партнёр", fontSize = 11.sp, color = Color(0xFF64B5F6), fontWeight = FontWeight.Medium)
                                                Text(it, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerBubble(label: String, answer: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(answer, fontSize = 15.sp)
        }
    }
}
