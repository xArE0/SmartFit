package com.example.project_smartfit.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_smartfit.presentation.screens.exercise.ExerciseTrackingState
import com.example.project_smartfit.presentation.theme.*

/**
 * Reusable metric row component
 */
@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate600)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}

/**
 * Exercise tracking metrics card for the new MediaPipe system
 */
@Composable
fun ExerciseMetricsCard(state: ExerciseTrackingState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Visibility",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate700,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${String.format("%.0f", state.averageVisibility * 100)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GovBlue
            )
        }

        Divider(color = Slate200.copy(alpha = 0.5f), thickness = 1.dp)

        MetricRow("Reps", "${state.repCount}")
        MetricRow("Phase", state.currentPhase.name)
        MetricRow("FPS", "${state.fps}")
        MetricRow("Exercise", state.selectedExercise.name.replace("_", " "))
    }
}

/**
 * Feedback section using new ExerciseTrackingState
 */
@Composable
fun FeedbackSection(state: ExerciseTrackingState) {
    if (state.primaryFeedback == null && state.currentErrors.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.primaryFeedback?.let { feedback ->
            Text(
                feedback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isFormCorrect) GovGreenLight else ErrorRed
            )
        }
    }
}
