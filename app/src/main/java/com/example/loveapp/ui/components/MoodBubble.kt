package com.example.loveapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS-style animated mood bubble
 * Perfect for mood tracking screens with smooth iOS-like animations
 */
@Composable
fun MoodBubble(
    moodLabel: String,
    bubbleColor: Color,
    size: Dp = 80.dp,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    emoji: String = "😊"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else if (isSelected) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "mood-scale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isSelected) 12f else if (isPressed) 4f else 6f,
        animationSpec = tween(durationMillis = 200),
        label = "mood-shadow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .shadow(                // shadow BEFORE clip so it renders outside the clipped boundary
                    elevation = shadowElevation.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(CircleShape)
                .background(bubbleColor.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = moodLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * iOS-style animated pulsing bubble (for notifications, activity)
 */
@Composable
fun PulsingBubble(
    color: Color,
    size: Dp = 60.dp,
    content: @Composable () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(pulseScale)
            .shadow(elevation = 8.dp, shape = CircleShape)  // shadow before clip
            .clip(CircleShape)
            .background(color.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * iOS-style bubble container for activity rings or progress
 */
@Composable
fun ProgressBubble(
    label: String,
    percentage: Float,
    bubbleColor: Color,
    size: Dp = 100.dp,
    onClick: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800),
        label = "progress-animate"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(             // shadow BEFORE clip so it renders outside the clipped boundary
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(CircleShape)
                .background(bubbleColor.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
