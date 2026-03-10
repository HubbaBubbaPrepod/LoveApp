package com.example.loveapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.AppLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val isLockEnabled by viewModel.isLockEnabled.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

    Scaffold(
        topBar = {
            IOSTopAppBar(
                title = "Блокировка",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFFFF6B9D)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Защитите приложение",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Установите PIN-код для блокировки приложения",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            if (isLockEnabled) {
                EnabledSection(viewModel)
            } else {
                SetPinSection(viewModel)
            }
        }
    }
}

@Composable
private fun SetPinSection(viewModel: AppLockViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Задайте PIN-код", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            PinField(
                value = pin,
                onValueChange = { pin = it; error = null },
                label = "PIN-код",
                showPin = showPin,
                onToggleVisibility = { showPin = !showPin }
            )

            Spacer(Modifier.height(12.dp))

            PinField(
                value = confirmPin,
                onValueChange = { confirmPin = it; error = null },
                label = "Подтвердите PIN",
                showPin = showPin,
                onToggleVisibility = { showPin = !showPin }
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN должен содержать минимум 4 цифры"
                        pin != confirmPin -> error = "PIN-коды не совпадают"
                        else -> viewModel.setPin(pin)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
            ) {
                Text("Установить", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun EnabledSection(viewModel: AppLockViewModel) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var showChange by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPin by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✅ Блокировка включена", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }

    Spacer(Modifier.height(16.dp))

    // Change PIN
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!showChange) {
                OutlinedButton(
                    onClick = { showChange = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Изменить PIN")
                }
            } else {
                Text("Изменить PIN-код", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                PinField(
                    value = currentPin,
                    onValueChange = { currentPin = it; error = null },
                    label = "Текущий PIN",
                    showPin = showPin,
                    onToggleVisibility = { showPin = !showPin }
                )

                Spacer(Modifier.height(12.dp))

                PinField(
                    value = newPin,
                    onValueChange = { newPin = it; error = null },
                    label = "Новый PIN",
                    showPin = showPin,
                    onToggleVisibility = { showPin = !showPin }
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        when {
                            currentPin.length < 4 -> error = "Введите текущий PIN"
                            newPin.length < 4 -> error = "Новый PIN должен содержать минимум 4 цифры"
                            else -> viewModel.updatePin(currentPin, newPin)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D))
                ) {
                    Text("Сохранить")
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Remove PIN
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            var removePin by remember { mutableStateOf("") }
            var removeError by remember { mutableStateOf<String?>(null) }

            Text("Отключить блокировку", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            PinField(
                value = removePin,
                onValueChange = { removePin = it; removeError = null },
                label = "Введите текущий PIN",
                showPin = showPin,
                onToggleVisibility = { showPin = !showPin }
            )

            if (removeError != null) {
                Spacer(Modifier.height(8.dp))
                Text(removeError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (removePin.length < 4) {
                        removeError = "Введите текущий PIN"
                    } else {
                        viewModel.removePin(removePin)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Отключить")
            }
        }
    }
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPin: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        },
        singleLine = true
    )
}
