package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.loveapp.R
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.ui.components.IOSButton
import com.example.loveapp.ui.components.IOSCard
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.components.MoodBubble
import com.example.loveapp.ui.theme.iOSMoodExcellent
import com.example.loveapp.ui.theme.iOSMoodGood
import com.example.loveapp.ui.theme.iOSMoodHappy
import com.example.loveapp.ui.theme.iOSMoodNeutral
import com.example.loveapp.ui.theme.iOSMoodSad
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.viewmodel.MoodViewModel

private val moodEmojiToApi = mapOf(
    "😢" to "Very Bad",
    "😕" to "Bad",
    "😐" to "Neutral",
    "🙂" to "Good",
    "😄" to "Very Good"
)

private val moodColors = mapOf(
    "😢" to iOSMoodSad,
    "😕" to Color(0xFFFF9500),
    "😐" to iOSMoodNeutral,
    "🙂" to iOSMoodHappy,
    "😄" to iOSMoodExcellent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    
    val moods by viewModel.moods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            IOSTopAppBar(
                title = stringResource(R.string.mood),
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
                isLoading && moods.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    stringResource(R.string.how_feeling),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    "How are you feeling today?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                // Mood bubbles in a scrollable row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    moodEmojiToApi.keys.forEach { emoji ->
                                        MoodBubble(
                                            moodLabel = moodEmojiToApi[emoji] ?: "Unknown",
                                            bubbleColor = moodColors[emoji] ?: MaterialTheme.colorScheme.primary,
                                            size = 64.dp,
                                            isSelected = selectedMood == emoji,
                                            emoji = emoji,
                                            onClick = { selectedMood = emoji }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                IOSButton(
                                    text = if (isLoading) "Saving..." else "Save Mood",
                                    onClick = {
                                        selectedMood?.let { emoji ->
                                            moodEmojiToApi[emoji]?.let { viewModel.createMood(it) }
                                            selectedMood = null
                                        }
                                    },
                                    enabled = selectedMood != null && !isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            Column {
                                Text(
                                    stringResource(R.string.recent_moods),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                if (moods.isEmpty()) {
                                    IOSCard {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "😊",
                                                fontSize = 40.sp,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            Text(
                                                stringResource(R.string.no_moods),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        moods.forEach { mood ->
                                            IOSMoodCard(mood = mood)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IOSMoodCard(mood: MoodResponse) {
    IOSCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getMoodEmoji(mood.moodType),
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${stringResource(R.string.mood)}: ${mood.moodType}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DateUtils.formatDateForDisplay(mood.timestamp.take(10)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = getMoodColor(mood.moodType),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

fun getMoodEmoji(moodType: String): String = when (moodType) {
    "Very Bad" -> "😢"
    "Bad" -> "😕"
    "Neutral" -> "😐"
    "Good" -> "🙂"
    "Very Good" -> "😄"
    else -> "😊"
}

fun getMoodColor(moodType: String): Color = when (moodType) {
    "Very Bad" -> iOSMoodSad
    "Bad" -> Color(0xFFFF9500)
    "Neutral" -> iOSMoodNeutral
    "Good" -> iOSMoodHappy
    "Very Good" -> iOSMoodExcellent
    else -> Color.Gray
}
