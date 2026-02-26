package com.example.loveapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============= iOS Light Theme - Glass Morphism Style =============
private val LightColorScheme = lightColorScheme(
    primary = iOSPrimaryPink,
    onPrimary = Color.White,
    primaryContainer = iOSPrimaryPinkVeryLight,
    onPrimaryContainer = iOSTextPrimary,
    
    secondary = iOSSecondaryPeach,
    onSecondary = Color.White,
    secondaryContainer = iOSSecondaryPeachLight,
    onSecondaryContainer = iOSTextPrimary,
    
    tertiary = iOSTertiaryCoral,
    onTertiary = Color.White,
    tertiaryContainer = iOSTerminaryCoralLight,
    onTertiaryContainer = iOSTextPrimary,
    
    error = iOSButtonDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E2),
    onErrorContainer = iOSButtonDanger,
    
    background = iOSBackgroundLight,
    onBackground = iOSTextPrimary,
    
    // iOS glass effect - semi-transparent white surface
    surface = iOSCardBackground.copy(alpha = 0.88f),
    onSurface = iOSTextPrimary,
    surfaceVariant = iOSPrimaryPinkVeryLight.copy(alpha = 0.6f),
    onSurfaceVariant = iOSTextSecondary,
    
    outline = iOSBorder,
)

// ============= iOS Dark Theme =============
private val DarkColorScheme = darkColorScheme(
    primary = iOSPrimaryPinkLight,
    onPrimary = iOSDarkBackground,
    primaryContainer = iOSDarkCardBg,
    onPrimaryContainer = iOSPrimaryPinkLight,
    
    secondary = iOSSecondaryPeach,
    onSecondary = iOSDarkBackground,
    secondaryContainer = iOSDarkCardBg,
    onSecondaryContainer = iOSSecondaryPeach,
    
    tertiary = iOSTertiaryCoral,
    onTertiary = iOSDarkBackground,
    tertiaryContainer = iOSDarkCardBg,
    onTertiaryContainer = iOSTertiaryCoral,
    
    error = iOSButtonDanger,
    onError = iOSDarkBackground,
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = Color(0xFFFFB4AB),
    
    background = iOSDarkBackground,
    onBackground = iOSDarkTextWhite,
    
    surface = iOSDarkCardBg.copy(alpha = 0.88f),
    onSurface = iOSDarkTextWhite,
    surfaceVariant = iOSDarkCardBg.copy(alpha = 0.6f),
    onSurfaceVariant = iOSDarkTextGray,
    
    outline = iOSBorderDark,
)

@Composable
fun LoveAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (view.context as Activity).window
            // Force edge-to-edge on ALL devices including OEM ROMs (Samsung, MIUI, etc.)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Transparent bars so composable backgrounds bleed through properly
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // Icon tint: light theme → dark icons on pink bg, dark theme → light icons
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = iOSTypography,
        shapes = iOSShapes,
        content = content
    )
}