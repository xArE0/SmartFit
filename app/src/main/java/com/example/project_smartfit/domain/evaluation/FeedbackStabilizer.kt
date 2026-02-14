package com.example.project_smartfit.domain.evaluation

import com.example.project_smartfit.domain.model.FormEvaluationResult
import com.example.project_smartfit.domain.model.FormError

/**
 * Stabilizes form feedback to prevent flickering and rapid changes.
 * 
 * Instead of showing every per-frame error, this system:
 * 1. Maintains a sliding window of recent form results
 * 2. Only surfaces an error if it appeared in 60%+ of the last N frames
 * 3. Holds displayed feedback for a minimum duration even if the error momentarily disappears
 * 4. Ranks errors by persistence/severity → shows the most consistent problem
 * 5. Requires "convergence" before switching to a different error message
 */
class FeedbackStabilizer(
    private val windowSize: Int = 15,           // ~0.5s at 30fps
    private val persistenceThreshold: Float = 0.6f, // Error must appear in 60%+ of window
    private val holdFrames: Int = 30,            // Hold feedback for ~1 second
    private val correctStreakRequired: Int = 20   // Must be correct for 20 frames before clearing
) {
    // Sliding window of recent form results
    private val resultWindow = ArrayDeque<FormEvaluationResult>(windowSize)
    
    // Currently displayed feedback
    private var displayedFeedback: String? = null
    private var displayedErrors: List<FormError> = emptyList()
    private var displayedIsCorrect: Boolean = true
    
    // Hold counter - how many frames to keep showing current feedback
    private var holdCounter: Int = 0
    
    // Correct form streak counter
    private var correctStreak: Int = 0
    
    /**
     * Process a new frame's evaluation result and return stabilized feedback.
     * Returns a StabilizedFeedback object with the converged state.
     */
    fun process(result: FormEvaluationResult): StabilizedFeedback {
        // Add to sliding window
        resultWindow.addLast(result)
        if (resultWindow.size > windowSize) {
            resultWindow.removeFirst()
        }
        
        // Track correct form streak
        if (result.isCorrectForm) {
            correctStreak++
        } else {
            correctStreak = 0
        }
        
        // Analyze the window for persistent errors
        val persistentErrors = findPersistentErrors()
        
        // Decide what to display
        if (persistentErrors.isNotEmpty()) {
            // We have persistent errors - show the most frequent one
            val primaryError = persistentErrors.first()
            displayedFeedback = primaryError.message
            displayedErrors = persistentErrors
            displayedIsCorrect = false
            holdCounter = holdFrames
            correctStreak = 0
        } else if (holdCounter > 0) {
            // Still holding previous feedback
            holdCounter--
        } else if (correctStreak >= correctStreakRequired) {
            // Clear feedback - form has been correct long enough
            displayedFeedback = null
            displayedErrors = emptyList()
            displayedIsCorrect = true
        }
        
        return StabilizedFeedback(
            isCorrect = displayedIsCorrect,
            primaryFeedback = displayedFeedback,
            errors = displayedErrors,
            rawIsCorrect = result.isCorrectForm,
            convergenceRatio = calculateConvergence()
        )
    }
    
    /**
     * Find errors that appear frequently enough in the sliding window
     * to be considered "persistent" (not just noise).
     */
    private fun findPersistentErrors(): List<FormError> {
        if (resultWindow.size < windowSize / 2) {
            // Not enough data yet - don't show anything
            return emptyList()
        }
        
        // Count occurrences of each error type
        val errorCounts = mutableMapOf<String, Int>()
        val errorExamples = mutableMapOf<String, FormError>()
        
        for (result in resultWindow) {
            for (error in result.errors) {
                val key = error.type.name
                errorCounts[key] = (errorCounts[key] ?: 0) + 1
                errorExamples[key] = error // Keep latest instance for message
            }
        }
        
        val threshold = (resultWindow.size * persistenceThreshold).toInt()
        
        // Filter to only persistent errors, sorted by frequency (most common first)
        return errorCounts.entries
            .filter { it.value >= threshold }
            .sortedByDescending { it.value }
            .mapNotNull { (key, _) -> errorExamples[key] }
    }
    
    /**
     * Calculate how converged the current window is (0.0 = all noise, 1.0 = consistent)
     */
    private fun calculateConvergence(): Float {
        if (resultWindow.isEmpty()) return 0f
        val correctCount = resultWindow.count { it.isCorrectForm }
        val incorrectCount = resultWindow.size - correctCount
        return maxOf(correctCount, incorrectCount).toFloat() / resultWindow.size
    }
    
    /**
     * Reset the stabilizer (e.g., when switching exercises)
     */
    fun reset() {
        resultWindow.clear()
        displayedFeedback = null
        displayedErrors = emptyList()
        displayedIsCorrect = true
        holdCounter = 0
        correctStreak = 0
    }
}

/**
 * Output of the FeedbackStabilizer - converged, stable feedback
 */
data class StabilizedFeedback(
    val isCorrect: Boolean,        // Stabilized correctness (not per-frame)
    val primaryFeedback: String?,  // The most persistent error message (or null if correct)
    val errors: List<FormError>,   // All persistent errors
    val rawIsCorrect: Boolean,     // Actual per-frame correctness (for debug)
    val convergenceRatio: Float    // How consistent the window is (0-1)
)
