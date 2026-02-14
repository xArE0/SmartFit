package com.example.project_smartfit.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Rule-based exercise form feedback engine.
 *
 * Analyses the current pose landmarks and the detected exercise type
 * to produce real-time form cues. Uses joint angles and body alignment
 * computed from MediaPipe's 33-landmark pose.
 *
 * Landmark indices follow MediaPipe Pose:
 *   11/12 = shoulders, 13/14 = elbows, 15/16 = wrists,
 *   23/24 = hips, 25/26 = knees, 27/28 = ankles.
 */
object ExerciseFeedbackEngine {

    data class Feedback(
        val tips: List<String>,
        val isGoodForm: Boolean
    ) {
        companion object {
            val GOOD = Feedback(listOf("Good form! Keep it up 💪"), true)
            val NONE = Feedback(emptyList(), true)
        }
    }

    /**
     * Analyse the current pose for the given exercise and return feedback.
     *
     * @param exerciseLabel  lowercase label from the classifier (e.g. "squat")
     * @param landmarks      MediaPipe 33-landmark list for one person
     */
    fun analyse(
        exerciseLabel: String,
        landmarks: List<NormalizedLandmark>
    ): Feedback {
        if (landmarks.size < 33) return Feedback.NONE

        return when (exerciseLabel) {
            "squat"         -> analyseSquat(landmarks)
            "push-up"       -> analysePushUp(landmarks)
            "plank"         -> analysePlank(landmarks)
            "hammer curl"   -> analyseHammerCurl(landmarks)
            "lateral raise" -> analyseLateralRaise(landmarks)
            "leg raises"    -> analyseLegRaises(landmarks)
            "russian twist" -> analyseRussianTwist(landmarks)
            else            -> Feedback.NONE
        }
    }

    // ── Squat ────────────────────────────────────────────────────────────

