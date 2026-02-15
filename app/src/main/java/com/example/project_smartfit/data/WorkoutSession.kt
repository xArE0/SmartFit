package com.example.project_smartfit.data

/**
 * Represents one completed exercise session.
 *
 * Saved to local storage after the user taps "End Exercise" on the camera screen.
 */
data class WorkoutSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exercise: String,
    val reps: Int,
    val holdSeconds: Int = 0,
    val durationSeconds: Int,
    val formScore: Int,            // 0–100
    val formTips: List<String>,    // unique tips received during the session
    val timestamp: Long = System.currentTimeMillis()
)
