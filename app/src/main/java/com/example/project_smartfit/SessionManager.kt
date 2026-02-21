package com.example.project_smartfit

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    // ── Auth session ─────────────────────────────────────────────────────

    fun saveSession(userId: String, email: String) {
        prefs.edit {
            putString("userId", userId)
                .putString("email", email)
        }
    }

    fun getUserId(): String? = prefs.getString("userId", null)
    fun getEmail(): String? = prefs.getString("email", null)

    fun isLoggedIn(): Boolean = getUserId() != null

    fun clearSession() {
        prefs.edit { clear() }
    }

    // ── User profile (cached locally for plan generator) ─────────────────

    fun saveProfile(
        name: String,
        gender: String,
        age: String,
        weight: String,
        height: String,
        fitnessLevel: String,
        fitnessGoal: String
    ) {
        prefs.edit {
            putString("profile_name", name)
            putString("profile_gender", gender)
            putString("profile_age", age)
            putString("profile_weight", weight)
            putString("profile_height", height)
            putString("profile_fitnessLevel", fitnessLevel)
            putString("profile_fitnessGoal", fitnessGoal)
        }
    }

    fun getName(): String = prefs.getString("profile_name", null)
        ?: getEmail()?.substringBefore("@") ?: "User"
    fun getGender(): String = prefs.getString("profile_gender", "") ?: ""
    fun getAge(): String = prefs.getString("profile_age", "") ?: ""
    fun getWeight(): String = prefs.getString("profile_weight", "") ?: ""
    fun getHeight(): String = prefs.getString("profile_height", "") ?: ""
    fun getFitnessLevel(): String = prefs.getString("profile_fitnessLevel", "Beginner") ?: "Beginner"
    fun getFitnessGoal(): String = prefs.getString("profile_fitnessGoal", "General Fitness") ?: "General Fitness"

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}