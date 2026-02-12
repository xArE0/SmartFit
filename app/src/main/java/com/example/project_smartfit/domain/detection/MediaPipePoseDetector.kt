package com.example.project_smartfit.domain.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.project_smartfit.domain.model.LandmarkType
import com.example.project_smartfit.domain.model.PoseFrame
import com.example.project_smartfit.domain.model.PoseLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage

/**
 * MediaPipe Pose detector for real-time pose estimation
 * Provides 33 landmarks with high accuracy using BlazePose
 * 
 * Replaces the old TensorFlow Lite MoveNet detector (17 landmarks)
 */
class MediaPipePoseDetector(private val context: Context) {
    
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "MediaPipePoseDetector"
        private const val MODEL_NAME = "pose_landmarker_lite.task"
        
        // MediaPipe provides 33 landmarks
        const val LANDMARK_COUNT = 33
    }
    
    /**
     * Initialize MediaPipe Pose Landmarker
     */
    fun initialize(): Boolean {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)  // Detect single person
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            
            Log.d(TAG, "MediaPipe Pose Landmarker initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPipe Pose Landmarker", e)
            isInitialized = false
            false
        }
    }
    
    /**
     * Detect pose from bitmap
     * Returns PoseFrame with 33 landmarks or null if detection fails
     */
    fun detectPose(bitmap: Bitmap, timestamp: Long = System.currentTimeMillis()): PoseFrame? {
        if (!isInitialized || poseLandmarker == null) {
            Log.w(TAG, "Pose landmarker not initialized")
            return null
        }
        
        return try {
            // Convert bitmap to MPImage
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // Run pose detection
            val result = poseLandmarker?.detect(mpImage)
            
            // Parse result
            val poseFrame = result?.let { parseResult(it, timestamp) }
            
            if (poseFrame != null) {
                Log.d(TAG, "Pose detected successfully - visibility: ${poseFrame.getAverageVisibility()}")
            } else {
                Log.w(TAG, "No pose detected in frame")
            }
            
            poseFrame
        } catch (e: Exception) {
            Log.e(TAG, "Error during pose detection", e)
            null
        }
    }
    
    /**
     * Parse MediaPipe result into PoseFrame
     */
    private fun parseResult(result: PoseLandmarkerResult, timestamp: Long): PoseFrame? {
        if (result.landmarks().isEmpty()) {
            return null
        }
        
        // Get first person's landmarks (we only detect one person)
        val landmarks = result.landmarks()[0]
        
        if (landmarks.size != LANDMARK_COUNT) {
            Log.w(TAG, "Unexpected landmark count: ${landmarks.size}")
            return null
        }
        
        // Convert to our PoseLandmark format
        val poseLandmarks = landmarks.mapIndexed { index, landmark ->
            PoseLandmark(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z(),
                visibility = landmark.visibility().orElse(0f)
            )
        }
        
        // Get world landmarks if available (3D coordinates in meters)
        val worldLandmarks = if (result.worldLandmarks().isNotEmpty()) {
            result.worldLandmarks()[0].map { landmark ->
                PoseLandmark(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    visibility = landmark.visibility().orElse(0f)
                )
            }
        } else null
        
        return PoseFrame(
            landmarks = poseLandmarks,
            timestamp = timestamp,
            worldLandmarks = worldLandmarks
        )
    }
    
    /**
     * Close and release resources
     */
    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        isInitialized = false
        Log.d(TAG, "MediaPipe Pose Landmarker closed")
    }
    
    /**
     * Check if detector is ready
     */
    fun isReady(): Boolean = isInitialized && poseLandmarker != null
}
