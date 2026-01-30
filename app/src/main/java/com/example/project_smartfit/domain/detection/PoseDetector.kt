package com.example.project_smartfit.domain.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

data class KeyPoint(
    val x: Float,
    val y: Float,
    val score: Float
)

data class Person(
    val keyPoints: List<KeyPoint>,
    val score: Float
)

/**
 * Handles pose detection using MoveNet TensorFlow Lite model
 * Responsible only for model loading and inference
 */
class PoseDetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var outputShape: IntArray? = null

    companion object {
        private const val TAG = "PoseDetector"
        private const val MODEL_FILENAME = "movenet_singlepose_lightning.tflite"

        // MoveNet keypoint names
        val keyPointNames = arrayOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )

        // Connections for skeleton visualization
        val bodyConnections = arrayOf(
            Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4), // Head
            Pair(5, 6), // Shoulders
            Pair(5, 7), Pair(7, 9), // Left arm
            Pair(6, 8), Pair(8, 10), // Right arm
            Pair(5, 11), Pair(6, 12), // Torso
            Pair(11, 12), // Hips
            Pair(11, 13), Pair(13, 15), // Left leg
            Pair(12, 14), Pair(14, 16) // Right leg
        )
    }

    /**
     * Initialize the TensorFlow Lite model
     */
    fun initialize(): Boolean {
        return try {
            val model = FileUtil.loadMappedFile(context, MODEL_FILENAME)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(false) // More stable
            }

            interpreter = Interpreter(model, options)

            // Get input shape
            val inputShape = interpreter!!.getInputTensor(0).shape()
            inputImageHeight = inputShape[1]
            inputImageWidth = inputShape[2]

            // Get output shape
            outputShape = interpreter!!.getOutputTensor(0).shape()

            Log.d(TAG, "Model initialized successfully")
            Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Output shape: ${outputShape!!.contentToString()}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing pose detector", e)
            false
        }
    }

    /**
     * Detect pose from bitmap
     * Returns Person object with keypoints and confidence score
     */
    fun detectPose(bitmap: Bitmap): Person? {
        return try {
            val tensorImage = TensorImage.fromBitmap(bitmap)

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            val processedImage = imageProcessor.process(tensorImage)

            // Prepare output buffer
            val outputArray = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }

            // Run inference
            interpreter?.run(processedImage.buffer, outputArray)

            // Parse results - MoveNet outputs normalized coordinates [0,1]
            val keyPoints = mutableListOf<KeyPoint>()

            for (i in 0 until 17) {
                val normalizedY = outputArray[0][0][i][0]
                val normalizedX = outputArray[0][0][i][1]
                val score = outputArray[0][0][i][2]

                // Convert normalized coordinates to bitmap coordinates
                val x = normalizedX * bitmap.width
                val y = normalizedY * bitmap.height

                keyPoints.add(KeyPoint(x, y, score))
            }

            // Calculate overall confidence
            val avgScore = keyPoints.map { it.score }.average().toFloat()

            Person(keyPoints, avgScore)

        } catch (e: Exception) {
            Log.e(TAG, "Error during pose detection", e)
            null
        }
    }

    /**
     * Close and release resources
     */
    fun close() {
        interpreter?.close()
    }
}
