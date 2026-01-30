package com.example.project_smartfit.pose.overlay

import com.example.project_smartfit.pose.Person
import com.example.project_smartfit.pose.KeyPoint
import com.example.project_smartfit.pose.PoseDetector
import com.example.project_smartfit.features.FeatureExtractor
import com.example.project_smartfit.features.PostureFeatures
import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Handles all pose visualization and drawing operations
 * Separated from business logic for easy customization and testing
 */
object PoseVisualization {

    // Visual configuration constants
    private const val POINT_RADIUS = 12f
    private const val CONNECTION_STROKE_WIDTH = 6f
    private const val CONFIDENCE_TEXT_SIZE = 24f
    private const val CONFIDENCE_THRESHOLD = 0.3f

    /**
     * Draw the complete pose skeleton on canvas
     */
    fun DrawScope.drawCompletePose(
        person: Person,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        drawConnections: Boolean = true,
        drawKeypoints: Boolean = true,
        drawConfidenceScores: Boolean = true
    ) {
        if (drawConnections) {
            drawSkeletonConnections(
                person = person,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight
            )
        }

        if (drawKeypoints) {
            drawKeypointCircles(
                person = person,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight
            )
        }

        if (drawConfidenceScores) {
            drawConfidenceText(
                person = person,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight
            )
        }
    }

    /**
     * Draw skeleton connections between keypoints
     */
    private fun DrawScope.drawSkeletonConnections(
        person: Person,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        PoseDetector.bodyConnections.forEach { connection ->
            val startKeyPoint = person.keyPoints[connection.first]
            val endKeyPoint = person.keyPoints[connection.second]

            if (startKeyPoint.score > CONFIDENCE_THRESHOLD && endKeyPoint.score > CONFIDENCE_THRESHOLD) {
                val startX = (startKeyPoint.x / bitmapWidth) * canvasWidth
                val startY = (startKeyPoint.y / bitmapHeight) * canvasHeight
                val endX = (endKeyPoint.x / bitmapWidth) * canvasWidth
                val endY = (endKeyPoint.y / bitmapHeight) * canvasHeight

                drawLine(
                    color = Color.Green,
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = CONNECTION_STROKE_WIDTH
                )
            }
        }
    }

