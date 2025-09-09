package com.example.project_smartfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.material3.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var screen by remember { mutableStateOf("signup") }
            when (screen) {
                "signup" -> Signup(onSignupSuccess = { screen = "login" })
                "login" -> Login(onLoginSuccess = { screen = "home" })
                "home" -> Homepage()
            }
        }
    }
}
