package com.example.loveapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MoodBubble(
    mood: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) color else color.copy(alpha = 0.5f),
        animationSpec = tween(300), label = "bubbleColor"
    )
    
    val animatedSize by animateDpAsState(
        targetValue = if (isSelected) 70.dp else 60.dp,
        animationSpec = tween(300), label = "bubbleSize"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedColor)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(animatedColor)
                .padding(animatedSize / 2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mood,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
