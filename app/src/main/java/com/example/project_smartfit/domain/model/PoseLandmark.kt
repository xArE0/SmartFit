package com.example.project_smartfit.domain.model

/**
 * Represents a single pose landmark from MediaPipe Pose
 * MediaPipe provides 33 landmarks with normalized coordinates
 */
data class PoseLandmark(
    val x: Float,           // Normalized x coordinate [0, 1]
    val y: Float,           // Normalized y coordinate [0, 1]
    val z: Float,           // Depth relative to hips (negative = closer to camera)
    val visibility: Float   // Confidence score [0, 1]
)

/**
 * MediaPipe Pose Landmark indices (33 total)
 * BlazePose topology
 */
enum class LandmarkType(val index: Int) {
    // Face
    NOSE(0),
    LEFT_EYE_INNER(1),
    LEFT_EYE(2),
    LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4),
    RIGHT_EYE(5),
    RIGHT_EYE_OUTER(6),
    LEFT_EAR(7),
    RIGHT_EAR(8),
    MOUTH_LEFT(9),
    MOUTH_RIGHT(10),
    
    // Upper body
    LEFT_SHOULDER(11),
    RIGHT_SHOULDER(12),
    LEFT_ELBOW(13),
    RIGHT_ELBOW(14),
    LEFT_WRIST(15),
    RIGHT_WRIST(16),
    LEFT_PINKY(17),
    RIGHT_PINKY(18),
    LEFT_INDEX(19),
    RIGHT_INDEX(20),
    LEFT_THUMB(21),
    RIGHT_THUMB(22),
    
    // Lower body
    LEFT_HIP(23),
    RIGHT_HIP(24),
    LEFT_KNEE(25),
    RIGHT_KNEE(26),
    LEFT_ANKLE(27),
    RIGHT_ANKLE(28),
    LEFT_HEEL(29),
    RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31),
    RIGHT_FOOT_INDEX(32);
    
    companion object {
        fun fromIndex(index: Int): LandmarkType? = entries.find { it.index == index }
    }
}
