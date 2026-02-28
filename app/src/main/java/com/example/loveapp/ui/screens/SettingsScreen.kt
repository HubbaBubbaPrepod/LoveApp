package com.example.loveapp.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.BuildConfig
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.ui.theme.AccentPurple
import com.example.loveapp.ui.theme.PrimaryPink
import com.example.loveapp.viewmodel.AuthViewModel
import com.example.loveapp.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsOfUse: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isDarkMode           by settingsViewModel.isDarkMode.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val remindersEnabled     by settingsViewModel.remindersEnabled.collectAsState()
    val currentUser          by authViewModel.currentUser.collectAsState()
    val isLoading            by authViewModel.isLoading.collectAsState()
    val isLoggedIn           by authViewModel.isLoggedIn.collectAsState()

    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load profile on open
    LaunchedEffect(Unit) { authViewModel.getProfile() }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            IOSTopAppBar(title = "Настройки", onBackClick = onNavigateBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            //  Profile card 
            item {
                ProfileHeader(
                    displayName = currentUser?.displayName ?: currentUser?.username ?: "",
                    username    = currentUser?.username?.let { "@$it" } ?: "",
                    email       = currentUser?.email ?: "",
                    isLoading   = isLoading && currentUser == null
                )
            }

            //  Appearance 
            item {
                SettingsSectionLabel(text = "Внешний вид")
            }
            item {
                SettingsGroup {
                    SettingsToggleRow(
                        icon        = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        iconTint    = if (isDarkMode) Color(0xFF9C8FD9) else Color(0xFFFFC107),
                        title       = if (isDarkMode) "Тёмная тема" else "Светлая тема",
                        subtitle    = "Переключить оформление приложения",
                        checked     = isDarkMode,
                        onCheckedChange = { settingsViewModel.setDarkMode(it) }
                    )
                }
            }

            //  Notifications 
            item { SettingsSectionLabel(text = "Уведомления") }
            item {
                SettingsGroup {
                    SettingsToggleRow(
                        icon     = if (notificationsEnabled) Icons.Default.Notifications
                                   else Icons.Default.NotificationsOff,
                        iconTint = Color(0xFF4CAF50),
                        title    = "Push-уведомления",
                        subtitle = "Получать уведомления от приложения",
                        checked  = notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
                    )
                    Divider(
                        modifier = Modifier.padding(start = 60.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsToggleRow(
                        icon     = Icons.Default.Sync,
                        iconTint = AccentPurple,
                        title    = "Ежедневные напоминания",
                        subtitle = "Напоминать добавить настроение и активности за день",
                        checked  = remindersEnabled,
                        onCheckedChange = { settingsViewModel.setRemindersEnabled(it) }
                    )
                }
            }

            //  Partner 
            item { SettingsSectionLabel(text = "Партнёр") }
            item {
                SettingsGroup {
                    SettingsActionRow(
                        icon        = Icons.Default.Favorite,
                        iconTint    = PrimaryPink,
                        title       = "Связать с партнёром",
                        subtitle    = "Введите код партнёра или поделитесь своим",
                        isDestructive = false,
                        onClick     = onNavigateToPairing
                    )
                }
            }

            //  Support 
            item { SettingsSectionLabel(text = "Поддержать") }
            item {
                val supportGradient = remember {
                    Brush.horizontalGradient(listOf(PrimaryPink, AccentPurple))
                }
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(supportGradient)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Favorite,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = "Поддержать проект",
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text  = "Если приложение нравится — помоги ему развиваться ❤️",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(supportGradient)
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    // TODO: замени на свою ссылку (Boosty, DonationAlerts и т.д.)
                                    Uri.parse("https://dalink.to/zxchubbabubba")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor   = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FavoriteBorder, null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Поддержать", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            //  About 
            item { SettingsSectionLabel(text = "О приложении") }
            item {
                SettingsGroup {
                    SettingsInfoRow(
                        icon     = Icons.Default.Info,
                        iconTint = Color(0xFF2196F3),
                        title    = "Версия",
                        value    = BuildConfig.VERSION_NAME
                    )
                    Divider(
                        modifier = Modifier.padding(start = 60.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsActionRow(
                        icon     = Icons.Default.Lock,
                        iconTint = Color(0xFF607D8B),
                        title    = "Политика конфиденциальности",
                        subtitle = "Как мы обрабатываем ваши данные",
                        onClick  = onNavigateToPrivacyPolicy
                    )
                    Divider(
                        modifier = Modifier.padding(start = 60.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsActionRow(
                        icon     = Icons.Default.Description,
                        iconTint = Color(0xFF607D8B),
                        title    = "Условия использования",
                        subtitle = "Правила использования приложения",
                        onClick  = onNavigateToTermsOfUse
                    )
                }
            }

            //  Account 
            item { SettingsSectionLabel(text = "Аккаунт") }
            item {
                SettingsGroup {
                    SettingsActionRow(
                        icon        = Icons.Default.ExitToApp,
                        iconTint    = MaterialTheme.colorScheme.error,
                        title       = "Выйти из аккаунта",
                        subtitle    = "Вы выйдете с этого устройства",
                        isDestructive = true,
                        onClick     = { showLogoutDialog = true }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    //  Logout confirmation dialog 
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Выйти из аккаунта?") },
            text  = {
                Text(
                    "Вы уверены, что хотите выйти?\nДля входа снова потребуется ввести данные.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

//  Profile Header 

@Composable
private fun ProfileHeader(
    displayName: String,
    username: String,
    email: String,
    isLoading: Boolean
) {
    val gradient = remember { Brush.linearGradient(listOf(PrimaryPink, AccentPurple)) }
    val initials = remember(displayName) {
        displayName
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 28.dp, bottom = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            } else {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (username.isNotEmpty()) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                if (email.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

//  Section label 

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)
    )
}

//  Grouped card 

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

//  Toggle row 

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor      = Color.White,
                checkedTrackColor      = PrimaryPink,
                uncheckedThumbColor    = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor    = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

//  Action row 

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

//  Info row (static value) 

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text  = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
