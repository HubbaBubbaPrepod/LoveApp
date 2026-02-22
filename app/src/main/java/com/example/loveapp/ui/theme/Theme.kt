package com.example.loveapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// iOS Light theme - soft pastels & translucent glass effect
private val LightColorScheme = lightColorScheme(
    primary = PrimaryPink.copy(alpha = 0.9f),
    onPrimary = Color.White,
    primaryContainer = PastelPink,
    onPrimaryContainer = TextDark,
    
    secondary = SecondaryPeach.copy(alpha = 0.9f),
    onSecondary = Color.White,
    secondaryContainer = PastelPeach,
    onSecondaryContainer = TextDark,
    
    tertiary = TertiaryRose.copy(alpha = 0.9f),
    onTertiary = Color.White,
    tertiaryContainer = PastelMint,
    onTertiaryContainer = TextDark,
    
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    background = PastelBackground,
    onBackground = TextDark,
    
    surface = GlassWhite.copy(alpha = 0.88f),
    onSurface = TextDark,
    surfaceVariant = PastelPink.copy(alpha = 0.5f),
    onSurfaceVariant = TextMuted,
    
    outline = TextMuted,
)

// iOS Dark theme - darker pastels
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPinkLight,
    onPrimary = Color(0xFF1A0E12),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = PrimaryPinkLight,
    
    secondary = SecondaryPeachDark.copy(alpha = 0.9f),
    onSecondary = Color(0xFF1A0E12),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = SecondaryPeachDark,
    
    tertiary = TertiaryRoseDark.copy(alpha = 0.9f),
    onTertiary = Color(0xFF1A0E12),
    tertiaryContainer = Color(0xFF333333),
    onTertiaryContainer = TertiaryRoseDark,
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF5F0E0A),
    errorContainer = Color(0xFF740E0B),
    onErrorContainer = Color(0xFFF9DEDC),
    
    background = Color(0xFF1A1A1A),
    onBackground = TextLight,
    
    surface = Color(0xFF2A2A2A).copy(alpha = 0.9f),
    onSurface = TextLight,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = TextMuted.copy(alpha = 0.7f),
    
    outline = TextMuted.copy(alpha = 0.7f),
)

@Composable
fun LoveAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}