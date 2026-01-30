package com.example.project_smartfit.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_smartfit.presentation.theme.*

/**
 * Button variants matching the design system
 */
enum class SGButtonVariant {
    Primary,    // Green gradient - main CTAs
    Secondary,  // Blue gradient - secondary actions
    Gold,       // Gold gradient - special actions
    Outline,    // Transparent with border
    Ghost       // Text only
}

/**
 * Enhanced button with gradient backgrounds, animations, and multiple variants
 */
@Composable
fun SGButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SGButtonVariant = SGButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.96f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_scale"
    )
    
    val gradients = SmartFitTheme.gradients
    
    when (variant) {
        SGButtonVariant.Primary, SGButtonVariant.Secondary, SGButtonVariant.Gold -> {
            GradientButton(
                text = text,
                onClick = {
                    if (enabled && !isLoading) {
                        onClick()
                    }
                },
                modifier = modifier.scale(scale),
                gradient = when (variant) {
                    SGButtonVariant.Primary -> gradients.buttonPrimary
                    SGButtonVariant.Secondary -> gradients.buttonSecondary
                    SGButtonVariant.Gold -> gradients.buttonGold
                    else -> gradients.buttonPrimary
                },
                shadowColor = when (variant) {
                    SGButtonVariant.Primary -> GovGreen
                    SGButtonVariant.Secondary -> GovBlue
                    SGButtonVariant.Gold -> GovGold
                    else -> GovGreen
                },
                enabled = enabled,
                isLoading = isLoading,
                icon = icon
            )
        }
        
        SGButtonVariant.Outline -> {
            OutlinedButton(
                onClick = {
                    if (!isLoading) onClick()
                },
                modifier = modifier
                    .scale(scale)
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = enabled && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GovGreen
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GovGreen, GovBlue)
                    )
                )
            ) {
                ButtonContent(text = text, isLoading = isLoading, icon = icon, textColor = GovGreen)
            }
        }
        
        SGButtonVariant.Ghost -> {
            TextButton(
                onClick = {
                    if (!isLoading) onClick()
                },
                modifier = modifier
                    .scale(scale)
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = enabled && !isLoading
            ) {
                ButtonContent(
                    text = text,
                    isLoading = isLoading,
                    icon = icon,
                    textColor = GovGreen
                )
            }
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    shadowColor: Color,
    enabled: Boolean,
    isLoading: Boolean,
    icon: @Composable (() -> Unit)?
) {
    val shape = RoundedCornerShape(12.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                ambientColor = shadowColor.copy(alpha = 0.3f),
                spotColor = shadowColor.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(
                brush = if (enabled) gradient else Brush.horizontalGradient(
                    colors = listOf(Slate300, Slate400)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                if (!isLoading) onClick()
            },
            modifier = Modifier.fillMaxSize(),
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Slate500
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            ButtonContent(
                text = text,
                isLoading = isLoading,
                icon = icon,
                textColor = Color.White
            )
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    icon: @Composable (() -> Unit)?,
    textColor: Color = Color.White
) {
    if (isLoading) {
        // Shimmer loading effect
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loading_alpha"
        )
        
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = textColor.copy(alpha = alpha),
            strokeWidth = 2.5.dp
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
