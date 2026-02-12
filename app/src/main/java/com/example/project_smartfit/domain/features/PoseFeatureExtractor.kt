package com.example.project_smartfit.domain.features

import com.example.project_smartfit.domain.model.*
import com.example.project_smartfit.domain.features.AngleCalculator as Calc

/**
 * Extracts exercise-specific features from pose landmarks
 * Designed to be reusable across different exercises and evaluation strategies
 * 
 * All features are computed independently without hardcoded thresholds
 * The evaluator layer will apply thresholds based on ExerciseProfile
 */
class PoseFeatureExtractor {
    
    private val visibilityThreshold = 0.5f
    
    /**
     * Extract all features from a pose frame
     * Returns FeatureVector with computed metrics for all exercises
     */
    fun extract(poseFrame: PoseFrame): FeatureVector {
        return FeatureVector(
            // Squat features
            leftKneeAngle = calculateKneeAngle(poseFrame, isLeft = true),
            rightKneeAngle = calculateKneeAngle(poseFrame, isLeft = false),
            leftHipAngle = calculateHipAngle(poseFrame, isLeft = true),
            rightHipAngle = calculateHipAngle(poseFrame, isLeft = false),
            backAngle = calculateBackAngle(poseFrame),
            leftAnkleAngle = calculateAnkleAngle(poseFrame, isLeft = true),
            rightAnkleAngle = calculateAnkleAngle(poseFrame, isLeft = false),
            kneeDistanceRatio = calculateKneeDistanceRatio(poseFrame),
            hipDepthRatio = calculateHipDepthRatio(poseFrame),
            torsoLeanAngle = calculateTorsoLean(poseFrame),
            
            // Pushup features
            leftElbowAngle = calculateElbowAngle(poseFrame, isLeft = true),
            rightElbowAngle = calculateElbowAngle(poseFrame, isLeft = false),
            leftShoulderAngle = calculateShoulderAngle(poseFrame, isLeft = true),
            rightShoulderAngle = calculateShoulderAngle(poseFrame, isLeft = false),
            bodyAlignment = calculateBodyAlignment(poseFrame),
            bodyHorizontalAngle = calculateBodyHorizontalAngle(poseFrame),
            handPlacementWidth = calculateHandPlacementWidth(poseFrame),
            
            // Dumbbell curl features (reuse elbow angles)
            leftElbowFlexion = calculateElbowAngle(poseFrame, isLeft = true),
            rightElbowFlexion = calculateElbowAngle(poseFrame, isLeft = false),
            leftShoulderStability = calculateShoulderStability(poseFrame, isLeft = true),
            rightShoulderStability = calculateShoulderStability(poseFrame, isLeft = false),
            torsoUprightness = calculateTorsoUprightness(poseFrame),
            leftWristAlignment = calculateWristAlignment(poseFrame, isLeft = true),
            rightWristAlignment = calculateWristAlignment(poseFrame, isLeft = false),
            
            // Common features
            frameStability = 1.0f, // Will be calculated by temporal tracker
            overallVisibility = poseFrame.getAverageVisibility(),
            timestamp = poseFrame.timestamp
        )
    }
    
    // ==================== SQUAT FEATURES ====================
    
