package com.example.loveapp.ui.theme

import androidx.compose.ui.graphics.Color

// ============= iOS Light Theme - Romantic Pastels =============

// Primary colors - Romantic pink tones
val iOSPrimaryPink = Color(0xFFFF6B9D)       // Main romantic pink
val iOSPrimaryPinkLight = Color(0xFFFFB6D9) // Light pink
val iOSPrimaryPinkVeryLight = Color(0xFFFDE4F0) // Very light pink

// Secondary colors - Peachy/coral tones  
val iOSSecondaryPeach = Color(0xFFFFB3A2)   // Soft peach
val iOSSecondaryPeachLight = Color(0xFFFFD9CE) // Very light peach
val iOSSecondaryRed = Color(0xFFFF6B9D)     // Romantic red
val iOSSecondaryRedLight = Color(0xFFFDD5E5) // Light red

// Tertiary colors - Purple/rose tones
val iOSTertiaryPurple = Color(0xFFB8A3D9)   // Soft purple  
val iOSTertiaryPurpleLight = Color(0xFFE8D9F2) // Light purple
val iOSTertiaryCoral = Color(0xFFFF9B85)    // Coral
val iOSTerminaryCoralLight = Color(0xFFFFD9CE) // Light coral

// Surface & Background - iOS Glass effect
val iOSGlassWhite = Color(0xFFFAFBFD)       // Glass white (like iOS widgets)
val iOSBackgroundWhite = Color(0xFFFEFEFE)  // Pure background white
val iOSSurfaceWhite = Color(0xFFFFFFFF)     // Content surface
val iOSBackgroundLight = Color(0xFFF2F2F7)  // Light gray background (iOS style)
val iOSCardBackground = Color(0xFFFFFFFF)   // Card background with 88-90% alpha

// ============= iOS Dark Theme Variants =============
val iOSDarkBackground = Color(0xFF0A0A0B)
val iOSDarkSurface = Color(0xFF1C1C1E)
val iOSDarkCardBg = Color(0xFF2C2C2E)
val iOSDarkTextWhite = Color(0xFFF2F2F7)
val iOSDarkTextGray = Color(0xFF8E8E93)

// ============= Mood Tracker Colors (iOS Style) =============
val iOSMoodExcellent = Color(0xFFFFD700)    // Gold - Excellent
val iOSMoodHappy = Color(0xFFFFA500)        // Orange - Happy
val iOSMoodGood = Color(0xFF81C784)         // Green - Good  
val iOSMoodNeutral = Color(0xFFC0C0C0)      // Silver - Neutral
val iOSMoodSad = Color(0xFF64B5F6)          // Blue - Sad
val iOSMoodRomantic = Color(0xFFFF6B9D)     // Pink - Romantic
val iOSMoodNervous = Color(0xFFFFB74D)      // Orange - Nervous
val iOSMoodTired = Color(0xFF999999)        // Gray - Tired

// ============= Cycle Tracking Colors =============
val iOSCycleMenstruation = Color(0xFFFF6B9D)    // Pink - Period
val iOSCycleFertile = Color(0xFFFFA500)         // Orange - Fertile
val iOSCycleOvulation = Color(0xFFFFCD39)       // Gold - Ovulation
val iOSCycleLuteal = Color(0xFF9C27B0)          // Purple - Luteal
val iOSCycleFollicular = Color(0xFF4CAF50)      // Green - Follicular

// ============= Activity & Status Colors =============
val iOSActivityRed = Color(0xFFFF6B6B)           // Activity red
val iOSActivityOrange = Color(0xFFFF9F43)        // Activity orange
val iOSActivityGreen = Color(0xFF2ED573)         // Activity green
val iOSActivityBlue = Color(0xFF1E90FF)          // Activity blue
val iOSActivityPurple = Color(0xFFB366FF)        // Activity purple

// ============= Text Colors =============
val iOSTextPrimary = Color(0xFF000000)          // Black text
val iOSTextSecondary = Color(0xFF3C3C43).copy(alpha = 0.6f) // Gray text (60% opacity)
val iOSTextTertiary = Color(0xFF3C3C43).copy(alpha = 0.3f)  // Gray text (30% opacity)
val iOSTextWhite = Color(0xFFFFFFFF)             // White text

// ============= Separators & Borders =============
val iOSBorder = Color(0xFF3C3C43).copy(alpha = 0.1f) // Light separator
val iOSBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.1f) // Light separator (dark mode)

// ============= Compatibility Aliases for Theme =============
val PrimaryPink = iOSPrimaryPink
val PrimaryPinkLight = iOSPrimaryPinkLight
val SecondaryPeach = iOSSecondaryPeach
val TertiaryRose = iOSTertiaryCoral
val SurfaceLight = iOSSurfaceWhite
val BackgroundLight = iOSBackgroundLight
val PastelBackground = iOSBackgroundLight
val GlassWhite = iOSGlassWhite
val PastelPink = iOSPrimaryPinkVeryLight
val PastelPeach = iOSSecondaryPeachLight
val PastelMint = Color(0xFFD9F2E8) // Pastel mint green
val TextDark = iOSTextPrimary
val TextLight = iOSDarkTextWhite
val TextMuted = iOSTextSecondary

// Dark mode compatibility
val PrimaryPinkDark = iOSPrimaryPink
val PrimaryPinkDarkLight = iOSPrimaryPinkLight
val SecondaryPeachDark = iOSSecondaryPeach
val TertiaryRoseDark = iOSTertiaryCoral
val SurfaceDark = iOSDarkCardBg
val BackgroundDark = iOSDarkBackground

// Additional accent colors
val AccentRed = iOSActivityRed
val AccentPurple = iOSActivityPurple
val AccentOrange = iOSActivityOrange
val AccentBlue = iOSActivityBlue
val AccentGreen = iOSActivityGreen

// Mood bubble colors (compatibility)
val MoodExcellent = iOSMoodExcellent
val MoodGood = iOSMoodGood
val MoodNeutral = iOSMoodNeutral
val MoodSad = iOSMoodSad
val MoodRomantic = iOSMoodRomantic
val MoodNervous = iOSMoodNervous
val MoodTired = iOSMoodTired

// Cycle tracking colors (compatibility)
val CycleDay1To5 = iOSCycleMenstruation
val CycleFertile = iOSCycleFertile
val CycleOvulation = iOSCycleOvulation
val CycleLuteal = iOSCycleLuteal
val CycleFollicular = iOSCycleFollicular

// ============= Interactive Elements =============
val iOSButtonPrimary = Color(0xFF007AFF)        // iOS Blue (standard button)
val iOSButtonSecondary = Color(0xFF5AC8FA)      // iOS Light Blue
val iOSButtonDanger = Color(0xFFFF3B30)         // iOS Red
val iOSButtonSuccess = Color(0xFF34C759)        // iOS Green
val iOSButtonWarning = Color(0xFFFF9500)        // iOS Orange

