package com.example.project_smartfit.domain.features

import com.example.project_smartfit.domain.detection.Person
import com.example.project_smartfit.domain.detection.KeyPoint
import android.util.Log
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Extracted features from pose detection
 * Contains angles and measurements for ML model input
 */
data class PostureFeatures(
    // Head & Neck
    val neckFlexion: Float = 0f,  // Forward head position (degrees)
    val headTilt: Float = 0f,      // Head tilt left/right (degrees)

    // Spine & Core
    val spineAlignment: Float = 0f, // Horizontal deviation from center (pixels)
    val torsoLean: Float = 0f,      // Forward lean (degrees)
    val shoulderLevel: Float = 0f,  // Asymmetry in shoulder height (%)

    // Arms
    val leftArmFlexion: Float = 0f, // Elbow angle (degrees)
    val rightArmFlexion: Float = 0f,
    val armElevation: Float = 0f,   // Shoulder elevation relative to hips (pixels)

    // Hips & Legs
    val hipAlignment: Float = 0f,   // Hip level symmetry (%)
    val leftKneeFlexion: Float = 0f, // Knee angle (degrees)
    val rightKneeFlexion: Float = 0f,
    val legAlignment: Float = 0f,   // Leg symmetry (degree difference)

    // Debug info
    val detectedJointCount: Int = 0,
    val confidenceMetrics: Map<String, Float> = emptyMap()
)


/**
 * Feature Extractor for pose-based posture analysis
 * Computes angles, distances, and alignment metrics from detected keypoints
 */
class FeatureExtractor(private val context: android.content.Context) {

    companion object {
        private const val TAG = "FeatureExtractor"
        private const val CONFIDENCE_THRESHOLD = 0.3f

        // Keypoint indices (from MoveNet)
        const val NOSE = 0
        const val LEFT_EYE = 1
        const val RIGHT_EYE = 2
        const val LEFT_EAR = 3
        const val RIGHT_EAR = 4
        const val LEFT_SHOULDER = 5
        const val RIGHT_SHOULDER = 6
        const val LEFT_ELBOW = 7
        const val RIGHT_ELBOW = 8
        const val LEFT_WRIST = 9
        const val RIGHT_WRIST = 10
        const val LEFT_HIP = 11
        const val RIGHT_HIP = 12
        const val LEFT_KNEE = 13
        const val RIGHT_KNEE = 14
        const val LEFT_ANKLE = 15
        const val RIGHT_ANKLE = 16
    }

    /**
     * Extract all features from detected pose
     */
    fun extractFeatures(person: Person): PostureFeatures {
        val keyPoints = person.keyPoints

        // Validate we have enough keypoints detected
        val detectedCount = keyPoints.count { it.score > CONFIDENCE_THRESHOLD }
        if (detectedCount < 10) {
            return PostureFeatures(detectedJointCount = detectedCount)
        }

        val metrics = mutableMapOf<String, Float>()

        // Extract head & neck metrics
        val neckFlexion = calculateNeckFlexion(keyPoints)
        val headTilt = calculateHeadTilt(keyPoints)
        metrics["neckFlexion"] = neckFlexion
        metrics["headTilt"] = headTilt

        // Extract spine & core metrics
        val spineAlignment = calculateSpineAlignment(keyPoints)
        val torsoLean = calculateTorsoLean(keyPoints)
        val shoulderLevel = calculateShoulderLevel(keyPoints)
        metrics["spineAlignment"] = spineAlignment
        metrics["torsoLean"] = torsoLean
        metrics["shoulderLevel"] = shoulderLevel

        // Extract arm metrics
        val leftArmFlexion = calculateArmFlexion(keyPoints, isLeft = true)
        val rightArmFlexion = calculateArmFlexion(keyPoints, isLeft = false)
        val armElevation = calculateArmElevation(keyPoints)
        metrics["leftArmFlexion"] = leftArmFlexion
        metrics["rightArmFlexion"] = rightArmFlexion
        metrics["armElevation"] = armElevation

        // Extract hip & leg metrics
        val hipAlignment = calculateHipAlignment(keyPoints)
        val leftKneeFlexion = calculateKneeFlexion(keyPoints, isLeft = true)
        val rightKneeFlexion = calculateKneeFlexion(keyPoints, isLeft = false)
        val legAlignment = calculateLegAlignment(keyPoints)
        metrics["hipAlignment"] = hipAlignment
        metrics["leftKneeFlexion"] = leftKneeFlexion
        metrics["rightKneeFlexion"] = rightKneeFlexion
        metrics["legAlignment"] = legAlignment

        return PostureFeatures(
            neckFlexion = neckFlexion,
            headTilt = headTilt,
            spineAlignment = spineAlignment,
            torsoLean = torsoLean,
            shoulderLevel = shoulderLevel,
            leftArmFlexion = leftArmFlexion,
            rightArmFlexion = rightArmFlexion,
            armElevation = armElevation,
            hipAlignment = hipAlignment,
            leftKneeFlexion = leftKneeFlexion,
            rightKneeFlexion = rightKneeFlexion,
            legAlignment = legAlignment,
            detectedJointCount = detectedCount,
            confidenceMetrics = metrics
        )
    }

