package com.example.project_smartfit.domain.exercises.impl

import android.util.Log
import com.example.project_smartfit.domain.model.Exercise
import com.example.project_smartfit.domain.model.ExerciseState
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.domain.detection.Person
import com.example.project_smartfit.domain.features.FeatureExtractor
import com.example.project_smartfit.domain.features.PostureFeatures
import kotlin.math.abs

/**
 * Squats Exercise Implementation
 * 
 * SQUAT MECHANICS:
 * - Starting position: Standing upright with knees extended (160°+)
 * - Down position: Knees bent to approximately 90° or less
 * - Up position: Return to standing with knees extended
 * 
 * REP COUNTING:
 * - A rep is counted when transitioning from down → up
 * - Requires minimum confidence to prevent false counts
 * - Uses debouncing to avoid counting partial movements
 * 
 * POSTURE VALIDATION:
 * - Torso should remain relatively upright (< 30° lean)
 * - Both knees should move together (symmetric movement)
 * - Adequate pose confidence (> 50%) required
 */
class SquatsExercise : Exercise {

    override val type: ExerciseType = ExerciseType.SQUAT

    companion object {
        private const val TAG = "SquatsExercise"
        
        // Angle thresholds for squat detection
        private const val SQUAT_DOWN_KNEE_ANGLE = 90f    // knees bent deeply
        private const val SQUAT_UP_KNEE_ANGLE = 160f     // knees extended
        
        // Posture validation thresholds
        private const val MAX_TORSO_LEAN = 30f           // maximum forward lean allowed
        private const val MIN_POSE_CONFIDENCE = 0.5f     // minimum confidence for detection
        private const val MIN_REP_CONFIDENCE = 0.6f      // minimum confidence to count rep
        
        // Debouncing for state changes
        private const val STATE_CHANGE_DEBOUNCE = 3      // frames to wait before counting state change
    }

    // Internal state tracking
    private var previousFrameState: String = ""
    private var framesSinceStateChange = 0

    /**
     * Detect if the current pose looks like a squat
     */
    override fun detectExercise(person: Person, features: PostureFeatures): Boolean {
        // Check if overall person confidence is good enough
        if (person.score < MIN_POSE_CONFIDENCE) {
            return false
        }

        // Squat detection: knees bent, torso upright
        val kneesBent = features.leftKneeFlexion < 140f && features.rightKneeFlexion < 140f
        val torsoUpright = abs(features.torsoLean) < MAX_TORSO_LEAN

        Log.d(TAG, "Squat detection - Knees: L=${features.leftKneeFlexion}°, R=${features.rightKneeFlexion}°, " +
                "Torso lean: ${features.torsoLean}°, Confidence: ${person.score}")

        return kneesBent && torsoUpright
    }

    /**
     * Process a single frame for squat tracking
     */
    override fun processFrame(
        person: Person,
        features: PostureFeatures,
        currentState: ExerciseState
    ): ExerciseState {
        // Calculate average knee angle from both legs
        val leftKneeAngle = features.leftKneeFlexion
        val rightKneeAngle = features.rightKneeFlexion
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2

        // Determine current position state based on knee angle
        val currentFrameState = when {
            avgKneeAngle < SQUAT_DOWN_KNEE_ANGLE -> "down"  // Deep squat
            avgKneeAngle > SQUAT_UP_KNEE_ANGLE -> "up"       // Standing
            else -> "mid"                                     // Transitioning
        }

        // Log state for debugging
        Log.d(TAG, "Frame state: $currentFrameState, Avg knee angle: $avgKneeAngle°, " +
                "Previous: $previousFrameState")

        // Determine if we should count a new rep
        val shouldCountRep = shouldCountNewRep(
            previousState = currentFrameState,
            currentStateValue = currentFrameState,
            confidence = person.score
        )

        // Calculate new rep count
        val newRepCount = if (shouldCountRep) {
            currentState.repCount + 1
        } else {
            currentState.repCount
        }

        // Update previous state
        previousFrameState = currentFrameState

        // Estimate confidence based on pose quality and symmetry
        val kneeSymmetry = 1.0f - (abs(leftKneeAngle - rightKneeAngle) / 180f)
        val postureConfidence = (person.score + kneeSymmetry) / 2f

        // Return updated state
        return ExerciseState(
            exerciseType = ExerciseType.SQUAT,
            repCount = newRepCount,
            isInMotion = currentFrameState != previousFrameState && previousFrameState.isNotEmpty(),
            lastFrameState = currentFrameState,
            confidenceScore = postureConfidence
        )
    }

    /**
     * Determine if a new repetition should be counted
     * 
     * REP COUNTING LOGIC:
     * - Must transition from "down" → "up"
     * - Requires minimum confidence threshold
     * - Uses debouncing to prevent counting partial movements
     */
    private fun shouldCountNewRep(
        previousState: String,
        currentStateValue: String,
        confidence: Float
    ): Boolean {
        // Don't count reps if confidence is too low
        if (confidence < MIN_REP_CONFIDENCE) {
            framesSinceStateChange = 0
            return false
        }

        // Check if state actually changed
        val stateChanged = previousState != currentStateValue && previousState.isNotEmpty()

        if (!stateChanged) {
            framesSinceStateChange = 0
            return false
        }

        // Increment debounce counter
        framesSinceStateChange++

        // Only process after debounce period
        if (framesSinceStateChange < STATE_CHANGE_DEBOUNCE) {
            return false
        }

        // Check if this is a valid down → up transition
        val isValidRep = previousState == "down" && currentStateValue == "up"

        if (isValidRep) {
            Log.d(TAG, "✓ Rep counted! (down → up transition)")
            framesSinceStateChange = 0
        }

        return isValidRep
    }
}
