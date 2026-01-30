package com.example.project_smartfit.pose

/**
 * Centralized configuration for pose detection and feature extraction
 * Adjust these values to fine-tune the system's behavior
 */
object PoseDetectionConfig {

    // ==================== MODEL & INFERENCE ====================

    /**
     * Minimum confidence score for a keypoint to be considered valid
     * Range: 0.0 - 1.0
     * Lower values = more detections but potentially noisy
     * Higher values = only confident detections
     */
    const val CONFIDENCE_THRESHOLD = 0.3f

    /**
     * Minimum confidence score for displaying keypoint on UI
     * Range: 0.0 - 1.0
     */
    const val UI_CONFIDENCE_THRESHOLD = 0.3f

    /**
     * Number of threads for TensorFlow Lite inference
     * Typical values: 2, 4, 8
     * Higher = faster but more CPU usage
     */
    const val INFERENCE_THREADS = 4

    /**
     * Use NNAPI for acceleration
     * true = potentially faster but less stable on some devices
     * false = slower but more reliable
     */
    const val USE_NNAPI = false

    /**
     * TensorFlow Lite model filename
     * Must be in assets/ folder
     */
    const val MODEL_FILENAME = "movenet_singlepose_lightning.tflite"

    // ==================== FEATURE EXTRACTION ====================

    /**
     * Maximum deviation in pixels for spine alignment before penalty
     * Smaller = stricter spine alignment requirement
     */
    const val SPINE_ALIGNMENT_MAX_DEVIATION_PX = 50f

    // ==================== POSTURE SCORE THRESHOLDS ====================

    /**
     * Posture score thresholds for classification
     * EXCELLENT: score >= EXCELLENT_THRESHOLD
     * GOOD:      score >= GOOD_THRESHOLD
     * FAIR:      score >= FAIR_THRESHOLD
     * POOR:      score > 0
     */
    const val EXCELLENT_THRESHOLD = 85f
    const val GOOD_THRESHOLD = 70f
    const val FAIR_THRESHOLD = 50f

    // ==================== NECK & HEAD WARNINGS ====================

    /**
     * Neck flexion threshold (degrees) for warning
     * Absolute value - warning triggers when |neckFlexion| > this value
     */
    const val NECK_FLEXION_WARNING_THRESHOLD = 15f

    /**
     * Forward head posture penalty multiplier in score calculation
     * Higher = stricter penalty for forward head
     */
    const val NECK_FLEXION_PENALTY_MULTIPLIER = 0.5f

    /**
     * Maximum penalty for neck flexion (degrees)
     * Score reduction will not exceed this value
     */
    const val NECK_FLEXION_MAX_PENALTY = 15f

    // ==================== SHOULDER & SPINE WARNINGS ====================

    /**
     * Shoulder level asymmetry threshold (percentage) for warning
     * Warning triggers when asymmetry > this value
     */
    const val SHOULDER_LEVEL_WARNING_THRESHOLD = 10f

    /**
     * Shoulder asymmetry penalty multiplier in score calculation
     */
    const val SHOULDER_ASYMMETRY_PENALTY_MULTIPLIER = 1f

    /**
     * Maximum penalty for shoulder asymmetry
     */
    const val SHOULDER_ASYMMETRY_MAX_PENALTY = 15f

    /**
     * Spine alignment percentage threshold
     * Warning triggers when alignment < this value
     */
    const val SPINE_ALIGNMENT_WARNING_THRESHOLD = 70f

    /**
     * Weight of spine alignment in overall score (0-1)
     */
    const val SPINE_ALIGNMENT_WEIGHT = 0.2f

    // ==================== TORSO & LEAN WARNINGS ====================

    /**
     * Forward torso lean threshold (degrees) for warning
     * Absolute value - warning triggers when |torsoLean| > this value
     */
    const val TORSO_LEAN_WARNING_THRESHOLD = 20f

    /**
     * Torso lean penalty multiplier
     */
    const val TORSO_LEAN_PENALTY_MULTIPLIER = 0.3f

    /**
     * Maximum penalty for torso lean
     */
    const val TORSO_LEAN_MAX_PENALTY = 20f

    // ==================== HIP & LEG WARNINGS ====================

    /**
     * Hip alignment asymmetry threshold (percentage) for warning
     */
    const val HIP_ALIGNMENT_WARNING_THRESHOLD = 10f

    /**
     * Hip asymmetry penalty multiplier
     */
    const val HIP_ASYMMETRY_PENALTY_MULTIPLIER = 0.8f

    /**
     * Maximum penalty for hip asymmetry
     */
    const val HIP_ASYMMETRY_MAX_PENALTY = 10f

    /**
     * Leg alignment asymmetry threshold (degrees) for warning
     */
    const val LEG_ALIGNMENT_WARNING_THRESHOLD = 15f

    /**
     * Leg alignment penalty multiplier
     */
    const val LEG_ALIGNMENT_PENALTY_MULTIPLIER = 0.5f

