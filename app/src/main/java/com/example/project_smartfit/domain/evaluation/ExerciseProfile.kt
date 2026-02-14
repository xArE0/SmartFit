package com.example.project_smartfit.domain.evaluation

import com.example.project_smartfit.domain.model.RepPhase

/**
 * Configuration for exercise-specific form rules
 * All thresholds and ranges are data-driven, not hardcoded in logic
 */
data class ExerciseProfile(
    val name: String,
    
    // Angle ranges (min, max) in degrees
    val kneeAngleRange: AngleRange? = null,
    val hipAngleRange: AngleRange? = null,
    val elbowAngleRange: AngleRange? = null,
    val shoulderAngleRange: AngleRange? = null,
    val ankleAngleRange: AngleRange? = null,
    val wristAngleRange: AngleRange? = null,
    
    // Alignment and posture thresholds
    val maxBackAngle: Float? = null,              // Max degrees from vertical
    val maxTorsoLean: Float? = null,              // Max forward lean
    val minBodyAlignment: Float? = null,          // Min alignment score (0-100)
    val maxBodyHorizontalAngle: Float? = null,    // Max angle from horizontal
    val minTorsoUprightness: Float? = null,       // Min uprightness score (0-100)
    
    // Ratio thresholds
    val kneeDistanceRatioRange: RatioRange? = null,  // Min/max knee-to-hip width ratio
    val hipDepthRatioThreshold: Float? = null,       // Max ratio for sufficient depth
    val handPlacementWidthRange: RatioRange? = null, // Min/max hand-to-shoulder width
    
    // Temporal rules
    val minFramesInPosition: Int = 3,             // Minimum frames to confirm position
    val minFramesForRep: Int = 10,                // Minimum frames for valid rep
    
    // Rep detection thresholds
    val repTopAngleThreshold: Float,              // Angle threshold for top position
    val repBottomAngleThreshold: Float,           // Angle threshold for bottom position
    val repPrimaryJoint: RepJoint,                // Which joint angle to track for reps
    val startPhase: RepPhase = RepPhase.TOP,      // Phase required to start tracking
    
    // Error severity mappings
    val criticalErrors: Set<String> = emptySet(), // Error types considered critical
    
    // Visibility requirements
    val minVisibility: Float = 0.5f,              // Minimum landmark visibility
    
    // Performance: which landmark groups are relevant for this exercise
    // Only these will be drawn/processed, reducing overhead
    val relevantLandmarkGroups: Set<LandmarkGroup> = LandmarkGroup.entries.toSet()
)

/**
 * Groups of MediaPipe landmarks for exercise-specific filtering.
 * Each group maps to a set of MediaPipe Pose landmark indices (0-32).
 */
enum class LandmarkGroup(val indices: Set<Int>) {
    FACE(setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),   // nose, eyes, ears, mouth
    SHOULDERS(setOf(11, 12)),                           // left/right shoulder
    ARMS(setOf(13, 14, 15, 16)),                        // elbows, wrists
    HANDS(setOf(17, 18, 19, 20, 21, 22)),               // fingers, thumbs, pinkies
    TORSO(setOf(11, 12, 23, 24)),                       // shoulders + hips
    HIPS(setOf(23, 24)),                                // left/right hip
    LEGS(setOf(25, 26, 27, 28)),                        // knees, ankles
    FEET(setOf(29, 30, 31, 32));                        // heels, toes
    
    companion object {
        /** Get all landmark indices for a set of groups */
        fun indicesFor(groups: Set<LandmarkGroup>): Set<Int> {
            return groups.flatMap { it.indices }.toSet()
        }
    }
}

/**
 * Angle range for validation
 */
data class AngleRange(
    val min: Float,
    val max: Float,
    val errorMessageBelow: String = "Angle too small",
    val errorMessageAbove: String = "Angle too large"
)

/**
 * Ratio range for validation
 */
data class RatioRange(
    val min: Float,
    val max: Float,
    val errorMessageBelow: String = "Ratio too low",
    val errorMessageAbove: String = "Ratio too high"
)

/**
 * Which joint to track for rep counting
 */
enum class RepJoint {
    KNEE,       // For squats
    ELBOW,      // For pushups and curls
    HIP,        // Alternative for squats
    SHOULDER    // For overhead exercises
}
