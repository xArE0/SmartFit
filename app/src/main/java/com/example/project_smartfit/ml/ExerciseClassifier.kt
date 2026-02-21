package com.example.project_smartfit.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Classifies exercises using a Bidirectional LSTM TFLite model.
 *
 * Model spec:
 *  - Input:  float32[1, 180, 66]  →  180 frames × 22 keypoints × 3 coords (x, y, z)
 *  - Output: float32[1, 7]        →  softmax probabilities for 7 exercise classes
 *
 * Keypoints are a 22-point subset of MediaPipe Pose's 33 landmarks,
 * mapped via [MP_TO_MODEL_INDICES].
 */
class ExerciseClassifier(context: Context) {

    companion object {
        private const val TAG = "ExerciseClassifier"
        private const val MODEL_FILE = "exercise_classifier.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val SEQUENCE_LENGTH = 180
        private const val NUM_KEYPOINTS = 22
        private const val COORDS_PER_KP = 3
        private const val FEATURES_PER_FRAME = NUM_KEYPOINTS * COORDS_PER_KP // 66
        private const val NUM_CLASSES = 7
        private const val SLIDE_AMOUNT = 30 // frames to drop for sliding window re-classification
        private const val MIN_CONFIDENCE_THRESHOLD = 0.40f

        /**
         * Maps model keypoint index → MediaPipe Pose landmark index.
         * 22 keypoints total (index 21 is a duplicate of Nose at MP index 0).
         */
        private val MP_TO_MODEL_INDICES = intArrayOf(
            0,  // Nose
            2,  // Left Eye
            5,  // Right Eye
            7,  // Left Ear
            8,  // Right Ear
            11, // Left Shoulder
            12, // Right Shoulder
            13, // Left Elbow
            14, // Right Elbow
            15, // Left Wrist
            16, // Right Wrist
            23, // Left Hip
            24, // Right Hip
            25, // Left Knee
            26, // Right Knee
            27, // Left Ankle
            28, // Right Ankle
            29, // Left Heel
            30, // Right Heel
            31, // Left Foot Index
            32, // Right Foot Index
            0   // Nose (duplicate)
        )
    }

    // ── Model ────────────────────────────────────────────────────────────
    private val interpreter: Interpreter
    private val labels: List<String>

    // ── Frame buffer (ring buffer) ───────────────────────────────────────
    private val frameBuffer = ArrayList<FloatArray>(SEQUENCE_LENGTH)
    private val bufferLock = Any()

    // ── Background inference ─────────────────────────────────────────────
    private val inferenceExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var inferenceRunning = false

    // ── Last classification result (for continuous mode) ─────────────────
    @Volatile private var lastResult: ClassificationResult? = null

