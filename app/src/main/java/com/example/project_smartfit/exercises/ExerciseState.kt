package com.example.project_smartfit.exercises

/**
 * State of exercise detection and repetition tracking
 */
data class ExerciseState(
    val exerciseType: ExerciseType = ExerciseType.UNKNOWN,
    val repCount: Int = 0,
    val isInMotion: Boolean = false,
    val lastFrameState: String = "", // "up", "down", "hold", etc.
    val confidenceScore: Float = 0f
)
