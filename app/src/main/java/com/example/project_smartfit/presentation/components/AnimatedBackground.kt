package com.example.project_smartfit.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.project_smartfit.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Configuration for a floating orb
 */
data class OrbConfig(
    val color: Color,
    val radius: Float,
    val initialX: Float,
    val initialY: Float,
    val movementRangeX: Float,
    val movementRangeY: Float,
    val duration: Int
)

/**
 * Animated background with floating gradient orbs
 */
@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = Slate900,
    secondaryColor: Color = Slate950,
    showOrbs: Boolean = true,
    showGrid: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val orbs = remember {
        listOf(
            OrbConfig(
                color = OrbGreen,
                radius = 300f,
                initialX = 0.2f,
                initialY = 0.15f,
                movementRangeX = 0.15f,
                movementRangeY = 0.1f,
                duration = 8000
            ),
            OrbConfig(
                color = OrbBlue,
                radius = 250f,
                initialX = 0.8f,
                initialY = 0.3f,
                movementRangeX = 0.12f,
                movementRangeY = 0.15f,
                duration = 10000
            ),
            OrbConfig(
                color = OrbGold,
                radius = 200f,
                initialX = 0.5f,
                initialY = 0.75f,
                movementRangeX = 0.2f,
                movementRangeY = 0.1f,
                duration = 12000
            )
        )
    }
    
    Box(modifier = modifier) {
        // Base gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(baseColor, secondaryColor)
                    )
                )
        )
        
        // Animated orbs
        if (showOrbs) {
            AnimatedOrbs(orbs = orbs)
        }
        
        // Grid pattern overlay
        if (showGrid) {
            GridPatternOverlay()
        }
        
        // Content on top
        content()
    }
}

@Composable
private fun AnimatedOrbs(orbs: List<OrbConfig>) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    
    val orbAnimations = orbs.mapIndexed { index, orb ->
        val phase = index * 0.33f // Offset each orb's animation phase
        
        val animatedX by infiniteTransition.animateFloat(
            initialValue = orb.initialX - orb.movementRangeX,
            targetValue = orb.initialX + orb.movementRangeX,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = orb.duration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_x_$index"
        )
        
        val animatedY by infiniteTransition.animateFloat(
            initialValue = orb.initialY - orb.movementRangeY,
            targetValue = orb.initialY + orb.movementRangeY,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (orb.duration * 1.3f).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_y_$index"
        )
        
        Triple(orb, animatedX, animatedY)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        orbAnimations.forEach { (orb, x, y) ->
            drawOrb(
                color = orb.color,
                radius = orb.radius,
                centerX = size.width * x,
                centerY = size.height * y
            )
        }
    }
}

private fun DrawScope.drawOrb(
    color: Color,
    radius: Float,
    centerX: Float,
    centerY: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color,
                color.copy(alpha = color.alpha * 0.5f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )
}

@Composable
private fun GridPatternOverlay() {
    val gridColor = Slate600.copy(alpha = 0.05f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40f
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}

/**
 * Light animated background for auth screens
 */
@Composable
fun LightAnimatedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedBackground(
        modifier = modifier,
        baseColor = Slate50,
        secondaryColor = Slate100,
        showOrbs = true,
        showGrid = false,
        content = content
    )
}

/**
 * Dark animated background for splash/hero screens
 */
@Composable
fun DarkAnimatedBackground(
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedBackground(
        modifier = modifier,
        baseColor = Slate900,
        secondaryColor = Slate950,
        showOrbs = true,
        showGrid = showGrid,
        content = content
    )
}

/**
 * Light Aurora animated background matching web citizen portal
 * Features colorful animated orbs (orange, cyan, pink, rose, peach, purple)
 */
