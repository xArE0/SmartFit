package com.example.project_smartfit.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.project_smartfit.domain.model.LandmarkType
import com.example.project_smartfit.domain.model.PoseFrame

/**
 * MediaPipe Pose skeleton visualization
 * Draws landmarks and connections for BlazePose topology
 * Supports exercise-specific landmark filtering to reduce drawing overhead
 */
object MediaPipePoseDrawer {
    
    // Skeleton connections based on MediaPipe BlazePose topology
    private val POSE_CONNECTIONS = listOf(
        // Face
        LandmarkType.LEFT_EYE to LandmarkType.RIGHT_EYE,
        LandmarkType.LEFT_EYE to LandmarkType.NOSE,
        LandmarkType.RIGHT_EYE to LandmarkType.NOSE,
        LandmarkType.LEFT_EYE_INNER to LandmarkType.LEFT_EYE,
        LandmarkType.RIGHT_EYE_INNER to LandmarkType.RIGHT_EYE,
        LandmarkType.LEFT_EYE to LandmarkType.LEFT_EYE_OUTER,
        LandmarkType.RIGHT_EYE to LandmarkType.RIGHT_EYE_OUTER,
        LandmarkType.LEFT_EAR to LandmarkType.LEFT_EYE_OUTER,
        LandmarkType.RIGHT_EAR to LandmarkType.RIGHT_EYE_OUTER,
        LandmarkType.MOUTH_LEFT to LandmarkType.MOUTH_RIGHT,
        
        // Torso
        LandmarkType.LEFT_SHOULDER to LandmarkType.RIGHT_SHOULDER,
        LandmarkType.LEFT_SHOULDER to LandmarkType.LEFT_HIP,
        LandmarkType.RIGHT_SHOULDER to LandmarkType.RIGHT_HIP,
        LandmarkType.LEFT_HIP to LandmarkType.RIGHT_HIP,
        
        // Left arm
        LandmarkType.LEFT_SHOULDER to LandmarkType.LEFT_ELBOW,
        LandmarkType.LEFT_ELBOW to LandmarkType.LEFT_WRIST,
        LandmarkType.LEFT_WRIST to LandmarkType.LEFT_PINKY,
        LandmarkType.LEFT_WRIST to LandmarkType.LEFT_INDEX,
        LandmarkType.LEFT_WRIST to LandmarkType.LEFT_THUMB,
        LandmarkType.LEFT_PINKY to LandmarkType.LEFT_INDEX,
        
        // Right arm
        LandmarkType.RIGHT_SHOULDER to LandmarkType.RIGHT_ELBOW,
        LandmarkType.RIGHT_ELBOW to LandmarkType.RIGHT_WRIST,
        LandmarkType.RIGHT_WRIST to LandmarkType.RIGHT_PINKY,
        LandmarkType.RIGHT_WRIST to LandmarkType.RIGHT_INDEX,
        LandmarkType.RIGHT_WRIST to LandmarkType.RIGHT_THUMB,
        LandmarkType.RIGHT_PINKY to LandmarkType.RIGHT_INDEX,
        
        // Left leg
        LandmarkType.LEFT_HIP to LandmarkType.LEFT_KNEE,
        LandmarkType.LEFT_KNEE to LandmarkType.LEFT_ANKLE,
        LandmarkType.LEFT_ANKLE to LandmarkType.LEFT_HEEL,
        LandmarkType.LEFT_ANKLE to LandmarkType.LEFT_FOOT_INDEX,
        LandmarkType.LEFT_HEEL to LandmarkType.LEFT_FOOT_INDEX,
        
        // Right leg
        LandmarkType.RIGHT_HIP to LandmarkType.RIGHT_KNEE,
        LandmarkType.RIGHT_KNEE to LandmarkType.RIGHT_ANKLE,
        LandmarkType.RIGHT_ANKLE to LandmarkType.RIGHT_HEEL,
        LandmarkType.RIGHT_ANKLE to LandmarkType.RIGHT_FOOT_INDEX,
        LandmarkType.RIGHT_HEEL to LandmarkType.RIGHT_FOOT_INDEX
    )
    
    /**
     * Draw pose skeleton with form feedback colors and optional landmark filtering.
     * 
     * @param relevantIndices If non-null, only draw landmarks/connections where both
     *                        endpoints are in this set. Pass null to draw all landmarks.
     */
    fun DrawScope.drawPoseWithFeedback(
        poseFrame: PoseFrame,
        canvasWidth: Float,
        canvasHeight: Float,
        isFormCorrect: Boolean,
        errorJoints: List<String> = emptyList(),
        minVisibility: Float = 0.5f,
        relevantIndices: Set<Int>? = null
    ) {
        val baseColor = if (isFormCorrect) Color.Green else Color.Yellow
        val errorColor = Color.Red
        
        // Draw connections
        POSE_CONNECTIONS.forEach { (start, end) ->
            // Skip connections not relevant to this exercise
            if (relevantIndices != null && 
                (start.index !in relevantIndices || end.index !in relevantIndices)) {
                return@forEach
            }
            
            if (poseFrame.isVisible(start, minVisibility) && 
                poseFrame.isVisible(end, minVisibility)) {
                
                val startLandmark = poseFrame.getLandmark(start)
                val endLandmark = poseFrame.getLandmark(end)
                
                // Check if this connection involves an error joint
                val hasError = errorJoints.any { joint ->
                    start.name.contains(joint, ignoreCase = true) ||
                    end.name.contains(joint, ignoreCase = true)
                }
                
                val lineColor = if (hasError) errorColor else baseColor
                
                drawLine(
                    color = lineColor,
                    start = Offset(
                        x = startLandmark.x * canvasWidth,
                        y = startLandmark.y * canvasHeight
                    ),
                    end = Offset(
                        x = endLandmark.x * canvasWidth,
                        y = endLandmark.y * canvasHeight
                    ),
                    strokeWidth = if (hasError) 6f else 4f
                )
            }
        }
        
        // Draw landmarks
        poseFrame.landmarks.forEachIndexed { index, landmark ->
            // Skip landmarks not relevant to this exercise
            if (relevantIndices != null && index !in relevantIndices) {
                return@forEachIndexed
            }
            
            if (landmark.visibility >= minVisibility) {
                val x = landmark.x * canvasWidth
                val y = landmark.y * canvasHeight
                
                val landmarkType = LandmarkType.fromIndex(index)
                val hasError = landmarkType?.let { type ->
                    errorJoints.any { joint ->
                        type.name.contains(joint, ignoreCase = true)
                    }
                } ?: false
                
                val dotColor = if (hasError) errorColor else baseColor
                val dotSize = if (hasError) 10f else 6f
                
                // Draw outer circle
                drawCircle(
                    color = Color.White,
                    radius = dotSize + 2f,
                    center = Offset(x, y)
                )
                
                // Draw inner circle
                drawCircle(
                    color = dotColor,
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }
        }
    }
}
