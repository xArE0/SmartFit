package com.example.project_smartfit.domain.evaluation

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
    
    // Error severity mappings
    val criticalErrors: Set<String> = emptySet(), // Error types considered critical
    
    // Visibility requirements
    val minVisibility: Float = 0.5f               // Minimum landmark visibility
)

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
