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
        startDestination = ScreenA
    ) {
        composable<ScreenA> {
            Login(navController)
        }
        composable<ScreenB> {
           Homepage(navController)
        }
    }
}

@Serializable
object ScreenA

@Serializable
object ScreenB