    /**
     * Maximum penalty for leg asymmetry
     */
    const val LEG_ALIGNMENT_MAX_PENALTY = 10f

    // ==================== VISUALIZATION SETTINGS ====================

    /**
     * Radius of keypoint circles on canvas (pixels)
     */
    const val KEYPOINT_RADIUS = 12f

    /**
     * Stroke width for skeleton connections (pixels)
     */
    const val CONNECTION_STROKE_WIDTH = 6f

    /**
     * Text size for confidence scores (pixels)
     */
    const val CONFIDENCE_TEXT_SIZE = 24f

    /**
     * Radius of warning highlight circle around keypoint (pixels)
     */
    const val WARNING_HIGHLIGHT_RADIUS_OFFSET = 8f

    /**
     * Stroke width for warning highlights (pixels)
     */
    const val WARNING_HIGHLIGHT_STROKE_WIDTH = 3f

    /**
     * Alpha (transparency) for statistics overlay background
     * Range: 0.0 - 1.0, where 1.0 = fully opaque
     */
    const val STATS_OVERLAY_ALPHA = 0.6f

    /**
     * Alpha for analysis report background
     */
    const val ANALYSIS_REPORT_ALPHA = 0.7f

    // ==================== CAMERA & PROCESSING ====================

    /**
     * Frame backpressure strategy
     * "keep_only_latest" - process only the most recent frame
     * Prevents frame queue buildup on slow devices
     */
    const val CAMERA_BACKPRESSURE_STRATEGY = "keep_only_latest"

    /**
     * Executor threads for camera frame processing
     * Usually 1 (single thread) for sequential processing
     */
    const val CAMERA_EXECUTOR_THREADS = 1

    // ==================== TIMING & PERFORMANCE ====================

    /**
     * Interval (milliseconds) for FPS calculation update
     */
    const val FPS_UPDATE_INTERVAL_MS = 1000L

    /**
     * Enable frame skipping for performance
     * Skip every N-th frame (1 = no skipping, 2 = skip every other, etc.)
     */
    const val FRAME_SKIP_RATE = 1 // No skipping by default

    // ==================== SCORE CALCULATION WEIGHTS ====================

    /**
     * Individual feature weights in overall score calculation
     * Sum should ideally equal 1.0 for normalized weighting
     */
    const val SPINE_ALIGNMENT_SCORE_WEIGHT = 0.2f
    const val NECK_FLEXION_SCORE_WEIGHT = 0.2f
    const val TORSO_LEAN_SCORE_WEIGHT = 0.15f
    const val SHOULDER_LEVEL_SCORE_WEIGHT = 0.15f
    const val HIP_ALIGNMENT_SCORE_WEIGHT = 0.1f
    const val LEG_ALIGNMENT_SCORE_WEIGHT = 0.1f
    const val ARM_ELEVATION_SCORE_WEIGHT = 0.1f

    // ==================== DEBUG & LOGGING ====================

    /**
     * Enable verbose logging
     * true = detailed logs for debugging
     * false = minimal logging for production
     */
    const val DEBUG_MODE = false

    /**
     * Log individual feature extraction calculations
     */
    const val LOG_FEATURE_EXTRACTION = false

    /**
     * Log frame processing timing
     */
    const val LOG_FRAME_TIMING = false

    /**
     * Log pose detection confidence scores
     */
    const val LOG_CONFIDENCE_SCORES = false

    // ==================== UTILITY FUNCTIONS ====================


    /**
     * Get color for score value
     */
    fun getScoreColor(score: Float): androidx.compose.ui.graphics.Color {
        return when {
            score >= EXCELLENT_THRESHOLD -> androidx.compose.ui.graphics.Color.Green
            score >= GOOD_THRESHOLD -> androidx.compose.ui.graphics.Color(0xFF90EE90)
            score >= FAIR_THRESHOLD -> androidx.compose.ui.graphics.Color(0xFFFFD700)
            else -> androidx.compose.ui.graphics.Color.Red
        }
    }

    /**
     * Validate configuration values
     * Returns true if all values are in valid ranges
     */
    fun validateConfiguration(): Boolean {
        var valid = true

        if (CONFIDENCE_THRESHOLD < 0f || CONFIDENCE_THRESHOLD > 1f) {
            android.util.Log.w("PoseDetectionConfig", "Invalid CONFIDENCE_THRESHOLD")
            valid = false
        }

        if (EXCELLENT_THRESHOLD < GOOD_THRESHOLD || GOOD_THRESHOLD < FAIR_THRESHOLD) {
            android.util.Log.w("PoseDetectionConfig", "Invalid threshold ordering")
            valid = false
        }

        if (INFERENCE_THREADS < 1) {
            android.util.Log.w("PoseDetectionConfig", "INFERENCE_THREADS must be >= 1")
            valid = false
        }

        return valid
    }
}
