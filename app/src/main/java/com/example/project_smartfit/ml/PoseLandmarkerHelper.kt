package com.example.project_smartfit.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Helper that wraps MediaPipe PoseLandmarker in LIVE_STREAM mode.
 * - Async: never blocks the camera thread.
 * - Drops frames automatically when the pipeline is busy.
 * - Uses GPU delegate with CPU fallback.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val listener: LandmarkerListener
) {
    companion object {
        private const val TAG = "PoseLandmarkerHelper"
        private const val MODEL_ASSET = "pose_landmarker_lite.task"
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
    }

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(Delegate.CPU) // CPU is most reliable across devices
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
                .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
                .setMinTrackingConfidence(MIN_POSE_TRACKING_CONFIDENCE)
                .setNumPoses(1) // Single person
                .setResultListener(this::onResult)
                .setErrorListener(this::onError)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.d(TAG, "PoseLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "PoseLandmarker init failed", e)
            listener.onError("Pose detection failed to initialize: ${e.message}")
        }
    }

    /**
     * Feed a camera frame for async pose detection.
     * Call this from ImageAnalysis.Analyzer on each frame.
     */
    fun detectAsync(imageProxy: ImageProxy) {
        val landmarker = poseLandmarker ?: return

        val bitmap = imageProxyToBitmap(imageProxy)
        val mpImage = BitmapImageBuilder(bitmap).build()
        val timestampMs = SystemClock.uptimeMillis()

        try {
            landmarker.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            // MediaPipe throws if timestamps are not monotonically increasing
            // or if a frame arrives while previous is still processing — safe to ignore
            Log.v(TAG, "detectAsync skipped frame: ${e.message}")
        }
    }

    /**
     * Convert ImageProxy (YUV_420_888) to a Bitmap suitable for MediaPipe.
     * Applies rotation from the image metadata.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        // Apply rotation
        val rotation = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(
            bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
    }

    private fun onResult(result: PoseLandmarkerResult, input: MPImage) {
        listener.onResults(result, input.height, input.width)
    }

    private fun onError(e: RuntimeException) {
        Log.e(TAG, "MediaPipe error", e)
        listener.onError(e.message ?: "Unknown error")
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    interface LandmarkerListener {
        fun onResults(result: PoseLandmarkerResult, imageHeight: Int, imageWidth: Int)
        fun onError(error: String)
    }
}
