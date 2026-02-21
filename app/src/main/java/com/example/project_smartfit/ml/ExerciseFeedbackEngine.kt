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
 * All tips are PHASE-AWARE — tips that only make sense at a specific
 * moment in the rep cycle (e.g. "go lower" at the top of a squat) are
 * gated by [repPhase] so they never fire at the wrong moment.
 *
 * Landmark indices follow MediaPipe Pose:
 *   0 = nose, 11/12 = shoulders, 13/14 = elbows, 15/16 = wrists,
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
     * @param repPhase       current phase from RepCounter — used to gate phase-specific tips
     */
    fun analyse(
        exerciseLabel: String,
        landmarks: List<NormalizedLandmark>,
        repPhase: RepCounter.Phase = RepCounter.Phase.IDLE
    ): Feedback {
        if (landmarks.size < 33) return Feedback.NONE

        return when (exerciseLabel) {
            "squat"         -> analyseSquat(landmarks, repPhase)
            "push-up"       -> analysePushUp(landmarks, repPhase)
            "plank"         -> analysePlank(landmarks)
            "hammer curl"   -> analyseHammerCurl(landmarks)
            "lateral raise" -> analyseLateralRaise(landmarks, repPhase)
            "leg raises"    -> analyseLegRaises(landmarks, repPhase)
            "russian twist" -> analyseRussianTwist(landmarks, repPhase)
            else            -> Feedback.NONE
        }
    }

    // ── Squat ────────────────────────────────────────────────────────────

    private fun analyseSquat(lm: List<NormalizedLandmark>, phase: RepCounter.Phase): Feedback {
        val tips = mutableListOf<String>()

        val leftKneeAngle  = angle(lm[23], lm[25], lm[27])
        val rightKneeAngle = angle(lm[24], lm[26], lm[28])
        val avgKnee = (leftKneeAngle + rightKneeAngle) / 2f

        // ── Always-checked (bad at any phase) ──────────────────────────

        // Knee cave — knees should stay over toes, not collapse inward
        val kneeWidth  = abs(lm[25].x() - lm[26].x())
        val ankleWidth = abs(lm[27].x() - lm[28].x())
        if (kneeWidth < ankleWidth * 0.75f) {
            tips += "Knees caving in — push knees outward"
        }

        // Foot width — stance too narrow makes balance harder
        val shoulderWidth = abs(lm[11].x() - lm[12].x())
        if (ankleWidth < shoulderWidth * 0.65f) {
            tips += "Widen your stance — feet shoulder-width apart"
        }

        // Torso lean — shoulders should stay roughly above hips
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX      = (lm[23].x() + lm[24].x()) / 2f
        if (abs(shoulderMidX - hipMidX) > 0.10f) {
            tips += "Keep your torso more upright"
        }

        // ── Phase-gated: only meaningful at the BOTTOM of the squat ────
        // FIX: "Go deeper" used to fire at the TOP (legs straight). Now only fires
        // when we're in the DOWN phase (i.e. user is actually at the bottom).
        if (phase == RepCounter.Phase.DOWN && avgKnee > 125f) {
            tips += "Go deeper — aim for thighs parallel to ground"
        }

        // Back arch — hip-shoulder vertical alignment (only meaningful at bottom)
        if (phase == RepCounter.Phase.DOWN) {
            val shoulderMidY = (lm[11].y() + lm[12].y()) / 2f
            val hipMidY      = (lm[23].y() + lm[24].y()) / 2f
            val hipKneeMidY  = (lm[25].y() + lm[26].y()) / 2f
            // If shoulders are much higher than hips relative to knee, torso is very upright — fine
            // If hips are extremely high above knees, they haven't sat back enough
            if ((hipMidY - hipKneeMidY) < 0.04f) {
                tips += "Sit back more — push hips further behind you"
            }
        }

        // Heel lift — ankles should stay low (heel on ground)
        // z() axis: smaller = closer to camera; if ankles rise, z decreases relative to hip
        val ankleY = (lm[27].y() + lm[28].y()) / 2f
        val hipY   = (lm[23].y() + lm[24].y()) / 2f
        // Heels are normally well below hips in Y space; if ankleY is unexpectedly close to hipY
        // it suggests the person is on their toes (this is a heuristic, works for side views)
        if (phase == RepCounter.Phase.DOWN && (hipY - ankleY) < 0.15f) {
            tips += "Keep heels on the ground"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Push-up ──────────────────────────────────────────────────────────

    private fun analysePushUp(lm: List<NormalizedLandmark>, phase: RepCounter.Phase): Feedback {
        val tips = mutableListOf<String>()

        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY      = (lm[23].y() + lm[24].y()) / 2f
        val ankleY    = (lm[27].y() + lm[28].y()) / 2f

        // ── Always-checked ──────────────────────────────────────────────

        // Hips sagging (always bad — core not engaged)
        val expectedHipY = (shoulderY + ankleY) / 2f
        if (hipY > expectedHipY + 0.06f) {
            tips += "Hips sagging — tighten your core"
        }
        // Hips piking (always bad)
        if (hipY < expectedHipY - 0.06f) {
            tips += "Hips too high — lower into a straight line"
        }

        // Hand width — wrists should be roughly shoulder-width
        val wristWidth    = abs(lm[15].x() - lm[16].x())
        val shoulderWidth = abs(lm[11].x() - lm[12].x())
        if (wristWidth > shoulderWidth * 1.6f) {
            tips += "Hands too wide — bring them closer to shoulder-width"
        }
        if (wristWidth < shoulderWidth * 0.6f) {
            tips += "Hands too narrow — widen to shoulder-width"
        }

        // Head drooping (always bad)
        val noseY = lm[0].y()
        if (noseY > shoulderY + 0.10f) {
            tips += "Don't drop your head — keep neck neutral"
        }

        // ── Phase-gated: only meaningful at the BOTTOM of the push-up ──
        // FIX: "Lower yourself more" used to fire at the TOP of the push-up
        // (elbows extended = avgElbow > 160). Now only fires in DOWN phase.
        val leftElbow  = angle(lm[11], lm[13], lm[15])
        val rightElbow = angle(lm[12], lm[14], lm[16])
        val avgElbow   = (leftElbow + rightElbow) / 2f

        if (phase == RepCounter.Phase.DOWN && avgElbow > 130f) {
            tips += "Lower yourself more — bend elbows to ~90°"
        }

        // Elbow flare — elbows should stay ~45° from torso, not fully flared out
        if (phase == RepCounter.Phase.DOWN) {
            val elbowWidth = abs(lm[13].x() - lm[14].x())
            if (elbowWidth > shoulderWidth * 1.5f) {
                tips += "Tuck elbows in slightly — don't flare them out"
            }
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Plank ────────────────────────────────────────────────────────────
    // Plank is a static hold — all checks are continuous (no phase gates needed)

    private fun analysePlank(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY      = (lm[23].y() + lm[24].y()) / 2f
        val ankleY    = (lm[27].y() + lm[28].y()) / 2f

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

        // Elbow placement — elbows should be roughly under the shoulders
        val leftElbowX    = lm[13].x()
        val leftShoulderX = lm[11].x()
        val rightElbowX   = lm[14].x()
        val rightShoulderX= lm[12].x()
        if (abs(leftElbowX - leftShoulderX) > 0.10f || abs(rightElbowX - rightShoulderX) > 0.10f) {
            tips += "Place elbows directly under your shoulders"
        }

        // Foot width — feet should be close together in a plank
        val footWidth = abs(lm[27].x() - lm[28].x())
        if (footWidth > 0.15f) {
            tips += "Keep feet close together"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Hammer Curl ──────────────────────────────────────────────────────
    // Hammer curl: IDLE = down (arms extended), UP = curl complete (arm bent)

    private fun analyseHammerCurl(lm: List<NormalizedLandmark>): Feedback {
        val tips = mutableListOf<String>()

        // Elbow drift — elbows should stay close to torso
        val leftElbowX    = lm[13].x()
        val leftShoulderX = lm[11].x()
        val rightElbowX   = lm[14].x()
        val rightShoulderX= lm[12].x()

        if (abs(leftElbowX - leftShoulderX) > 0.09f) {
            tips += "Keep left elbow tucked to your side"
        }
        if (abs(rightElbowX - rightShoulderX) > 0.09f) {
            tips += "Keep right elbow tucked to your side"
        }

        // Shoulder shrug — shoulders should stay level, not shrugging up
        val shoulderDiffY = abs(lm[11].y() - lm[12].y())
        if (shoulderDiffY > 0.06f) {
            tips += "Keep shoulders level — don't shrug"
        }

        // Shoulder rise — both shoulders rising (shrugging with weight)
        val shoulderMidY = (lm[11].y() + lm[12].y()) / 2f
        val hipMidY      = (lm[23].y() + lm[24].y()) / 2f
        val torsoHeight  = abs(shoulderMidY - hipMidY)
        // If torso appears to shorten (shoulders drop toward hips in Y), user may be shrugging
        if (torsoHeight < 0.18f) {
            tips += "Don't shrug shoulders — keep them down and back"
        }

        // Body sway — torso should stay upright
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX      = (lm[23].x() + lm[24].x()) / 2f
        if (abs(shoulderMidX - hipMidX) > 0.07f) {
            tips += "Don't swing your body — keep torso still"
        }

        // Wrist alignment — wrists shouldn't drop far below elbows at the top
        val leftWristY  = lm[15].y()
        val leftElbowY  = lm[13].y()
        val rightWristY = lm[16].y()
        val rightElbowY = lm[14].y()
        // At the top of a hammer curl the wrist should be near or above elbow height
        // If wrist is well below elbow (large positive Y diff), user isn't curling fully
        if ((leftWristY - leftElbowY) > 0.12f && (rightWristY - rightElbowY) > 0.12f) {
            tips += "Curl the weight all the way up — full range of motion"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Lateral Raise ────────────────────────────────────────────────────

    private fun analyseLateralRaise(lm: List<NormalizedLandmark>, phase: RepCounter.Phase): Feedback {
        val tips = mutableListOf<String>()

        val leftWristY    = lm[15].y()
        val rightWristY   = lm[16].y()
        val leftShoulderY = lm[11].y()
        val rightShoulderY= lm[12].y()

        // ── Phase-gated ─────────────────────────────────────────────────
        // FIX: "Raise arms higher" used to fire during the DOWN phase (when lowering is
        // intentional). Now it only fires in IDLE or UP phase — i.e. when the user is
        // supposed to be raising or holding.
        if (phase == RepCounter.Phase.IDLE || phase == RepCounter.Phase.UP) {
            val leftDiff  = leftWristY  - leftShoulderY
            val rightDiff = rightWristY - rightShoulderY
            if (leftDiff > 0.12f && rightDiff > 0.12f) {
                tips += "Raise arms higher — to shoulder level"
            }

            // Arms going too high (only relevant during the raise)
            if (leftWristY < leftShoulderY - 0.10f || rightWristY < rightShoulderY - 0.10f) {
                tips += "Don't raise arms above shoulder height"
            }

            // Elbow bend — slight bend is OK, but not too much during the raise
            // FIX: elbow bend check also suppressed during DOWN to avoid false alerts
            val leftElbowAngle  = angle(lm[11], lm[13], lm[15])
            val rightElbowAngle = angle(lm[12], lm[14], lm[16])
            if (leftElbowAngle < 110f || rightElbowAngle < 110f) {
                tips += "Keep arms straighter — slight bend only"
            }
        }

        // ── Always-checked ──────────────────────────────────────────────

        // Shoulder shrug — shoulders shouldn't rise during the raise
        val shoulderDiffY = abs(lm[11].y() - lm[12].y())
        if (shoulderDiffY > 0.07f) {
            tips += "Keep shoulders level — don't shrug"
        }

        // Body lean — torso should be upright, not swaying to assist the raise
        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX      = (lm[23].x() + lm[24].x()) / 2f
        if (abs(shoulderMidX - hipMidX) > 0.08f) {
            tips += "Keep your torso upright — don't lean to the side"
        }

        // Momentum / swinging — if wrists move very far apart from each other
        val wristSpread = abs(leftWristY - rightWristY)
        if (wristSpread > 0.12f) {
            tips += "Raise both arms evenly"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Leg Raises ───────────────────────────────────────────────────────

    private fun analyseLegRaises(lm: List<NormalizedLandmark>, phase: RepCounter.Phase): Feedback {
        val tips = mutableListOf<String>()

        val leftKnee  = angle(lm[23], lm[25], lm[27])
        val rightKnee = angle(lm[24], lm[26], lm[28])

        // ── Phase-gated ─────────────────────────────────────────────────
        // FIX: "Keep legs straighter" used to fire during DOWN phase too. Bending
        // knees a bit while lowering is natural and reduces lower-back stress.
        // Only enforce straight legs during the UP phase.
        if (phase == RepCounter.Phase.UP) {
            if (leftKnee < 150f || rightKnee < 150f) {
                tips += "Keep legs straighter — don't bend knees"
            }
        }

        // ── Always-checked ──────────────────────────────────────────────

        // Lower back — keep back pressed to the ground (shoulders vs. hips in Y)
        val shoulderY = (lm[11].y() + lm[12].y()) / 2f
        val hipY      = (lm[23].y() + lm[24].y()) / 2f
        if (shoulderY < hipY - 0.10f) {
            tips += "Keep your lower back pressed to the ground"
        }

        // Back arch — if hips are lifting off the ground
        if (phase == RepCounter.Phase.DOWN) {
            val ankleY = (lm[27].y() + lm[28].y()) / 2f
            // When lowering, ankles should go lower (larger Y) than hips
            if (hipY < ankleY - 0.20f) {
                tips += "Control the descent — don't let your back arch"
            }
        }

        // Arms pushing off the floor — usually detected by wrist being close to hip level
        val wristY = (lm[15].y() + lm[16].y()) / 2f
        if (abs(wristY - hipY) < 0.06f) {
            tips += "Don't push off with your arms — keep them flat"
        }

        return if (tips.isEmpty()) Feedback.GOOD else Feedback(tips, false)
    }

    // ── Russian Twist ────────────────────────────────────────────────────

    private fun analyseRussianTwist(lm: List<NormalizedLandmark>, phase: RepCounter.Phase): Feedback {
        val tips = mutableListOf<String>()

        val shoulderMidX = (lm[11].x() + lm[12].x()) / 2f
        val hipMidX      = (lm[23].x() + lm[24].x()) / 2f
        val twist        = abs(shoulderMidX - hipMidX)

        // ── Phase-gated ─────────────────────────────────────────────────
        // FIX: "Rotate more" used to fire at the CENTER (between twists) which is
        // exactly where the user is supposed to return. Now only fires during UP
        // (first twist) to encourage full rotation.
        if (phase == RepCounter.Phase.UP) {
            if (twist < 0.03f) {
                tips += "Rotate your torso more — twist from the core"
            }
        }

        // ── Always-checked ──────────────────────────────────────────────

        // Feet stability — feet shouldn't move too much or spread wide
        val leftAnkle  = lm[27]
        val rightAnkle = lm[28]
        val ankleSpread = abs(leftAnkle.x() - rightAnkle.x())
        if (ankleSpread > 0.20f) {
            tips += "Keep feet together and stable"
        }

        // Torso lean-back angle — user should be leaning back ~45°, not sitting upright
        // or fully lying down. Check shoulder vs. hip Y difference.
        val shoulderMidY = (lm[11].y() + lm[12].y()) / 2f
        val hipMidY      = (lm[23].y() + lm[24].y()) / 2f
        val torsoAngleDiff = shoulderMidY - hipMidY // positive = shoulders higher than hips (reclined)

        if (torsoAngleDiff < 0.04f) {
            // Shoulders and hips at nearly the same height = too upright
            tips += "Lean back slightly — torso at ~45°"
        }
        if (torsoAngleDiff > 0.20f) {
            // Shoulders much higher than hips = too reclined (nearly lying flat)
            tips += "Sit up more — don't lie too far back"
        }

        // Arm reach — hands should reach across the body, not just forward
        val leftWristX  = lm[15].x()
        val rightWristX = lm[16].x()
        val handSpread  = abs(leftWristX - rightWristX)
        if (handSpread < 0.05f && twist > 0.03f) {
            // Twisting but hands are close together = hands not crossing the body
            tips += "Reach hands across your body as you twist"
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
