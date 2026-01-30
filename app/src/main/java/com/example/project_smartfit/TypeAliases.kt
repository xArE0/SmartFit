package com.example.project_smartfit

/**
 * Backward compatibility: Type aliases for moved classes
 * 
 * This file provides type aliases to maintain backward compatibility
 * with existing code that imports from the root package.
 * 
 * New code should import directly from the new package locations:
 * - com.example.project_smartfit.pose.*
 * - com.example.project_smartfit.features.*
 * - com.example.project_smartfit.exercises.*
 */

// Pose detection classes
typealias Person = com.example.project_smartfit.pose.Person
typealias KeyPoint = com.example.project_smartfit.pose.KeyPoint
typealias PoseDetector = com.example.project_smartfit.pose.PoseDetector
typealias PoseDetectionConfig = com.example.project_smartfit.pose.PoseDetectionConfig
typealias PoseDetectionViewModel = com.example.project_smartfit.pose.PoseDetectionViewModel
typealias PoseDetectionViewModelFactory = com.example.project_smartfit.pose.PoseDetectionViewModelFactory
typealias PoseDetectionState = com.example.project_smartfit.pose.PoseDetectionState

// Feature extraction classes
typealias FeatureExtractor = com.example.project_smartfit.features.FeatureExtractor
typealias PostureFeatures = com.example.project_smartfit.features.PostureFeatures

// Exercise classes
typealias ExerciseType = com.example.project_smartfit.exercises.ExerciseType
typealias ExerciseState = com.example.project_smartfit.exercises.ExerciseState
typealias Exercise = com.example.project_smartfit.exercises.Exercise

// Overlay/Visualization (object, cannot be typealiased but referenced)
val PoseVisualization = com.example.project_smartfit.pose.overlay.PoseVisualization
