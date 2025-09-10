package com.example.project_smartfit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Composable
fun Navigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val sessionManager = SessionManager.getInstance(context)
    val startDestination = if (sessionManager.isLoggedIn()) NavHomepage else NavSignup
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<NavHomepage> {
            Homepage(navController,sessionManager)
        }

        composable<NavProfile> {
            Profile(navController,sessionManager)
        }

        composable<NavLogin> {
            Login(navController,sessionManager)
        }

        composable<NavSignup> {
            Signup(navController)
        }
    }
}

@Serializable
object NavHomepage

@Serializable
object NavProfile

@Serializable
object NavLogin

@Serializable
object NavSignup
