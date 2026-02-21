package com.example.project_smartfit.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Landmark connection pairs defining the skeleton.
 * Each pair is (startIndex, endIndex) into the 33-landmark array.
 */
private val POSE_CONNECTIONS = listOf(
    // Torso
    11 to 12, 11 to 23, 12 to 24, 23 to 24,
    // Left arm
    11 to 13, 13 to 15,
    // Right arm
    12 to 14, 14 to 16,
    // Left leg
    23 to 25, 25 to 27,
    // Right leg
    24 to 26, 26 to 28,
    // Left hand
    15 to 17, 15 to 19, 17 to 19,
    // Right hand
    16 to 18, 16 to 20, 18 to 20,
    // Left foot
    27 to 29, 27 to 31, 29 to 31,
    // Right foot
    28 to 30, 28 to 32, 30 to 32,
)

private val LANDMARK_COLOR = Color(0xFF00E676) // Neon green
private val LANDMARK_COLOR_DIM = Color(0xAAFF5252) // Dimmed red when not visible
private val CONNECTION_COLOR = Color(0xAAFFFFFF) // Semi-transparent white
private val CONNECTION_COLOR_DIM = Color(0x55FF5252) // Dimmed red connections
private const val LANDMARK_RADIUS = 8f
private const val CONNECTION_STROKE = 4f

/**
 * Compose Canvas overlay that draws pose landmarks and skeleton connections.
 *
 * Uses FILL_CENTER scaling to match PreviewView.ScaleType.FILL_CENTER:
 * the image is scaled to FILL the canvas (cropping any overflow),
 * so the overlay aligns perfectly with the camera feed.
 *
 * For front camera, X is mirrored.
 * When [isPoseVisible] is false, skeleton renders in dimmed red.
 */
@Composable
fun PoseOverlayView(
    result: PoseLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true,
    isPoseVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lmColor = if (isPoseVisible) LANDMARK_COLOR else LANDMARK_COLOR_DIM
    val connColor = if (isPoseVisible) CONNECTION_COLOR else CONNECTION_COLOR_DIM

    Canvas(modifier = modifier.fillMaxSize()) {
        val landmarks = result?.landmarks()?.firstOrNull() ?: return@Canvas

        val canvasW = size.width
        val canvasH = size.height

        // FILL_CENTER: scale to fill the entire canvas, cropping overflow.
        // This matches PreviewView.ScaleType.FILL_CENTER exactly.
        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float

        if (imageWidth > 0 && imageHeight > 0) {
            val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
            val canvasAspect = canvasW / canvasH

            if (imageAspect > canvasAspect) {
                scaleY = canvasH
                scaleX = canvasH * imageAspect
                offsetX = (canvasW - scaleX) / 2f
                offsetY = 0f
            } else {
                scaleX = canvasW
                scaleY = canvasW / imageAspect
                offsetX = 0f
                offsetY = (canvasH - scaleY) / 2f
            }
        } else {
            scaleX = canvasW
            scaleY = canvasH
            offsetX = 0f
            offsetY = 0f
        }

        fun landmarkToOffset(idx: Int): Offset {
            val lm = landmarks[idx]
            val x = if (isFrontCamera) (1f - lm.x()) else lm.x()
            val y = lm.y()
            return Offset(
                x = offsetX + x * scaleX,
                y = offsetY + y * scaleY
            )
        }

        // Skip low-visibility landmarks when pose is not fully visible
        val minVis = if (isPoseVisible) 0f else 0.3f

        // Draw connections first (behind landmarks)
        for ((start, end) in POSE_CONNECTIONS) {
            if (start < landmarks.size && end < landmarks.size) {
                val startVis = landmarks[start].visibility().orElse(0f)
                val endVis = landmarks[end].visibility().orElse(0f)
                if (startVis < minVis || endVis < minVis) continue
                val startPt = landmarkToOffset(start)
                val endPt = landmarkToOffset(end)
                drawLine(
                    color = connColor,
                    start = startPt,
                    end = endPt,
                    strokeWidth = CONNECTION_STROKE,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw landmark points
        for (i in landmarks.indices) {
            val vis = landmarks[i].visibility().orElse(0f)
            if (vis < minVis) continue
            val pt = landmarkToOffset(i)
            drawCircle(
                color = lmColor,
                radius = LANDMARK_RADIUS,
                center = pt
            )
        }
    }
}
