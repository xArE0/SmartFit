package com.example.project_smartfit.presentation.screens.exercise

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.project_smartfit.domain.detection.MediaPipePoseDetector
import com.example.project_smartfit.domain.features.PoseFeatureExtractor
import com.example.project_smartfit.domain.evaluation.RuleBasedFormEvaluator
import com.example.project_smartfit.domain.evaluation.ExerciseProfiles
import com.example.project_smartfit.domain.evaluation.FeedbackStabilizer
import com.example.project_smartfit.domain.tracking.RepTracker
import com.example.project_smartfit.domain.model.*

/**
 * UI State for pose detection and exercise tracking
 */
data class ExerciseTrackingState(
    val isModelLoaded: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    
    // Pose detection
    val currentPoseFrame: PoseFrame? = null,
    val currentFeatures: FeatureVector? = null,
    
    // Exercise tracking
    val selectedExercise: ExerciseType = ExerciseType.SQUAT,
    val isTrackingActive: Boolean = false,
    
    // Form evaluation
    val formResult: FormEvaluationResult? = null,
    val isFormCorrect: Boolean = false,
    val currentErrors: List<String> = emptyList(),
    val primaryFeedback: String? = null,
    
    // Rep tracking
    val repCount: Int = 0,
    val currentPhase: RepPhase = RepPhase.UNKNOWN,
    
    // Performance metrics
    val fps: Int = 0,
    val averageVisibility: Float = 0f,
    
    // Debug mode
    val debugMode: Boolean = false,
    val debugAngles: Map<String, Float> = emptyMap()
)

/**
 * ViewModel for real-time exercise form evaluation
 * Integrates: MediaPipe Pose → Feature Extraction → Rule-Based Evaluation → Rep Tracking
 */
class PoseDetectionViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "PoseDetectionVM"
    }
    
    private val _state = MutableStateFlow(ExerciseTrackingState())
    val state: StateFlow<ExerciseTrackingState> = _state
    
    // Core components
    private var poseDetector: MediaPipePoseDetector? = null
    private var featureExtractor: PoseFeatureExtractor? = null
    private var formEvaluator: RuleBasedFormEvaluator? = null
    private var repTracker: RepTracker? = null
    private val feedbackStabilizer = FeedbackStabilizer()
    
    // Performance tracking
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var lastProcessedFrameTime = 0L
    private val minFrameIntervalMs = 33L  // ~30 FPS max to prevent backlog
    
    init {
        initializeSystem()
    }
    
    /**
     * Initialize all components
     */
    private fun initializeSystem() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing exercise tracking system...")
                
                // Initialize MediaPipe Pose detector
                poseDetector = MediaPipePoseDetector(context).apply {
                    if (!initialize()) {
                        _state.value = _state.value.copy(
                            errorMessage = "Failed to initialize MediaPipe Pose. Check if model file exists."
                        )
                        return@launch
                    }
                }
                
                // Initialize feature extractor
                featureExtractor = PoseFeatureExtractor()
                
                // Initialize with default exercise (Squat)
                updateExerciseProfile(ExerciseType.SQUAT)
                
                _state.value = _state.value.copy(
                    isModelLoaded = true,
                    errorMessage = null
                )
                
                Log.d(TAG, "System initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing system", e)
                _state.value = _state.value.copy(
                    errorMessage = "Initialization error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Process camera frame through complete pipeline
     */
    fun processCameraFrame(bitmap: Bitmap) {
        if (!_state.value.isModelLoaded) return
        
        // Throttle frame processing to prevent backlog
        val now = System.currentTimeMillis()
        if (now - lastProcessedFrameTime < minFrameIntervalMs) {
            return  // Skip this frame
        }
        lastProcessedFrameTime = now
        
        // Skip if already processing (prevent queue buildup)
        if (_state.value.isProcessing) {
            return
        }
        
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true)
                
                // Run detection on background thread
                val result = withContext(Dispatchers.Default) {
                    processFramePipeline(bitmap)
                }
                
                // Update UI state
                _state.value = result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
                _state.value = _state.value.copy(
                    isProcessing = false,
                    currentPoseFrame = null,  // Clear on error
                    errorMessage = "Processing error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Complete processing pipeline (runs on background thread)
     */
    private fun processFramePipeline(bitmap: Bitmap): ExerciseTrackingState {
        Log.d(TAG, "Processing frame: ${bitmap.width}x${bitmap.height}")
        
        // Step 1: Pose Detection (MediaPipe)
        val poseFrame = poseDetector?.detectPose(bitmap)
        if (poseFrame == null) {
            Log.w(TAG, "No pose detected in frame")
            // IMPORTANT: Clear the pose frame when detection fails
            return _state.value.copy(
                currentPoseFrame = null,  // Clear stale data
                currentFeatures = null,
                formResult = null,
                isFormCorrect = false,
                currentErrors = emptyList(),
                primaryFeedback = null,
                isProcessing = false,
                averageVisibility = 0f,
                errorMessage = null  // Don't show error for no detection
            )
        }
        
        Log.d(TAG, "Pose detected! Landmarks: ${poseFrame.landmarks.size}, Avg visibility: ${poseFrame.getAverageVisibility()}")
        
        // Step 2: Feature Extraction
        val features = featureExtractor?.extract(poseFrame)
        if (features == null) {
            return _state.value.copy(
                isProcessing = false,
                errorMessage = "Feature extraction failed"
            )
        }
        
        // Step 3: Form Evaluation
        val formResult = formEvaluator?.evaluate(features)
        if (formResult == null) {
            return _state.value.copy(
                isProcessing = false,
                errorMessage = "Form evaluation failed"
            )
        }
        
        // Step 4: Rep Tracking
        val trackedResult = repTracker?.track(features, formResult) ?: formResult
        
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
        
        // Extract debug angles if debug mode is on
        val debugAngles = if (_state.value.debugMode) {
            extractDebugAngles(features)
        } else {
            emptyMap()
        }
        
        // Step 5: Stabilize feedback (prevent flickering)
        val stabilized = feedbackStabilizer.process(trackedResult)
        
        // Check if user has started the exercise (entered start phase)
        val hasStarted = repTracker?.hasStarted() == true
        
        val finalIsCorrect: Boolean
        val finalErrors: List<String>
        val finalFeedback: String?
        
        if (!hasStarted) {
            // User hasn't entered start position yet
            finalIsCorrect = true // Don't show red error state
            finalErrors = emptyList()
            
            // Generate contextual "Start" message
            val profile = ExerciseProfiles.getProfile(_state.value.selectedExercise)
            finalFeedback = when (_state.value.selectedExercise) {
                ExerciseType.SQUAT -> "Stand up straight to start"
                ExerciseType.PUSHUP -> "Get into plank position"
                ExerciseType.DUMBBELL_CURL -> "Straighten arms to start"
                else -> "Get into starting position"
            }
        } else {
            // Normal feedback
            finalIsCorrect = stabilized.isCorrect
            finalErrors = stabilized.errors.map { it.message }
            finalFeedback = stabilized.primaryFeedback
        }
        
        // Build updated state with stabilized feedback
        return _state.value.copy(
            currentPoseFrame = poseFrame,
            currentFeatures = features,
            formResult = trackedResult,
            isFormCorrect = finalIsCorrect,
            currentErrors = finalErrors,
            primaryFeedback = finalFeedback,
            repCount = repTracker?.getRepCount() ?: 0,
            currentPhase = trackedResult.repPhase,
            fps = fps,
            averageVisibility = features.overallVisibility,
            isProcessing = false,
            errorMessage = null,
            debugAngles = debugAngles
        )
    }
    
    /**
     * Extract angles for debug display
     */
    private fun extractDebugAngles(features: FeatureVector): Map<String, Float> {
        return when (_state.value.selectedExercise) {
            ExerciseType.SQUAT -> mapOf(
                "Left Knee" to features.leftKneeAngle,
                "Right Knee" to features.rightKneeAngle,
                "Left Hip" to features.leftHipAngle,
                "Right Hip" to features.rightHipAngle,
                "Back Angle" to features.backAngle,
                "Knee Ratio" to features.kneeDistanceRatio
            )
            ExerciseType.PUSHUP -> mapOf(
                "Left Elbow" to features.leftElbowAngle,
                "Right Elbow" to features.rightElbowAngle,
                "Body Alignment" to features.bodyAlignment,
                "Body Horizontal" to features.bodyHorizontalAngle
            )
            ExerciseType.DUMBBELL_CURL -> mapOf(
                "Left Elbow" to features.leftElbowFlexion,
                "Right Elbow" to features.rightElbowFlexion,
                "Torso Upright" to features.torsoUprightness
            )
            else -> emptyMap()
        }
    }
    
    /**
     * Start tracking exercise
     */
    fun startTracking(exerciseType: ExerciseType) {
        updateExerciseProfile(exerciseType)
        repTracker?.reset()
        feedbackStabilizer.reset()
        frameCount = 0
        lastFpsTimestamp = System.currentTimeMillis()
        
        _state.value = _state.value.copy(
            selectedExercise = exerciseType,
            isTrackingActive = true,
            repCount = 0,
            currentPhase = RepPhase.UNKNOWN,
            errorMessage = null
        )
        
        Log.d(TAG, "Started tracking: $exerciseType")
    }
    
    /**
     * Stop tracking
     */
    fun stopTracking() {
        _state.value = _state.value.copy(
            isTrackingActive = false
        )
        Log.d(TAG, "Stopped tracking")
    }
    
    /**
     * Reset rep counter
     */
    fun resetReps() {
        repTracker?.reset()
        feedbackStabilizer.reset()
        _state.value = _state.value.copy(
            repCount = 0,
            currentPhase = RepPhase.UNKNOWN
        )
    }
    
    /**
     * Toggle debug mode
     */
    fun toggleDebugMode() {
        _state.value = _state.value.copy(
            debugMode = !_state.value.debugMode
        )
    }
    
    /**
     * Update exercise profile and recreate evaluator/tracker
     */
    private fun updateExerciseProfile(exerciseType: ExerciseType) {
        val profile = ExerciseProfiles.getProfile(exerciseType)
        formEvaluator = RuleBasedFormEvaluator(profile)
        repTracker = RepTracker(profile)
    }
    
    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        poseDetector?.close()
        Log.d(TAG, "ViewModel cleared")
    }
    
    // ========== Backward Compatibility Methods ==========
    // These methods exist for compatibility with old UI screens
    // New code should use startTracking/stopTracking instead
    
    fun startSession() {
        // Auto-start tracking for camera screens (backward compatibility)
        if (!_state.value.isTrackingActive) {
            startTracking(_state.value.selectedExercise)
        }
    }
    
    fun endSession() {
        // Stub for backward compatibility
        stopTracking()
    }
    
    fun reset() {
        // Stub for backward compatibility
        resetReps()
    }
    
    fun startExerciseTracking(exerciseType: ExerciseType) {
        // Stub for backward compatibility
        startTracking(exerciseType)
    }
    
    fun stopExerciseTracking() {
        // Stub for backward compatibility
        stopTracking()
    }
}

/**
 * Factory for creating ViewModel
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
