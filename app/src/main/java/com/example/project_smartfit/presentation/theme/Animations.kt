package com.example.project_smartfit.presentation.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

// =============================================================================
// ANIMATION DURATIONS
// =============================================================================

object AnimationDurations {
    const val Instant = 100
    const val Fast = 200
    const val Medium = 300
    const val Slow = 500
    const val VerySlow = 800
    const val Entrance = 600
    const val Exit = 400
    const val Stagger = 100  // Delay between staggered items
}

// =============================================================================
// EASING FUNCTIONS
// =============================================================================

object AnimationEasing {
    val Standard = FastOutSlowInEasing
    val Emphasized = EaseInOutCubic
    val Decelerate = LinearOutSlowInEasing
    val Accelerate = FastOutLinearInEasing
    val Bounce = EaseOutBounce
    val Spring = EaseOutBack
    val Linear = LinearEasing
}

// Custom easing curves
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutBounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val EaseOutBack = CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f)

// =============================================================================
// ANIMATION SPECS
// =============================================================================

// Button animations
val ButtonPressAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh
)

val ButtonScaleAnimationSpec = tween<Float>(
    durationMillis = AnimationDurations.Fast,
    easing = AnimationEasing.Standard
)

// Card animations
val CardEntranceAnimationSpec = tween<Float>(
    durationMillis = AnimationDurations.Entrance,
    easing = AnimationEasing.Decelerate
)

val CardHoverAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium
)

// Float animation for cards
val FloatAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 3000,
        easing = AnimationEasing.Linear
    ),
    repeatMode = RepeatMode.Reverse
)

// Pulse animation for live indicators
val PulseAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 1500,
        easing = AnimationEasing.Standard
    ),
    repeatMode = RepeatMode.Reverse
)

// Shimmer animation
val ShimmerAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 1200,
        easing = AnimationEasing.Linear
    ),
    repeatMode = RepeatMode.Restart
)

// Orb blob movement
val OrbAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 8000,
        easing = AnimationEasing.Linear
    ),
    repeatMode = RepeatMode.Reverse
)

// Marquee scroll animation
val MarqueeAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 25000,
        easing = AnimationEasing.Linear
    ),
    repeatMode = RepeatMode.Restart
)

// Page transition specs
val PageEnterTransitionSpec = tween<Float>(
    durationMillis = AnimationDurations.Medium,
    easing = AnimationEasing.Decelerate
)

val PageExitTransitionSpec = tween<Float>(
    durationMillis = AnimationDurations.Fast,
    easing = AnimationEasing.Accelerate
)

// =============================================================================
// MODIFIERS WITH ANIMATIONS
// =============================================================================

/**
 * Adds a floating animation to a composable
 */
fun Modifier.floatingAnimation(
    amplitude: Float = 8f,
    durationMs: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    graphicsLayer { translationY = offsetY }
}

/**
 * Adds a pulsing scale animation
 */
fun Modifier.pulseAnimation(
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMs: Int = 1500
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    scale(scale)
}

/**
 * Adds a gentle rotation animation
 */
fun Modifier.rotateAnimation(
    durationMs: Int = 10000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    graphicsLayer { rotationZ = rotation }
}

/**
 * Adds shimmer loading effect
 */
@Composable
fun rememberShimmerOffset(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = ShimmerAnimationSpec,
        label = "shimmer_offset"
    )
    return offset
}

// =============================================================================
// SPRING CONFIGURATIONS
// =============================================================================

object SpringConfigs {
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val Smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
