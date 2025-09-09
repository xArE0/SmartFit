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
    NavHost(
        navController = navController,
        startDestination = NavHomepage
    ) {
        composable<NavLogin> {
            Login(navController)
        }
        composable<NavHomepage> {
           Homepage(navController)
        }
        composable<NavSignup> {
            Signup(navController)
        }
    }
}

@Serializable
object NavHomepage

@Serializable
object NavLogin

@Serializable
object NavSignup
