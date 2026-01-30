package com.example.project_smartfit.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

// =============================================================================
// GRADIENT DEFINITIONS
// =============================================================================

@Stable
data class SubsidyGuardGradients(
    // Primary gradients
    val primaryGradient: Brush,
    val primaryVertical: Brush,
    
    // Accent gradients
    val accentGradient: Brush,
    val goldGradient: Brush,
    
    // Background gradients
    val backgroundGradient: Brush,
    val darkBackgroundGradient: Brush,
    val meshGradient: Brush,
    
    // Button gradients
    val buttonPrimary: Brush,
    val buttonSecondary: Brush,
    val buttonGold: Brush,
    
    // Card gradients
    val glassGradient: Brush,
    val cardGradient: Brush,
    
    // Text gradients
    val textGradient: Brush,
    val neonGradient: Brush,
    
    // Border gradients
    val borderGradient: Brush,
    val glowBorder: Brush
)

// =============================================================================
// LIGHT THEME GRADIENTS
// =============================================================================

val LightGradients = SubsidyGuardGradients(
    // Primary - Green to Blue
    primaryGradient = Brush.horizontalGradient(
        colors = listOf(GovGreen, GovBlue)
    ),
    primaryVertical = Brush.verticalGradient(
        colors = listOf(GovGreen, GovBlue)
    ),
    
    // Accent - Blue to Gold
    accentGradient = Brush.horizontalGradient(
        colors = listOf(GovBlue, GovGold)
    ),
    goldGradient = Brush.horizontalGradient(
        colors = listOf(GovGold, GovGoldLight)
    ),
    
    // Background - Soft slate
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Slate50, Slate100)
    ),
    darkBackgroundGradient = Brush.verticalGradient(
        colors = listOf(Slate900, Slate950)
    ),
    meshGradient = Brush.radialGradient(
        colors = listOf(
            GovBlueSoft.copy(alpha = 0.3f),
            GovGreenSoft.copy(alpha = 0.2f),
            Color.Transparent
        )
    ),
    
    // Buttons
    buttonPrimary = Brush.horizontalGradient(
        colors = listOf(GovGreen, GovGreenDark)
    ),
    buttonSecondary = Brush.horizontalGradient(
        colors = listOf(GovBlue, GovBlueDark)
    ),
    buttonGold = Brush.horizontalGradient(
        colors = listOf(GovGold, GovGoldDark)
    ),
    
    // Glass effect
    glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.9f),
            Color.White.copy(alpha = 0.7f)
        )
    ),
    cardGradient = Brush.verticalGradient(
        colors = listOf(Color.White, Slate50)
    ),
    
    // Text
    textGradient = Brush.horizontalGradient(
        colors = listOf(GovGreen, GovBlue, GovGold)
    ),
    neonGradient = Brush.horizontalGradient(
        colors = listOf(NeonGreen, NeonBlue)
    ),
    
    // Borders
    borderGradient = Brush.horizontalGradient(
        colors = listOf(GovGreen, GovBlue, GovGold)
    ),
    glowBorder = Brush.sweepGradient(
        colors = listOf(GovGreen, GovBlue, GovGold, GovGreen)
    )
)

// =============================================================================
// DARK THEME GRADIENTS
// =============================================================================

val DarkGradients = SubsidyGuardGradients(
    primaryGradient = Brush.horizontalGradient(
        colors = listOf(GovGreenLight, GovBlueLight)
    ),
    primaryVertical = Brush.verticalGradient(
        colors = listOf(GovGreenLight, GovBlueLight)
    ),
    
    accentGradient = Brush.horizontalGradient(
        colors = listOf(GovBlueLight, GovGoldLight)
    ),
    goldGradient = Brush.horizontalGradient(
        colors = listOf(GovGoldLight, GovGold)
    ),
    
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Slate950, Slate900)
    ),
    darkBackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Slate950)
    ),
    meshGradient = Brush.radialGradient(
        colors = listOf(
            GovBlue.copy(alpha = 0.15f),
            GovGreen.copy(alpha = 0.1f),
            Color.Transparent
        )
    ),
    
    buttonPrimary = Brush.horizontalGradient(
        colors = listOf(GovGreen, GovGreenLight)
    ),
    buttonSecondary = Brush.horizontalGradient(
        colors = listOf(GovBlue, GovBlueLight)
    ),
    buttonGold = Brush.horizontalGradient(
        colors = listOf(GovGold, GovGoldLight)
    ),
    
    glassGradient = Brush.verticalGradient(
        colors = listOf(
            Slate800.copy(alpha = 0.8f),
            Slate900.copy(alpha = 0.6f)
        )
    ),
    cardGradient = Brush.verticalGradient(
        colors = listOf(Slate800, Slate900)
    ),
    
    textGradient = Brush.horizontalGradient(
        colors = listOf(GovGreenLight, GovBlueLight, GovGoldLight)
    ),
    neonGradient = Brush.horizontalGradient(
        colors = listOf(NeonGreen, NeonBlue)
    ),
    
    borderGradient = Brush.horizontalGradient(
        colors = listOf(GovGreenLight, GovBlueLight, GovGoldLight)
    ),
    glowBorder = Brush.sweepGradient(
        colors = listOf(NeonGreen, NeonBlue, NeonGold, NeonGreen)
    )
)

// =============================================================================
// COMPOSITION LOCAL
// =============================================================================

val LocalGradients = staticCompositionLocalOf { LightGradients }

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Creates a diagonal gradient from top-left to bottom-right
 */
fun diagonalGradient(colors: List<Color>): Brush = Brush.linearGradient(
    colors = colors,
    start = Offset.Zero,
    end = Offset.Infinite
)

/**
 * Creates a mesh-style gradient with orb positions
 */
fun meshGradient(
    color1: Color,
    color2: Color,
    color3: Color,
    center1: Offset = Offset(0.2f, 0.3f),
    center2: Offset = Offset(0.8f, 0.2f),
    center3: Offset = Offset(0.5f, 0.8f)
): List<Brush> = listOf(
    Brush.radialGradient(
        colors = listOf(color1.copy(alpha = 0.4f), Color.Transparent),
        center = Offset(center1.x * 1000, center1.y * 1000),
        radius = 400f
    ),
    Brush.radialGradient(
        colors = listOf(color2.copy(alpha = 0.4f), Color.Transparent),
        center = Offset(center2.x * 1000, center2.y * 1000),
        radius = 350f
    ),
    Brush.radialGradient(
        colors = listOf(color3.copy(alpha = 0.3f), Color.Transparent),
        center = Offset(center3.x * 1000, center3.y * 1000),
        radius = 450f
    )
)

/**
 * Light cards container colors
 */
val LightGreenBg = GovGreenSoft
val LightBlueBg = GovBlueSoft
val LightGoldBg = GovGoldSoft
