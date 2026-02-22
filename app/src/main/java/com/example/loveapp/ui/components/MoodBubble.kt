package com.example.loveapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoodBubble(
    mood: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) color else color.copy(alpha = 0.6f),
        animationSpec = tween(300), label = "bubbleColor"
    )
    
    val animatedSize by animateDpAsState(
        targetValue = if (isSelected) 75.dp else 60.dp,
        animationSpec = tween(300), label = "bubbleSize"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(300), label = "bubbleScale"
    )

    Box(
        modifier = modifier
            .size(animatedSize)
            .scale(animatedScale)
            .clip(CircleShape)
            .shadow(elevation = if (isSelected) 12.dp else 6.dp, shape = CircleShape)
            .background(animatedColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mood,
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}
