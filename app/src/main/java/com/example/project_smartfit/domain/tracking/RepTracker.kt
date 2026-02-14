package com.example.project_smartfit.domain.tracking

import com.example.project_smartfit.domain.model.*
import com.example.project_smartfit.domain.evaluation.ExerciseProfile
import com.example.project_smartfit.domain.evaluation.RepJoint
import java.util.LinkedList

/**
 * Tracks repetition phases and counts completed reps
 * Implements temporal smoothing to prevent false positives
 */
class RepTracker(
    private val profile: ExerciseProfile
) {
    private var currentPhase = RepPhase.UNKNOWN
    private var repCount = 0
    private var framesSincePhaseChange = 0
    private var pendingPhase = RepPhase.UNKNOWN
    
    // Moving average buffer for angle smoothing
    private val angleBuffer = LinkedList<Float>()
    private val bufferSize = 5  // Smooth over 5 frames
    
    // Track if we've completed bottom phase (required for rep)
    private var hasReachedBottom = false
    
    // Track if user has entered the starting position
    private var hasEnteredStartPhase = false
    
    /**
     * Process a feature vector and update rep tracking
     * Returns updated FormEvaluationResult with rep phase and count
     */
    fun track(features: FeatureVector, baseResult: FormEvaluationResult): FormEvaluationResult {
        // Get the primary angle to track based on exercise
        val currentAngle = getPrimaryAngle(features)
        
        // Add to smoothing buffer
        angleBuffer.add(currentAngle)
        if (angleBuffer.size > bufferSize) {
            angleBuffer.removeFirst()
        }
        
        // Calculate smoothed angle
        val smoothedAngle = angleBuffer.average().toFloat()
        
        // Determine current phase based on smoothed angle
        val detectedPhase = detectPhase(smoothedAngle)
        
        // Debounce phase changes
        if (detectedPhase == pendingPhase) {
            framesSincePhaseChange++
        } else {
            pendingPhase = detectedPhase
            framesSincePhaseChange = 1
        }
        
        // Only update phase after minimum frames
        var shouldIncrementRep = false
        if (framesSincePhaseChange >= profile.minFramesInPosition) {
            val oldPhase = currentPhase
            currentPhase = pendingPhase
            
            // Track bottom phase
            if (currentPhase == RepPhase.BOTTOM) {
                hasReachedBottom = true
            }
            
            // Track start phase entry
            if (!hasEnteredStartPhase && currentPhase == profile.startPhase) {
                hasEnteredStartPhase = true
            }
            
            // Count rep when returning to top after reaching bottom
            if (oldPhase != RepPhase.TOP && currentPhase == RepPhase.TOP && hasReachedBottom) {
                // Only count if form was correct
                if (baseResult.isCorrectForm || !baseResult.hasCriticalErrors()) {
                    repCount++
                    shouldIncrementRep = true
                }
                hasReachedBottom = false
            }
        }
        
        return baseResult.copy(
            repPhase = currentPhase,
            shouldIncrementRep = shouldIncrementRep
        )
    }
    
    /**
     * Get the primary angle to track based on exercise type
     */
    private fun getPrimaryAngle(features: FeatureVector): Float {
        return when (profile.repPrimaryJoint) {
            RepJoint.KNEE -> (features.leftKneeAngle + features.rightKneeAngle) / 2
            RepJoint.ELBOW -> (features.leftElbowAngle + features.rightElbowAngle) / 2
            RepJoint.HIP -> (features.leftHipAngle + features.rightHipAngle) / 2
            RepJoint.SHOULDER -> (features.leftShoulderAngle + features.rightShoulderAngle) / 2
        }
    }
    
    /**
     * Detect rep phase based on angle
     */
    private fun detectPhase(angle: Float): RepPhase {
        return when {
            angle >= profile.repTopAngleThreshold -> RepPhase.TOP
            angle <= profile.repBottomAngleThreshold -> RepPhase.BOTTOM
            angle < profile.repTopAngleThreshold && currentPhase == RepPhase.TOP -> RepPhase.DESCENDING
            angle > profile.repBottomAngleThreshold && currentPhase == RepPhase.BOTTOM -> RepPhase.ASCENDING
            currentPhase == RepPhase.DESCENDING -> RepPhase.DESCENDING
            currentPhase == RepPhase.ASCENDING -> RepPhase.ASCENDING
            else -> RepPhase.UNKNOWN
        }
    }
    
    /**
     * Get current rep count
     */
    fun getRepCount(): Int = repCount
    
    /**
     * Reset rep counter
     */
    fun reset() {
        repCount = 0
        currentPhase = RepPhase.UNKNOWN
        hasReachedBottom = false
        hasEnteredStartPhase = false
        angleBuffer.clear()
        framesSincePhaseChange = 0
    }
    
    /**
     * Get current phase
     */
    fun getCurrentPhase(): RepPhase = currentPhase
    
    /**
     * Check if user has entered the start phase
     */
    fun hasStarted(): Boolean = hasEnteredStartPhase
}
