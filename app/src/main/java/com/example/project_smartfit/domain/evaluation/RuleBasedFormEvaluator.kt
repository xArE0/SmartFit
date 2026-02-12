package com.example.project_smartfit.domain.evaluation

import com.example.project_smartfit.domain.model.*

/**
 * Rule-based form evaluator
 * Evaluates pose features against exercise-specific rules defined in ExerciseProfile
 * 
 * This is the PRIMARY evaluator - ML layer will enhance its output later
 */
class RuleBasedFormEvaluator(
    private val profile: ExerciseProfile
) {
    
    /**
     * Evaluate form based on feature vector and exercise profile
     * Returns detailed evaluation result with errors and feedback
     */
    fun evaluate(features: FeatureVector): FormEvaluationResult {
        val errors = mutableListOf<FormError>()
        val feedbackMessages = mutableListOf<String>()
        
        // Check visibility first
        if (features.overallVisibility < profile.minVisibility) {
            return FormEvaluationResult(
                isCorrectForm = false,
                errors = listOf(
                    FormError(
                        type = ErrorType.LOW_VISIBILITY,
                        message = "Poor pose detection - adjust camera angle",
                        severity = ErrorSeverity.CRITICAL
                    )
                ),
                feedbackMessages = listOf("Move into better lighting or adjust camera position"),
                confidenceScore = features.overallVisibility
            )
        }
        
        // Evaluate based on exercise type
        when (profile.repPrimaryJoint) {
            RepJoint.KNEE -> evaluateSquatForm(features, errors, feedbackMessages)
            RepJoint.ELBOW -> {
                if (profile.name == "Pushup") {
                    evaluatePushupForm(features, errors, feedbackMessages)
                } else {
                    evaluateCurlForm(features, errors, feedbackMessages)
                }
            }
            else -> {}
        }
        
        // Determine if form is correct
        val isCorrect = errors.none { it.severity == ErrorSeverity.CRITICAL } && 
                        errors.size <= 1  // Allow one minor warning
        
        return FormEvaluationResult(
            isCorrectForm = isCorrect,
            errors = errors,
            feedbackMessages = feedbackMessages,
            confidenceScore = features.overallVisibility
        )
    }
    
    /**
     * Evaluate squat-specific form
     */
    private fun evaluateSquatForm(
        features: FeatureVector,
        errors: MutableList<FormError>,
        feedback: MutableList<String>
    ) {
        val avgKneeAngle = (features.leftKneeAngle + features.rightKneeAngle) / 2
        
        // Check knee angle range
        profile.kneeAngleRange?.let { range ->
            when {
                avgKneeAngle < range.min -> {
                    errors.add(FormError(
                        type = ErrorType.INSUFFICIENT_DEPTH,
                        message = range.errorMessageBelow,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("knees")
                    ))
                    feedback.add("Squat depth is excessive - risk of injury")
                }
                avgKneeAngle > range.max -> {
                    errors.add(FormError(
                        type = ErrorType.INSUFFICIENT_DEPTH,
                        message = range.errorMessageAbove,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("knees")
                    ))
                    feedback.add("Go deeper - aim for thighs parallel to ground")
                }
            }
        }
        
        // Check knee valgus (knees caving in)
        profile.kneeDistanceRatioRange?.let { range ->
            when {
                features.kneeDistanceRatio < range.min -> {
                    errors.add(FormError(
                        type = ErrorType.KNEES_CAVING_INWARD,
                        message = range.errorMessageBelow,
                        severity = ErrorSeverity.CRITICAL,
                        affectedJoints = listOf("knees")
                    ))
                    feedback.add("Push knees outward - align with toes")
                }
                features.kneeDistanceRatio > range.max -> {
                    errors.add(FormError(
                        type = ErrorType.KNEES_CAVING_INWARD,
                        message = range.errorMessageAbove,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("knees")
                    ))
                    feedback.add("Knees pushing out too much")
                }
            }
        }
        
        // Check back angle
        profile.maxBackAngle?.let { maxAngle ->
            if (features.backAngle > maxAngle) {
                errors.add(FormError(
                    type = ErrorType.EXCESSIVE_FORWARD_LEAN,
                    message = "Excessive forward lean",
                    severity = ErrorSeverity.CRITICAL,
                    affectedJoints = listOf("back", "hips")
                ))
                feedback.add("Keep chest up and back straight")
            }
        }
        
        // Check torso lean
        profile.maxTorsoLean?.let { maxLean ->
            if (kotlin.math.abs(features.torsoLeanAngle) > maxLean) {
                errors.add(FormError(
                    type = ErrorType.EXCESSIVE_FORWARD_LEAN,
                    message = "Torso leaning too far forward",
                    severity = ErrorSeverity.WARNING,
                    affectedJoints = listOf("torso")
                ))
                feedback.add("Maintain upright torso position")
            }
        }
        
        // Check ankle angles (heel lifting)
        profile.ankleAngleRange?.let { range ->
            val avgAnkleAngle = (features.leftAnkleAngle + features.rightAnkleAngle) / 2
            if (avgAnkleAngle > range.max) {
                errors.add(FormError(
                    type = ErrorType.HEEL_LIFTING,
                    message = "Heels lifting off ground",
                    severity = ErrorSeverity.WARNING,
                    affectedJoints = listOf("ankles")
                ))
                feedback.add("Keep heels planted on the ground")
            }
        }
        
        // Check asymmetry
        val kneeAngleDiff = kotlin.math.abs(features.leftKneeAngle - features.rightKneeAngle)
        if (kneeAngleDiff > 15f) {
            errors.add(FormError(
                type = ErrorType.ASYMMETRIC_MOVEMENT,
                message = "Uneven movement between left and right",
                severity = ErrorSeverity.WARNING,
                affectedJoints = listOf("knees")
            ))
            feedback.add("Balance weight evenly on both legs")
        }
    }
    
    /**
     * Evaluate pushup-specific form
     */
    private fun evaluatePushupForm(
        features: FeatureVector,
        errors: MutableList<FormError>,
        feedback: MutableList<String>
    ) {
        val avgElbowAngle = (features.leftElbowAngle + features.rightElbowAngle) / 2
        
        // Check elbow angle range
        profile.elbowAngleRange?.let { range ->
            when {
                avgElbowAngle < range.min -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_RANGE,
                        message = range.errorMessageBelow,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("elbows")
                    ))
                    feedback.add("Going too low - risk of shoulder injury")
                }
                avgElbowAngle > range.max -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_RANGE,
                        message = range.errorMessageAbove,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("elbows")
                    ))
                    feedback.add("Lower your chest closer to the ground")
                }
            }
        }
        
        // Check elbow flaring
        profile.shoulderAngleRange?.let { range ->
            val avgShoulderAngle = (features.leftShoulderAngle + features.rightShoulderAngle) / 2
            if (avgShoulderAngle > range.max) {
                errors.add(FormError(
                    type = ErrorType.ELBOWS_FLARING,
                    message = "Elbows flaring out too much",
                    severity = ErrorSeverity.CRITICAL,
                    affectedJoints = listOf("elbows", "shoulders")
                ))
                feedback.add("Keep elbows at 45° angle from body")
            }
        }
        
        // Check body alignment
        profile.minBodyAlignment?.let { minAlignment ->
            if (features.bodyAlignment < minAlignment) {
                val deviation = minAlignment - features.bodyAlignment
                if (deviation > 10f) {
                    errors.add(FormError(
                        type = ErrorType.SAGGING_HIPS,
                        message = "Body not in straight line",
                        severity = ErrorSeverity.CRITICAL,
                        affectedJoints = listOf("core", "hips")
                    ))
                    feedback.add("Engage core - keep body straight")
                } else {
                    errors.add(FormError(
                        type = ErrorType.RAISED_HIPS,
                        message = "Hips slightly raised",
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("hips")
                    ))
                    feedback.add("Lower hips slightly for better alignment")
                }
            }
        }
        
        // Check hand placement
        profile.handPlacementWidthRange?.let { range ->
            when {
                features.handPlacementWidth < range.min -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_RANGE,
                        message = "Hands too narrow",
                        severity = ErrorSeverity.INFO,
                        affectedJoints = listOf("hands")
                    ))
                    feedback.add("Widen hand placement slightly")
                }
                features.handPlacementWidth > range.max -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_RANGE,
                        message = "Hands too wide",
                        severity = ErrorSeverity.INFO,
                        affectedJoints = listOf("hands")
                    ))
                    feedback.add("Bring hands closer together")
                }
            }
        }
    }
    
    /**
     * Evaluate dumbbell curl-specific form
     */
    private fun evaluateCurlForm(
        features: FeatureVector,
        errors: MutableList<FormError>,
        feedback: MutableList<String>
    ) {
        val avgElbowAngle = (features.leftElbowFlexion + features.rightElbowFlexion) / 2
        
        // Check elbow flexion range
        profile.elbowAngleRange?.let { range ->
            when {
                avgElbowAngle < range.min -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_CURL,
                        message = range.errorMessageBelow,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("elbows")
                    ))
                    feedback.add("Over-curling - stop at shoulder level")
                }
                avgElbowAngle > range.max -> {
                    errors.add(FormError(
                        type = ErrorType.INCOMPLETE_CURL,
                        message = range.errorMessageAbove,
                        severity = ErrorSeverity.WARNING,
                        affectedJoints = listOf("elbows")
                    ))
                    feedback.add("Curl weight higher - full range of motion")
                }
            }
        }
        
        // Check torso stability
        profile.minTorsoUprightness?.let { minUprightness ->
            if (features.torsoUprightness < minUprightness) {
                errors.add(FormError(
                    type = ErrorType.SWINGING_BODY,
                    message = "Excessive body movement",
                    severity = ErrorSeverity.CRITICAL,
                    affectedJoints = listOf("torso")
                ))
                feedback.add("Keep torso stable - no swinging")
            }
        }
        
        // Check wrist alignment
        profile.wristAngleRange?.let { range ->
            val avgWristAngle = (features.leftWristAlignment + features.rightWristAlignment) / 2
            if (avgWristAngle < range.min) {
                errors.add(FormError(
                    type = ErrorType.WRIST_BENDING,
                    message = "Wrists bending excessively",
                    severity = ErrorSeverity.WARNING,
                    affectedJoints = listOf("wrists")
                ))
                feedback.add("Keep wrists neutral and aligned")
            }
        }
        
        // Check shoulder stability (placeholder - needs temporal tracking)
        val avgShoulderMovement = (features.leftShoulderStability + features.rightShoulderStability) / 2
        if (avgShoulderMovement > 10f) {
            errors.add(FormError(
                type = ErrorType.ELBOW_MOVEMENT,
                message = "Elbows moving forward",
                severity = ErrorSeverity.WARNING,
                affectedJoints = listOf("shoulders", "elbows")
            ))
            feedback.add("Keep elbows stationary at your sides")
        }
    }
}
