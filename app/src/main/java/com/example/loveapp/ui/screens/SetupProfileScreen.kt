package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.utils.rememberResponsiveConfig
import com.example.loveapp.viewmodel.AuthViewModel

@Composable
fun SetupProfileScreen(
    onSetupComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authSuccess by viewModel.authSuccessEvent.collectAsState()
    val r = rememberResponsiveConfig()

    // Pre-fill display name from Google account
    var displayName by remember(currentUser) {
        mutableStateOf(currentUser?.displayName ?: "")
    }
    var selectedGender by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authSuccess) {
        if (authSuccess) {
            viewModel.clearAuthSuccessEvent()
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = r.hPadding, vertical = r.vSpacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = r.maxContentWidth).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Завершение регистрации",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = r.titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(r.vSpacingSmall))

            Text(
                text = "Укажи как тебя называть и выбери пол",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = r.bodyFontSize,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(r.vSpacingLarge))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Имя (как отображается)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(r.vSpacingMedium))

            Text(
                text = "Пол",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = r.bodyFontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = r.vSpacingSmall)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(r.vSpacingMedium)
            ) {
                listOf("male" to "Мужской", "female" to "Женский").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedGender == value,
                        onClick = { selectedGender = value },
                        label = {
                            Text(
                                label,
                                fontSize = r.bodyFontSize,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(r.vSpacingMedium))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = r.captionFontSize,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(r.vSpacingMedium))

            Button(
                onClick = {
                    if (selectedGender != null) {
                        viewModel.setupProfile(displayName.trim(), selectedGender!!)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(r.buttonHeight),
                enabled = !isLoading && displayName.isNotBlank() && selectedGender != null
            ) {
                Text(
                    text = if (isLoading) "Сохранение..." else "Продолжить",
                    fontSize = r.bodyFontSize
                )
            }
        }
    }
}
