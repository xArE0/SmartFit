package com.example.project_smartfit.presentation.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.domain.model.RepPhase

/**
 * Real-time exercise tracking screen with form feedback
 */
@Composable
fun ExerciseTrackingScreen(
    state: ExerciseTrackingState,
    onStartTracking: (ExerciseType) -> Unit,
    onStopTracking: () -> Unit,
    onResetReps: () -> Unit,
    onToggleDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with exercise selection
        ExerciseHeader(
            selectedExercise = state.selectedExercise,
            isTracking = state.isTrackingActive,
            onExerciseSelected = { exercise ->
                if (!state.isTrackingActive) {
                    onStartTracking(exercise)
                }
            }
        )
        
        // Camera preview placeholder (integrate with CameraX)
        CameraPreviewPlaceholder(
            isModelLoaded = state.isModelLoaded,
            errorMessage = state.errorMessage
        )
        
        // Form feedback indicator
        FormFeedbackCard(
            isCorrect = state.isFormCorrect,
            primaryFeedback = state.primaryFeedback,
            errors = state.currentErrors,
            visibility = state.averageVisibility
        )
        
        // Rep counter and phase
        RepCounterCard(
            repCount = state.repCount,
            currentPhase = state.currentPhase,
            onReset = onResetReps
        )
        
        // Debug info (if enabled)
        if (state.debugMode) {
            DebugInfoCard(
                fps = state.fps,
                angles = state.debugAngles
            )
        }
        
        // Control buttons
        ControlButtons(
            isTracking = state.isTrackingActive,
            debugMode = state.debugMode,
            onStartStop = {
                if (state.isTrackingActive) {
                    onStopTracking()
                } else {
                    onStartTracking(state.selectedExercise)
                }
            },
            onToggleDebug = onToggleDebug
        )
    }
}

@Composable
private fun ExerciseHeader(
    selectedExercise: ExerciseType,
    isTracking: Boolean,
    onExerciseSelected: (ExerciseType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Exercise Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseChip(
                    text = "Squat",
                    isSelected = selectedExercise == ExerciseType.SQUAT,
                    enabled = !isTracking,
                    onClick = { onExerciseSelected(ExerciseType.SQUAT) }
                )
                ExerciseChip(
                    text = "Pushup",
                    isSelected = selectedExercise == ExerciseType.PUSHUP,
                    enabled = !isTracking,
                    onClick = { onExerciseSelected(ExerciseType.PUSHUP) }
                )
                ExerciseChip(
                    text = "Curl",
                    isSelected = selectedExercise == ExerciseType.DUMBBELL_CURL,
                    enabled = !isTracking,
                    onClick = { onExerciseSelected(ExerciseType.DUMBBELL_CURL) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseChip(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

@Composable
private fun CameraPreviewPlaceholder(
    isModelLoaded: Boolean,
    errorMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                !isModelLoaded -> {
                    CircularProgressIndicator(color = Color.White)
                }
                else -> {
                    Text(
                        text = "Camera Preview\n(Integrate CameraX here)",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FormFeedbackCard(
    isCorrect: Boolean,
    primaryFeedback: String?,
    errors: List<String>,
    visibility: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                Color(0xFF4CAF50).copy(alpha = 0.2f)
            } else {
                Color(0xFFF44336).copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Feedback text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCorrect) "Good Form!" else "Form Issue Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                
                if (primaryFeedback != null) {
                    Text(
                        text = primaryFeedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (errors.isNotEmpty() && errors.size > 1) {
                    Text(
                        text = "+${errors.size - 1} more issue${if (errors.size > 2) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Visibility indicator
                if (visibility < 0.7f) {
                    Text(
                        text = "⚠ Low visibility - adjust position",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF57C00)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepCounterCard(
    repCount: Int,
    currentPhase: RepPhase,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = repCount.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Phase",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = currentPhase.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            IconButton(onClick = onReset) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DebugInfoCard(
    fps: Int,
    angles: Map<String, Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Debug Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "FPS: $fps",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            if (angles.isNotEmpty()) {
                Text(
                    text = "Angles:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                angles.forEach { (name, value) ->
                    Text(
                        text = "$name: ${String.format("%.1f", value)}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlButtons(
    isTracking: Boolean,
    debugMode: Boolean,
    onStartStop: () -> Unit,
    onToggleDebug: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartStop,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTracking) "Stop" else "Start")
        }
        
        OutlinedButton(
            onClick = onToggleDebug,
            modifier = Modifier.weight(0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = if (debugMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
