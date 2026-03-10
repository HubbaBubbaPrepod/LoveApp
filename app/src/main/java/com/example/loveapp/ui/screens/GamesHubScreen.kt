package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.GameRoundResponse
import com.example.loveapp.data.api.models.GameSessionResponse
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.GamesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(
    onNavigateBack: () -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myUserId by viewModel.myUserId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = if (activeSession != null) gameTitle(activeSession!!.gameType) else "Игры для пар",
                onBackClick = {
                    if (activeSession != null) viewModel.clearActiveSession()
                    else onNavigateBack()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activeSession != null) {
                GameSessionView(
                    session = activeSession!!,
                    myUserId = myUserId,
                    onAnswer = { round, answer -> viewModel.submitAnswer(round, answer) }
                )
            } else if (isLoading && sessions.isEmpty()) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6B9D),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Выберите игру",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    item {
                        GameTypeCard(
                            emoji = "🤔",
                            title = "Что ты выберешь?",
                            desc = "Отвечайте на одинаковые вопросы и узнайте насколько вы совпадаете",
                            color = Color(0xFFFF6B9D),
                            onClick = { viewModel.startGame("would_you_rather") }
                        )
                    }
                    item {
                        GameTypeCard(
                            emoji = "🎯",
                            title = "Правда или Действие",
                            desc = "Классическая игра — выбирайте по очереди и выполняйте задания",
                            color = Color(0xFF7C4DFF),
                            onClick = { viewModel.startGame("truth_or_dare") }
                        )
                    }
                    item {
                        GameTypeCard(
                            emoji = "🧠",
                            title = "Насколько ты меня знаешь?",
                            desc = "Проверьте как хорошо вы знаете друг друга",
                            color = Color(0xFF26A69A),
                            onClick = { viewModel.startGame("quiz") }
                        )
                    }

                    if (sessions.isNotEmpty()) {
                        item {
                            Text(
                                "История игр",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(sessions.take(10), key = { it.id }) { session ->
                            HistoryCard(session) { viewModel.loadSession(session.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTypeCard(emoji: String, title: String, desc: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.08f), Color.White)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Играть", tint = color)
        }
    }
}

@Composable
private fun HistoryCard(session: GameSessionResponse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(gameEmoji(session.gameType), fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gameTitle(session.gameType), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "Счёт: ${session.player1Score} — ${session.player2Score}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (session.compatibilityScore != null) {
                    Text(
                        "Совместимость: ${session.compatibilityScore}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            session.compatibilityScore >= 80 -> Color(0xFF30D158)
                            session.compatibilityScore >= 50 -> Color(0xFFFFD60A)
                            else -> Color(0xFFFF375F)
                        }
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (session.status == "finished") Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else Color(0xFFFF6B9D).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (session.status == "finished") "Завершена" else "Активна",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = if (session.status == "finished") Color(0xFF4CAF50) else Color(0xFFFF6B9D)
                )
            }
        }
    }
}

@Composable
private fun GameSessionView(session: GameSessionResponse, myUserId: Int, onAnswer: (Int, String) -> Unit) {
    val rounds = session.rounds ?: emptyList()
    val isPlayer1 = session.player1Id == myUserId
    // Find the first round where the current user hasn't answered yet
    val currentRound = rounds.find { r ->
        if (isPlayer1) r.player1Answer == null else r.player2Answer == null
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(gameEmoji(session.gameType), fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(gameTitle(session.gameType), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    val currentRoundIdx = rounds.indexOfFirst { r -> if (isPlayer1) r.player1Answer == null else r.player2Answer == null }
                    Text(
                        if (currentRoundIdx == -1) "${rounds.size} / ${rounds.size} ✅" else "Раунд ${currentRoundIdx + 1} / ${rounds.size}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Счёт: ${session.player1Score} — ${session.player2Score}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(0xFFFF6B9D)
                    )
                }
            }
        }

        if (currentRound != null && session.status == "active") {
            item {
                RoundCard(
                    round = currentRound,
                    gameType = session.gameType,
                    onAnswer = onAnswer
                )
            }
        } else if (session.status == "finished") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Игра завершена!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (session.compatibilityScore != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Совместимость: ${session.compatibilityScore}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = when {
                                    session.compatibilityScore >= 80 -> Color(0xFF30D158)
                                    session.compatibilityScore >= 50 -> Color(0xFFFFD60A)
                                    else -> Color(0xFFFF375F)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Completed rounds
        items(rounds.filter { it.player1Answer != null && it.player2Answer != null }) { round ->
            CompletedRoundCard(round)
        }
    }
}

@Composable
private fun RoundCard(round: GameRoundResponse, gameType: String, onAnswer: (Int, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = round.questionText ?: "",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(20.dp))

            if (round.optionA != null && round.optionB != null) {
                // Would-you-rather or choice-based
                Button(
                    onClick = { onAnswer(round.roundNumber, round.optionA) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                ) {
                    Text(round.optionA, modifier = Modifier.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("или", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onAnswer(round.roundNumber, round.optionB) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Text(round.optionB, modifier = Modifier.padding(vertical = 4.dp))
                }
            } else {
                // Open answer (truth/dare/quiz)
                var answer by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Ваш ответ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        cursorColor = Color(0xFFFF6B9D)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { if (answer.isNotBlank()) onAnswer(round.roundNumber, answer) },
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ответить")
                }
            }
        }
    }
}

@Composable
private fun CompletedRoundCard(round: GameRoundResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (round.isMatch == true) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (round.isMatch == true) "✅" else "❌",
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    round.questionText?.take(60) ?: "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Ответы: ${round.player1Answer ?: "—"} / ${round.player2Answer ?: "—"}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

private fun gameTitle(type: String) = when (type) {
    "would_you_rather" -> "Что ты выберешь?"
    "truth_or_dare" -> "Правда или Действие"
    "quiz" -> "Насколько ты меня знаешь?"
    else -> "Игра"
}

private fun gameEmoji(type: String) = when (type) {
    "would_you_rather" -> "🤔"
    "truth_or_dare" -> "🎯"
    "quiz" -> "🧠"
    else -> "🎮"
}
