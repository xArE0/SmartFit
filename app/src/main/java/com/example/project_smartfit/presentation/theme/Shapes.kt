package com.example.project_smartfit.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// MATERIAL 3 SHAPES
// =============================================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Tags, chips
    small = RoundedCornerShape(8.dp),        // Small buttons
    medium = RoundedCornerShape(12.dp),      // Cards, dialogs
    large = RoundedCornerShape(16.dp),       // Large cards
    extraLarge = RoundedCornerShape(28.dp)   // Bottom sheets, FAB
)

// =============================================================================
// EXTENDED SHAPE TOKENS
// =============================================================================

// Pill shape for buttons
val PillShape = RoundedCornerShape(50)

// Card shapes with different intensities
val CardShapeSmall = RoundedCornerShape(12.dp)
val CardShapeMedium = RoundedCornerShape(16.dp)
val CardShapeLarge = RoundedCornerShape(20.dp)
val CardShapeXL = RoundedCornerShape(24.dp)

// Dashboard card shape
val DashboardCardShape = RoundedCornerShape(20.dp)

// Glass card shape
val GlassCardShape = RoundedCornerShape(24.dp)

// Button shapes
val ButtonShapeSmall = RoundedCornerShape(8.dp)
val ButtonShapeMedium = RoundedCornerShape(12.dp)
val ButtonShapeLarge = RoundedCornerShape(16.dp)
val ButtonShapePill = RoundedCornerShape(50)

// Input field shape
val InputFieldShape = RoundedCornerShape(12.dp)

// Badge shape
val BadgeShape = RoundedCornerShape(6.dp)

// Avatar shapes
val AvatarShapeSmall = RoundedCornerShape(8.dp)
val AvatarShapeMedium = RoundedCornerShape(12.dp)
val AvatarShapeLarge = RoundedCornerShape(16.dp)
val AvatarShapeCircle = RoundedCornerShape(50)

// Bottom sheet shape
val BottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// Top navigation gradient border
val TopNavShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)
