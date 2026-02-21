package com.example.project_smartfit.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * Local repository for workout sessions.
 *
 * Uses SharedPreferences + Gson for persistence — simple, no external DB needed.
 * Singleton access via [getInstance].
 */
class WorkoutRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val sessionsKey = "sessions"

    // ── Write ────────────────────────────────────────────────────────────

    fun saveSession(session: WorkoutSession) {
        val sessions = getAllSessions().toMutableList()
        sessions.add(0, session) // newest first
        val json = gson.toJson(sessions)
        prefs.edit().putString(sessionsKey, json).apply()
    }

    // ── Read ─────────────────────────────────────────────────────────────

    fun getAllSessions(): List<WorkoutSession> {
        val json = prefs.getString(sessionsKey, null) ?: return emptyList()
        val type = object : TypeToken<List<WorkoutSession>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSessionsInRange(startMs: Long, endMs: Long): List<WorkoutSession> {
        return getAllSessions().filter { it.timestamp in startMs..endMs }
    }

    fun getTodaySessions(): List<WorkoutSession> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        return getAllSessions().filter { it.timestamp >= startOfDay }
    }

    fun getThisWeekSessions(): List<WorkoutSession> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return getAllSessions().filter { it.timestamp >= cal.timeInMillis }
    }

    fun getThisMonthSessions(): List<WorkoutSession> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return getAllSessions().filter { it.timestamp >= cal.timeInMillis }
    }

    fun getSessionById(id: String): WorkoutSession? {
        return getAllSessions().find { it.id == id }
    }

    // ── Stats ────────────────────────────────────────────────────────────

    data class WorkoutStats(
        val totalWorkouts: Int,
        val totalReps: Int,
        val favoriteExercise: String,
        val currentStreak: Int,     // consecutive days with workouts
        val avgFormScore: Int
    )

    fun getStats(): WorkoutStats {
        val all = getAllSessions()
        if (all.isEmpty()) {
            return WorkoutStats(0, 0, "—", 0, 0)
        }

        val totalReps = all.sumOf { it.reps }
        val favorite = all.groupingBy { it.exercise }.eachCount()
            .maxByOrNull { it.value }?.key ?: "—"
        val avgScore = all.map { it.formScore }.average().toInt()
        val streak = calculateStreak(all)

        return WorkoutStats(
            totalWorkouts = all.size,
            totalReps = totalReps,
            favoriteExercise = favorite,
            currentStreak = streak,
            avgFormScore = avgScore
        )
    }

    /**
     * Returns reps per day for the last 7 days (index 0 = 6 days ago, 6 = today).
     */
    fun getWeeklyReps(): List<Int> {
        val result = MutableList(7) { 0 }
        val today = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(6 - i))
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startMs = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endMs = cal.timeInMillis

            result[i] = getAllSessions()
                .filter { it.timestamp in startMs until endMs }
                .sumOf { it.reps }
        }
        return result
    }

    // ── Period Report ────────────────────────────────────────────────────

    data class PeriodReport(
        val periodLabel: String,
        val totalWorkouts: Int,
        val totalReps: Int,
        val exercisesDone: List<String>,
        val avgFormScore: Int,
        val bestExercise: String,
        val totalMinutes: Int
    )

    fun generateReport(periodLabel: String, sessions: List<WorkoutSession>): PeriodReport {
        if (sessions.isEmpty()) {
            return PeriodReport(periodLabel, 0, 0, emptyList(), 0, "—", 0)
        }
        val exercises = sessions.map { it.exercise }.distinct()
        val avgScore = sessions.map { it.formScore }.average().toInt()
        val best = sessions.groupingBy { it.exercise }.eachCount()
            .maxByOrNull { it.value }?.key ?: "—"
        val totalMins = sessions.sumOf { it.durationSeconds } / 60

        return PeriodReport(
            periodLabel = periodLabel,
            totalWorkouts = sessions.size,
            totalReps = sessions.sumOf { it.reps },
            exercisesDone = exercises,
            avgFormScore = avgScore,
            bestExercise = best,
            totalMinutes = totalMins
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun calculateStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val daysWithWorkouts = sessions.map { session ->
            cal.timeInMillis = session.timestamp
            val year = cal.get(Calendar.YEAR)
            val day = cal.get(Calendar.DAY_OF_YEAR)
            year * 1000 + day
        }.distinct().sorted().reversed()

        if (daysWithWorkouts.isEmpty()) return 0

        val todayCal = Calendar.getInstance()
        val todayKey = todayCal.get(Calendar.YEAR) * 1000 + todayCal.get(Calendar.DAY_OF_YEAR)
        todayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = todayCal.get(Calendar.YEAR) * 1000 + todayCal.get(Calendar.DAY_OF_YEAR)

        // Streak must include today or yesterday
        if (daysWithWorkouts[0] != todayKey && daysWithWorkouts[0] != yesterdayKey) return 0

        var streak = 1
        for (i in 1 until daysWithWorkouts.size) {
            val diff = daysWithWorkouts[i - 1] - daysWithWorkouts[i]
            if (diff == 1) streak++ else break
        }
        return streak
    }

    // ── Singleton ────────────────────────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: WorkoutRepository? = null

        fun getInstance(context: Context): WorkoutRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkoutRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
