package com.example.loveapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * Circular user avatar.
 * - Shows a real photo loaded via Coil if [imageUrl] is non-blank.
 * - Falls back to a gradient circle with initials extracted from [displayName].
 *
 * @param imageUrl    Full URL to the profile image (absolute http/https), or null/empty for initials.
 * @param displayName Name used to derive initials and gradient colour.
 * @param size        Diameter of the circle in dp.
 * @param borderColor Optional border colour. Pass Color.Unspecified (the default) to skip.
 * @param isUploading When true, shows a small progress spinner overlay.
 * @param onClick     Optional click handler. If null the composable is not clickable.
 */
@Composable
fun UserAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderColor: Color = Color.Unspecified,
    isUploading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val initials = remember(displayName) {
        displayName
            .trim()
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    // Deterministic pastel gradient based on the first character code
    val hue = remember(displayName) { ((displayName.firstOrNull()?.code ?: 0) * 47) % 360 }
    val gradientStart = remember(hue) { Color.hsl(hue.toFloat(), 0.55f, 0.60f) }
    val gradientEnd   = remember(hue) { Color.hsl(((hue + 40) % 360).toFloat(), 0.65f, 0.48f) }
    val gradient = remember(gradientStart, gradientEnd) {
        Brush.linearGradient(listOf(gradientStart, gradientEnd))
    }

    val fontSize = remember(size) { (size.value * 0.38f).sp }

    var mod = modifier
        .size(size)
        .clip(CircleShape)

    if (borderColor != Color.Unspecified) {
        mod = mod.border(1.5.dp, borderColor, CircleShape)
    }
    if (onClick != null) {
        mod = mod.clickable(onClick = onClick)
    }

    Box(modifier = mod, contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            ) {
                when (painter.state) {
                    is coil.compose.AsyncImagePainter.State.Loading,
                    is coil.compose.AsyncImagePainter.State.Error -> {
                        // Show initials while loading / on error
                        Box(
                            modifier = Modifier.matchParentSize().background(gradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }
        } else {
            // No URL → initials fallback
            Box(
                modifier = Modifier.matchParentSize().background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Uploading overlay
        if (isUploading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.45f),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
