package com.example.project_smartfit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_smartfit.data.local.SessionManager
import com.example.project_smartfit.presentation.screens.auth.login.LoginScreen
import com.example.project_smartfit.presentation.screens.auth.signup.SignupScreen
import com.example.project_smartfit.presentation.screens.home.HomeScreen
import com.example.project_smartfit.presentation.screens.profile.ProfileScreen
import com.example.project_smartfit.presentation.screens.exercise.WorkoutMonitoringScreen
import com.example.project_smartfit.presentation.screens.exercise.PostureAnalysisScreen
import com.example.project_smartfit.presentation.screens.exercise.QuickPoseCheckScreen

@Composable
fun NavigationGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val sessionManager = SessionManager.getInstance(context)
    val startDestination = if (sessionManager.isLoggedIn()) NavHome else NavLogin

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<NavLogin> {
            LoginScreen(navController, sessionManager)
        }
        composable<NavSignup> {
            SignupScreen(navController)
        }
        composable<NavHome> {
            HomeScreen(navController, sessionManager)
        }
        composable<NavProfile> {
            ProfileScreen(navController, sessionManager)
        }
        composable<NavWorkoutMonitoring> {
            WorkoutMonitoringScreen(navController)
        }
        composable<NavPostureAnalysis> {
            PostureAnalysisScreen(navController)
        }
        composable<NavQuickCheck> {
            QuickPoseCheckScreen(navController)
        }
    }
}
