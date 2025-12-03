package com.example.project_smartfit

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Types of exercises that can be detected
 */
enum class ExerciseType {
    PUSHUP,
    SQUAT,
    PLANK,
    JUMPING_JACKS,
    DUMBBELL_CURL,
    UNKNOWN
}

/**
 * State of exercise detection and repetition tracking
 */
data class ExerciseState(
    val exerciseType: ExerciseType = ExerciseType.UNKNOWN,
    val repCount: Int = 0,
    val isInMotion: Boolean = false,
    val lastFrameState: String = "", // "up", "down", "hold", etc.
    val confidenceScore: Float = 0f
)

/**
 * Exercise detector that identifies which exercise is being performed
 * and tracks repetitions based on joint angle changes
 */
class ExerciseDetector {

    companion object {
        private const val TAG = "ExerciseDetector"
        private const val CONFIDENCE_THRESHOLD = 0.3f
        private const val MIN_REP_CONFIDENCE = 0.6f // Minimum confidence to count reps
        private const val STATE_CHANGE_DEBOUNCE = 3 // Frames to debounce state changes

        // Angle thresholds for different exercises
        private const val PUSHUP_DOWN_ELBOW_ANGLE = 85f  // elbows bent
        private const val PUSHUP_UP_ELBOW_ANGLE = 160f   // elbows extended

        private const val SQUAT_DOWN_KNEE_ANGLE = 90f    // knees bent deeply
        private const val SQUAT_UP_KNEE_ANGLE = 160f     // knees extended

        private const val PLANK_ELBOW_ANGLE = 170f       // arms mostly extended
        private const val PLANK_SPINE_DEVIATION = 15f    // spine should be straight

        private const val JUMPING_JACKS_HIP_RANGE = 40f  // hip width change

        private const val DUMBBELL_CURL_UP_ELBOW = 30f   // elbow flexion at top
        private const val DUMBBELL_CURL_DOWN_ELBOW = 160f // elbow extension at bottom
    }

    private var previousState: ExerciseState = ExerciseState()
    private var repInProgress = false
    private var repStartQuality = 0f
    private var lastValidStateTransition: Pair<String, String>? = null // Track valid state transitions
    private var framesSinceStateChange = 0 // Debounce state changes

    /**
     * Detect exercise type based on current pose
     */
    fun detectExerciseType(person: Person, features: PostureFeatures): ExerciseType {
        return when {
            looksLikePushup(person, features) -> ExerciseType.PUSHUP
            looksLikeSquat(person, features) -> ExerciseType.SQUAT
            looksLikePlank(person, features) -> ExerciseType.PLANK
            looksLikeJumpingJacks(person, features) -> ExerciseType.JUMPING_JACKS
            looksLikeDumbbellCurl(person, features) -> ExerciseType.DUMBBELL_CURL
            else -> ExerciseType.UNKNOWN
        }
    }

    /**
     * Process frame for exercise detection and rep counting
     */
    fun processFrame(
        person: Person,
        features: PostureFeatures,
        currentExerciseType: ExerciseType
    ): ExerciseState {
        if (currentExerciseType == ExerciseType.UNKNOWN) {
            return ExerciseState()
        }

        val keyPoints = person.keyPoints
        val allConfident = keyPoints.all { it.score > CONFIDENCE_THRESHOLD }

        if (!allConfident) {
            return previousState.copy(confidenceScore = 0f)
        }

        val state = when (currentExerciseType) {
            ExerciseType.PUSHUP -> processPushup(keyPoints, features)
            ExerciseType.SQUAT -> processSquat(keyPoints, features)
            ExerciseType.PLANK -> processPlank(keyPoints, features)
            ExerciseType.JUMPING_JACKS -> processJumpingJacks(keyPoints, features)
            ExerciseType.DUMBBELL_CURL -> processDumbbellCurl(keyPoints, features)
            else -> ExerciseState()
        }

        // Use person's overall confidence score
        val stateWithConfidence = state.copy(
            confidenceScore = person.score,
            exerciseType = currentExerciseType
        )

        // Update rep count and track rep progress
        val updatedState = if (shouldCountNewRep(previousState, stateWithConfidence)) {
            stateWithConfidence.copy(repCount = previousState.repCount + 1)
        } else {
            stateWithConfidence.copy(repCount = previousState.repCount)
        }

        previousState = updatedState
        return updatedState
    }

    /**
     * Process pushup: track elbow angle
     */
    private fun processPushup(keyPoints: List<KeyPoint>, features: PostureFeatures): ExerciseState {
        val leftElbowAngle = features.leftArmFlexion
        val rightElbowAngle = features.rightArmFlexion
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        val currentState = when {
            avgElbowAngle < PUSHUP_DOWN_ELBOW_ANGLE -> "down"
            avgElbowAngle > PUSHUP_UP_ELBOW_ANGLE -> "up"
            else -> "mid"
        }

        val confidence = 0.85f

        return ExerciseState(
            exerciseType = ExerciseType.PUSHUP,
            isInMotion = currentState != previousState.lastFrameState,
            lastFrameState = currentState,
            confidenceScore = confidence
        )
    }