    /**
     * Calculate neck flexion (forward head posture)
     * Positive = head forward, Negative = head back
     */
    private fun calculateNeckFlexion(keyPoints: List<KeyPoint>): Float {
        val nose = keyPoints[NOSE]
        val leftShoulder = keyPoints[LEFT_SHOULDER]
        val rightShoulder = keyPoints[RIGHT_SHOULDER]

        if (!isConfident(nose, leftShoulder, rightShoulder)) return 0f

        val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2
        val shoulderMidY = (leftShoulder.y + rightShoulder.y) / 2

        // Angle from shoulders to nose
        val dx = nose.x - shoulderMidX
        val dy = nose.y - shoulderMidY
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // Normalize: 0 degrees = perfect alignment, positive = forward
        return angle - 90f
    }

    /**
     * Calculate head tilt (left/right)
     * Positive = right tilt, Negative = left tilt
     */
    private fun calculateHeadTilt(keyPoints: List<KeyPoint>): Float {
        val leftEar = keyPoints[LEFT_EAR]
        val rightEar = keyPoints[RIGHT_EAR]

        if (!isConfident(leftEar, rightEar)) return 0f

        val dx = rightEar.x - leftEar.x
        val dy = rightEar.y - leftEar.y

        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /**
     * Calculate spine alignment (0-100, 100 = perfect)
     * Based on shoulder-hip vertical alignment
     */
    private fun calculateSpineAlignment(keyPoints: List<KeyPoint>): Float {
        val leftShoulder = keyPoints[LEFT_SHOULDER]
        val rightShoulder = keyPoints[RIGHT_SHOULDER]
        val leftHip = keyPoints[LEFT_HIP]
        val rightHip = keyPoints[RIGHT_HIP]

        if (!isConfident(leftShoulder, rightShoulder, leftHip, rightHip)) return 0f

        val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2
        val hipMidX = (leftHip.x + rightHip.x) / 2

        // Calculate horizontal deviation
        val deviation = kotlin.math.abs(shoulderMidX - hipMidX)
        val maxDeviation = 50f // pixels

        // Return 0-100 score
        return (100f * (1f - kotlin.math.min(deviation / maxDeviation, 1f)))
    }

    /**
     * Calculate torso lean (forward/backward)
     */
    private fun calculateTorsoLean(keyPoints: List<KeyPoint>): Float {
        val midShoulderY = (keyPoints[LEFT_SHOULDER].y + keyPoints[RIGHT_SHOULDER].y) / 2
        val midHipY = (keyPoints[LEFT_HIP].y + keyPoints[RIGHT_HIP].y) / 2

        val midShoulderX = (keyPoints[LEFT_SHOULDER].x + keyPoints[RIGHT_SHOULDER].x) / 2
        val midHipX = (keyPoints[LEFT_HIP].x + keyPoints[RIGHT_HIP].x) / 2

        val dx = midHipX - midShoulderX
        val dy = midHipY - midShoulderY

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        return angle - 90f // Normalize to vertical
    }

    /**
     * Calculate shoulder level difference (asymmetry)
     * 0 = perfect symmetry, increases with asymmetry
     */
    private fun calculateShoulderLevel(keyPoints: List<KeyPoint>): Float {
        val leftShoulder = keyPoints[LEFT_SHOULDER]
        val rightShoulder = keyPoints[RIGHT_SHOULDER]

        if (!isConfident(leftShoulder, rightShoulder)) return 0f

        val yDiff = kotlin.math.abs(leftShoulder.y - rightShoulder.y)
        val shoulderDistance = calculateDistance(leftShoulder, rightShoulder)

        // Return percentage difference
        return (yDiff / shoulderDistance) * 100f
    }

    /**
     * Calculate arm flexion (elbow angle)
     */
    private fun calculateArmFlexion(keyPoints: List<KeyPoint>, isLeft: Boolean): Float {
        val shoulder = if (isLeft) keyPoints[LEFT_SHOULDER] else keyPoints[RIGHT_SHOULDER]
        val elbow = if (isLeft) keyPoints[LEFT_ELBOW] else keyPoints[RIGHT_ELBOW]
        val wrist = if (isLeft) keyPoints[LEFT_WRIST] else keyPoints[RIGHT_WRIST]

        return calculateAngle(shoulder, elbow, wrist)
    }

    /**
     * Calculate arm elevation (shoulder position relative to hips)
     */
    private fun calculateArmElevation(keyPoints: List<KeyPoint>): Float {
        val leftShoulder = keyPoints[LEFT_SHOULDER]
        val rightShoulder = keyPoints[RIGHT_SHOULDER]
        val leftHip = keyPoints[LEFT_HIP]
        val rightHip = keyPoints[RIGHT_HIP]

        val shoulderMidY = (leftShoulder.y + rightShoulder.y) / 2
        val hipMidY = (leftHip.y + rightHip.y) / 2

        // Positive = shoulders elevated (hunched), Negative = shoulders down
        return shoulderMidY - hipMidY
    }

    /**
     * Calculate hip alignment (left/right symmetry)
     */
    private fun calculateHipAlignment(keyPoints: List<KeyPoint>): Float {
        val leftHip = keyPoints[LEFT_HIP]
        val rightHip = keyPoints[RIGHT_HIP]

        if (!isConfident(leftHip, rightHip)) return 0f

        val yDiff = kotlin.math.abs(leftHip.y - rightHip.y)
        val hipDistance = calculateDistance(leftHip, rightHip)

        return (yDiff / hipDistance) * 100f
    }

    /**
     * Calculate knee flexion angle
     */
    private fun calculateKneeFlexion(keyPoints: List<KeyPoint>, isLeft: Boolean): Float {
        val hip = if (isLeft) keyPoints[LEFT_HIP] else keyPoints[RIGHT_HIP]
        val knee = if (isLeft) keyPoints[LEFT_KNEE] else keyPoints[RIGHT_KNEE]
        val ankle = if (isLeft) keyPoints[LEFT_ANKLE] else keyPoints[RIGHT_ANKLE]

        return calculateAngle(hip, knee, ankle)
    }

    /**
     * Calculate leg alignment (left/right symmetry)
     */
    private fun calculateLegAlignment(keyPoints: List<KeyPoint>): Float {
        val leftKneeFlexion = calculateKneeFlexion(keyPoints, isLeft = true)
        val rightKneeFlexion = calculateKneeFlexion(keyPoints, isLeft = false)

        return kotlin.math.abs(leftKneeFlexion - rightKneeFlexion)
    }

    /**
     * Calculate angle between three points (in degrees)
     */
    private fun calculateAngle(p1: KeyPoint, p2: KeyPoint, p3: KeyPoint): Float {
        if (!isConfident(p1, p2, p3)) return 0f

        // Vector from p2 to p1
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y

        // Vector from p2 to p3
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y

        // Dot product and magnitudes
        val dotProduct = v1x * v2x + v1y * v2y
        val mag1 = sqrt((v1x * v1x + v1y * v1y).toDouble()).toFloat()
        val mag2 = sqrt((v2x * v2x + v2y * v2y).toDouble()).toFloat()

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(kotlin.math.acos(cosAngle.toDouble())).toFloat()
    }

    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: KeyPoint, p2: KeyPoint): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * Check if keypoints have sufficient confidence
     */
    private fun isConfident(vararg points: KeyPoint): Boolean {
        return points.all { it.score > CONFIDENCE_THRESHOLD }
    }
}
