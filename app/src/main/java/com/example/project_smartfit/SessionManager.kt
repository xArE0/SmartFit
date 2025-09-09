package com.example.project_smartfit

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

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