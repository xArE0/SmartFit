package com.example.project_smartfit.domain.model

/**
 * Structured container for computed pose features
 * Designed to be input for both rule-based evaluation and future ML models
 * 
 * Features are organized by exercise type for clarity
 * All angles in degrees, all distances/ratios normalized
 */
data class FeatureVector(
    // === SQUAT FEATURES ===
    val leftKneeAngle: Float = 0f,          // Hip-Knee-Ankle angle (left)
    val rightKneeAngle: Float = 0f,         // Hip-Knee-Ankle angle (right)
    val leftHipAngle: Float = 0f,           // Shoulder-Hip-Knee angle (left)
    val rightHipAngle: Float = 0f,          // Shoulder-Hip-Knee angle (right)
    val backAngle: Float = 0f,              // Torso angle from vertical
    val leftAnkleAngle: Float = 0f,         // Knee-Ankle-Foot angle (dorsiflexion)
    val rightAnkleAngle: Float = 0f,        // Knee-Ankle-Foot angle (dorsiflexion)
    val kneeDistanceRatio: Float = 0f,      // Knee separation / hip width (valgus detection)
    val hipDepthRatio: Float = 0f,          // Hip height / ankle height (depth measurement)
    val torsoLeanAngle: Float = 0f,         // Forward lean of torso
    
    // === PUSHUP FEATURES ===
    val leftElbowAngle: Float = 0f,         // Shoulder-Elbow-Wrist angle (left)
    val rightElbowAngle: Float = 0f,        // Shoulder-Elbow-Wrist angle (right)
    val leftShoulderAngle: Float = 0f,      // Elbow-Shoulder-Hip angle (left)
    val rightShoulderAngle: Float = 0f,     // Elbow-Shoulder-Hip angle (right)
    val bodyAlignment: Float = 0f,          // Shoulder-Hip-Ankle linearity (0-100 score)
    val bodyHorizontalAngle: Float = 0f,    // Body angle from horizontal
    val handPlacementWidth: Float = 0f,     // Distance between wrists / shoulder width
    
    // === DUMBBELL CURL FEATURES ===
    val leftElbowFlexion: Float = 0f,       // Elbow flexion for curl (same as elbow angle)
    val rightElbowFlexion: Float = 0f,      // Elbow flexion for curl
    val leftShoulderStability: Float = 0f,  // Shoulder movement (should be minimal)
    val rightShoulderStability: Float = 0f, // Shoulder movement (should be minimal)
    val torsoUprightness: Float = 0f,       // Torso vertical alignment (0-100 score)
    val leftWristAlignment: Float = 0f,     // Wrist angle relative to forearm
    val rightWristAlignment: Float = 0f,    // Wrist angle relative to forearm
    
    // === COMMON FEATURES ===
    val frameStability: Float = 1.0f,       // Smoothness score (1.0 = stable, lower = jittery)
    val overallVisibility: Float = 0f,      // Average landmark visibility
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convert to float array for ML model input
     * Maintains consistent feature order
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            // Squat features (10)
            leftKneeAngle, rightKneeAngle,
            leftHipAngle, rightHipAngle,
            backAngle,
            leftAnkleAngle, rightAnkleAngle,
            kneeDistanceRatio, hipDepthRatio,
            torsoLeanAngle,
            
            // Pushup features (7)
            leftElbowAngle, rightElbowAngle,
            leftShoulderAngle, rightShoulderAngle,
            bodyAlignment, bodyHorizontalAngle,
            handPlacementWidth,
            
            // Dumbbell curl features (7)
            leftElbowFlexion, rightElbowFlexion,
            leftShoulderStability, rightShoulderStability,
            torsoUprightness,
            leftWristAlignment, rightWristAlignment,
            
            // Common features (2)
            frameStability, overallVisibility
        )
    }
    
    /**
     * Get feature count for ML model configuration
     */
    companion object {
        const val FEATURE_COUNT = 26
        
        /**
         * Get feature names for debugging and ML model interpretation
         */
        fun getFeatureNames(): List<String> {
            return listOf(
                "left_knee_angle", "right_knee_angle",
                "left_hip_angle", "right_hip_angle",
                "back_angle",
                "left_ankle_angle", "right_ankle_angle",
                "knee_distance_ratio", "hip_depth_ratio",
                "torso_lean_angle",
                "left_elbow_angle", "right_elbow_angle",
                "left_shoulder_angle", "right_shoulder_angle",
                "body_alignment", "body_horizontal_angle",
                "hand_placement_width",
                "left_elbow_flexion", "right_elbow_flexion",
                "left_shoulder_stability", "right_shoulder_stability",
                "torso_uprightness",
                "left_wrist_alignment", "right_wrist_alignment",
                "frame_stability", "overall_visibility"
            )
        }
    }
}