    private fun analyseSquat(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Knee angle (both sides)
        val leftKneeAngle = angle(lm[23], lm[25], lm[27])
        val rightKneeAngle = angle(lm[24], lm[26], lm[28])
        val avgKnee = (leftKneeAngle + rightKneeAngle) / 2f

        // Knee cave check — knees should stay over toes, not collapse inward
        val kneeWidth = abs(lm[25].x() - lm[26].x())
        val ankleWidth = abs(lm[27].x() - lm[28].x())
        if (kneeWidth < ankleWidth * 0.75f) {
            tips += "Knees caving in — push knees outward"
        }

        // Torso lean — shoulders should stay roughly above hips
        val shoulderMidY = (lm[11].y() + lm[12].y()) / 2f
        val hipMidY = (lm[23].y() + lm[24].y()) / 2f
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX = (lm[23].x() + lm[24].x()) / 2f
        val torsoLean = abs(shoulderMidX - hipMidX)
        if (torsoLean > 0.08f) {
            tips += "Keep your torso more upright"
        }

        // Depth cue
        if (avgKnee > 160f) {
            tips += "Go deeper — aim for thighs parallel to ground"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Push-up ──────────────────────────────────────────────────────────

    private fun analysePushUp(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Body alignment — shoulder, hip, ankle should be roughly in line
        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY = (lm[23].y() + lm[24].y()) / 2f
        val ankleY = (lm[27].y() + lm[28].y()) / 2f

        // Hips sagging (hip much lower than line from shoulder to ankle)
        val expectedHipY = (shoulderY + ankleY) / 2f
        if (hipY > expectedHipY + 0.06f) {
            tips += "Hips sagging — tighten your core"
        }
        // Hips piking up
        if (hipY < expectedHipY - 0.06f) {
            tips += "Hips too high — lower into a straight line"
        }

        // Elbow angle — at bottom of push-up should be ~90°
        val leftElbow = angle(lm[11], lm[13], lm[15])
        val rightElbow = angle(lm[12], lm[14], lm[16])
        val avgElbow = (leftElbow + rightElbow) / 2f
        if (avgElbow > 160f) {
            tips += "Lower yourself more — bend elbows to ~90°"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Plank ────────────────────────────────────────────────────────────

    private fun analysePlank(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY = (lm[23].y() + lm[24].y()) / 2f
        val ankleY = (lm[27].y() + lm[28].y()) / 2f

        val expectedHipY = (shoulderY + ankleY) / 2f
        if (hipY > expectedHipY + 0.05f) {
            tips += "Hips dropping — engage your core"
        }
        if (hipY < expectedHipY - 0.05f) {
            tips += "Hips piking up — flatten your back"
        }

        // Neck alignment — nose shouldn't be much higher or lower than shoulder line
        val noseY = lm[0].y()
        if (noseY < shoulderY - 0.07f) {
            tips += "Don't look up — keep neck neutral"
        }
        if (noseY > shoulderY + 0.07f) {
            tips += "Don't drop your head — look at the floor ahead"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Hammer Curl ──────────────────────────────────────────────────────

    private fun analyseHammerCurl(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Elbow drift — elbows should stay close to torso
        val leftElbowX = lm[13].x()
        val leftShoulderX = lm[11].x()
        val rightElbowX = lm[14].x()
        val rightShoulderX = lm[12].x()

        if (abs(leftElbowX - leftShoulderX) > 0.08f) {
            tips += "Keep left elbow tucked to your side"
        }
        if (abs(rightElbowX - rightShoulderX) > 0.08f) {
            tips += "Keep right elbow tucked to your side"
        }

        // Shoulder movement — shoulders should stay level, not shrugging
        val shoulderDiffY = abs(lm[11].y() - lm[12].y())
        if (shoulderDiffY > 0.06f) {
            tips += "Keep shoulders level — don't shrug"
        }

        // Body sway — torso should stay upright
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX = (lm[23].x() + lm[24].x()) / 2f
        if (abs(shoulderMidX - hipMidX) > 0.06f) {
            tips += "Don't swing your body — keep torso still"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Lateral Raise ────────────────────────────────────────────────────

    private fun analyseLateralRaise(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Arms should raise to roughly shoulder height
        val leftWristY = lm[15].y()
        val rightWristY = lm[16].y()
        val leftShoulderY = lm[11].y()
        val rightShoulderY = lm[12].y()

        // Check if arms are too low (wrist well below shoulder)
        val leftDiff = leftWristY - leftShoulderY
        val rightDiff = rightWristY - rightShoulderY
        if (leftDiff > 0.10f && rightDiff > 0.10f) {
            tips += "Raise arms higher — to shoulder level"
        }

        // Check if arms go too high (above shoulder)
        if (leftWristY < leftShoulderY - 0.08f || rightWristY < rightShoulderY - 0.08f) {
            tips += "Don't raise arms above shoulder height"
        }

        // Elbow bend — slight bend is OK, but not too much
        val leftElbowAngle = angle(lm[11], lm[13], lm[15])
        val rightElbowAngle = angle(lm[12], lm[14], lm[16])
        if (leftElbowAngle < 120f || rightElbowAngle < 120f) {
            tips += "Keep arms straighter — slight bend only"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Leg Raises ───────────────────────────────────────────────────────

    private fun analyseLegRaises(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Legs should stay relatively straight
        val leftKnee = angle(lm[23], lm[25], lm[27])
        val rightKnee = angle(lm[24], lm[26], lm[28])

        if (leftKnee < 150f || rightKnee < 150f) {
            tips += "Keep legs straighter — don't bend knees"
        }

        // Lower back — shoulders should stay on the ground (y roughly stable)
        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY = (lm[23].y() + lm[24].y()) / 2f
        if (shoulderY < hipY - 0.10f) {
            tips += "Keep your lower back on the ground"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Russian Twist ────────────────────────────────────────────────────

    private fun analyseRussianTwist(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Torso rotation — shoulders should twist relative to hips
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX = (lm[23].x() + lm[24].x()) / 2f
        val twist = abs(shoulderMidX - hipMidX)

        if (twist < 0.03f) {
            tips += "Rotate your torso more — twist from the core"
        }

        // Feet position — feet shouldn't move too much
        val leftAnkle = lm[27]
        val rightAnkle = lm[28]
        val ankleSpread = abs(leftAnkle.x() - rightAnkle.x())
        if (ankleSpread > 0.20f) {
            tips += "Keep feet together and stable"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Geometry helpers ─────────────────────────────────────────────────

    /**
     * Computes the angle at point B in the triangle A-B-C, in degrees.
     * Uses normalized landmark coordinates.
     */
    private fun angle(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Float {
        val radians = atan2(
            (c.y() - b.y()).toDouble(),
            (c.x() - b.x()).toDouble()
        ) - atan2(
            (a.y() - b.y()).toDouble(),
            (a.x() - b.x()).toDouble()
        )
        var degrees = Math.toDegrees(abs(radians)).toFloat()
        if (degrees > 180f) degrees = 360f - degrees
        return degrees
    }
}
