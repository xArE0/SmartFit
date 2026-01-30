package com.example.project_smartfit.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.project_smartfit.presentation.theme.*

/**
 * Glass card variants
 */
enum class GlassCardVariant {
    Light,      // White glass for light backgrounds
    Dark,       // Dark glass for dark backgrounds  
    Accent      // Colored accent glass
}

/**
 * A glassmorphism card with frosted blur effect and optional gradient border
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    variant: GlassCardVariant = GlassCardVariant.Light,
    borderGradient: Boolean = false,
    animateEntrance: Boolean = true,
    entranceDelay: Int = 0,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(!animateEntrance) }
    
    LaunchedEffect(Unit) {
        if (animateEntrance) {
            kotlinx.coroutines.delay(entranceDelay.toLong())
            isVisible = true
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(AnimationDurations.Entrance),
        label = "glass_alpha"
    )
    
    val shape = RoundedCornerShape(cornerRadius)
    
    val backgroundColor = when (variant) {
        GlassCardVariant.Light -> GlassWhiteHeavy
        GlassCardVariant.Dark -> GlassDarkMedium
        GlassCardVariant.Accent -> GlassBlue
    }
    
    val borderColor = when (variant) {
        GlassCardVariant.Light -> Color.White.copy(alpha = 0.5f)
        GlassCardVariant.Dark -> Slate700.copy(alpha = 0.5f)
        GlassCardVariant.Accent -> GovBlue.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = modifier
            .scale(scale)
    ) {
        // Background blur layer (simulated with gradient)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = backgroundColor.alpha * 0.8f)
                        )
                    )
                )
                .then(
                    if (borderGradient) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(GovGreen, GovBlue, GovGold)
                            ),
                            shape = shape
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = borderColor,
                            shape = shape
                        )
                    }
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .clip(shape)
                .padding(20.dp),
            content = content
        )
    }
}
