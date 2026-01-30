package com.example.project_smartfit.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_smartfit.domain.features.PostureFeatures
import com.example.project_smartfit.presentation.screens.exercise.PoseDetectionState

/**
 * Reusable card component for posture metrics
 */
@Composable
fun PostureMetricsCard(features: PostureFeatures) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Overall score with progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detected Joints", color = Color.White)
                Text(
                    "${features.detectedJointCount}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
            }

            // Key metrics
            MetricRow("Neck Flexion", "${String.format("%.1f", features.neckFlexion)}°")
            MetricRow("Shoulder Level", "${String.format("%.1f", features.shoulderLevel)}%")
            MetricRow("Spine Alignment", "${String.format("%.1f", features.spineAlignment)}%")
        }
    }
}

/**
 * Reusable feedback component
 */
@Composable
fun FeedbackSection(state: PoseDetectionState) {
    if (state.postureFeatures == null) return

    val features = state.postureFeatures
    val feedback = mutableListOf<String>()

    // Generate feedback based on features
    if (kotlin.math.abs(features.neckFlexion) > 20f) {
        feedback.add("⚠ Straighten your neck - reduce forward lean")
    }
    if (features.shoulderLevel > 15f) {
        feedback.add("⚠ Level your shoulders")
    }
    if (kotlin.math.abs(features.torsoLean) > 25f) {
        feedback.add("⚠ Reduce forward trunk lean")
    }
    if (features.spineAlignment < 60f) {
        feedback.add("⚠ Improve spine alignment")
    }

    if (feedback.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFE4B5).copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                feedback.forEach { msg ->
                    Text(msg, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * Recommendations based on posture analysis
 */
@Composable
fun RecommendationsCard(features: PostureFeatures) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Blue.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Key Measurements", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)

            Text("• Neck Flexion: ${String.format("%.1f", features.neckFlexion)}°", fontSize = 12.sp, color = Color.White)
            Text("• Torso Lean: ${String.format("%.1f", features.torsoLean)}°", fontSize = 12.sp, color = Color.White)
            Text("• Left Arm: ${String.format("%.0f", features.leftArmFlexion)}°", fontSize = 12.sp, color = Color.White)
            Text("• Right Arm: ${String.format("%.0f", features.rightArmFlexion)}°", fontSize = 12.sp, color = Color.White)
            Text("• Left Knee: ${String.format("%.0f", features.leftKneeFlexion)}°", fontSize = 12.sp, color = Color.White)
            Text("• Right Knee: ${String.format("%.0f", features.rightKneeFlexion)}°", fontSize = 12.sp, color = Color.White)
        }
    }
}

/**
 * Simple metric row component
 */
@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.LightGray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
