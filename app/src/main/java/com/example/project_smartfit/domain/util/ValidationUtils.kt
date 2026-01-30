package com.example.project_smartfit.domain.util

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isStrongPassword(password: String): Boolean {
        // At least 8 chars, 1 digit, 1 upper, 1 lower, 1 special char
        val regex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*()_+\\-={}:;'<>,.?]).{8,}\$")
        return regex.containsMatchIn(password)
    }

    fun isValidAge(age: String): Boolean {
        val n = age.toIntOrNull() ?: return false
        return n in 5..120
    }

    fun isValidWeight(weight: String): Boolean {
        val w = weight.toFloatOrNull() ?: return false
        return w in 20.0..400.0
    }

    fun isValidHeight(height: String): Boolean {
        val h = height.toFloatOrNull() ?: return false
        return h in 50.0..250.0
    }
}