    /**
     * Process squat: track knee angle
     */
    private fun processSquat(keyPoints: List<KeyPoint>, features: PostureFeatures): ExerciseState {
        val leftKneeAngle = features.leftKneeFlexion
        val rightKneeAngle = features.rightKneeFlexion
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2

        val currentState = when {
            avgKneeAngle < SQUAT_DOWN_KNEE_ANGLE -> "down"
            avgKneeAngle > SQUAT_UP_KNEE_ANGLE -> "up"
            else -> "mid"
        }

        val confidence = 0.8f

        return ExerciseState(
            exerciseType = ExerciseType.SQUAT,
            isInMotion = currentState != previousState.lastFrameState,
            lastFrameState = currentState,
            confidenceScore = confidence
        )
    }

    /**
     * Process plank: track if body is in horizontal line
     */
    private fun processPlank(keyPoints: List<KeyPoint>, features: PostureFeatures): ExerciseState {
        // For plank, check spine alignment and shoulder elevation
        val spineAlignment = features.spineAlignment
        val armElevation = abs(features.armElevation)

        val isProperly = spineAlignment > 75f && armElevation < PLANK_SPINE_DEVIATION

        val currentState = if (isProperly) "hold" else "bad"
        val confidence = 0.7f

        return ExerciseState(
            exerciseType = ExerciseType.PLANK,
            isInMotion = false, // plank is static
            lastFrameState = currentState,
            confidenceScore = confidence
        )
    }

    /**
     * Process jumping jacks: track hip width change
     */
    private fun processJumpingJacks(keyPoints: List<KeyPoint>, features: PostureFeatures): ExerciseState {
        val leftHip = keyPoints[FeatureExtractor.LEFT_HIP]
        val rightHip = keyPoints[FeatureExtractor.RIGHT_HIP]
        val hipDistance = sqrt((rightHip.x - leftHip.x) * (rightHip.x - leftHip.x) +
                (rightHip.y - leftHip.y) * (rightHip.y - leftHip.y))

        val currentState = when {
            hipDistance > JUMPING_JACKS_HIP_RANGE -> "wide"
            hipDistance < JUMPING_JACKS_HIP_RANGE / 2 -> "narrow"
            else -> "mid"
        }

        val confidence = 0.75f

        return ExerciseState(
            exerciseType = ExerciseType.JUMPING_JACKS,
            isInMotion = currentState != previousState.lastFrameState,
            lastFrameState = currentState,
            confidenceScore = confidence
        )
    }

    /**
     * Process dumbbell curl: track elbow angle (similar to pushup but looking for curl pattern)
     */
    private fun processDumbbellCurl(keyPoints: List<KeyPoint>, features: PostureFeatures): ExerciseState {
        val leftElbowAngle = features.leftArmFlexion
        val rightElbowAngle = features.rightArmFlexion
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        val currentState = when {
            avgElbowAngle < DUMBBELL_CURL_UP_ELBOW -> "up"
            avgElbowAngle > DUMBBELL_CURL_DOWN_ELBOW -> "down"
            else -> "mid"
        }

        val confidence = 0.8f

        return ExerciseState(
            exerciseType = ExerciseType.DUMBBELL_CURL,
            isInMotion = currentState != previousState.lastFrameState,
            lastFrameState = currentState,
            confidenceScore = confidence
        )
    }

    /**
     * Check if a new repetition should be counted
     * Includes confidence validation to prevent false counts from poor poses
     */
    private fun shouldCountNewRep(
        previousState: ExerciseState,
        currentState: ExerciseState
    ): Boolean {
        if (previousState.exerciseType != currentState.exerciseType) return false

        // Don't count reps if confidence is too low (e.g., camera at ceiling)
        if (currentState.confidenceScore < MIN_REP_CONFIDENCE) {
            framesSinceStateChange = 0
            return false
        }

        // Debounce: only process state changes after several stable frames
        val stateChanged = previousState.lastFrameState != currentState.lastFrameState &&
                previousState.lastFrameState.isNotEmpty()

        if (!stateChanged) {
            framesSinceStateChange = 0
            return false
        }

        framesSinceStateChange++
        if (framesSinceStateChange < STATE_CHANGE_DEBOUNCE) {
            return false
        }

        val shouldCount = when (currentState.exerciseType) {
            ExerciseType.PUSHUP -> {
                // Count rep when returning to up position from down
                previousState.lastFrameState == "down" && currentState.lastFrameState == "up"
            }
            ExerciseType.SQUAT -> {
                // Count rep when returning to up position from down
                previousState.lastFrameState == "down" && currentState.lastFrameState == "up"
            }
            ExerciseType.PLANK -> false // plank is timed, not rep-based
            ExerciseType.JUMPING_JACKS -> {
                // Count rep when returning to narrow from wide
                previousState.lastFrameState == "wide" && currentState.lastFrameState == "narrow"
            }
            ExerciseType.DUMBBELL_CURL -> {
                // Count rep when returning to down from up
                previousState.lastFrameState == "up" && currentState.lastFrameState == "down"
            }
            else -> false
        }

        if (shouldCount) {
            lastValidStateTransition = previousState.lastFrameState to currentState.lastFrameState
            framesSinceStateChange = 0
        }

        return shouldCount
    }

