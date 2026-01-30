package com.example.project_smartfit.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// DARK COLOR SCHEME
// =============================================================================

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = GovGreenLight,
    onPrimary = Slate900,
    primaryContainer = GovGreenDark,
    onPrimaryContainer = GovGreenSoft,
    
    // Secondary colors
    secondary = GovBlueLight,
    onSecondary = Slate900,
    secondaryContainer = GovBlueDark,
    onSecondaryContainer = GovBlueSoft,
    
    // Tertiary colors
    tertiary = GovGoldLight,
    onTertiary = Slate900,
    tertiaryContainer = GovGoldDark,
    onTertiaryContainer = GovGoldSoft,
    
    // Error colors
    error = ErrorRedLight,
    onError = Slate900,
    errorContainer = ErrorRedDark,
    onErrorContainer = ErrorBg,
    
    // Background colors
    background = Slate950,
    onBackground = Slate50,
    
    // Surface colors
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    
    // Other
    outline = Slate600,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = GovGreen,
    surfaceTint = GovGreenLight,
    scrim = Color.Black.copy(alpha = 0.5f)
)

// =============================================================================
// LIGHT COLOR SCHEME
// =============================================================================

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = GovGreen,
    onPrimary = Color.White,
    primaryContainer = GovGreenSoft,
    onPrimaryContainer = GovGreenDark,
    
    // Secondary colors
    secondary = GovBlue,
    onSecondary = Color.White,
    secondaryContainer = GovBlueSoft,
    onSecondaryContainer = GovBlueDark,
    
    // Tertiary colors
    tertiary = GovGold,
    onTertiary = Color.White,
    tertiaryContainer = GovGoldSoft,
    onTertiaryContainer = GovGoldDark,
    
    // Error colors
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorRedDark,
    
    // Background colors
    background = Slate50,
    onBackground = Slate900,
    
    // Surface colors
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    
    // Other
    outline = Slate400,
    outlineVariant = Slate200,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = GovGreenLight,
    surfaceTint = GovGreen,
    scrim = Color.Black.copy(alpha = 0.3f)
)

// =============================================================================
// THEME COMPOSABLE
// =============================================================================

@Composable
fun SmartFitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gradients = if (darkTheme) DarkGradients else LightGradients
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use light status bar for light theme
            window.statusBarColor = if (darkTheme) Slate900.toArgb() else Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGradients provides gradients
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

// =============================================================================
// THEME ACCESSORS
// =============================================================================

/**
 * Access gradients from the current theme
 */
object SmartFitTheme {
    val gradients: SubsidyGuardGradients
        @Composable
        get() = LocalGradients.current
}
