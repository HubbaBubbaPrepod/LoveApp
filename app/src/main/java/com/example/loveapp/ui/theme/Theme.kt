package com.example.loveapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PrimaryPinkLight,
    onPrimaryContainer = Color(0xFF4A0020),
    
    secondary = SecondaryPeach,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SecondaryPeach,
    onSecondaryContainer = Color(0xFF4A2B20),
    
    tertiary = TertiaryRose,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = TertiaryRose,
    onTertiaryContainer = Color(0xFF4A1F2F),
    
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    background = BackgroundLight,
    onBackground = TextDark,
    
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFFFE4E4),
    onSurfaceVariant = Color(0xFF7D5E62),
    
    outline = Color(0xFF8D6E73),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPinkLight,
    onPrimary = Color(0xFF5A0029),
    primaryContainer = Color(0xFF7D3A5C),
    onPrimaryContainer = Color(0xFFFFCDE2),
    
    secondary = SecondaryPeachDark,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF8B4B3F),
    onSecondaryContainer = Color(0xFFFFD9CC),
    
    tertiary = TertiaryRoseDark,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9D5A7E),
    onTertiaryContainer = Color(0xFFFFD9EC),
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF5F0E0A),
    errorContainer = Color(0xFF740E0B),
    onErrorContainer = Color(0xFFF9DEDC),
    
    background = BackgroundDark,
    onBackground = TextLight,
    
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = Color(0xFF543D41),
    onSurfaceVariant = Color(0xFFD9C2C3),
    
    outline = Color(0xFFB3A3A4),
)

@Composable
fun LoveAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}