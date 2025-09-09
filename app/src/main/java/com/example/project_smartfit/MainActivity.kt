package com.example.project_smartfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.example.project_smartfit.ui.theme.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var screen by remember {
                mutableStateOf(
                    if (SessionManager.getInstance(context).isLoggedIn()) "home" else "signup"
                )
            }
            when (screen) {
                "signup" -> Signup(onSignupSuccess = { screen = "login" })
                "login" -> Login(onLoginSuccess = { screen = "home" })
                "home" -> Homepage(onLogout = {
                    SessionManager.getInstance(context).clearSession()
                    screen = "login"
                })
            }
        }
    }
}
