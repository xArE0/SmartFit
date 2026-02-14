package com.example.project_smartfit.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Counts exercise repetitions using joint-angle state machines.
 *
 * Each exercise defines an "up" and "down" phase based on a key angle.
 * A rep is counted when the angle traverses from one phase to the other
 * and back. Hysteresis thresholds prevent jitter.
 *
 * For hold exercises (plank), counts elapsed seconds instead.
 */
class RepCounter {

    var repCount: Int = 0
        private set

    /** Current phase of the rep cycle. */
    var phase: Phase = Phase.IDLE
        private set

    /** Hold timer for isometric exercises (plank). */
    private var holdStartMs: Long = 0L
    var holdSeconds: Int = 0
        private set

    enum class Phase { IDLE, UP, DOWN }

    /**
     * Process a new frame and update rep count.
     * Call this on every frame once the exercise is locked in.
     *
     * @return true if a new rep was just completed
     */
    fun update(exercise: String, landmarks: List<NormalizedLandmark>): Boolean {
        if (landmarks.size < 33) return false

        return when (exercise) {
            "squat"         -> countByKneeAngle(landmarks, downThresh = 120f, upThresh = 155f)
            "push-up"       -> countByElbowAngle(landmarks, downThresh = 100f, upThresh = 155f)
            "hammer curl"   -> countByCurlAngle(landmarks, topThresh = 60f, bottomThresh = 140f)
            "lateral raise" -> countByArmRaise(landmarks, upThresh = 0.04f, downThresh = 0.10f)
            "leg raises"    -> countByLegRaise(landmarks, upThresh = 0.05f, downThresh = 0.12f)
            "russian twist" -> countByTwist(landmarks, twistThresh = 0.04f, centerThresh = 0.02f)
            "plank"         -> { updateHoldTimer(); false }
            else            -> false
        }
    }

    fun reset() {
        repCount = 0
        phase = Phase.IDLE
        holdStartMs = 0L
        holdSeconds = 0
    }

    // ── Squat: knee angle ────────────────────────────────────────────────

    private fun countByKneeAngle(
        lm: List<NormalizedLandmark>,
        downThresh: Float,
        upThresh: Float
    ): Boolean {
        val leftKnee = angle(lm[23], lm[25], lm[27])
        val rightKnee = angle(lm[24], lm[26], lm[28])
        val avg = (leftKnee + rightKnee) / 2f

        return countUpDown(avg, downThresh = downThresh, upThresh = upThresh)
    }

    // ── Push-up: elbow angle ─────────────────────────────────────────────

    private fun countByElbowAngle(
        lm: List<NormalizedLandmark>,
        downThresh: Float,
        upThresh: Float
    ): Boolean {
        val leftElbow = angle(lm[11], lm[13], lm[15])
        val rightElbow = angle(lm[12], lm[14], lm[16])
        val avg = (leftElbow + rightElbow) / 2f

        return countUpDown(avg, downThresh = downThresh, upThresh = upThresh)
    }

    // ── Hammer curl: elbow angle (inverted — down=straight, up=bent) ────

    private fun countByCurlAngle(
        lm: List<NormalizedLandmark>,
        topThresh: Float,
        bottomThresh: Float
    ): Boolean {
        val leftElbow = angle(lm[11], lm[13], lm[15])
        val rightElbow = angle(lm[12], lm[14], lm[16])
        val avg = (leftElbow + rightElbow) / 2f

        // For curls: small angle = top (UP), large angle = bottom (DOWN)
        return countUpDown(avg, downThresh = bottomThresh, upThresh = topThresh, invertedCurl = true)
    }

    // ── Lateral raise: wrist height relative to shoulder ─────────────────

    private fun countByArmRaise(
        lm: List<NormalizedLandmark>,
        upThresh: Float,
        downThresh: Float
    ): Boolean {
        val leftDiff = lm[15].y() - lm[11].y()   // positive = wrist below shoulder
        val rightDiff = lm[16].y() - lm[12].y()
        val avg = (leftDiff + rightDiff) / 2f

        // Small/negative avg = arms up, large positive = arms down
        return when (phase) {
            Phase.IDLE, Phase.DOWN -> {
                if (avg <= upThresh) { phase = Phase.UP; false }
                else false
            }
            Phase.UP -> {
                if (avg >= downThresh) {
                    phase = Phase.DOWN
                    repCount++
                    true
                } else false
            }
        }
    }

