package com.example.project_smartfit.pose

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.project_smartfit.features.FeatureExtractor
import com.example.project_smartfit.features.PostureFeatures
import com.example.project_smartfit.ExerciseDetector
import com.example.project_smartfit.ExerciseType
import com.example.project_smartfit.ExerciseState

/**
 * Encapsulates all pose detection state for reusability across pages
 */
data class PoseDetectionState(
    val currentPerson: Person? = null,
    val postureFeatures: PostureFeatures? = null,
    val isModelLoaded: Boolean = false,
    val fps: Int = 0,
    val elapsedTimeMs: Long = 0L,
    val bitmapSize: Pair<Int, Int> = Pair(640, 480),
    val isDetecting: Boolean = false,
    val errorMessage: String? = null,
    // Exercise detection fields
    val currentExercise: ExerciseType = ExerciseType.UNKNOWN,
    val exerciseState: ExerciseState? = null,
    val isTrackingExercise: Boolean = false
)

/**
 * ViewModel for managing pose detection across the app
 * Handles model initialization, pose detection, and feature extraction
 */
class PoseDetectionViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "PoseDetectionVM"
    }

    private val _state = MutableStateFlow(PoseDetectionState())
    val state: StateFlow<PoseDetectionState> = _state

    private var poseDetector: PoseDetector? = null
    private var featureExtractor: FeatureExtractor? = null
    private var exerciseDetector: ExerciseDetector? = null

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var startTime: Long? = null

    init {
        initializeModels()
    }

    /**
     * Initialize TensorFlow and feature extraction models
     */
    private fun initializeModels() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing pose detection models...")

                poseDetector = PoseDetector(context).apply {
                    if (!initialize()) {
                        _state.value = _state.value.copy(
                            errorMessage = "Failed to initialize pose detector"
                        )
                        return@launch
                    }
                }

                featureExtractor = FeatureExtractor(context)
                exerciseDetector = ExerciseDetector()

                _state.value = _state.value.copy(
                    isModelLoaded = true,
                    errorMessage = null
                )

                Log.d(TAG, "Models initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing models", e)
                _state.value = _state.value.copy(
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Process a frame from camera
     */
    fun processCameraFrame(bitmap: Bitmap) {
        if (!_state.value.isModelLoaded || poseDetector == null) return

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isDetecting = true)

                // Detect pose
                val person = poseDetector!!.detectPose(bitmap)

                // Extract features
                val features = if (person != null) {
                    featureExtractor?.extractFeatures(person)
                } else {
                    null
                }

                // Process exercise if tracking
                var exerciseState: ExerciseState? = null
                var currentExercise = _state.value.currentExercise

                if (_state.value.isTrackingExercise && person != null && features != null) {
                    // Auto-detect exercise type if not manually selected
                    if (currentExercise == ExerciseType.UNKNOWN) {
                        currentExercise = exerciseDetector?.detectExerciseType(person, features)
                            ?: ExerciseType.UNKNOWN
                    }

                    // Process exercise frame
                    if (currentExercise != ExerciseType.UNKNOWN) {
                        exerciseState = exerciseDetector?.processFrame(person, features, currentExercise)
                    }
                }

                // Update FPS
                frameCount++
                val now = System.currentTimeMillis()
                val fps = if (now - lastFpsTimestamp > 1000) {
                    val calculatedFps = frameCount
                    frameCount = 0
                    lastFpsTimestamp = now
                    calculatedFps
                } else {
                    _state.value.fps
                }

                // Update elapsed time
                if (person != null) {
                    if (startTime == null) startTime = System.currentTimeMillis()
                    val elapsedTime = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())

                    _state.value = _state.value.copy(
                        currentPerson = person,
                        postureFeatures = features,
                        fps = fps,
                        elapsedTimeMs = elapsedTime,
                        bitmapSize = Pair(bitmap.width, bitmap.height),
                        isDetecting = false,
                        currentExercise = currentExercise,
                        exerciseState = exerciseState
                    )
                } else {
                    _state.value = _state.value.copy(
                        isDetecting = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
                _state.value = _state.value.copy(
                    errorMessage = "Frame processing error",
                    isDetecting = false
                )
            }
        }
    }

    /**
     * Start a new detection session
     */
    fun startSession() {
        startTime = System.currentTimeMillis()
        frameCount = 0
        lastFpsTimestamp = System.currentTimeMillis()
        _state.value = _state.value.copy(
            elapsedTimeMs = 0L,
            fps = 0
        )
    }

    /**
     * End detection session
     */
    fun endSession() {
        startTime = null
        _state.value = _state.value.copy(
            currentPerson = null,
            postureFeatures = null,
            elapsedTimeMs = 0L,
            fps = 0
        )
    }

    /**
     * Reset all state
     */
    fun reset() {
        endSession()
        _state.value = PoseDetectionState(isModelLoaded = _state.value.isModelLoaded)
    }

    /**
     * Start tracking an exercise
     */
    fun startExerciseTracking(exerciseType: ExerciseType) {
        _state.value = _state.value.copy(
            isTrackingExercise = true,
            currentExercise = exerciseType,
            exerciseState = ExerciseState(exerciseType = exerciseType)
        )
        startSession()
    }

    /**
     * Stop tracking exercise
     */
    fun stopExerciseTracking() {
        _state.value = _state.value.copy(
            isTrackingExercise = false
        )
    }

    /**
     * Reset exercise tracking to try a different exercise
     */
    fun resetExerciseTracking() {
        _state.value = _state.value.copy(
            currentExercise = ExerciseType.UNKNOWN,
            exerciseState = null,
            isTrackingExercise = false
        )
    }

    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        poseDetector?.close()
        Log.d(TAG, "PoseDetectionViewModel cleared")
    }
}

/**
 * Factory for creating PoseDetectionViewModel
 */
class PoseDetectionViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoseDetectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PoseDetectionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
