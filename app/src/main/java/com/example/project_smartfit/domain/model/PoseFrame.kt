package com.example.project_smartfit.domain.model

/**
 * Container for a complete pose detection result from MediaPipe
 * Holds all 33 landmarks with timestamp
 */
data class PoseFrame(
    val landmarks: List<PoseLandmark>,
    val timestamp: Long = System.currentTimeMillis(),
    val worldLandmarks: List<PoseLandmark>? = null  // Optional 3D world coordinates
) {
    init {
        require(landmarks.size == 33) { 
            "MediaPipe Pose requires exactly 33 landmarks, got ${landmarks.size}" 
        }
    }
    
    /**
     * Get landmark by type with safe access
     */
    fun getLandmark(type: LandmarkType): PoseLandmark = landmarks[type.index]
    
    /**
     * Check if a specific landmark is visible above threshold
     */
    fun isVisible(type: LandmarkType, threshold: Float = 0.5f): Boolean {
        return landmarks[type.index].visibility >= threshold
    }
    
    /**
     * Check if all required landmarks for an exercise are visible
     */
    fun areVisible(vararg types: LandmarkType, threshold: Float = 0.5f): Boolean {
        return types.all { isVisible(it, threshold) }
    }
    
    /**
     * Get average visibility score for quality assessment
     */
    fun getAverageVisibility(): Float {
        return landmarks.map { it.visibility }.average().toFloat()
    }
    
    /**
     * Get landmarks for a specific body part
     */
    fun getUpperBodyLandmarks(): List<Pair<LandmarkType, PoseLandmark>> {
        return listOf(
            LandmarkType.LEFT_SHOULDER to getLandmark(LandmarkType.LEFT_SHOULDER),
            LandmarkType.RIGHT_SHOULDER to getLandmark(LandmarkType.RIGHT_SHOULDER),
            LandmarkType.LEFT_ELBOW to getLandmark(LandmarkType.LEFT_ELBOW),
            LandmarkType.RIGHT_ELBOW to getLandmark(LandmarkType.RIGHT_ELBOW),
            LandmarkType.LEFT_WRIST to getLandmark(LandmarkType.LEFT_WRIST),
            LandmarkType.RIGHT_WRIST to getLandmark(LandmarkType.RIGHT_WRIST)
        )
    }
    
    fun getLowerBodyLandmarks(): List<Pair<LandmarkType, PoseLandmark>> {
        return listOf(
            LandmarkType.LEFT_HIP to getLandmark(LandmarkType.LEFT_HIP),
            LandmarkType.RIGHT_HIP to getLandmark(LandmarkType.RIGHT_HIP),
            LandmarkType.LEFT_KNEE to getLandmark(LandmarkType.LEFT_KNEE),
            LandmarkType.RIGHT_KNEE to getLandmark(LandmarkType.RIGHT_KNEE),
            LandmarkType.LEFT_ANKLE to getLandmark(LandmarkType.LEFT_ANKLE),
            LandmarkType.RIGHT_ANKLE to getLandmark(LandmarkType.RIGHT_ANKLE)
        )
    }
}