    /**
     * Draw keypoint circles with color-coding by body part
     */
    private fun DrawScope.drawKeypointCircles(
        person: Person,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        person.keyPoints.forEachIndexed { index, keyPoint ->
            if (keyPoint.score > CONFIDENCE_THRESHOLD) {
                val x = (keyPoint.x / bitmapWidth) * canvasWidth
                val y = (keyPoint.y / bitmapHeight) * canvasHeight

                val color = getKeypointColor(index)

                drawCircle(
                    color = color,
                    radius = POINT_RADIUS,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
    }

    /**
     * Draw confidence scores above keypoints
     */
    @SuppressLint("DefaultLocale")
    private fun DrawScope.drawConfidenceText(
        person: Person,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        person.keyPoints.forEachIndexed { index, keyPoint ->
            if (keyPoint.score > 0.5) {
                val x = (keyPoint.x / bitmapWidth) * canvasWidth
                val y = (keyPoint.y / bitmapHeight) * canvasHeight

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.2f", keyPoint.score),
                        x + POINT_RADIUS + 5f,
                        y - POINT_RADIUS,
                        Paint().apply {
                            textSize = CONFIDENCE_TEXT_SIZE
                            isAntiAlias = true
                            setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
                        }
                    )
                }
            }
        }
    }

    /**
     * Draw pose skeleton with features highlighted
     * Used for feature visualization in posture analysis
     */
    fun DrawScope.drawPoseWithFeatures(
        person: Person,
        features: PostureFeatures?,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        highlightIssues: Boolean = true
    ) {
        // Draw base skeleton
        drawCompletePose(
            person = person,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight
        )

        if (features != null && highlightIssues) {
            // Highlight problematic areas based on features
            highlightPostureIssues(
                person = person,
                features = features,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight
            )
        }
    }

    /**
     * Highlight areas with poor posture
     */
    private fun DrawScope.highlightPostureIssues(
        person: Person,
        features: PostureFeatures,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        // Highlight forward head posture
        if (kotlin.math.abs(features.neckFlexion) > 15f) {
            highlightKeypoint(person.keyPoints[FeatureExtractor.NOSE], canvasWidth, canvasHeight, bitmapWidth, bitmapHeight, Color.Red)
        }

        // Highlight shoulder asymmetry
        if (features.shoulderLevel > 10f) {
            highlightKeypoint(person.keyPoints[FeatureExtractor.LEFT_SHOULDER], canvasWidth, canvasHeight, bitmapWidth, bitmapHeight, Color.Yellow)
            highlightKeypoint(person.keyPoints[FeatureExtractor.RIGHT_SHOULDER], canvasWidth, canvasHeight, bitmapWidth, bitmapHeight, Color.Yellow)
        }

        // Highlight excessive forward lean
        if (kotlin.math.abs(features.torsoLean) > 20f) {
            highlightKeypoint(person.keyPoints[FeatureExtractor.LEFT_HIP], canvasWidth, canvasHeight, bitmapWidth, bitmapHeight, Color.Yellow)
            highlightKeypoint(person.keyPoints[FeatureExtractor.RIGHT_HIP], canvasWidth, canvasHeight, bitmapWidth, bitmapHeight, Color.Yellow)
        }
    }

    /**
     * Highlight a specific keypoint with a warning color
     */
    private fun DrawScope.highlightKeypoint(
        keyPoint: KeyPoint,
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        color: Color = Color.Red
    ) {
        if (keyPoint.score > CONFIDENCE_THRESHOLD) {
            val x = (keyPoint.x / bitmapWidth) * canvasWidth
            val y = (keyPoint.y / bitmapHeight) * canvasHeight

            // Draw warning circle around keypoint
            drawCircle(
                color = color,
                radius = POINT_RADIUS + 8f,
                center = androidx.compose.ui.geometry.Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
    }

    /**
     * Get color for keypoint based on body part
     */
    private fun getKeypointColor(index: Int): Color {
        return when (index) {
            0, 1, 2, 3, 4 -> Color.Red        // Head (nose, eyes, ears)
            5, 6 -> Color.Magenta              // Shoulders
            7, 8, 9, 10 -> Color.Blue          // Arms (elbows, wrists)
            11, 12 -> Color.Cyan               // Hips
            13, 14, 15, 16 -> Color.Green      // Legs (knees, ankles)
            else -> Color.White
        }
    }
}

/**
 * Composable UI for displaying pose statistics and metrics
 */
@SuppressLint("DefaultLocale")
@Composable
fun PoseStatistics(
    person: Person?,
    fps: Int = 0,
    elapsedTimeMs: Long = 0L,
    postureFeatures: PostureFeatures? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Performance metrics
            Text(
                text = "FPS: $fps | ${elapsedTimeMs / 1000}s",
                color = Color.Yellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            // Pose detection metrics
            if (person != null) {
                Text(
                    text = "Overall: ${String.format("%.2f", person.score)}",
                    color = Color.White,
                    fontSize = 10.sp
                )
                val highConfidenceKeypoints = person.keyPoints.count { it.score > 0.5 }
                val visibleKeypoints = person.keyPoints.count { it.score > 0.3 }
                Text(
                    text = "Detected: $highConfidenceKeypoints/$visibleKeypoints",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            // Posture feature metrics
            if (postureFeatures != null && postureFeatures.detectedJointCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "DETECTED JOINTS: ${postureFeatures.detectedJointCount}",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )


                Spacer(modifier = Modifier.height(2.dp))

                // Show key metrics
                if (kotlin.math.abs(postureFeatures.neckFlexion) > 5f) {
                    Text(
                        text = "⚠ Neck: ${String.format("%.0f", postureFeatures.neckFlexion)}°",
                        color = Color.Yellow,
                        fontSize = 9.sp
                    )
                }

                if (postureFeatures.shoulderLevel > 5f) {
                    Text(
                        text = "⚠ Shoulders: ${String.format("%.1f", postureFeatures.shoulderLevel)}%",
                        color = Color.Yellow,
                        fontSize = 9.sp
                    )
                }

                if (kotlin.math.abs(postureFeatures.torsoLean) > 10f) {
                    Text(
                        text = "⚠ Lean: ${String.format("%.0f", postureFeatures.torsoLean)}°",
                        color = Color.Yellow,
                        fontSize = 9.sp
                    )
                }
            } else if (person != null) {
                Text(
                    text = "Insufficient data",
                    color = Color.Red,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Composable for displaying detailed posture analysis report
 */
@SuppressLint("DefaultLocale")
@Composable
fun PostureAnalysisReport(
    features: PostureFeatures,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "EXTRACTED FEATURES",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Feature sections
            AnalysisSection(
                title = "HEAD & NECK",
                items = listOf(
                    "Neck Flexion: ${String.format("%.1f", features.neckFlexion)}°" to Color.White,
                    "Head Tilt: ${String.format("%.1f", features.headTilt)}°" to Color.White
                )
            )

            AnalysisSection(
                title = "SPINE & CORE",
                items = listOf(
                    "Spine Alignment: ${String.format("%.1f", features.spineAlignment)}" to Color.White,
                    "Torso Lean: ${String.format("%.1f", features.torsoLean)}°" to Color.White,
                    "Shoulder Level: ${String.format("%.1f", features.shoulderLevel)}" to Color.White
                )
            )

            AnalysisSection(
                title = "ARMS",
                items = listOf(
                    "L Arm Flexion: ${String.format("%.0f", features.leftArmFlexion)}°" to Color.White,
                    "R Arm Flexion: ${String.format("%.0f", features.rightArmFlexion)}°" to Color.White,
                    "Arm Elevation: ${String.format("%.1f", features.armElevation)}" to Color.White
                )
            )

            AnalysisSection(
                title = "LEGS & HIPS",
                items = listOf(
                    "Hip Alignment: ${String.format("%.1f", features.hipAlignment)}" to Color.White,
                    "L Knee Flexion: ${String.format("%.0f", features.leftKneeFlexion)}°" to Color.White,
                    "R Knee Flexion: ${String.format("%.0f", features.rightKneeFlexion)}°" to Color.White,
                    "Leg Symmetry: ${String.format("%.1f", features.legAlignment)}°" to Color.White
                )
            )

            Text(
                text = "Detected Joints: ${features.detectedJointCount}",
                color = Color.LightGray,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun AnalysisSection(
    title: String,
    items: List<Pair<String, Color>>
) {
    Column {
        Text(
            text = title,
            color = Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { (text, color) ->
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


