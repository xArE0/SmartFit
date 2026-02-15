package com.example.project_smartfit

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.project_smartfit.camera.CameraScreen
import kotlinx.serialization.Serializable

@Composable
fun Navigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val sessionManager = SessionManager.getInstance(context)
    val startDestination = if (sessionManager.isLoggedIn()) NavHomepage else NavSignup

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable<NavHomepage> {
            Homepage(navController, sessionManager)
        }

        composable<NavWorkout> {
            WorkoutHubScreen(navController)
        }

        composable<NavExplore> {
            ExploreScreen(navController)
        }

        composable<NavProgress> {
            ProgressScreen(navController)
        }

        composable<NavProfile> {
            Profile(navController, sessionManager)
        }

        composable<NavLogin> {
            Login(navController, sessionManager)
        }

        composable<NavSignup> {
            Signup(navController)
        }

        composable<NavExerciseCamera> { backStackEntry ->
            val route = backStackEntry.toRoute<NavExerciseCamera>()
            CameraScreen(navController, route.planJson)
        }

        composable<NavWorkoutSetup> {
            WorkoutSetupScreen(navController)
        }

        composable<NavSessionDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavSessionDetail>()
            SessionDetailScreen(navController, route.sessionId)
        }

        composable<NavWorkoutSummary> { backStackEntry ->
            val route = backStackEntry.toRoute<NavWorkoutSummary>()
            WorkoutSummaryScreen(navController, route.summaryJson)
        }

        composable<NavCustomPlanBuilder> {
            CustomPlanBuilderScreen(navController)
        }

        composable<NavChallenges> {
            ChallengesScreen(navController)
        }

        composable<NavChallengeDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavChallengeDetail>()
            ChallengeDetailScreen(navController, route.challengeId)
        }

        composable<NavExerciseInfo> { backStackEntry ->
            val route = backStackEntry.toRoute<NavExerciseInfo>()
            ExerciseInfoScreen(navController, route.exerciseId)
        }
    }
}

// ── Route Definitions ─────────────────────────────────────────────────

@Serializable object NavHomepage
@Serializable object NavWorkout
@Serializable object NavExplore
@Serializable object NavProgress
@Serializable object NavProfile
@Serializable object NavLogin
@Serializable object NavSignup
@Serializable data class NavExerciseCamera(val planJson: String = "")
@Serializable object NavWorkoutSetup
@Serializable data class NavSessionDetail(val sessionId: String)
@Serializable data class NavWorkoutSummary(val summaryJson: String)
@Serializable object NavCustomPlanBuilder
@Serializable object NavChallenges
@Serializable data class NavChallengeDetail(val challengeId: String)
@Serializable data class NavExerciseInfo(val exerciseId: String)