    /**
     * Calculate knee angle (Hip-Knee-Ankle)
     * Lower angle = deeper squat
     */
    private fun calculateKneeAngle(frame: PoseFrame, isLeft: Boolean): Float {
        val hip = if (isLeft) LandmarkType.LEFT_HIP else LandmarkType.RIGHT_HIP
        val knee = if (isLeft) LandmarkType.LEFT_KNEE else LandmarkType.RIGHT_KNEE
        val ankle = if (isLeft) LandmarkType.LEFT_ANKLE else LandmarkType.RIGHT_ANKLE
        
        if (!frame.areVisible(hip, knee, ankle, threshold = visibilityThreshold)) return 0f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(hip),
            frame.getLandmark(knee),
            frame.getLandmark(ankle)
        )
    }
    
    /**
     * Calculate hip angle (Shoulder-Hip-Knee)
     * Indicates hip flexion
     */
    private fun calculateHipAngle(frame: PoseFrame, isLeft: Boolean): Float {
        val shoulder = if (isLeft) LandmarkType.LEFT_SHOULDER else LandmarkType.RIGHT_SHOULDER
        val hip = if (isLeft) LandmarkType.LEFT_HIP else LandmarkType.RIGHT_HIP
        val knee = if (isLeft) LandmarkType.LEFT_KNEE else LandmarkType.RIGHT_KNEE
        
        if (!frame.areVisible(shoulder, hip, knee, threshold = visibilityThreshold)) return 0f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(shoulder),
            frame.getLandmark(hip),
            frame.getLandmark(knee)
        )
    }
    
    /**
     * Calculate back angle from vertical
     * 0° = perfectly vertical, higher = more lean
     */
    private fun calculateBackAngle(frame: PoseFrame): Float {
        val shoulderMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        
        return Calc.calculateAngleFromVertical(shoulderMid, hipMid)
    }
    
    /**
     * Calculate ankle dorsiflexion angle (Knee-Ankle-Foot)
     * Important for squat depth and heel stability
     */
    private fun calculateAnkleAngle(frame: PoseFrame, isLeft: Boolean): Float {
        val knee = if (isLeft) LandmarkType.LEFT_KNEE else LandmarkType.RIGHT_KNEE
        val ankle = if (isLeft) LandmarkType.LEFT_ANKLE else LandmarkType.RIGHT_ANKLE
        val footIndex = if (isLeft) LandmarkType.LEFT_FOOT_INDEX else LandmarkType.RIGHT_FOOT_INDEX
        
        if (!frame.areVisible(knee, ankle, footIndex, threshold = visibilityThreshold)) return 90f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(knee),
            frame.getLandmark(ankle),
            frame.getLandmark(footIndex)
        )
    }
    
    /**
     * Calculate knee distance ratio for valgus detection
     * Ratio of knee separation to hip width
     * < 1.0 = knees caving in, > 1.0 = knees pushing out
     */
    private fun calculateKneeDistanceRatio(frame: PoseFrame): Float {
        if (!frame.areVisible(
                LandmarkType.LEFT_KNEE, LandmarkType.RIGHT_KNEE,
                LandmarkType.LEFT_HIP, LandmarkType.RIGHT_HIP,
                threshold = visibilityThreshold
            )) return 1.0f
        
        val kneeDistance = Calc.calculateDistance2D(
            frame.getLandmark(LandmarkType.LEFT_KNEE),
            frame.getLandmark(LandmarkType.RIGHT_KNEE)
        )
        
        val hipDistance = Calc.calculateDistance2D(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        
        return if (hipDistance > 0f) kneeDistance / hipDistance else 1.0f
    }
    
    /**
     * Calculate hip depth ratio
     * Ratio of hip height to ankle height (lower = deeper squat)
     */
    private fun calculateHipDepthRatio(frame: PoseFrame): Float {
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        val ankleMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_ANKLE),
            frame.getLandmark(LandmarkType.RIGHT_ANKLE)
        )
        
        val hipHeight = hipMid.y
        val ankleHeight = ankleMid.y
        
        // In normalized coordinates, higher y = lower on screen
        // Ratio > 1 = hips above ankles (standing), < 1 = deep squat
        return if (ankleHeight > 0f) hipHeight / ankleHeight else 1.0f
    }
    
    /**
     * Calculate forward torso lean
     * Measures horizontal displacement of shoulders relative to hips
     */
    private fun calculateTorsoLean(frame: PoseFrame): Float {
        val shoulderMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        
        // Calculate angle from vertical
        val dx = shoulderMid.x - hipMid.x
        val dy = shoulderMid.y - hipMid.y
        
        return Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dy.toDouble())).toFloat()
    }
    
    // ==================== PUSHUP FEATURES ====================
    
    /**
     * Calculate elbow angle (Shoulder-Elbow-Wrist)
     * Lower angle = deeper pushup
     */
    private fun calculateElbowAngle(frame: PoseFrame, isLeft: Boolean): Float {
        val shoulder = if (isLeft) LandmarkType.LEFT_SHOULDER else LandmarkType.RIGHT_SHOULDER
        val elbow = if (isLeft) LandmarkType.LEFT_ELBOW else LandmarkType.RIGHT_ELBOW
        val wrist = if (isLeft) LandmarkType.LEFT_WRIST else LandmarkType.RIGHT_WRIST
        
        if (!frame.areVisible(shoulder, elbow, wrist, threshold = visibilityThreshold)) return 0f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(shoulder),
            frame.getLandmark(elbow),
            frame.getLandmark(wrist)
        )
    }
    
    /**
     * Calculate shoulder angle (Elbow-Shoulder-Hip)
     * Detects elbow flaring
     */
    private fun calculateShoulderAngle(frame: PoseFrame, isLeft: Boolean): Float {
        val elbow = if (isLeft) LandmarkType.LEFT_ELBOW else LandmarkType.RIGHT_ELBOW
        val shoulder = if (isLeft) LandmarkType.LEFT_SHOULDER else LandmarkType.RIGHT_SHOULDER
        val hip = if (isLeft) LandmarkType.LEFT_HIP else LandmarkType.RIGHT_HIP
        
        if (!frame.areVisible(elbow, shoulder, hip, threshold = visibilityThreshold)) return 0f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(elbow),
            frame.getLandmark(shoulder),
            frame.getLandmark(hip)
        )
    }
    
    /**
     * Calculate body alignment score (Shoulder-Hip-Ankle linearity)
     * 100 = perfectly straight, 0 = severely bent
     */
    private fun calculateBodyAlignment(frame: PoseFrame): Float {
        val shoulderMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        val ankleMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_ANKLE),
            frame.getLandmark(LandmarkType.RIGHT_ANKLE)
        )
        
        return Calc.calculateAlignmentScore(shoulderMid, hipMid, ankleMid)
    }
    
    /**
     * Calculate body angle from horizontal
     * 0° = perfectly horizontal, higher = more inclined
     */
    private fun calculateBodyHorizontalAngle(frame: PoseFrame): Float {
        val shoulderMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        
        return Calc.calculateAngleFromHorizontal(shoulderMid, hipMid)
    }
    
    /**
     * Calculate hand placement width ratio
     * Ratio of wrist distance to shoulder width
     */
    private fun calculateHandPlacementWidth(frame: PoseFrame): Float {
        if (!frame.areVisible(
                LandmarkType.LEFT_WRIST, LandmarkType.RIGHT_WRIST,
                LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER,
                threshold = visibilityThreshold
            )) return 1.0f
        
        val wristDistance = Calc.calculateDistance2D(
            frame.getLandmark(LandmarkType.LEFT_WRIST),
            frame.getLandmark(LandmarkType.RIGHT_WRIST)
        )
        
        val shoulderDistance = Calc.calculateDistance2D(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        
        return if (shoulderDistance > 0f) wristDistance / shoulderDistance else 1.0f
    }
    
    // ==================== DUMBBELL CURL FEATURES ====================
    
    /**
     * Calculate shoulder stability (movement score)
     * Lower = more stable, higher = more swinging
     * This is a placeholder - real implementation would track shoulder position over time
     */
    private fun calculateShoulderStability(frame: PoseFrame, isLeft: Boolean): Float {
        // For now, return a default value
        // Real implementation would compare shoulder position across frames
        return 0f
    }
    
    /**
     * Calculate torso uprightness score
     * 100 = perfectly vertical, 0 = horizontal
     */
    private fun calculateTorsoUprightness(frame: PoseFrame): Float {
        val shoulderMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_SHOULDER),
            frame.getLandmark(LandmarkType.RIGHT_SHOULDER)
        )
        val hipMid = Calc.calculateMidpoint(
            frame.getLandmark(LandmarkType.LEFT_HIP),
            frame.getLandmark(LandmarkType.RIGHT_HIP)
        )
        
        val angleFromVertical = Calc.calculateAngleFromVertical(shoulderMid, hipMid)
        
        // Convert to 0-100 score (0° from vertical = 100 score)
        return 100f - angleFromVertical
    }
    
    /**
     * Calculate wrist alignment angle
     * Measures wrist bend relative to forearm
     */
    private fun calculateWristAlignment(frame: PoseFrame, isLeft: Boolean): Float {
        val elbow = if (isLeft) LandmarkType.LEFT_ELBOW else LandmarkType.RIGHT_ELBOW
        val wrist = if (isLeft) LandmarkType.LEFT_WRIST else LandmarkType.RIGHT_WRIST
        val index = if (isLeft) LandmarkType.LEFT_INDEX else LandmarkType.RIGHT_INDEX
        
        if (!frame.areVisible(elbow, wrist, index, threshold = visibilityThreshold)) return 180f
        
        return Calc.calculate3PointAngle(
            frame.getLandmark(elbow),
            frame.getLandmark(wrist),
            frame.getLandmark(index)
        )
    }
}