    /**
     * Determine if pose looks like pushup
     */
    private fun looksLikePushup(person: Person, features: PostureFeatures): Boolean {
        val keyPoints = person.keyPoints
        val shoulderY = (keyPoints[FeatureExtractor.LEFT_SHOULDER].y +
                        keyPoints[FeatureExtractor.RIGHT_SHOULDER].y) / 2
        val hipY = (keyPoints[FeatureExtractor.LEFT_HIP].y +
                   keyPoints[FeatureExtractor.RIGHT_HIP].y) / 2

        // In pushup, shoulders and hips should be close in height (horizontal body)
        val isHorizontal = abs(shoulderY - hipY) < 50f // More lenient for various positions
        val elbowsBent = features.leftArmFlexion < 150f && features.rightArmFlexion < 150f

        // Check if overall person confidence is good enough
        val hasGoodPoseDetection = person.score > 0.5f

        return isHorizontal && elbowsBent && hasGoodPoseDetection
    }

    /**
     * Determine if pose looks like squat
     */
    private fun looksLikeSquat(person: Person, features: PostureFeatures): Boolean {
        // Squat detection: knees bent, torso upright
        val kneesBent = features.leftKneeFlexion < 140f && features.rightKneeFlexion < 140f
        val torsoUpright = abs(features.torsoLean) < 30f // Allow some lean

        // Check if overall person confidence is good enough
        val hasGoodPoseDetection = person.score > 0.5f

        return kneesBent && torsoUpright && hasGoodPoseDetection
    }

    /**
     * Determine if pose looks like plank
     */
    private fun looksLikePlank(person: Person, features: PostureFeatures): Boolean {
        val keyPoints = person.keyPoints
        val shoulderY = (keyPoints[FeatureExtractor.LEFT_SHOULDER].y +
                        keyPoints[FeatureExtractor.RIGHT_SHOULDER].y) / 2
        val wristY = (keyPoints[FeatureExtractor.LEFT_WRIST].y +
                     keyPoints[FeatureExtractor.RIGHT_WRIST].y) / 2
        val hipY = (keyPoints[FeatureExtractor.LEFT_HIP].y +
                   keyPoints[FeatureExtractor.RIGHT_HIP].y) / 2

        // Plank: horizontal alignment, elbows extended or semi-extended
        val isHorizontal = abs(shoulderY - hipY) < 50f && abs(wristY - hipY) < 70f
        val armsExtended = features.leftArmFlexion > 140f && features.rightArmFlexion > 140f

        // Check if overall person confidence is good enough
        val hasGoodPoseDetection = person.score > 0.5f

        return isHorizontal && armsExtended && hasGoodPoseDetection
    }

    /**
     * Determine if pose looks like jumping jacks
     */
    private fun looksLikeJumpingJacks(person: Person, features: PostureFeatures): Boolean {
        val keyPoints = person.keyPoints
        val leftHip = keyPoints[FeatureExtractor.LEFT_HIP]
        val rightHip = keyPoints[FeatureExtractor.RIGHT_HIP]
        val hipDistance = sqrt((rightHip.x - leftHip.x) * (rightHip.x - leftHip.x) +
                (rightHip.y - leftHip.y) * (rightHip.y - leftHip.y))

        // Jumping jacks: legs apart or together (high variance in hip distance)
        val legsChanging = hipDistance > 20f // has meaningful hip separation

        // Check if overall person confidence is good enough
        val hasGoodPoseDetection = person.score > 0.5f

        return legsChanging && hasGoodPoseDetection
    }

    /**
     * Determine if pose looks like dumbbell curl
     */
    private fun looksLikeDumbbellCurl(person: Person, features: PostureFeatures): Boolean {
        val keyPoints = person.keyPoints
        val shoulderY = (keyPoints[FeatureExtractor.LEFT_SHOULDER].y +
                        keyPoints[FeatureExtractor.RIGHT_SHOULDER].y) / 2
        val hipY = (keyPoints[FeatureExtractor.LEFT_HIP].y +
                   keyPoints[FeatureExtractor.RIGHT_HIP].y) / 2

        // Dumbbell curl: standing upright, arms moving (elbows bent/unbent pattern)
        val isStanding = abs(shoulderY - hipY) > 80f // vertical body
        val armsFlexing = features.leftArmFlexion < 140f || features.rightArmFlexion < 140f

        // Check if overall person confidence is good enough
        val hasGoodPoseDetection = person.score > 0.5f

        return isStanding && armsFlexing && hasGoodPoseDetection
    }
}

