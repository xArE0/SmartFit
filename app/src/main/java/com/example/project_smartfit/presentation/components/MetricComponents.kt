package com.example.project_smartfit.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_smartfit.domain.features.PostureFeatures
import com.example.project_smartfit.presentation.screens.exercise.PoseDetectionState
import com.example.project_smartfit.presentation.theme.*

/**
 * Reusable card component for posture metrics
 */
@Composable
fun PostureMetricsCard(features: PostureFeatures, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Detected joints metric
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Detected Joints",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate700,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${features.detectedJointCount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GovBlue
            )
        }

        Divider(color = Slate200.copy(alpha = 0.5f), thickness = 1.dp)

        // Key metrics
        MetricRow("Neck Flexion", "${String.format("%.1f", features.neckFlexion)}°")
        MetricRow("Shoulder Level", "${String.format("%.1f", features.shoulderLevel)}%")
        MetricRow("Spine Alignment", "${String.format("%.1f", features.spineAlignment)}%")
        MetricRow("Torso Lean", "${String.format("%.1f", features.torsoLean)}°")
    }
}

/**
 * Reusable feedback component
 */
@Composable
fun FeedbackSection(state: PoseDetectionState) {
    if (state.postureFeatures == null) return

    val features = state.postureFeatures!!
    val warnings = mutableListOf<String>()

    if (kotlin.math.abs(features.neckFlexion) > 20f) {
        warnings.add("Straighten your neck - reduce forward lean")
    }
    if (features.shoulderLevel > 15f) {
        warnings.add("Level your shoulders - maintain horizontal balance")
    }
    if (kotlin.math.abs(features.torsoLean) > 25f) {
        warnings.add("Reduce forward trunk lean for better core stability")
    }
    if (features.spineAlignment < 60f) {
        warnings.add("Improve spine alignment to prevent strain")
    }

    if (warnings.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Improvements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            
            warnings.forEach { msg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                    )
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationsCard(features: PostureFeatures) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Key Measurements",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )

        val metrics = listOf(
            "Neck Flexion" to "${String.format("%.1f", features.neckFlexion)}°",
            "Torso Lean" to "${String.format("%.1f", features.torsoLean)}°",
            "Arm Flexion" to "${String.format("%.0f", (features.leftArmFlexion + features.rightArmFlexion) / 2)}°",
            "Knee Flexion" to "${String.format("%.0f", (features.leftKneeFlexion + features.rightKneeFlexion) / 2)}°"
        )

        metrics.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate800)
            }
        }
    }
}

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