@Composable
fun LightAuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val auroraOrbs = remember {
        listOf(
            OrbConfig(
                color = LightOrbOrange.copy(alpha = 0.6f),
                radius = 350f,
                initialX = 0.4f,
                initialY = 0.2f,
                movementRangeX = 0.2f,
                movementRangeY = 0.15f,
                duration = 12000
            ),
            OrbConfig(
                color = LightOrbCyan.copy(alpha = 0.5f),
                radius = 300f,
                initialX = 0.8f,
                initialY = 0.0f,
                movementRangeX = 0.15f,
                movementRangeY = 0.2f,
                duration = 10000
            ),
            OrbConfig(
                color = LightOrbPink.copy(alpha = 0.7f),
                radius = 280f,
                initialX = 0.0f,
                initialY = 0.5f,
                movementRangeX = 0.18f,
                movementRangeY = 0.12f,
                duration = 14000
            ),
            OrbConfig(
                color = LightOrbRose.copy(alpha = 0.5f),
                radius = 320f,
                initialX = 0.8f,
                initialY = 0.5f,
                movementRangeX = 0.15f,
                movementRangeY = 0.2f,
                duration = 11000
            ),
            OrbConfig(
                color = LightOrbPeach.copy(alpha = 0.6f),
                radius = 300f,
                initialX = 0.0f,
                initialY = 1.0f,
                movementRangeX = 0.2f,
                movementRangeY = 0.15f,
                duration = 13000
            ),
            OrbConfig(
                color = LightOrbPurple.copy(alpha = 0.4f),
                radius = 280f,
                initialX = 0.8f,
                initialY = 1.0f,
                movementRangeX = 0.15f,
                movementRangeY = 0.18f,
                duration = 15000
            ),
            OrbConfig(
                color = LightOrbRose.copy(alpha = 0.5f),
                radius = 260f,
                initialX = 0.0f,
                initialY = 0.0f,
                movementRangeX = 0.2f,
                movementRangeY = 0.2f,
                duration = 9000
            )
        )
    }
    
    Box(modifier = modifier) {
        // White/light base background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
        
        // Animated aurora orbs
        LightAuroraOrbs(orbs = auroraOrbs)
        
        // Content on top
        content()
    }
}

@Composable
private fun LightAuroraOrbs(orbs: List<OrbConfig>) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_orbs")
    
    val orbAnimations = orbs.mapIndexed { index, orb ->
        val animatedX by infiniteTransition.animateFloat(
            initialValue = orb.initialX - orb.movementRangeX,
            targetValue = orb.initialX + orb.movementRangeX,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = orb.duration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_x_$index"
        )
        
        val animatedY by infiniteTransition.animateFloat(
            initialValue = orb.initialY - orb.movementRangeY,
            targetValue = orb.initialY + orb.movementRangeY,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (orb.duration * 1.2f).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_y_$index"
        )
        
        Triple(orb, animatedX, animatedY)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        orbAnimations.forEach { (orb, x, y) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orb.color,
                        orb.color.copy(alpha = orb.color.alpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * x, size.height * y),
                    radius = orb.radius
                ),
                radius = orb.radius,
                center = Offset(size.width * x, size.height * y)
            )
        }
    }
}
@Composable
fun DarkAuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val auroraOrbs = remember {
        listOf(
            OrbConfig(
                color = GovGreenLight.copy(alpha = 0.4f),
                radius = 400f,
                initialX = 0.3f,
                initialY = 0.2f,
                movementRangeX = 0.2f,
                movementRangeY = 0.15f,
                duration = 15000
            ),
            OrbConfig(
                color = GovBlueLight.copy(alpha = 0.4f),
                radius = 350f,
                initialX = 0.7f,
                initialY = 0.4f,
                movementRangeX = 0.15f,
                movementRangeY = 0.2f,
                duration = 12000
            ),
            OrbConfig(
                color = GovGoldLight.copy(alpha = 0.3f),
                radius = 300f,
                initialX = 0.5f,
                initialY = 0.8f,
                movementRangeX = 0.25f,
                movementRangeY = 0.1f,
                duration = 18000
            )
        )
    }
    
    Box(modifier = modifier) {
        // Dark base background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        )
        
        // Animated aurora orbs
        LightAuroraOrbs(orbs = auroraOrbs)
        
        // Content on top
        content()
    }
}
