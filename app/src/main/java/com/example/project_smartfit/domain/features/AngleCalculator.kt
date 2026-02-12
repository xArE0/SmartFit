package com.example.project_smartfit.domain.features

import com.example.project_smartfit.domain.model.PoseLandmark
import kotlin.math.*

/**
 * Pure utility object for geometric calculations on pose landmarks
 * All functions are stateless and unit-testable
 */
object AngleCalculator {
    
    /**
     * Calculate angle between three points using vector dot product
     * Returns angle in degrees [0, 180]
     * 
     * @param p1 First point (e.g., shoulder)
     * @param p2 Vertex point (e.g., elbow) - the angle is measured here
     * @param p3 Third point (e.g., wrist)
     * @return Angle in degrees
     */
    fun calculate3PointAngle(p1: PoseLandmark, p2: PoseLandmark, p3: PoseLandmark): Float {
        // Vector from p2 to p1
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v1z = p1.z - p2.z
        
        // Vector from p2 to p3
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y
        val v2z = p3.z - p2.z
        
        // Dot product
        val dotProduct = v1x * v2x + v1y * v2y + v1z * v2z
        
        // Magnitudes
        val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
        
        if (mag1 == 0f || mag2 == 0f) return 0f
        
        // Cosine of angle
        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        
        // Convert to degrees
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }
    
    /**
     * Calculate 2D angle (ignoring z-axis) for side-view analysis
     */
    fun calculate2DAngle(p1: PoseLandmark, p2: PoseLandmark, p3: PoseLandmark): Float {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y
        
        val dotProduct = v1x * v2x + v1y * v2y
        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)
        
        if (mag1 == 0f || mag2 == 0f) return 0f
        
        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }
    
    /**
     * Calculate angle from vertical (for measuring torso lean, etc.)
     * Returns 0° for perfectly vertical, 90° for horizontal
     */
    fun calculateAngleFromVertical(p1: PoseLandmark, p2: PoseLandmark): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        
        // Angle from vertical (y-axis)
        val angleFromHorizontal = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        
        // Convert to angle from vertical
        return abs(90f - abs(angleFromHorizontal))
    }
    
    /**
     * Calculate angle from horizontal (for measuring body alignment in pushups)
     * Returns 0° for perfectly horizontal, 90° for vertical
     */
    fun calculateAngleFromHorizontal(p1: PoseLandmark, p2: PoseLandmark): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        
        return abs(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
    }
    
    /**
     * Calculate 2D Euclidean distance between two landmarks
     */
    fun calculateDistance2D(p1: PoseLandmark, p2: PoseLandmark): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate 3D Euclidean distance between two landmarks
     */
    fun calculateDistance3D(p1: PoseLandmark, p2: PoseLandmark): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Calculate midpoint between two landmarks
     */
    fun calculateMidpoint(p1: PoseLandmark, p2: PoseLandmark): PoseLandmark {
        return PoseLandmark(
            x = (p1.x + p2.x) / 2,
            y = (p1.y + p2.y) / 2,
            z = (p1.z + p2.z) / 2,
            visibility = min(p1.visibility, p2.visibility)
        )
    }
    
    /**
     * Calculate ratio between two distances (useful for normalized measurements)
     */
    fun calculateDistanceRatio(
        p1: PoseLandmark, p2: PoseLandmark,
        p3: PoseLandmark, p4: PoseLandmark
    ): Float {
        val dist1 = calculateDistance2D(p1, p2)
        val dist2 = calculateDistance2D(p3, p4)
        
        return if (dist2 != 0f) dist1 / dist2 else 0f
    }
    
    /**
     * Check if three points are roughly collinear (for body alignment checks)
     * Returns a score from 0 (not aligned) to 100 (perfectly aligned)
     */
    fun calculateAlignmentScore(p1: PoseLandmark, p2: PoseLandmark, p3: PoseLandmark): Float {
        // Calculate the angle at the middle point
        val angle = calculate3PointAngle(p1, p2, p3)
        
        // Perfect alignment = 180°, worst = 0°
        // Convert to 0-100 score
        return (angle / 180f) * 100f
    }
    
    /**
     * Calculate vertical distance (useful for depth measurements)
     */
    fun calculateVerticalDistance(p1: PoseLandmark, p2: PoseLandmark): Float {
        return abs(p2.y - p1.y)
    }
    
    /**
     * Calculate horizontal distance (useful for width measurements)
     */
    fun calculateHorizontalDistance(p1: PoseLandmark, p2: PoseLandmark): Float {
        return abs(p2.x - p1.x)
    }
}
