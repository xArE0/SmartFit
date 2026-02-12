package com.example.project_smartfit.domain.model

/**
 * Result of form evaluation containing correctness, errors, and feedback
 */
data class FormEvaluationResult(
    val isCorrectForm: Boolean,
    val errors: List<FormError> = emptyList(),
    val feedbackMessages: List<String> = emptyList(),
    val repPhase: RepPhase = RepPhase.UNKNOWN,
    val shouldIncrementRep: Boolean = false,
    val confidenceScore: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get primary error message for UI display
     */
    fun getPrimaryError(): String? {
        return errors.maxByOrNull { it.severity.priority }?.message
    }
    
    /**
     * Check if there are critical errors
     */
    fun hasCriticalErrors(): Boolean {
        return errors.any { it.severity == ErrorSeverity.CRITICAL }
    }
}

/**
 * Represents a specific form error detected
 */
data class FormError(
    val type: ErrorType,
    val message: String,
    val severity: ErrorSeverity,
    val affectedJoints: List<String> = emptyList()
)

/**
 * Types of form errors that can be detected
 */
enum class ErrorType {
    // Squat errors
    INSUFFICIENT_DEPTH,
    KNEES_CAVING_INWARD,
    KNEES_OVER_TOES,
    EXCESSIVE_FORWARD_LEAN,
    BACK_ROUNDING,
    HEEL_LIFTING,
    ASYMMETRIC_MOVEMENT,
    
    // Pushup errors
    ELBOWS_FLARING,
    SAGGING_HIPS,
    RAISED_HIPS,
    INCOMPLETE_RANGE,
    HEAD_DROPPING,
    
    // Dumbbell curl errors
    SWINGING_BODY,
    ELBOW_MOVEMENT,
    INCOMPLETE_CURL,
    WRIST_BENDING,
    
    // General errors
    POOR_STABILITY,
    LOW_VISIBILITY,
    UNKNOWN
}

/**
 * Severity levels for form errors
 */
enum class ErrorSeverity(val priority: Int) {
    INFO(1),        // Minor issue, informational
    WARNING(2),     // Form could be improved
    CRITICAL(3)     // Dangerous or significantly incorrect form
}

/**
 * Phase of a repetition
 */
enum class RepPhase {
    TOP,            // Starting position (standing for squat, arms extended for pushup)
    DESCENDING,     // Moving down
    BOTTOM,         // Bottom position (deep squat, chest near ground)
    ASCENDING,      // Moving up
    UNKNOWN         // Cannot determine phase
}
