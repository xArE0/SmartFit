package com.example.project_smartfit.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Tracks challenge enrollment and day-by-day progress via SharedPreferences.
 */
class ChallengeRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("challenge_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Enrollment ───────────────────────────────────────────────────

    fun enroll(challengeId: String) {
        prefs.edit()
            .putString("enrolled_$challengeId", challengeId)
            .putLong("enrolled_at_$challengeId", System.currentTimeMillis())
            .apply()
    }

    fun isEnrolled(challengeId: String): Boolean =
        prefs.contains("enrolled_$challengeId")

    fun getEnrolledChallengeIds(): List<String> =
        Challenges.all.filter { isEnrolled(it.id) }.map { it.id }

    // ── Day completion ───────────────────────────────────────────────

    fun completeDay(challengeId: String, day: Int) {
        val completed = getCompletedDays(challengeId).toMutableSet()
        completed.add(day)
        prefs.edit()
            .putString("completed_${challengeId}", gson.toJson(completed.toList()))
            .apply()
    }

    fun getCompletedDays(challengeId: String): Set<Int> {
        val json = prefs.getString("completed_${challengeId}", null) ?: return emptySet()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson<List<Int>>(json, type).toSet()
    }

    fun isDayCompleted(challengeId: String, day: Int): Boolean =
        day in getCompletedDays(challengeId)

    // ── Progress ─────────────────────────────────────────────────────

    fun getProgress(challengeId: String): Float {
        val challenge = Challenges.getById(challengeId) ?: return 0f
        val completed = getCompletedDays(challengeId).size
        return completed.toFloat() / challenge.totalDays
    }

    fun getCurrentDay(challengeId: String): Int {
        val completed = getCompletedDays(challengeId)
        return (completed.maxOrNull() ?: 0) + 1
    }

    fun getStreak(challengeId: String): Int {
        val completed = getCompletedDays(challengeId).sorted()
        if (completed.isEmpty()) return 0
        var streak = 1
        for (i in completed.size - 1 downTo 1) {
            if (completed[i] - completed[i - 1] == 1) streak++
            else break
        }
        return streak
    }

    // ── Unenroll ─────────────────────────────────────────────────────

    fun unenroll(challengeId: String) {
        prefs.edit()
            .remove("enrolled_$challengeId")
            .remove("enrolled_at_$challengeId")
            .remove("completed_$challengeId")
            .apply()
    }

    companion object {
        @Volatile private var instance: ChallengeRepository? = null
        fun getInstance(context: Context): ChallengeRepository =
            instance ?: synchronized(this) {
                instance ?: ChallengeRepository(context.applicationContext).also { instance = it }
            }
    }
}
