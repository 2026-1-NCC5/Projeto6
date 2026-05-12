package com.AlimempatIA.stockai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AlimempatIA.stockai.ui.theme.*

@Composable
fun AIStatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(if (isActive) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isActive) AIGlow.copy(alpha = 0.3f)
                        else TextDisabled.copy(alpha = 0.3f)
                    )
            )
            // Inner dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) AIGlow else TextDisabled)
            )
        }
        Text(
            text = if (isActive) "IA Ativa" else "IA Inativa",
            fontSize = 12.sp,
            color = if (isActive) AIGlowLight else TextDisabled
        )
    }
}
