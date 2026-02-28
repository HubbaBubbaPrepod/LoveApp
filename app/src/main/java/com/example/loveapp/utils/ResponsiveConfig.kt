package com.example.loveapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen-width breakpoints:
 *   compact  < 360dp   — small phones (older budget devices)
 *   normal   360–599   — typical phones (Redmi Note 12S = ~393dp)
 *   medium   600–839   — large phones / small tablets
 *   expanded ≥ 840dp   — tablets
 */
enum class ScreenSize { COMPACT, NORMAL, MEDIUM, EXPANDED }

data class ResponsiveConfig(
    val screenSize: ScreenSize,
    /** Horizontal screen padding for top-level containers */
    val hPadding: Dp,
    /** Standard vertical spacing between sections */
    val vSpacingLarge: Dp,
    val vSpacingMedium: Dp,
    val vSpacingSmall: Dp,
    /** Standard button height */
    val buttonHeight: Dp,
    /** Height for text input fields */
    val inputHeight: Dp,
    /** Max content width — centres content on tablets */
    val maxContentWidth: Dp,
    /** Number of grid columns (Dashboard tiles, Notes grid, Wishes grid) */
    val gridColumns: Int,
    /** Font size for screen titles / hero headings */
    val titleFontSize: TextUnit,
    /** Font size for section headings */
    val headingFontSize: TextUnit,
    /** Font size for body text */
    val bodyFontSize: TextUnit,
    /** Font size for captions / labels */
    val captionFontSize: TextUnit,
    /** Size for icons used in tiles and navigation */
    val iconSize: Dp,
    /** Corner radius for cards */
    val cardCornerRadius: Dp,
) {
    val isCompact: Boolean get() = screenSize == ScreenSize.COMPACT
    val isTablet: Boolean  get() = screenSize == ScreenSize.EXPANDED
    val isMediumOrLarger: Boolean get() = screenSize == ScreenSize.MEDIUM || isTablet
}

@Composable
fun rememberResponsiveConfig(): ResponsiveConfig {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 360 -> ResponsiveConfig(
                screenSize      = ScreenSize.COMPACT,
                hPadding        = 14.dp,
                vSpacingLarge   = 16.dp,
                vSpacingMedium  = 10.dp,
                vSpacingSmall   = 5.dp,
                buttonHeight    = 42.dp,
                inputHeight     = 48.dp,
                maxContentWidth = 360.dp,
                gridColumns     = 2,
                titleFontSize   = 22.sp,
                headingFontSize = 16.sp,
                bodyFontSize    = 12.sp,
                captionFontSize = 10.sp,
                iconSize        = 22.dp,
                cardCornerRadius = 10.dp,
            )
            widthDp < 600 -> ResponsiveConfig(
                screenSize      = ScreenSize.NORMAL,
                hPadding        = 20.dp,
                vSpacingLarge   = 28.dp,
                vSpacingMedium  = 16.dp,
                vSpacingSmall   = 8.dp,
                buttonHeight    = 50.dp,
                inputHeight     = 56.dp,
                maxContentWidth = 520.dp,
                gridColumns     = 2,
                titleFontSize   = 28.sp,
                headingFontSize = 18.sp,
                bodyFontSize    = 14.sp,
                captionFontSize = 12.sp,
                iconSize        = 26.dp,
                cardCornerRadius = 14.dp,
            )
            widthDp < 840 -> ResponsiveConfig(
                screenSize      = ScreenSize.MEDIUM,
                hPadding        = 32.dp,
                vSpacingLarge   = 36.dp,
                vSpacingMedium  = 22.dp,
                vSpacingSmall   = 11.dp,
                buttonHeight    = 54.dp,
                inputHeight     = 60.dp,
                maxContentWidth = 640.dp,
                gridColumns     = 3,
                titleFontSize   = 32.sp,
                headingFontSize = 20.sp,
                bodyFontSize    = 15.sp,
                captionFontSize = 13.sp,
                iconSize        = 30.dp,
                cardCornerRadius = 16.dp,
            )
            else -> ResponsiveConfig(
                screenSize      = ScreenSize.EXPANDED,
                hPadding        = 56.dp,
                vSpacingLarge   = 48.dp,
                vSpacingMedium  = 28.dp,
                vSpacingSmall   = 14.dp,
                buttonHeight    = 58.dp,
                inputHeight     = 64.dp,
                maxContentWidth = 760.dp,
                gridColumns     = 4,
                titleFontSize   = 36.sp,
                headingFontSize = 22.sp,
                bodyFontSize    = 16.sp,
                captionFontSize = 14.sp,
                iconSize        = 34.dp,
                cardCornerRadius = 20.dp,
            )
        }
    }
}