    // ── Leg raises: ankle height relative to hip ─────────────────────────

    private fun countByLegRaise(
        lm: List<NormalizedLandmark>,
        upThresh: Float,
        downThresh: Float
    ): Boolean {
        val hipY = (lm[23].y() + lm[24].y()) / 2f
        val ankleY = (lm[27].y() + lm[28].y()) / 2f
        val diff = ankleY - hipY  // positive = legs below hip, small/negative = legs up

        return when (phase) {
            Phase.IDLE, Phase.DOWN -> {
                if (diff <= upThresh) { phase = Phase.UP; false }
                else false
            }
            Phase.UP -> {
                if (diff >= downThresh) {
                    phase = Phase.DOWN
                    repCount++
                    true
                } else false
            }
        }
    }

    // ── Russian twist: shoulder-hip x offset ─────────────────────────────

    private fun countByTwist(
        lm: List<NormalizedLandmark>,
        twistThresh: Float,
        centerThresh: Float
    ): Boolean {
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX = (lm[23].x() + lm[24].x()) / 2f
        val offset = shoulderMidX - hipMidX  // positive = twisted right, negative = left

        // Count each twist to either side as half a rep (two twists = 1 rep)
        return when (phase) {
            Phase.IDLE -> {
                if (abs(offset) > twistThresh) { phase = Phase.UP; false }
                else false
            }
            Phase.UP -> {
                // Returned to center
                if (abs(offset) < centerThresh) {
                    phase = Phase.DOWN
                    false
                } else false
            }
            Phase.DOWN -> {
                // Twisted to the other side
                if (abs(offset) > twistThresh) {
                    phase = Phase.IDLE
                    repCount++
                    true
                } else false
            }
        }
    }

    // ── Plank hold timer ─────────────────────────────────────────────────

    private fun updateHoldTimer() {
        val now = System.currentTimeMillis()
        if (holdStartMs == 0L) holdStartMs = now
        holdSeconds = ((now - holdStartMs) / 1000).toInt()
    }

    // ── Generic up/down counter ──────────────────────────────────────────

    /**
     * Counts reps based on an angle crossing two thresholds.
     * Default: angle goes DOWN (below [downThresh]) then UP (above [upThresh]) = 1 rep.
     * For curls (invertedCurl=true): angle goes UP first (below topThresh) then DOWN.
     */
    private fun countUpDown(
        currentAngle: Float,
        downThresh: Float,
        upThresh: Float,
        invertedCurl: Boolean = false
    ): Boolean {
        return if (invertedCurl) {
            // Curl: small angle = top of curl, large angle = bottom
            when (phase) {
                Phase.IDLE, Phase.DOWN -> {
                    if (currentAngle <= upThresh) { phase = Phase.UP; false }
                    else false
                }
                Phase.UP -> {
                    if (currentAngle >= downThresh) {
                        phase = Phase.DOWN
                        repCount++
                        true
                    } else false
                }
            }
        } else {
            // Standard: large angle = up/standing, small angle = down/bent
            when (phase) {
                Phase.IDLE, Phase.UP -> {
                    if (currentAngle <= downThresh) { phase = Phase.DOWN; false }
                    else false
                }
                Phase.DOWN -> {
                    if (currentAngle >= upThresh) {
                        phase = Phase.UP
                        repCount++
                        true
                    } else false
                }
            }
        }
    }

    // ── Geometry ─────────────────────────────────────────────────────────

    private fun angle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Float {
        val radians = atan2(
            (c.y() - b.y()).toDouble(), (c.x() - b.x()).toDouble()
        ) - atan2(
            (a.y() - b.y()).toDouble(), (a.x() - b.x()).toDouble()
        )
        var degrees = Math.toDegrees(abs(radians)).toFloat()
        if (degrees > 180f) degrees = 360f - degrees
        return degrees
    }
}
