package com.example.project_smartfit.domain.evaluation

/**
 * Predefined exercise profiles with validated thresholds
 * Based on exercise science and biomechanics research
 */
object ExerciseProfiles {
    
    /**
     * SQUAT PROFILE
     * Proper squat form requirements:
     * - Knees: 70-110° at bottom (parallel or below)
     * - Hips: 60-100° at bottom
     * - Back: < 45° from vertical (minimal forward lean)
     * - Knees: Should track over toes, not cave inward
     * - Depth: Hips should drop below knee level
     */
    val SQUAT = ExerciseProfile(
        name = "Squat",
        
        // Joint angles
        kneeAngleRange = AngleRange(
            min = 70f,
            max = 110f,
            errorMessageBelow = "Squat too deep - risk of knee strain",
            errorMessageAbove = "Not deep enough - go lower"
        ),
        hipAngleRange = AngleRange(
            min = 60f,
            max = 100f,
            errorMessageBelow = "Excessive hip flexion",
            errorMessageAbove = "Insufficient hip flexion"
        ),
        ankleAngleRange = AngleRange(
            min = 70f,
            max = 110f,
            errorMessageBelow = "Excessive ankle dorsiflexion",
            errorMessageAbove = "Limited ankle mobility - heels lifting?"
        ),
        
        // Posture thresholds
        maxBackAngle = 45f,  // Max 45° lean from vertical
        maxTorsoLean = 30f,  // Max 30° forward lean
        
        // Alignment ratios
        kneeDistanceRatioRange = RatioRange(
            min = 0.85f,  // Knees shouldn't cave in more than 15%
            max = 1.3f,   // Knees shouldn't push out excessively
            errorMessageBelow = "Knees collapsing inward (valgus)",
            errorMessageAbove = "Knees pushing out excessively"
        ),
        hipDepthRatioThreshold = 1.0f,  // Hips should be at or below knee level
        
        // Rep detection
        repTopAngleThreshold = 160f,    // Standing: knee angle > 160°
        repBottomAngleThreshold = 100f, // Bottom: knee angle < 100°
        repPrimaryJoint = RepJoint.KNEE,
        
        // Temporal rules
        minFramesInPosition = 3,
        minFramesForRep = 15,
        
        // Critical errors
        criticalErrors = setOf(
            "KNEES_CAVING_INWARD",
            "EXCESSIVE_FORWARD_LEAN",
            "BACK_ROUNDING"
        )
    )
    
    /**
     * PUSHUP PROFILE
     * Proper pushup form requirements:
     * - Elbows: 70-100° at bottom
     * - Body: Straight line from shoulders to ankles
     * - Elbows: 45° angle from body (not flaring)
     * - Full range of motion
     */
    val PUSHUP = ExerciseProfile(
        name = "Pushup",
        
        // Joint angles
        elbowAngleRange = AngleRange(
            min = 70f,
            max = 100f,
            errorMessageBelow = "Going too low - risk of shoulder strain",
            errorMessageAbove = "Not low enough - increase range of motion"
        ),
        shoulderAngleRange = AngleRange(
            min = 30f,   // Elbows tucked
            max = 90f,   // Elbows flaring
            errorMessageBelow = "Elbows too tucked",
            errorMessageAbove = "Elbows flaring out - tuck them in"
        ),
        
        // Body alignment
        minBodyAlignment = 85f,  // Min 85/100 alignment score
        maxBodyHorizontalAngle = 15f,  // Body should be nearly horizontal
        
        // Hand placement
        handPlacementWidthRange = RatioRange(
            min = 1.0f,
            max = 1.5f,
            errorMessageBelow = "Hands too narrow",
            errorMessageAbove = "Hands too wide"
        ),
        
        // Rep detection
        repTopAngleThreshold = 160f,    // Arms extended: elbow > 160°
        repBottomAngleThreshold = 90f,  // Bottom: elbow < 90°
        repPrimaryJoint = RepJoint.ELBOW,
        
        // Temporal rules
        minFramesInPosition = 2,
        minFramesForRep = 12,
        
        // Critical errors
        criticalErrors = setOf(
            "SAGGING_HIPS",
            "RAISED_HIPS",
            "ELBOWS_FLARING"
        )
    )
    
    /**
     * DUMBBELL CURL PROFILE
     * Proper curl form requirements:
     * - Elbows: 30-50° at top (full flexion)
     * - Elbows: 160-180° at bottom (full extension)
     * - Torso: Upright, minimal swinging
     * - Shoulders: Stable, not moving forward
     */
    val DUMBBELL_CURL = ExerciseProfile(
        name = "Dumbbell Curl",
        
        // Joint angles
        elbowAngleRange = AngleRange(
            min = 30f,
            max = 50f,
            errorMessageBelow = "Over-curling",
            errorMessageAbove = "Incomplete curl - bring weight higher"
        ),
        
        // Posture
        minTorsoUprightness = 85f,  // Torso should be nearly vertical
        
        // Wrist alignment
        wristAngleRange = AngleRange(
            min = 160f,
            max = 200f,  // Slight extension is okay
            errorMessageBelow = "Wrist bending too much",
            errorMessageAbove = "Wrist hyperextended"
        ),
        
        // Rep detection
        repTopAngleThreshold = 50f,     // Top: elbow < 50° (flexed)
        repBottomAngleThreshold = 160f, // Bottom: elbow > 160° (extended)
        repPrimaryJoint = RepJoint.ELBOW,
        
        // Temporal rules
        minFramesInPosition = 2,
        minFramesForRep = 10,
        
        // Critical errors
        criticalErrors = setOf(
            "SWINGING_BODY",
            "ELBOW_MOVEMENT"
        )
    )
    
    /**
     * Get profile by exercise type
     */
    fun getProfile(exerciseType: com.example.project_smartfit.domain.model.ExerciseType): ExerciseProfile {
        return when (exerciseType) {
            com.example.project_smartfit.domain.model.ExerciseType.SQUAT -> SQUAT
            com.example.project_smartfit.domain.model.ExerciseType.PUSHUP -> PUSHUP
            com.example.project_smartfit.domain.model.ExerciseType.DUMBBELL_CURL -> DUMBBELL_CURL
            else -> SQUAT // Default fallback
        }
    }
}