    init {
        interpreter = Interpreter(loadModelFile(context, MODEL_FILE))
        labels = loadLabels(context, LABELS_FILE)
        Log.d(TAG, "Loaded model ($NUM_CLASSES classes): $labels")
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Add a frame of MediaPipe pose landmarks and trigger classification
     * once the buffer reaches [SEQUENCE_LENGTH].
     *
     * After the first classification, a sliding window drops the oldest
     * [SLIDE_AMOUNT] frames so re-classification happens continuously
     * without a full reset.
     *
     * @param landmarks  MediaPipe NormalizedLandmark list (33 landmarks)
     * @param onResult   callback with the classification result (called on background thread)
     */
    fun addFrameAndClassify(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        onResult: (ClassificationResult) -> Unit
    ) {
        // Extract 22-keypoint feature vector from 33 MediaPipe landmarks
        val frame = FloatArray(FEATURES_PER_FRAME)
        for ((modelIdx, mpIdx) in MP_TO_MODEL_INDICES.withIndex()) {
            if (mpIdx < landmarks.size) {
                val lm = landmarks[mpIdx]
                frame[modelIdx * COORDS_PER_KP + 0] = lm.x()
                frame[modelIdx * COORDS_PER_KP + 1] = lm.y()
                frame[modelIdx * COORDS_PER_KP + 2] = lm.z()
            }
        }

        synchronized(bufferLock) {
            frameBuffer.add(frame)
        }

        // Once we have enough frames, run inference (if not already running)
        val currentSize = synchronized(bufferLock) { frameBuffer.size }
        if (currentSize >= SEQUENCE_LENGTH && !inferenceRunning) {
            inferenceRunning = true
            inferenceExecutor.execute {
                try {
                    val result = runInference()
                    lastResult = result
                    onResult(result)

                    // Sliding window: keep the latest frames, drop the oldest
                    synchronized(bufferLock) {
                        if (frameBuffer.size > SLIDE_AMOUNT) {
                            val keep = ArrayList(frameBuffer.subList(
                                frameBuffer.size - (SEQUENCE_LENGTH - SLIDE_AMOUNT),
                                frameBuffer.size
                            ))
                            frameBuffer.clear()
                            frameBuffer.addAll(keep)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Inference failed", e)
                } finally {
                    inferenceRunning = false
                }
            }
        }
    }

    /**
     * Returns `(currentFrames, totalRequired)` for progress UI.
     */
    fun bufferProgress(): Pair<Int, Int> {
        val size = synchronized(bufferLock) { frameBuffer.size }
        return size.coerceAtMost(SEQUENCE_LENGTH) to SEQUENCE_LENGTH
    }

    /**
     * Clears the frame buffer and cached result, restarting analysis.
     */
    fun resetBuffer() {
        synchronized(bufferLock) { frameBuffer.clear() }
        lastResult = null
    }

    /**
     * Returns the last classification result, if any.
     */
    fun lastResult(): ClassificationResult? = lastResult

    /**
     * Release model resources.
     */
    fun close() {
        inferenceExecutor.shutdownNow()
        interpreter.close()
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private fun runInference(): ClassificationResult {
        // Snapshot the buffer
        val frames: List<FloatArray> = synchronized(bufferLock) {
            ArrayList(frameBuffer).takeLast(SEQUENCE_LENGTH)
        }

        // Interpolate to exactly SEQUENCE_LENGTH if needed
        val sequence = if (frames.size == SEQUENCE_LENGTH) {
            frames.toTypedArray()
        } else {
            interpolateSequence(frames, SEQUENCE_LENGTH)
        }

        // Prepare input tensor [1, 180, 66]
        val input = Array(1) { Array(SEQUENCE_LENGTH) { i -> sequence[i] } }

        // Prepare output tensor [1, 7]
        val output = Array(1) { FloatArray(NUM_CLASSES) }

        interpreter.run(input, output)

        val probabilities = output[0]
        val bestIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val bestConf = probabilities[bestIdx]
        val label = if (bestIdx < labels.size) labels[bestIdx] else "Unknown"

        Log.d(TAG, "Classification: $label (${(bestConf * 100).toInt()}%) | probs=${probabilities.joinToString { "%.2f".format(it) }}")

        return ClassificationResult(
            label = label,
            confidence = bestConf,
            probabilities = probabilities.clone(),
            isConfident = bestConf >= MIN_CONFIDENCE_THRESHOLD
        )
    }

    /**
     * Linearly interpolates a list of frames to exactly [targetLen] frames.
     */
    private fun interpolateSequence(
        frames: List<FloatArray>,
        targetLen: Int
    ): Array<FloatArray> {
        val n = frames.size
        if (n == 0) return Array(targetLen) { FloatArray(FEATURES_PER_FRAME) }
        if (n == 1) return Array(targetLen) { frames[0].clone() }

        return Array(targetLen) { t ->
            val srcIdx = t.toFloat() * (n - 1) / (targetLen - 1)
            val lo = srcIdx.toInt().coerceIn(0, n - 2)
            val hi = (lo + 1).coerceIn(0, n - 1)
            val frac = srcIdx - lo
            FloatArray(FEATURES_PER_FRAME) { i ->
                frames[lo][i] * (1f - frac) + frames[hi][i] * frac
            }
        }
    }

    // ── File loading ─────────────────────────────────────────────────────

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val input = FileInputStream(fd.fileDescriptor)
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun loadLabels(context: Context, filename: String): List<String> {
        return context.assets.open(filename).bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    }

    // ── Result data class ────────────────────────────────────────────────

    data class ClassificationResult(
        val label: String,
        val confidence: Float,
        val probabilities: FloatArray,
        val isConfident: Boolean
    ) {
        /** Formatted label for display (title case). */
        val displayLabel: String
            get() = label.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ClassificationResult) return false
            return label == other.label && confidence == other.confidence
        }

        override fun hashCode(): Int = 31 * label.hashCode() + confidence.hashCode()
    }
}
