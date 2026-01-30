package com.example.project_smartfit.domain.model

import com.example.project_smartfit.domain.detection.Person
import com.example.project_smartfit.domain.features.PostureFeatures

/**
 * Base interface for all exercises
 * Each exercise implementation should extend this interface
 */
interface Exercise {
    /**
     * The type of this exercise
     */
    val type: ExerciseType

    /**
     * Detect if the current pose matches this exercise
     * @param person Detected person with keypoints
     * @param features Extracted posture features
     * @return true if pose matches this exercise
     */
    fun detectExercise(person: Person, features: PostureFeatures): Boolean

    /**
     * Process a single frame for this exercise
     * @param person Detected person with keypoints
     * @param features Extracted posture features
     * @param currentState Current exercise state
     * @return Updated exercise state
     */
    fun processFrame(
        person: Person,
        features: PostureFeatures,
        currentState: ExerciseState
    ): ExerciseState
}
