package com.example.project_smartfit.ml

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Manages voice feedback via Android TTS.
 *
 * Design goals:
 *  - Never overwhelm the user: minimum [MIN_INTERVAL_MS] between utterances.
 *  - Don't repeat the same tip back-to-back.
 *  - Announce reps briefly ("3", "4", …).
 *  - Speak form corrections concisely.
 */
class VoiceFeedbackManager(context: Context) {

    companion object {
        private const val TAG = "VoiceFeedback"
        private const val MIN_INTERVAL_MS = 6_000L   // 6 seconds between tips
        private const val REP_INTERVAL_MS = 2_000L   // 2 seconds between rep announcements
    }

    private var tts: TextToSpeech? = null
    private var isReady = false

    private var lastSpokenTip: String? = null
    private var lastTipTimeMs = 0L
    private var lastRepTimeMs = 0L
    private var lastAnnouncedRep = 0

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.1f)  // slightly faster for exercise cues
                isReady = true
                Log.d(TAG, "TTS ready")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    /**
     * Speak a form correction tip if enough time has passed
     * and it's not a repeat of the last tip.
     */
    fun speakTip(tip: String) {
        if (!isReady) return
        val now = System.currentTimeMillis()

        // Throttle
        if (now - lastTipTimeMs < MIN_INTERVAL_MS) return
        // Don't repeat same tip consecutively
        if (tip == lastSpokenTip) return

        lastSpokenTip = tip
        lastTipTimeMs = now
        tts?.speak(tip, TextToSpeech.QUEUE_FLUSH, null, "tip_${now}")
    }

    /**
     * Announce a rep count (e.g. "5").
     * Only speaks if the rep number has changed and enough time has passed.
     */
    fun announceRep(count: Int) {
        if (!isReady) return
        if (count <= lastAnnouncedRep) return

        val now = System.currentTimeMillis()
        if (now - lastRepTimeMs < REP_INTERVAL_MS) return

        lastAnnouncedRep = count
        lastRepTimeMs = now
        tts?.speak("$count", TextToSpeech.QUEUE_ADD, null, "rep_${count}")
    }

    /**
     * Announce the exercise that was locked in.
     */
    fun announceExercise(displayName: String) {
        if (!isReady) return
        tts?.speak(
            "$displayName detected. Let's go!",
            TextToSpeech.QUEUE_FLUSH, null, "exercise_lock"
        )
    }

    /**
     * Speak a hold duration milestone (for plank).
     */
    fun announceHold(seconds: Int) {
        if (!isReady) return
        // Announce every 15 seconds
        if (seconds > 0 && seconds % 15 == 0) {
            val now = System.currentTimeMillis()
            if (now - lastRepTimeMs < REP_INTERVAL_MS) return
            lastRepTimeMs = now
            tts?.speak("$seconds seconds", TextToSpeech.QUEUE_ADD, null, "hold_${seconds}")
        }
    }

    fun reset() {
        lastSpokenTip = null
        lastTipTimeMs = 0L
        lastRepTimeMs = 0L
        lastAnnouncedRep = 0
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
